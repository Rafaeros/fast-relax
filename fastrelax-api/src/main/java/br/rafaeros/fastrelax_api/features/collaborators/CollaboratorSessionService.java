package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.security.AccessGuard;
import br.rafaeros.fastrelax_api.core.security.Principals;
import br.rafaeros.fastrelax_api.features.chairs.Chair;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.AvailableChairDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.AvailableDayDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.AvailableSlotsResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorSessionDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorSessionFilterDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorSessionResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.SessionSlotDTO;
import br.rafaeros.fastrelax_api.features.settings.SessionSettingsService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollaboratorSessionService {

    private static final List<SessionStatus> ACTIVE_STATUSES = List.of(SessionStatus.SCHEDULED, SessionStatus.STARTED);

    private static final java.time.format.DateTimeFormatter DATE_FORMAT = java.time.format.DateTimeFormatter
            .ofPattern("dd/MM/yyyy");
    private static final java.time.format.DateTimeFormatter TIME_FORMAT = java.time.format.DateTimeFormatter
            .ofPattern("HH:mm");

    private final CollaboratorSessionRepository sessionRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorWorkScheduleRepository workScheduleRepository;
    private final AccessGuard access;
    private final SessionSettingsService sessionSettingsService;
    private final SessionExpirationService sessionExpirationService;
    private final br.rafaeros.fastrelax_api.features.chairs.ChairCommandService chairCommandService;
    private final br.rafaeros.fastrelax_api.features.chairs.ChairService chairService;
    private final br.rafaeros.fastrelax_api.features.chairs.ChairRepository chairRepository;
    private final org.springframework.context.ApplicationEventPublisher events;
    private final br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant currentTenant;

    public Page<CollaboratorSessionResponseDTO> findAll(CollaboratorSessionFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        // Reavalia antes de responder para que o status não fique defasado entre
        // duas execuções do job.
        sessionExpirationService.expireAbandonedSessions();

        // Colaborador logado só vê as próprias linhas: o id dele sobrepõe qualquer
        // collaboratorId que venha no filtro.
        Long collaboratorId = dto != null ? dto.collaboratorId() : null;
        collaboratorId = Principals.collaborator().map(Collaborator::getId).orElse(collaboratorId);

        Specification<CollaboratorSession> spec = Specification.allOf(
                CollaboratorSessionSpecifications.hasStatus(dto != null ? dto.status() : null),
                CollaboratorSessionSpecifications.onDate(dto != null ? dto.sessionDate() : null),
                CollaboratorSessionSpecifications.betweenDates(dto != null ? dto.from() : null,
                        dto != null ? dto.to() : null),
                CollaboratorSessionSpecifications.hasCollaborator(collaboratorId));

        return sessionRepository.findAllScoped(spec, Objects.requireNonNull(pageable))
                .map(session -> new CollaboratorSessionResponseDTO(session));
    }

    public CollaboratorSessionResponseDTO findById(Long id) {
        return new CollaboratorSessionResponseDTO(findEntityById(id));
    }

    /**
     * Sessão ativa do colaborador logado — o que a tela inicial do app precisa
     * saber a cada abertura.
     *
     * @return vazio quando não há nada agendado ou em andamento
     */
    public Optional<CollaboratorSessionResponseDTO> findMyCurrentSession() {
        // Expira antes de responder: uma sessão abandonada não deve aparecer como
        // ativa.
        sessionExpirationService.expireAbandonedSessions();

        Long collaboratorId = resolveCollaboratorId(null);
        return sessionRepository.findByCollaboratorIdAndStatusIn(collaboratorId, ACTIVE_STATUSES)
                .map(session -> new CollaboratorSessionResponseDTO(session));
    }

    /**
     * Grade de horários ainda livres num período, para o app listar tudo que o
     * colaborador pode escolher sem precisar consultar dia a dia.
     *
     * <p>
     * Dias sem janela de horário permitido e dias lotados não entram no resultado,
     * então a
     * lista devolvida já é exatamente o que pode ser selecionado.
     *
     * @param collaboratorId opcional para colaborador logado — assume o próprio id
     * @param from           opcional — assume hoje
     * @param to             opcional — assume {@code from} mais a antecedência
     *                       máxima
     */
    public AvailableSlotsResponseDTO findAvailableSlots(Long collaboratorId, LocalDate from, LocalDate to) {
        // Libera os horários de sessões abandonadas antes de calcular a grade.
        sessionExpirationService.expireAbandonedSessions();

        Long targetId = resolveCollaboratorId(collaboratorId);
        if (!access.operatesCompany()
                && !access.canAccessCollaborator(targetId)) {
            throw new AccessDeniedException("Acesso negado. Você só pode consultar seus próprios horários.");
        }

        int durationMinutes = sessionSettingsService.getDefaultDurationMinutes();
        int maxAdvanceDays = sessionSettingsService.getMaxAdvanceDays();
        int stabilizationMinutes = sessionSettingsService.getStabilizationMinutes();

        LocalDate today = LocalDate.now();
        LocalDate lastBookable = today.plusDays(maxAdvanceDays);
        // Datas passadas e além da antecedência máxima são aparadas em vez de
        // rejeitadas: o filtro é do usuário, o teto é da regra de negócio.
        LocalDate start = (from == null || from.isBefore(today)) ? today : from;
        LocalDate end = (to == null || to.isAfter(lastBookable)) ? lastBookable : to;
        if (end.isBefore(start)) {
            throw new BusinessException("A data final não pode ser anterior à inicial");
        }

        List<CollaboratorSession> busy = sessionRepository
                .findByCompanyIdAndSessionDateBetweenAndStatusIn(currentTenant.companyId(), start, end,
                        ACTIVE_STATUSES);

        // Capacidade simultânea da empresa: um horário só fica indisponível quando
        // as reservas dele igualam o número de cadeiras.
        List<AvailableChairDTO> activeChairs = chairService.listActiveChairs().stream()
                .map(AvailableChairDTO::new)
                .toList();

        List<AvailableDayDTO> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            buildDay(targetId, date, durationMinutes, stabilizationMinutes, busy, activeChairs)
                    .ifPresent(day -> days.add(day));
        }

        return new AvailableSlotsResponseDTO(start, end, durationMinutes, stabilizationMinutes, maxAdvanceDays, days);
    }

    /**
     * Grade completa do dia: horários ocupados continuam na lista, marcados com
     * {@code available = false}, para a tela poder exibi-los desabilitados.
     *
     * <p>
     * Vazio apenas quando o dia não tem janela de horário permitido configurada —
     * domingo ou
     * dia que o colaborador não trabalha.
     */
    private Optional<AvailableDayDTO> buildDay(Long collaboratorId, LocalDate date, int durationMinutes,
            int stabilizationMinutes, List<CollaboratorSession> busy, List<AvailableChairDTO> activeChairs) {
        Optional<WorkDay> workDay = WorkDay.from(date);
        if (workDay.isEmpty()) {
            return Optional.empty();
        }
        Optional<CollaboratorWorkSchedule> schedule = workScheduleRepository
                .findByCollaboratorIdAndDayOfWeekAndActiveTrue(collaboratorId, workDay.get());
        if (schedule.isEmpty()) {
            return Optional.empty();
        }

        CollaboratorWorkSchedule window = schedule.get();
        List<CollaboratorSession> busyOnDate = busy.stream()
                .filter(session -> session.getSessionDate().isEqual(date))
                .toList();

        List<SessionSlotDTO> slots = new ArrayList<>();

        for (LocalTime slotStart : dayGrid(window, durationMinutes, stabilizationMinutes)) {
            // Horário que já passou some da grade em vez de aparecer desabilitado:
            // não é escolha possível nem informação útil. Ocupado é diferente —
            // continua na lista para a tela mostrar que o horário existe e está
            // tomado por outra pessoa.
            if (hasPassed(slotStart, date)) {
                continue;
            }

            LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);
            slots.add(new SessionSlotDTO(slotStart, slotEnd,
                    getAvailableChairs(slotStart, slotEnd, busyOnDate, activeChairs, stabilizationMinutes)));
        }

        return slots.isEmpty()
                ? Optional.empty()
                : Optional.of(new AvailableDayDTO(date, window.getDayOfWeek(), window.getAllowedStartTime(),
                        window.getAllowedEndTime(), slots));
    }

    /**
     * Horários de início do dia: da abertura da janela do colaborador em
     * diante, de {@code duração + estabilização} em
     * {@code duração + estabilização}.
     *
     * <p>
     * A folga entra no passo porque não é um detalhe de conflito, é parte do
     * ciclo da cadeira. Com 5 min de duração e 1 min de folga, a sessão das
     * 12:05 termina 12:10 e o próximo início possível é <b>12:11</b> — 12:10
     * não existe como horário. A grade anterior andava só pela duração e
     * escondia depois o que estivesse ocupado, o que produzia a lista torta que
     * a tela mostrava: 12:10 oferecido para uma cadeira e 12:11 para outra, no
     * mesmo dia.
     *
     * <p>
     * A grade é por colaborador porque a janela permitida é dele. Ancorá-la na
     * abertura da própria janela é o que garante que duas pessoas com janelas
     * diferentes recebam cada uma horários que cabem inteiros no seu período.
     */
    private List<LocalTime> dayGrid(CollaboratorWorkSchedule window, int durationMinutes, int stabilizationMinutes) {
        List<LocalTime> starts = new ArrayList<>();
        int step = durationMinutes + stabilizationMinutes;

        LocalTime tick = window.getAllowedStartTime();
        while (true) {
            LocalTime tickEnd = tick.plusMinutes(durationMinutes);
            // tickEnd para trás é virada de meia-noite; depois do fim permitido
            // é sessão que não cabe na janela. Nos dois casos a grade acabou.
            if (!tickEnd.isAfter(tick) || tickEnd.isAfter(window.getAllowedEndTime())) {
                break;
            }
            starts.add(tick);

            LocalTime next = tick.plusMinutes(step);
            if (!next.isAfter(tick)) {
                break; // virou a meia-noite
            }
            tick = next;
        }

        return starts;
    }

    @Transactional
    public CollaboratorSessionResponseDTO create(CollaboratorSessionDTO dto) {
        // Escopado: agendar para um colaborador de outra empresa responde 404.
        Collaborator collaborator = collaboratorRepository
                .findByIdScoped(Objects.requireNonNull(dto.collaboratorId()))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));

        Chair chair = chairRepository.findByIdScoped(dto.chairId())
                .orElseThrow(() -> new BusinessException("Cadeira não encontrada ou não pertence à sua empresa"));

        // Sem isto, uma sessão abandonada continuaria bloqueando o horário e o
        // próprio colaborador pelo índice de sessão ativa única.
        sessionExpirationService.expireAbandonedSessions();

        LocalTime endTime = resolveEndTime(dto.startTime());
        validateWindow(dto.sessionDate(), dto.startTime(), endTime);
        validateWithinAllowedWindow(dto.collaboratorId(), dto.sessionDate(), dto.startTime(), endTime);
        requireNoActiveSession(dto.collaboratorId());
        requireSlotFree(dto.sessionDate(), dto.startTime(), endTime, null);
        requireChairStabilized(dto.chairId(), dto.sessionDate(), dto.startTime(), endTime, null);

        CollaboratorSession session = new CollaboratorSession();
        // A empresa vem do colaborador, não do contexto: assim a sessão nunca pode
        // acabar em um tenant diferente do dono dela, nem que o contexto esteja errado.
        session.setCompany(collaborator.getCompany());
        session.setCollaborator(collaborator);
        session.setSessionDate(dto.sessionDate());
        session.setStartTime(dto.startTime());
        session.setEndTime(endTime);
        session.setChair(chair);
        // O status nunca vem do cliente: toda sessão nasce agendada.
        session.setStatus(SessionStatus.SCHEDULED);

        CollaboratorSession saved = sessionRepository.save(session);
        events.publishEvent(SessionLifecycleEvent.of(SessionLifecycleEvent.Type.SCHEDULED, saved));
        return new CollaboratorSessionResponseDTO(saved);
    }

    /**
     * Reagenda uma sessão que ainda não começou. Não muda de estado nem de dono.
     */
    @Transactional
    public CollaboratorSessionResponseDTO update(Long id, CollaboratorSessionDTO dto) {
        CollaboratorSession session = findEntityById(Objects.requireNonNull(id));
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException("Só é possível reagendar massagens que ainda não começaram.");
        }

        // A duração é reaplicada no reagendamento, então mudanças de configuração
        // valem para a nova data.
        LocalTime endTime = resolveEndTime(dto.startTime());
        validateWindow(dto.sessionDate(), dto.startTime(), endTime);
        validateWithinAllowedWindow(session.getCollaborator().getId(), dto.sessionDate(), dto.startTime(), endTime);
        requireSlotFree(dto.sessionDate(), dto.startTime(), endTime, session.getId());
        requireChairStabilized(session.getChair().getId(), dto.sessionDate(), dto.startTime(), endTime,
                session.getId());

        session.setSessionDate(dto.sessionDate());
        session.setStartTime(dto.startTime());
        session.setEndTime(endTime);

        return new CollaboratorSessionResponseDTO(sessionRepository.save(session));
    }

    @Transactional
    public CollaboratorSessionResponseDTO start(Long id) {
        // Expira antes de avaliar, para que uma sessão vencida responda "EXPIRED"
        // em vez de reclamar da janela de horário.
        sessionExpirationService.expireAbandonedSessions();

        CollaboratorSession session = findEntityById(id);
        requireStatus(session, SessionStatus.SCHEDULED, "iniciada");
        validateStartWindow(session);

        return beginSession(session);
    }

    /**
     * Liga a cadeira e marca a sessão como em andamento.
     *
     * <p>
     * As duas rotas de início — por id e "a minha de agora" — terminam aqui. Elas
     * diferem só em como descobrem a sessão; o que acontece depois tem que ser
     * idêntico, e manter isso em um lugar só é o que impede uma delas de voltar a
     * marcar a sessão sem acionar o relé.
     *
     * <p>
     * A cadeira é acionada antes de gravar: se o relé não ligar, a sessão continua
     * agendada e o colaborador pode tentar de novo dentro da tolerância — melhor
     * que ficar com o app dizendo "em andamento" na frente de um equipamento
     * parado.
     */
    private CollaboratorSessionResponseDTO beginSession(CollaboratorSession session) {
        // Contado a partir de agora até o fim agendado, não a duração nominal:
        // quem começa atrasado (dentro da tolerância) faz uma massagem mais curta
        // em vez de empurrar o desligamento — e com ele a folga de estabilização
        // da próxima sessão — para depois do horário previsto.
        LocalDateTime scheduledEnd = LocalDateTime.of(session.getSessionDate(), session.getEndTime());
        int durationSeconds = (int) Math.max(1,
                java.time.Duration.between(LocalDateTime.now(), scheduledEnd).getSeconds());
        session.setChair(chairCommandService.startFor(session.getId(), durationSeconds));

        session.setStatus(SessionStatus.STARTED);
        session.setStartedAt(LocalDateTime.now());

        CollaboratorSession saved = sessionRepository.save(session);
        events.publishEvent(SessionLifecycleEvent.of(SessionLifecycleEvent.Type.STARTED, saved));
        return new CollaboratorSessionResponseDTO(saved);
    }

    /**
     * Inicia a sessão vigente do colaborador logado, sem receber id.
     *
     * <p>
     * O app não precisa guardar id nem perguntar antes qual sessão é a de agora:
     * o índice único {@code uq_collaborator_active_session} garante no máximo uma
     * ativa por colaborador, então "a sessão dele" é sempre inequívoca. A janela
     * de início continua sendo validada — o que muda é só quem descobre o id.
     *
     * <p>
     * Chamar de novo com a sessão já em andamento devolve a mesma sessão em vez
     * de erro: o toque duplicado na cadeira não pode virar falha.
     */
    @Transactional
    public CollaboratorSessionResponseDTO startCurrent() {
        // Expira antes de avaliar, para uma sessão vencida responder "EXPIRED"
        // em vez de reclamar da janela de horário.
        sessionExpirationService.expireAbandonedSessions();

        Long collaboratorId = requireLoggedCollaboratorId();
        CollaboratorSession session = sessionRepository
                .findByCollaboratorIdAndStatusIn(collaboratorId, ACTIVE_STATUSES)
                .orElseThrow(() -> new BusinessException(
                        "Você não tem sessão agendada. Agende um horário para iniciar."));

        if (session.getStatus() == SessionStatus.STARTED) {
            return new CollaboratorSessionResponseDTO(session);
        }

        validateStartWindow(session);

        return beginSession(session);
    }

    /** Só faz sentido para colaborador: ADMIN e RH não têm sessão própria. */
    private Long requireLoggedCollaboratorId() {
        return Principals.requireCollaborator().getId();
    }

    @Transactional
    public CollaboratorSessionResponseDTO finish(Long id) {
        CollaboratorSession session = findEntityById(id);
        requireStatus(session, SessionStatus.STARTED, "finalizada");

        chairCommandService.stopFor(session.getChair(), session.getId());

        session.setStatus(SessionStatus.DONE);
        session.setFinishedAt(LocalDateTime.now());

        CollaboratorSession saved = sessionRepository.save(session);
        events.publishEvent(SessionLifecycleEvent.of(SessionLifecycleEvent.Type.FINISHED, saved));
        return new CollaboratorSessionResponseDTO(saved);
    }

    @Transactional
    public CollaboratorSessionResponseDTO cancel(Long id) {
        CollaboratorSession session = findEntityById(id);
        if (!session.getStatus().isActive()) {
            throw new BusinessException(
                    "Sessão com status " + session.getStatus() + " não pode ser cancelada");
        }

        // Só há o que desligar se a sessão chegou a ligar a cadeira.
        if (session.getStatus() == SessionStatus.STARTED) {
            chairCommandService.stopFor(session.getChair(), session.getId());
        }

        session.setStatus(SessionStatus.CANCELLED);

        CollaboratorSession saved = sessionRepository.save(session);
        events.publishEvent(SessionLifecycleEvent.of(SessionLifecycleEvent.Type.CANCELLED, saved));
        return new CollaboratorSessionResponseDTO(saved);
    }

    /**
     * Encerra a sessão em andamento numa cadeira, se houver — chamado quando a
     * Physical desativa a cadeira.
     *
     * <p>
     * Não passa por {@link #findEntityById}: aquele método valida que quem
     * pediu tem acesso à empresa da sessão, o que faz sentido para uma
     * requisição de RH/colaborador, mas não para esta reação a um evento de
     * sistema — a Physical desativando uma cadeira não está autenticada como
     * a empresa dona dela, e não precisa estar.
     */
    @Transactional
    public void forceStopByChair(Long chairId) {
        sessionRepository.findByChairIdAndStatus(chairId, SessionStatus.STARTED)
                .ifPresent(session -> {
                    chairCommandService.stopFor(session.getChair(), session.getId());
                    session.setStatus(SessionStatus.CANCELLED);
                    CollaboratorSession saved = sessionRepository.save(session);
                    events.publishEvent(SessionLifecycleEvent.of(SessionLifecycleEvent.Type.CANCELLED, saved));
                });
    }

    private CollaboratorSession findEntityById(Long id) {
        CollaboratorSession session = sessionRepository.findByIdScoped(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada"));
        if (!access.operatesCompany()
                && !access.canAccessCollaborator(session.getCollaborator().getId())) {
            throw new AccessDeniedException("Acesso negado. Você só pode acessar suas próprias sessões.");
        }
        return session;
    }

    /**
     * O horário só está cheio quando as sessões simultâneas igualam o número de
     * cadeiras da empresa.
     *
     * <p>
     * Antes qualquer sobreposição era conflito, porque havia um recurso só. Com
     * várias cadeiras isso passou a recusar agendamento por nada: duas pessoas
     * podem descansar às 12:00 se existirem duas cadeiras.
     *
     * <p>
     * A checagem aqui é para o cliente receber 400 com explicação; quem garante de
     * fato é o {@code EXCLUDE uq_session_no_overlap}, que impede duas sessões
     * ativas na <em>mesma</em> cadeira — a checagem da aplicação roda antes do
     * commit e, em dois agendamentos simultâneos, os dois passariam.
     */
    private void requireSlotFree(LocalDate sessionDate, LocalTime startTime, LocalTime endTime, Long excludeId) {
        int chairs = chairService.countActiveChairs();
        if (chairs == 0) {
            throw new BusinessException("Nenhuma cadeira cadastrada para esta empresa. Procure o RH.");
        }

        long busy = sessionRepository.countOverlapping(currentTenant.companyId(), sessionDate, ACTIVE_STATUSES,
                excludeId != null ? excludeId : -1L, startTime, endTime);

        if (busy >= chairs) {
            throw new BusinessException("Todas as cadeiras já estão reservadas entre " + startTime + " e "
                    + endTime + " em " + sessionDate + ". Escolha outro horário.");
        }
    }

    /**
     * A folga de estabilização é por cadeira: o relé precisa desarmar e a
     * poltrona assentar antes do próximo ciclo, então duas sessões que só
     * "encostam" no papel (uma termina exatamente quando a outra começa nessa
     * mesma cadeira) não são seguras mesmo passando por
     * {@link #requireSlotFree}, que só olha capacidade agregada.
     *
     * <p>
     * A folga é aplicada nos dois lados da sessão candidata — cobre tanto
     * "essa cadeira acabou de ser usada" quanto "essa cadeira já tem outra
     * sessão marcada logo depois" — e por isso basta alargar a janela
     * candidata em vez de cada sessão existente.
     */
    private void requireChairStabilized(Long chairId, LocalDate sessionDate, LocalTime startTime, LocalTime endTime,
            Long excludeId) {
        int stabilizationMinutes = sessionSettingsService.getStabilizationMinutes();
        if (stabilizationMinutes <= 0) {
            return;
        }

        LocalTime paddedStart = padBefore(startTime, stabilizationMinutes);
        LocalTime paddedEnd = padAfter(endTime, stabilizationMinutes);

        long conflicting = sessionRepository.countOverlappingChair(chairId, sessionDate, ACTIVE_STATUSES,
                excludeId != null ? excludeId : -1L, paddedStart, paddedEnd);

        if (conflicting > 0) {
            throw new BusinessException("Esta cadeira precisa de " + stabilizationMinutes
                    + " min entre uma sessão e outra para estabilizar. Escolha outro horário ou outra cadeira.");
        }
    }

    /**
     * {@code minusMinutes} sem voltar ao dia anterior — perto da meia-noite, a
     * folga de estabilização é ignorada daquele lado em vez de comparar contra
     * o dia errado.
     */
    private static LocalTime padBefore(LocalTime time, int minutes) {
        LocalTime padded = time.minusMinutes(minutes);
        return padded.isAfter(time) ? LocalTime.MIN : padded;
    }

    /** Mesma ideia de {@link #padBefore}, para o lado de depois da meia-noite. */
    private static LocalTime padAfter(LocalTime time, int minutes) {
        LocalTime padded = time.plusMinutes(minutes);
        return padded.isBefore(time) ? LocalTime.MAX : padded;
    }

    private void requireNoActiveSession(Long collaboratorId) {
        sessionRepository.findByCollaboratorIdAndStatusIn(collaboratorId, ACTIVE_STATUSES)
                .ifPresent(active -> {
                    // Label e data em pt-BR: a mensagem é exibida direto ao
                    // colaborador, e o nome do enum não diz nada para ele.
                    throw new BusinessException("Você já tem uma massagem "
                            + active.getStatus().getLabel().toLowerCase() + " em "
                            + active.getSessionDate().format(DATE_FORMAT) + " às "
                            + active.getStartTime().format(TIME_FORMAT)
                            + ". Cancele antes de marcar outra.");
                });
    }

    /**
     * A sessão só pode ser iniciada no dia e dentro da janela em que foi agendada:
     * de {@code startTime} até {@code startTime + tolerância}. Sem isto, dava para
     * iniciar dias antes e ocupar o recurso fora do horário reservado.
     *
     * <p>
     * A janela não abre antes do horário agendado. Adiantar o início só teria
     * duas saídas, ambas erradas: encerrar no fim agendado (massagem menor que a
     * duração contratada) ou rodar a duração cheia a partir do início real,
     * comendo a folga de estabilização e o começo de quem vem depois na mesma
     * cadeira. Atraso, ao contrário, é tolerado — o custo fica com quem se
     * atrasou, em {@link #beginSession}.
     */
    private void validateStartWindow(CollaboratorSession session) {
        LocalDate today = LocalDate.now();
        if (!session.getSessionDate().isEqual(today)) {
            throw new BusinessException("A sessão está agendada para "
                    + session.getSessionDate().format(DATE_FORMAT) + " às "
                    + session.getStartTime().format(TIME_FORMAT) + ". Só é possível iniciá-la nesse dia.");
        }

        LocalTime now = LocalTime.now();

        if (now.isBefore(session.getStartTime())) {
            throw new BusinessException("A sessão só pode ser iniciada a partir das "
                    + session.getStartTime().format(TIME_FORMAT) + ".");
        }

        LocalTime deadline = session.getStartTime()
                .plusMinutes(sessionSettingsService.getStartGraceMinutes());
        // deadline anterior ao início significa virada de dia: nesse caso não venceu.
        if (deadline.isAfter(session.getStartTime()) && now.isAfter(deadline)) {
            throw new BusinessException("O prazo para iniciar esta sessão terminou às "
                    + deadline.format(TIME_FORMAT) + ".");
        }
    }

    private void requireStatus(CollaboratorSession session, SessionStatus expected, String action) {
        if (session.getStatus() != expected) {
            throw new BusinessException("Esta massagem está " + session.getStatus().getLabel().toLowerCase()
                    + " e não pode ser " + action + ".");
        }
    }

    /**
     * Duração vem da configuração global, nunca do cliente. O wrap de
     * {@link LocalTime#plusMinutes} é tratado por quem valida a janela.
     */
    private LocalTime resolveEndTime(LocalTime startTime) {
        return startTime.plusMinutes(sessionSettingsService.getDefaultDurationMinutes());
    }

    /**
     * Colaborador logado não precisa informar o próprio id; ADMIN/RH precisa, já
     * que não há um "próprio" a assumir.
     */
    private Long resolveCollaboratorId(Long collaboratorId) {
        if (collaboratorId != null) {
            return collaboratorId;
        }
        Optional<Collaborator> logged = Principals.collaborator();
        if (logged.isPresent()) {
            return logged.get().getId();
        }
        throw new BusinessException("Informe o parâmetro collaboratorId");
    }

    /**
     * Horário do passado, contado só no dia de hoje — datas futuras nunca passaram.
     */
    private boolean hasPassed(LocalTime slotStart, LocalDate sessionDate) {
        return sessionDate.isEqual(LocalDate.now()) && !slotStart.isAfter(LocalTime.now());
    }

    /**
     * Livre enquanto sobrar cadeira no horário.
     *
     * <p>
     * Deixou de ser "ninguém marcou" e passou a ser contagem quando uma empresa
     * pôde ter várias cadeiras — com duas, o segundo agendamento das 12:00 é
     * legítimo, e recusá-lo desperdiçaria metade do parque.
     *
     * <p>
     * O slot é alargado pela folga de estabilização antes de comparar: sem
     * isso, a grade mostraria uma cadeira como livre no minuto exato em que a
     * sessão anterior termina nela, quando na prática ainda falta o tempo de
     * desarmar o relé.
     */
    private List<AvailableChairDTO> getAvailableChairs(LocalTime start, LocalTime end, List<CollaboratorSession> busy,
            List<AvailableChairDTO> activeChairs, int stabilizationMinutes) {
        LocalTime paddedStart = padBefore(start, stabilizationMinutes);
        LocalTime paddedEnd = padAfter(end, stabilizationMinutes);

        List<AvailableChairDTO> availableChairs = new ArrayList<>(activeChairs);
        for (CollaboratorSession session : busy) {
            // Se a sessão (com a folga) conflita com o slot
            if (session.getStartTime().isBefore(paddedEnd) && session.getEndTime().isAfter(paddedStart)) {
                if (session.getChair() != null) {
                    availableChairs.removeIf(c -> c.id().equals(session.getChair().getId()));
                }
            }
        }
        return availableChairs;
    }

    private CollaboratorWorkSchedule requireAllowedWindow(Long collaboratorId, LocalDate sessionDate) {
        WorkDay day = WorkDay.from(sessionDate)
                .orElseThrow(() -> new BusinessException(
                        "Não há expediente aos domingos; agende de segunda a sábado"));

        return workScheduleRepository.findByCollaboratorIdAndDayOfWeekAndActiveTrue(collaboratorId, day)
                .orElseThrow(() -> new BusinessException(
                        "Colaborador não possui horário permitido configurado para " + day));
    }

    /**
     * A sessão precisa caber inteira na janela de horário permitido configurada
     * para aquele dia da semana — é a razão de {@code CollaboratorWorkSchedule}
     * existir — e começar num tique da grade daquela janela.
     *
     * <p>
     * A checagem da grade não é redundante com a da janela: sem ela, um POST
     * com um horário que a tela nunca ofereceu (12:10, no exemplo de
     * {@link #dayGrid}) passaria por tudo e voltaria a encostar duas sessões na
     * mesma cadeira. A grade só vale como regra se o servidor a impuser.
     */
    private void validateWithinAllowedWindow(Long collaboratorId, LocalDate sessionDate, LocalTime startTime,
            LocalTime endTime) {
        CollaboratorWorkSchedule schedule = requireAllowedWindow(collaboratorId, sessionDate);

        if (startTime.isBefore(schedule.getAllowedStartTime()) || endTime.isAfter(schedule.getAllowedEndTime())) {
            throw new BusinessException("A sessão deve ficar dentro do horário permitido ("
                    + schedule.getAllowedStartTime() + " às " + schedule.getAllowedEndTime()
                    + "); o horário solicitado vai de " + startTime + " a " + endTime);
        }

        int durationMinutes = sessionSettingsService.getDefaultDurationMinutes();
        int stabilizationMinutes = sessionSettingsService.getStabilizationMinutes();
        if (!dayGrid(schedule, durationMinutes, stabilizationMinutes).contains(startTime)) {
            throw new BusinessException("O horário " + startTime.format(TIME_FORMAT)
                    + " não é um horário oferecido. Escolha um da lista: ela já respeita os "
                    + durationMinutes + " min de sessão mais " + stabilizationMinutes
                    + " min de estabilização da cadeira.");
        }
    }

    private void validateWindow(LocalDate sessionDate, LocalTime startTime, LocalTime endTime) {
        // Só acontece se startTime + duração cruzar a meia-noite.
        if (!endTime.isAfter(startTime)) {
            throw new BusinessException("O horário de início não permite encaixar a duração da sessão no mesmo dia");
        }
        if (sessionDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Não é possível agendar sessão em data passada");
        }
        if (sessionDate.isEqual(LocalDate.now()) && endTime.isBefore(LocalTime.now())) {
            throw new BusinessException("O horário informado já passou");
        }
        int maxAdvanceDays = sessionSettingsService.getMaxAdvanceDays();
        if (sessionDate.isAfter(LocalDate.now().plusDays(maxAdvanceDays))) {
            throw new BusinessException("Só é possível agendar com até " + maxAdvanceDays + " dias de antecedência");
        }
    }
}

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
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

    private static final List<SessionStatus> ACTIVE_STATUSES =
            List.of(SessionStatus.SCHEDULED, SessionStatus.STARTED);

    private static final java.time.format.DateTimeFormatter DATE_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final java.time.format.DateTimeFormatter TIME_FORMAT =
            java.time.format.DateTimeFormatter.ofPattern("HH:mm");

    private final CollaboratorSessionRepository sessionRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorWorkScheduleRepository workScheduleRepository;
    private final CollaboratorSecurity collaboratorSecurity;
    private final SessionSettingsService sessionSettingsService;
    private final SessionExpirationService sessionExpirationService;
    private final br.rafaeros.fastrelax_api.features.chairs.ChairCommandService chairCommandService;

    public Page<CollaboratorSessionResponseDTO> findAll(CollaboratorSessionFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        // Reavalia antes de responder para que o status não fique defasado entre
        // duas execuções do job.
        sessionExpirationService.expireAbandonedSessions();

        // A logged collaborator may only see their own sessions, so their id
        // overrides any collaboratorId supplied in the filter.
        Long collaboratorId = dto != null ? dto.collaboratorId() : null;
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Collaborator loggedCollab) {
            collaboratorId = loggedCollab.getId();
        }

        Specification<CollaboratorSession> spec = Specification.allOf(
                CollaboratorSessionSpecifications.hasStatus(dto != null ? dto.status() : null),
                CollaboratorSessionSpecifications.onDate(dto != null ? dto.sessionDate() : null),
                CollaboratorSessionSpecifications.betweenDates(dto != null ? dto.from() : null,
                        dto != null ? dto.to() : null),
                CollaboratorSessionSpecifications.hasCollaborator(collaboratorId));

        return sessionRepository.findAll(spec, Objects.requireNonNull(pageable))
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
        // Expira antes de responder: uma sessão abandonada não deve aparecer como ativa.
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
     * Dias sem janela de horário permitido e dias lotados não entram no resultado, então a
     * lista devolvida já é exatamente o que pode ser selecionado.
     *
     * @param collaboratorId opcional para colaborador logado — assume o próprio id
     * @param from           opcional — assume hoje
     * @param to             opcional — assume {@code from} mais a antecedência máxima
     */
    public AvailableSlotsResponseDTO findAvailableSlots(Long collaboratorId, LocalDate from, LocalDate to) {
        // Libera os horários de sessões abandonadas antes de calcular a grade.
        sessionExpirationService.expireAbandonedSessions();

        Long targetId = resolveCollaboratorId(collaboratorId);
        if (!collaboratorSecurity.hasAdminOrRhAccess()
                && !collaboratorSecurity.canAccessCollaborator(targetId)) {
            throw new AccessDeniedException("Acesso negado. Você só pode consultar seus próprios horários.");
        }

        int durationMinutes = sessionSettingsService.getDefaultDurationMinutes();
        int maxAdvanceDays = sessionSettingsService.getMaxAdvanceDays();

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
                .findBySessionDateBetweenAndStatusIn(start, end, ACTIVE_STATUSES);

        List<AvailableDayDTO> days = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            buildDay(targetId, date, durationMinutes, busy).ifPresent(day -> days.add(day));
        }

        return new AvailableSlotsResponseDTO(start, end, durationMinutes, maxAdvanceDays, days);
    }

    /**
     * Grade completa do dia: horários ocupados continuam na lista, marcados com
     * {@code available = false}, para a tela poder exibi-los desabilitados.
     *
     * <p>
     * Vazio apenas quando o dia não tem janela de horário permitido configurada — domingo ou
     * dia que o colaborador não trabalha.
     */
    private Optional<AvailableDayDTO> buildDay(Long collaboratorId, LocalDate date, int durationMinutes,
            List<CollaboratorSession> busy) {
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
        LocalTime slotStart = window.getAllowedStartTime();
        LocalTime slotEnd = slotStart.plusMinutes(durationMinutes);

        // Para enquanto o slot inteiro couber na janela permitida; o teste de isAfter também
        // encerra o laço se a soma cruzar a meia-noite.
        while (!slotEnd.isAfter(window.getAllowedEndTime()) && slotEnd.isAfter(slotStart)) {
            // Horário que já passou some da grade em vez de aparecer desabilitado:
            // não é escolha possível nem informação útil. Ocupado é diferente —
            // continua na lista para a tela mostrar que o horário existe e está
            // tomado por outra pessoa.
            if (!hasPassed(slotStart, date)) {
                slots.add(new SessionSlotDTO(slotStart, slotEnd,
                        isFree(slotStart, slotEnd, busyOnDate)));
            }
            slotStart = slotEnd;
            slotEnd = slotStart.plusMinutes(durationMinutes);
        }

        return slots.isEmpty()
                ? Optional.empty()
                : Optional.of(new AvailableDayDTO(date, window.getDayOfWeek(), window.getAllowedStartTime(),
                        window.getAllowedEndTime(), slots));
    }

    @Transactional
    public CollaboratorSessionResponseDTO create(CollaboratorSessionDTO dto) {
        Collaborator collaborator = collaboratorRepository.findById(Objects.requireNonNull(dto.collaboratorId()))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));

        // Sem isto, uma sessão abandonada continuaria bloqueando o horário e o
        // próprio colaborador pelo índice de sessão ativa única.
        sessionExpirationService.expireAbandonedSessions();

        LocalTime endTime = resolveEndTime(dto.startTime());
        validateWindow(dto.sessionDate(), dto.startTime(), endTime);
        validateWithinAllowedWindow(dto.collaboratorId(), dto.sessionDate(), dto.startTime(), endTime);
        requireNoActiveSession(dto.collaboratorId());
        requireSlotFree(dto.sessionDate(), dto.startTime(), endTime, null);

        CollaboratorSession session = new CollaboratorSession();
        session.setCollaborator(collaborator);
        session.setSessionDate(dto.sessionDate());
        session.setStartTime(dto.startTime());
        session.setEndTime(endTime);
        // O status nunca vem do cliente: toda sessão nasce agendada.
        session.setStatus(SessionStatus.SCHEDULED);

        return new CollaboratorSessionResponseDTO(sessionRepository.save(session));
    }

    /** Reagenda uma sessão que ainda não começou. Não muda de estado nem de dono. */
    @Transactional
    public CollaboratorSessionResponseDTO update(Long id, CollaboratorSessionDTO dto) {
        CollaboratorSession session = findEntityById(Objects.requireNonNull(id));
        if (session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException("Só é possível reagendar sessões com status SCHEDULED");
        }

        // A duração é reaplicada no reagendamento, então mudanças de configuração
        // valem para a nova data.
        LocalTime endTime = resolveEndTime(dto.startTime());
        validateWindow(dto.sessionDate(), dto.startTime(), endTime);
        validateWithinAllowedWindow(session.getCollaborator().getId(), dto.sessionDate(), dto.startTime(), endTime);
        requireSlotFree(dto.sessionDate(), dto.startTime(), endTime, session.getId());

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

        // Aciona a cadeira antes de gravar: se o relé não ligar, a sessão continua
        // agendada e o colaborador pode tentar de novo dentro da tolerância.
        int durationSeconds = (int) java.time.Duration
                .between(session.getStartTime(), session.getEndTime()).getSeconds();
        session.setChair(chairCommandService.startFor(session.getId(), durationSeconds));

        session.setStatus(SessionStatus.STARTED);
        session.setStartedAt(LocalDateTime.now());
        return new CollaboratorSessionResponseDTO(sessionRepository.save(session));
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

        session.setStatus(SessionStatus.STARTED);
        session.setStartedAt(LocalDateTime.now());
        return new CollaboratorSessionResponseDTO(sessionRepository.save(session));
    }

    /** Só faz sentido para colaborador: ADMIN e RH não têm sessão própria. */
    private Long requireLoggedCollaboratorId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Collaborator logged)) {
            throw new AccessDeniedException("Rota disponível apenas para colaboradores autenticados");
        }
        return logged.getId();
    }

    @Transactional
    public CollaboratorSessionResponseDTO finish(Long id) {
        CollaboratorSession session = findEntityById(id);
        requireStatus(session, SessionStatus.STARTED, "finalizada");

        chairCommandService.stopFor(session.getChair(), session.getId());

        session.setStatus(SessionStatus.DONE);
        session.setFinishedAt(LocalDateTime.now());
        return new CollaboratorSessionResponseDTO(sessionRepository.save(session));
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
        return new CollaboratorSessionResponseDTO(sessionRepository.save(session));
    }

    private CollaboratorSession findEntityById(Long id) {
        CollaboratorSession session = sessionRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Sessão não encontrada"));
        if (!collaboratorSecurity.hasAdminOrRhAccess()
                && !collaboratorSecurity.canAccessCollaborator(session.getCollaborator().getId())) {
            throw new AccessDeniedException("Acesso negado. Você só pode acessar suas próprias sessões.");
        }
        return session;
    }

    /**
     * Checa antes de inserir o que o índice {@code uq_collaborator_active_session}
     * garante no banco — assim o cliente recebe 400 com explicação em vez de 500
     * por violação de constraint.
     */
    /** Recurso único de atendimento: duas sessões ativas não podem se sobrepor. */
    private void requireSlotFree(LocalDate sessionDate, LocalTime startTime, LocalTime endTime, Long excludeId) {
        if (sessionRepository.existsOverlapping(sessionDate, ACTIVE_STATUSES,
                excludeId != null ? excludeId : -1L, startTime, endTime)) {
            throw new BusinessException("Já existe uma sessão agendada entre " + startTime + " e " + endTime
                    + " em " + sessionDate + ". Escolha outro horário.");
        }
    }

    private void requireNoActiveSession(Long collaboratorId) {
        sessionRepository.findByCollaboratorIdAndStatusIn(collaboratorId, ACTIVE_STATUSES)
                .ifPresent(active -> {
                    throw new BusinessException("Colaborador já possui uma sessão " + active.getStatus()
                            + " em " + active.getSessionDate() + " às " + active.getStartTime());
                });
    }

    /**
     * A sessão só pode ser iniciada no dia e dentro da janela em que foi agendada:
     * de {@code startTime} até {@code startTime + tolerância}. Sem isto, dava para
     * iniciar dias antes e ocupar o recurso fora do horário reservado.
     */
    private void validateStartWindow(CollaboratorSession session) {
        LocalDate today = LocalDate.now();
        if (!session.getSessionDate().isEqual(today)) {
            throw new BusinessException("A sessão está agendada para "
                    + session.getSessionDate().format(DATE_FORMAT) + " às "
                    + session.getStartTime().format(TIME_FORMAT) + ". Só é possível iniciá-la nesse dia.");
        }

        LocalTime now = LocalTime.now();

        // Quem chega adiantado pode começar antes: sem esta folga, a pessoa ficaria
        // parada em frente à cadeira esperando o relógio virar.
        LocalTime opensAt = session.getStartTime()
                .minusMinutes(sessionSettingsService.getEarlyStartMinutes());
        // opensAt depois do início significa virada de dia; nesse caso a janela
        // abre à meia-noite e não há como estar adiantado.
        boolean crossesMidnight = opensAt.isAfter(session.getStartTime());

        if (!crossesMidnight && now.isBefore(opensAt)) {
            throw new BusinessException("A sessão só pode ser iniciada a partir das "
                    + opensAt.format(TIME_FORMAT) + ".");
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
            throw new BusinessException("Sessão com status " + session.getStatus()
                    + " não pode ser " + action + "; era esperado " + expected);
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
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Collaborator logged) {
            return logged.getId();
        }
        throw new BusinessException("Informe o parâmetro collaboratorId");
    }

    /** Horário do passado, contado só no dia de hoje — datas futuras nunca passaram. */
    private boolean hasPassed(LocalTime slotStart, LocalDate sessionDate) {
        return sessionDate.isEqual(LocalDate.now()) && !slotStart.isAfter(LocalTime.now());
    }

    /** Livre quando nenhuma sessão ativa se sobrepõe ao intervalo. */
    private boolean isFree(LocalTime slotStart, LocalTime slotEnd, List<CollaboratorSession> busy) {
        return busy.stream().noneMatch(
                session -> session.getStartTime().isBefore(slotEnd) && session.getEndTime().isAfter(slotStart));
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
     * A sessão precisa caber inteira na janela de horário permitido configurada para aquele
     * dia da semana — é a razão de {@code CollaboratorWorkSchedule} existir.
     */
    private void validateWithinAllowedWindow(Long collaboratorId, LocalDate sessionDate, LocalTime startTime,
            LocalTime endTime) {
        CollaboratorWorkSchedule schedule = requireAllowedWindow(collaboratorId, sessionDate);

        if (startTime.isBefore(schedule.getAllowedStartTime()) || endTime.isAfter(schedule.getAllowedEndTime())) {
            throw new BusinessException("A sessão deve ficar dentro do horário permitido ("
                    + schedule.getAllowedStartTime() + " às " + schedule.getAllowedEndTime()
                    + "); o horário solicitado vai de " + startTime + " a " + endTime);
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

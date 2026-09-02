package br.rafaeros.fastrelax_api.features.imports;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.core.dto.CredentialDeliveryDTO;
import br.rafaeros.fastrelax_api.core.security.CredentialProvisioning;
import br.rafaeros.fastrelax_api.core.security.CredentialProvisioningService;
import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.util.CpfUtils;
import br.rafaeros.fastrelax_api.core.util.PhoneUtils;
import br.rafaeros.fastrelax_api.features.collaborators.Collaborator;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorRepository;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorWorkSchedule;
import br.rafaeros.fastrelax_api.features.collaborators.CollaboratorWorkScheduleRepository;
import br.rafaeros.fastrelax_api.features.collaborators.WorkDay;
import br.rafaeros.fastrelax_api.features.departments.Department;
import br.rafaeros.fastrelax_api.features.departments.DepartmentRepository;
import br.rafaeros.fastrelax_api.features.imports.dtos.ImportResultDTO;
import br.rafaeros.fastrelax_api.features.imports.dtos.ImportedCredentialDTO;
import br.rafaeros.fastrelax_api.features.imports.dtos.ImportRowErrorDTO;
import lombok.RequiredArgsConstructor;

/**
 * Importação em massa de colaboradores a partir de planilha.
 *
 * <p>
 * Cada linha percorre a cadeia departamento → colaborador → horário permitido,
 * sempre em modo upsert: o que já existe é reaproveitado ou atualizado, nunca
 * duplicado. Isso torna a importação repetível — reenviar o mesmo arquivo não
 * cria registros novos.
 */
@Service
@RequiredArgsConstructor
public class CollaboratorImportService {

    static final int COL_NAME = 0;
    static final int COL_CPF = 1;
    static final int COL_PHONE = 2;
    static final int COL_DEPARTMENT = 3;
    static final int COL_ALLOWED_START = 4;
    static final int COL_ALLOWED_END = 5;
    /**
     * Última coluna, acrescentada depois das demais de propósito: planilha antiga
     * continua sendo aceita, só entra sem e-mail — e a pessoa recebe senha
     * temporária em vez de convite.
     */
    static final int COL_EMAIL = 6;
    static final int LAST_COLUMN = COL_EMAIL;

    /** Dias aplicados a todos os importados: o arquivo traz só os horários. */
    private static final Set<WorkDay> DEFAULT_WORK_DAYS =
            EnumSet.of(WorkDay.MONDAY, WorkDay.TUESDAY, WorkDay.WEDNESDAY, WorkDay.THURSDAY, WorkDay.FRIDAY);

    private final DepartmentRepository departmentRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorWorkScheduleRepository scheduleRepository;
    private final CryptoService cryptoService;
    private final CredentialProvisioningService provisioningService;
    private final CurrentTenant currentTenant;

    @Transactional
    public ImportResultDTO importFrom(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Envie um arquivo .xlsx com os colaboradores");
        }

        List<ImportRowErrorDTO> errors = new ArrayList<>();
        // Senhas dos criados nesta importação. Vivem só até a resposta sair: o
        // banco guarda apenas o hash.
        List<ImportedCredentialDTO> credentials = new ArrayList<>();
        // Cache por nome normalizado: várias linhas costumam repetir o mesmo
        // departamento, e sem isto cada uma faria a própria consulta.
        Map<String, Department> departmentCache = new HashMap<>();
        int totalRows = 0;
        int processed = 0;
        int departmentsCreated = 0;
        int collaboratorsCreated = 0;
        int collaboratorsUpdated = 0;
        int schedulesSaved = 0;

        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Planilha vazia");
            }

            // Linha 0 é o cabeçalho; a contagem exibida ao usuário é 1-based.
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || ImportCellReader.isBlank(row, LAST_COLUMN)) {
                    continue;
                }
                totalRows++;
                int displayRow = rowIndex + 1;
                String name = ImportCellReader.readString(row, COL_NAME);

                try {
                    RowOutcome outcome = processRow(row, departmentCache);
                    if (outcome.credential() != null) {
                        credentials.add(outcome.credential());
                    }
                    processed++;
                    departmentsCreated += outcome.departmentCreated() ? 1 : 0;
                    collaboratorsCreated += outcome.collaboratorCreated() ? 1 : 0;
                    collaboratorsUpdated += outcome.collaboratorCreated() ? 0 : 1;
                    schedulesSaved += outcome.schedulesSaved();
                } catch (BusinessException e) {
                    errors.add(new ImportRowErrorDTO(displayRow, name, e.getMessage()));
                }
            }
        } catch (IOException e) {
            throw new BusinessException("Não foi possível ler o arquivo. Confirme que é um .xlsx válido.");
        }

        return new ImportResultDTO(totalRows, processed, errors.size(), departmentsCreated,
                collaboratorsCreated, collaboratorsUpdated, schedulesSaved, errors, credentials);
    }

    private RowOutcome processRow(Row row, Map<String, Department> departmentCache) {
        String name = requireText(ImportCellReader.readString(row, COL_NAME), "Nome");
        String rawCpf = requireText(ImportCellReader.readString(row, COL_CPF), "CPF");
        String departmentName = requireText(ImportCellReader.readString(row, COL_DEPARTMENT), "Departamento");

        LocalTime allowedStart = ImportCellReader.readTime(row, COL_ALLOWED_START, "Início da janela permitida");
        LocalTime allowedEnd = ImportCellReader.readTime(row, COL_ALLOWED_END, "Fim da janela permitida");
        if (!allowedEnd.isAfter(allowedStart)) {
            throw new BusinessException("Fim da janela permitida deve ser posterior ao início");
        }

        // Colunas opcionais: em branco, a linha entra (ou permanece) sem o dado —
        // reimportar sem preencher não apaga o que já estava cadastrado.
        String phone = ImportCellReader.readString(row, COL_PHONE);
        String email = ImportCellReader.readString(row, COL_EMAIL);

        // Aceita CPF formatado ("123.456.789-00") e repõe os zeros à esquerda que o
        // Excel apaga ao tratar a coluna como número.
        String cpf = CpfUtils.normalize(CpfUtils.padLeadingZeros(rawCpf.replaceAll("\\D", "")));

        DepartmentOutcome department = resolveDepartment(departmentName, departmentCache);
        CollaboratorOutcome collaborator = upsertCollaborator(name, cpf, phone, email,
                department.department());
        int schedules = replaceWeekdaySchedules(collaborator.collaborator(), allowedStart, allowedEnd);

        return new RowOutcome(department.created(), collaborator.created(), schedules,
                credentialOf(name, cpf, collaborator.provisioning()));
    }

    /**
     * O CPF sai mascarado: a lista é feita para ser exibida e impressa, e o RH só
     * precisa reconhecer de quem é a linha, não ter o documento inteiro na mão de
     * quem passar pela mesa.
     */
    private ImportedCredentialDTO credentialOf(String name, String cpf, CredentialProvisioning provisioning) {
        if (provisioning == null) {
            return null;
        }
        String masked = "***." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-**";
        return new ImportedCredentialDTO(name, masked, CredentialDeliveryDTO.from(provisioning));
    }

    /** Reaproveita o departamento existente — inclusive reativando um removido. */
    private DepartmentOutcome resolveDepartment(String departmentName, Map<String, Department> cache) {
        String key = departmentName.trim().toLowerCase();
        Department cached = cache.get(key);
        if (cached != null) {
            return new DepartmentOutcome(cached, false);
        }

        Department existing = departmentRepository
                .findByNameIncludingDeleted(currentTenant.companyId(), departmentName.trim())
                .orElse(null);
        if (existing != null) {
            if (existing.getDeletedAt() != null) {
                existing.setDeletedAt(null);
                existing.setActive(true);
                existing = departmentRepository.save(existing);
            }
            cache.put(key, existing);
            return new DepartmentOutcome(existing, false);
        }

        Department created = new Department();
        created.setCompany(currentTenant.reference());
        created.setName(departmentName.trim());
        created = departmentRepository.save(created);
        cache.put(key, created);
        return new DepartmentOutcome(created, true);
    }

    /** Identidade é o CPF: mesmo CPF atualiza, CPF novo cria. */
    private CollaboratorOutcome upsertCollaborator(String name, String cpf, String phone, String email,
            Department department) {
        String cpfHash = cryptoService.blindIndex(cpf);
        Collaborator existing = collaboratorRepository
                .findByCpfHashIncludingDeleted(currentTenant.companyId(), cpfHash)
                .orElse(null);

        if (existing != null) {
            existing.setName(name);
            applyPhone(existing, phone);
            existing.setDepartment(department);
            applyEmail(existing, email);

            if (!existing.isDeleted()) {
                // Atualização comum: mexer na credencial de quem já usa o app
                // quebraria o acesso de todo mundo a cada correção de telefone.
                return new CollaboratorOutcome(collaboratorRepository.save(existing), false, null);
            }

            // Quem foi removido e voltou recebe credencial nova: a antiga já
            // circulou fora do sistema, e o acesso dela foi encerrado uma vez.
            existing.restore();
            provisioningService.initialize(existing);
            Collaborator reactivated = collaboratorRepository.save(existing);
            return new CollaboratorOutcome(reactivated, false, provisioningService.provision(reactivated));
        }

        Collaborator created = new Collaborator();
        created.setCompany(currentTenant.reference());
        created.setName(name);
        created.setCpfEncrypted(cryptoService.encrypt(cpf));
        created.setCpfHash(cpfHash);
        applyPhone(created, phone);
        created.setDepartment(department);
        applyEmail(created, email);
        provisioningService.initialize(created);

        Collaborator saved = collaboratorRepository.save(created);
        return new CollaboratorOutcome(saved, true, provisioningService.provision(saved));
    }

    /**
     * E-mail da planilha, quando a coluna vier preenchida.
     *
     * <p>
     * Não é checado contra os demais colaboradores: só o CPF identifica de forma
     * única dentro da empresa. Duas linhas podem trazer o mesmo e-mail.
     */
    private void applyEmail(Collaborator collaborator, String email) {
        if (email == null || email.isBlank()) {
            // Reimportar sem a coluna não deve apagar o e-mail já cadastrado: a
            // planilha modelo é a mesma de antes, e a ausência não é uma escolha.
            return;
        }

        collaborator.setEmail(email.trim().toLowerCase());
    }

    /**
     * Telefone da planilha, reconciliado como o e-mail: em branco não apaga o
     * que já estava cadastrado, e a coluna passou a ser opcional.
     */
    private void applyPhone(Collaborator collaborator, String phone) {
        if (phone == null || phone.isBlank()) {
            return;
        }

        // Aceita "(43) 98412-8306" e grava só os dígitos.
        collaborator.setPhoneNumber(PhoneUtils.normalize(phone));
    }

    /**
     * Aplica o mesmo horário de segunda a sexta. Sábado, se existir, é desativado:
     * a planilha define a semana inteira, então o que ela não traz não vale mais.
     */
    private int replaceWeekdaySchedules(Collaborator collaborator, LocalTime allowedStart, LocalTime allowedEnd) {
        Map<WorkDay, CollaboratorWorkSchedule> existing = new HashMap<>();
        for (CollaboratorWorkSchedule schedule : scheduleRepository
                .findByCollaboratorIdIncludingDeleted(collaborator.getId())) {
            existing.put(schedule.getDayOfWeek(), schedule);
        }

        int saved = 0;
        for (WorkDay day : DEFAULT_WORK_DAYS) {
            CollaboratorWorkSchedule schedule = existing.get(day);
            if (schedule == null) {
                schedule = new CollaboratorWorkSchedule();
                schedule.setCollaborator(collaborator);
                schedule.setDayOfWeek(day);
            }
            schedule.setAllowedStartTime(allowedStart);
            schedule.setAllowedEndTime(allowedEnd);
            schedule.setActive(true);
            schedule.setDeletedAt(null);
            scheduleRepository.save(schedule);
            saved++;
        }

        for (Map.Entry<WorkDay, CollaboratorWorkSchedule> entry : existing.entrySet()) {
            CollaboratorWorkSchedule schedule = entry.getValue();
            if (!DEFAULT_WORK_DAYS.contains(entry.getKey()) && schedule.getDeletedAt() == null) {
                schedule.setActive(false);
                schedule.setDeletedAt(java.time.LocalDateTime.now());
                scheduleRepository.save(schedule);
            }
        }
        return saved;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(fieldName + " é obrigatório");
        }
        return value.trim();
    }

    /** @param credential nulo quando a linha só atualizou um cadastro existente */
    private record RowOutcome(boolean departmentCreated, boolean collaboratorCreated, int schedulesSaved,
            ImportedCredentialDTO credential) {
    }

    private record DepartmentOutcome(Department department, boolean created) {
    }

    /** @param provisioning nulo quando o cadastro já existia e manteve a credencial */
    private record CollaboratorOutcome(Collaborator collaborator, boolean created,
            CredentialProvisioning provisioning) {
    }
}

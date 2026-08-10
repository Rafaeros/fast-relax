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
import br.rafaeros.fastrelax_api.features.imports.dtos.ImportRowErrorDTO;
import lombok.RequiredArgsConstructor;

/**
 * Importação em massa de colaboradores a partir de planilha.
 *
 * <p>
 * Cada linha percorre a cadeia departamento → colaborador → horário de almoço,
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
    static final int COL_LUNCH_START = 4;
    static final int COL_LUNCH_END = 5;
    static final int LAST_COLUMN = COL_LUNCH_END;

    /** Dias aplicados a todos os importados: o arquivo traz só os horários. */
    private static final Set<WorkDay> DEFAULT_WORK_DAYS =
            EnumSet.of(WorkDay.MONDAY, WorkDay.TUESDAY, WorkDay.WEDNESDAY, WorkDay.THURSDAY, WorkDay.FRIDAY);

    private final DepartmentRepository departmentRepository;
    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorWorkScheduleRepository scheduleRepository;
    private final CryptoService cryptoService;

    @Transactional
    public ImportResultDTO importFrom(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Envie um arquivo .xlsx com os colaboradores");
        }

        List<ImportRowErrorDTO> errors = new ArrayList<>();
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
                collaboratorsCreated, collaboratorsUpdated, schedulesSaved, errors);
    }

    private RowOutcome processRow(Row row, Map<String, Department> departmentCache) {
        String name = requireText(ImportCellReader.readString(row, COL_NAME), "Nome");
        String rawCpf = requireText(ImportCellReader.readString(row, COL_CPF), "CPF");
        String phone = requireText(ImportCellReader.readString(row, COL_PHONE), "Telefone");
        String departmentName = requireText(ImportCellReader.readString(row, COL_DEPARTMENT), "Departamento");

        LocalTime lunchStart = ImportCellReader.readTime(row, COL_LUNCH_START, "Início do almoço");
        LocalTime lunchEnd = ImportCellReader.readTime(row, COL_LUNCH_END, "Fim do almoço");
        if (!lunchEnd.isAfter(lunchStart)) {
            throw new BusinessException("Fim do almoço deve ser posterior ao início");
        }

        // Aceita CPF formatado ("123.456.789-00") e repõe os zeros à esquerda que o
        // Excel apaga ao tratar a coluna como número.
        String cpf = CpfUtils.normalize(CpfUtils.padLeadingZeros(rawCpf.replaceAll("\\D", "")));
        // Aceita "(43) 98412-8306" e grava só os dígitos.
        String normalizedPhone = PhoneUtils.normalize(phone);

        DepartmentOutcome department = resolveDepartment(departmentName, departmentCache);
        CollaboratorOutcome collaborator = upsertCollaborator(name, cpf, normalizedPhone, department.department());
        int schedules = replaceWeekdaySchedules(collaborator.collaborator(), lunchStart, lunchEnd);

        return new RowOutcome(department.created(), collaborator.created(), schedules);
    }

    /** Reaproveita o departamento existente — inclusive reativando um removido. */
    private DepartmentOutcome resolveDepartment(String departmentName, Map<String, Department> cache) {
        String key = departmentName.trim().toLowerCase();
        Department cached = cache.get(key);
        if (cached != null) {
            return new DepartmentOutcome(cached, false);
        }

        Department existing = departmentRepository.findByNameIncludingDeleted(departmentName.trim()).orElse(null);
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
        created.setName(departmentName.trim());
        created = departmentRepository.save(created);
        cache.put(key, created);
        return new DepartmentOutcome(created, true);
    }

    /** Identidade é o CPF: mesmo CPF atualiza, CPF novo cria. */
    private CollaboratorOutcome upsertCollaborator(String name, String cpf, String phone, Department department) {
        String cpfHash = cryptoService.blindIndex(cpf);
        Collaborator existing = collaboratorRepository.findByCpfHashIncludingDeleted(cpfHash).orElse(null);

        if (existing != null) {
            existing.setName(name);
            existing.setPhoneNumber(phone);
            existing.setDepartment(department);
            if (existing.getDeletedAt() != null) {
                existing.setDeletedAt(null);
                existing.setActive(true);
            }
            return new CollaboratorOutcome(collaboratorRepository.save(existing), false);
        }

        Collaborator created = new Collaborator();
        created.setName(name);
        created.setCpfEncrypted(cryptoService.encrypt(cpf));
        created.setCpfHash(cpfHash);
        created.setPhoneNumber(phone);
        created.setDepartment(department);
        return new CollaboratorOutcome(collaboratorRepository.save(created), true);
    }

    /**
     * Aplica o mesmo horário de segunda a sexta. Sábado, se existir, é desativado:
     * a planilha define a semana inteira, então o que ela não traz não vale mais.
     */
    private int replaceWeekdaySchedules(Collaborator collaborator, LocalTime lunchStart, LocalTime lunchEnd) {
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
            schedule.setLunchStartTime(lunchStart);
            schedule.setLunchEndTime(lunchEnd);
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

    private record RowOutcome(boolean departmentCreated, boolean collaboratorCreated, int schedulesSaved) {
    }

    private record DepartmentOutcome(Department department, boolean created) {
    }

    private record CollaboratorOutcome(Collaborator collaborator, boolean created) {
    }
}

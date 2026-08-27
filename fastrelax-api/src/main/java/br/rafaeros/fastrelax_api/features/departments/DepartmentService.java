package br.rafaeros.fastrelax_api.features.departments;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.features.departments.dtos.CreateDepartmentDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentFilterDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentRequestDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentResponseDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final CurrentTenant currentTenant;

    public Page<DepartmentResponseDTO> findAll(DepartmentFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        Specification<Department> spec = Specification.allOf(
                DepartmentSpecifications.nameContains(dto != null ? dto.name() : null),
                DepartmentSpecifications.hasActive(dto != null ? dto.active() : null));
        return departmentRepository.findAllScoped(spec, Objects.requireNonNull(pageable))
                .map(DepartmentResponseDTO::new);
    }

    public DepartmentResponseDTO findById(Long id) {
        return new DepartmentResponseDTO(findEntityById(id));
    }

    @Transactional
    public DepartmentResponseDTO create(CreateDepartmentDTO dto) {
        Department existing = departmentRepository
                .findByNameIncludingDeleted(currentTenant.companyId(), dto.name())
                .orElse(null);

        if (existing != null) {
            if (existing.getDeletedAt() != null) {
                // Reativa o removido em vez de inserir duplicata: a constraint
                // uq_departments_company_name não ignora soft delete.
                existing.restore();
                return new DepartmentResponseDTO(departmentRepository.save(existing));
            }
            throw new BusinessException("Departamento já existe");
        }

        Department department = new Department();
        department.setCompany(currentTenant.reference());
        department.setName(dto.name());
        return new DepartmentResponseDTO(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponseDTO update(Long id, DepartmentRequestDTO dto) {
        Department department = findEntityById(id);
        department.setName(dto.name());
        department.setActive(dto.active());
        return new DepartmentResponseDTO(departmentRepository.save(department));
    }

    @Transactional
    public DepartmentResponseDTO toggleActive(Long id) {
        Department department = findEntityById(id);
        department.setActive(!department.isActive());
        return new DepartmentResponseDTO(departmentRepository.save(department));
    }

    @Transactional
    public void softDelete(Long id) {
        Department department = findEntityById(id);
        department.markDeleted();
        departmentRepository.save(department);
    }

    /** Escopada: um id de outra empresa responde 404, como se não existisse. */
    private Department findEntityById(Long id) {
        return departmentRepository.findByIdScoped(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado"));
    }
}

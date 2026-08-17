package br.rafaeros.fastrelax_api.features.departments;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.departments.dtos.CreateDepartmentDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentFilterDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentRequestDTO;
import br.rafaeros.fastrelax_api.features.departments.dtos.DepartmentResponseDTO;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public Page<DepartmentResponseDTO> findAll(DepartmentFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        Specification<Department> spec = Specification.allOf(
                DepartmentSpecifications.nameContains(dto != null ? dto.name() : null),
                DepartmentSpecifications.hasActive(dto != null ? dto.active() : null));
        return departmentRepository.findAll(spec, Objects.requireNonNull(pageable))
                .map(department -> new DepartmentResponseDTO(department));
    }

    public DepartmentResponseDTO findById(Long id) {
        return new DepartmentResponseDTO(findEntityById(id));
    }

    public DepartmentResponseDTO create(CreateDepartmentDTO dto) {
        Department existing = departmentRepository.findByNameIncludingDeleted(dto.name()).orElse(null);
        if (existing != null) {
            if (existing.getDeletedAt() != null) {
                // Reactivate soft-deleted department instead of inserting a duplicate.
                existing.setDeletedAt(null);
                existing.setActive(true);
                return new DepartmentResponseDTO(departmentRepository.save(existing));
            }
            throw new BusinessException("Departamento já existe");
        }
        Department department = new Department();
        department.setName(dto.name());
        return new DepartmentResponseDTO(departmentRepository.save(department));
    }

    public DepartmentResponseDTO update(Long id, DepartmentRequestDTO dto) {
        Department department = findEntityById(id);
        department.setName(dto.name());
        department.setActive(dto.active());
        return new DepartmentResponseDTO(departmentRepository.save(department));
    }

    public DepartmentResponseDTO toggleActive(Long id) {
        Department department = findEntityById(id);
        department.setActive(!department.isActive());
        return new DepartmentResponseDTO(departmentRepository.save(department));
    }

    public void softDelete(Long id) {
        Department department = findEntityById(id);
        department.setActive(false);
        department.setDeletedAt(LocalDateTime.now());
        departmentRepository.save(department);
    }

    private Department findEntityById(Long id) {
        return departmentRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado"));
    }
}

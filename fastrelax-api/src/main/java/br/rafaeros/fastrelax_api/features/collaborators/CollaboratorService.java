package br.rafaeros.fastrelax_api.features.collaborators;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.util.CpfUtils;
import br.rafaeros.fastrelax_api.core.util.PhoneUtils;
import br.rafaeros.fastrelax_api.features.auth.RefreshToken;
import br.rafaeros.fastrelax_api.features.auth.RefreshTokenService;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorFilterDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CreateCollaboratorRequestDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.UpdateCollaboratorDTO;
import br.rafaeros.fastrelax_api.features.departments.DepartmentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollaboratorService {

    private final CollaboratorRepository collaboratorRepository;
    private final DepartmentRepository departmentRepository;
    private final CryptoService cryptoService;
    private final RefreshTokenService refreshTokenService;

    public Page<CollaboratorResponseDTO> findAll(CollaboratorFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        // The CPF filter runs against the blind index, so it needs the full CPF.
        String cpfHash = (dto != null && dto.cpf() != null && !dto.cpf().isBlank())
                ? cryptoService.blindIndex(normalizeCpf(dto.cpf()))
                : null;

        Specification<Collaborator> spec = Specification.allOf(
                CollaboratorSpecifications.hasActive(dto != null ? dto.active() : null),
                CollaboratorSpecifications.nameContains(dto != null ? dto.name() : null),
                CollaboratorSpecifications.cpfHashEquals(cpfHash),
                CollaboratorSpecifications.phoneNumberContains(dto != null ? dto.phoneNumber() : null),
                CollaboratorSpecifications.inDepartment(dto != null ? dto.departmentId() : null));

        return collaboratorRepository.findAll(spec, Objects.requireNonNull(pageable))
                .map(this::toResponse);
    }

    public CollaboratorResponseDTO findById(Long id) {
        return toResponse(findEntityById(id));
    }

    /**
     * Dados do colaborador logado, resolvidos pelo principal — o app não precisa
     * guardar o id nem arriscar 403 pedindo o registro de outra pessoa. Recarrega
     * do banco para refletir alterações feitas pelo RH após a emissão do token.
     */
    public CollaboratorResponseDTO findAuthenticated() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Collaborator logged)) {
            throw new AccessDeniedException("Rota disponível apenas para colaboradores autenticados");
        }
        return toResponse(collaboratorRepository.findById(Objects.requireNonNull(logged.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado")));
    }

    /** Entity lookup for authentication — callers outside auth should use {@link #findById(Long)}. */
    public Collaborator findByCpf(String cpf) {
        Collaborator collaborator = collaboratorRepository
                .findByCpfHash(cryptoService.blindIndex(normalizeCpf(cpf)))
                .orElseThrow(() -> new BusinessException("Colaborador não encontrado"));

        if (!collaborator.isActive()) {
            throw new BusinessException("Seu acesso está desativado. Entre em contato com o RH.");
        }

        return collaborator;
    }

    @Transactional
    public CollaboratorResponseDTO toggleActive(Long id) {
        Collaborator collaborator = findEntityById(id);
        collaborator.setActive(!collaborator.isActive());
        // Desativar tem que cortar as sessões abertas: sem revogar o refresh token,
        // o app continuaria renovando o acesso indefinidamente.
        if (!collaborator.isActive()) {
            refreshTokenService.revokeAllFor(RefreshToken.SubjectType.COLLABORATOR, collaborator.getId());
        }
        return toResponse(collaboratorRepository.save(collaborator));
    }

    @Transactional
    public CollaboratorResponseDTO create(CreateCollaboratorRequestDTO dto) {
        String cpf = normalizeCpf(dto.cpf());
        String cpfHash = cryptoService.blindIndex(cpf);

        Collaborator existing = collaboratorRepository.findByCpfHashIncludingDeleted(cpfHash).orElse(null);
        if (existing != null) {
            if (existing.getDeletedAt() == null) {
                throw new BusinessException("Já existe um colaborador cadastrado com este CPF");
            }
            // Reactivate the soft-deleted row instead of violating the unique constraint.
            existing.setDeletedAt(null);
            existing.setActive(true);
            existing.setDepartment(findDepartment(dto.departmentId()));
            existing.setName(dto.name());
            existing.setPhoneNumber(PhoneUtils.normalize(dto.phoneNumber()));
            return toResponse(collaboratorRepository.save(existing));
        }

        Collaborator collaborator = new Collaborator();
        collaborator.setDepartment(findDepartment(dto.departmentId()));
        collaborator.setName(dto.name());
        collaborator.setCpfEncrypted(cryptoService.encrypt(cpf));
        collaborator.setCpfHash(cpfHash);
        collaborator.setPhoneNumber(PhoneUtils.normalize(dto.phoneNumber()));

        return toResponse(collaboratorRepository.save(collaborator));
    }

    @Transactional
    public CollaboratorResponseDTO update(Long id, UpdateCollaboratorDTO dto) {
        Collaborator collaborator = findEntityById(id);
        Objects.requireNonNull(dto);

        collaborator.setDepartment(findDepartment(dto.departmentId()));
        collaborator.setName(dto.name());
        collaborator.setPhoneNumber(PhoneUtils.normalize(dto.phoneNumber()));
        collaborator.setActive(dto.active());
        applyCpfChange(collaborator, dto.cpf());

        return toResponse(collaboratorRepository.save(collaborator));
    }

    /**
     * Corrige um CPF cadastrado errado. Campo ausente ou em branco mantém o atual;
     * um CPF igual ao que já está gravado também é ignorado, para que reenviar o
     * registro inteiro não conte como troca de credencial.
     */
    private void applyCpfChange(Collaborator collaborator, String rawCpf) {
        if (rawCpf == null || rawCpf.isBlank()) {
            return;
        }
        String cpf = normalizeCpf(rawCpf);
        String cpfHash = cryptoService.blindIndex(cpf);
        if (cpfHash.equals(collaborator.getCpfHash())) {
            return;
        }

        collaboratorRepository.findByCpfHashIncludingDeleted(cpfHash)
                .filter(other -> !other.getId().equals(collaborator.getId()))
                .ifPresent(other -> {
                    // Inclui soft-deletados: a constraint unique não os ignora.
                    throw new BusinessException(other.getDeletedAt() == null
                            ? "Já existe um colaborador cadastrado com este CPF"
                            : "Existe um colaborador removido com este CPF; reative-o em vez de duplicar");
                });

        collaborator.setCpfEncrypted(cryptoService.encrypt(cpf));
        collaborator.setCpfHash(cpfHash);
    }

    @Transactional
    public void softDelete(Long id) {
        Collaborator collaborator = findEntityById(id);
        collaborator.setActive(false);
        collaborator.setDeletedAt(LocalDateTime.now());
        collaboratorRepository.save(collaborator);
        refreshTokenService.revokeAllFor(RefreshToken.SubjectType.COLLABORATOR, collaborator.getId());
    }

    private Collaborator findEntityById(Long id) {
        return collaboratorRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
    }

    private br.rafaeros.fastrelax_api.features.departments.Department findDepartment(Long departmentId) {
        return departmentRepository.findById(Objects.requireNonNull(departmentId))
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado"));
    }

    private CollaboratorResponseDTO toResponse(Collaborator collaborator) {
        return new CollaboratorResponseDTO(collaborator, cryptoService.decrypt(collaborator.getCpfEncrypted()));
    }

    /**
     * Os DTOs já exigem 11 dígitos sem pontuação; isto é a rede de segurança que
     * garante que o blind index seja sempre calculado sobre o mesmo formato —
     * caso contrário "123.456.789-00" e "12345678900" gerariam digests
     * diferentes e a constraint de unicidade seria trivial de burlar.
     */
    private String normalizeCpf(String cpf) {
        return CpfUtils.normalize(cpf);
    }
}

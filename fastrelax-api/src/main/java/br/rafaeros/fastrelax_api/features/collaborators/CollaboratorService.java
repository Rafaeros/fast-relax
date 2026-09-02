package br.rafaeros.fastrelax_api.features.collaborators;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.dto.CredentialDeliveryDTO;
import br.rafaeros.fastrelax_api.core.security.CredentialProvisioning;
import br.rafaeros.fastrelax_api.core.security.CredentialProvisioningService;
import br.rafaeros.fastrelax_api.core.security.CredentialService;
import br.rafaeros.fastrelax_api.core.security.Principals;
import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.core.util.CpfUtils;
import br.rafaeros.fastrelax_api.core.util.PhoneUtils;
import br.rafaeros.fastrelax_api.features.auth.RefreshToken;
import br.rafaeros.fastrelax_api.features.auth.RefreshTokenService;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorFilterDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CollaboratorResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CreateCollaboratorRequestDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.CreatedCollaboratorResponseDTO;
import br.rafaeros.fastrelax_api.features.collaborators.dtos.UpdateCollaboratorDTO;
import br.rafaeros.fastrelax_api.features.departments.Department;
import br.rafaeros.fastrelax_api.features.departments.DepartmentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollaboratorService {

    private final CollaboratorRepository collaboratorRepository;
    private final DepartmentRepository departmentRepository;
    private final CryptoService cryptoService;
    private final CredentialService credentialService;
    private final CredentialProvisioningService provisioningService;
    private final RefreshTokenService refreshTokenService;
    private final CurrentTenant currentTenant;

    public Page<CollaboratorResponseDTO> findAll(CollaboratorFilterDTO dto,
            @org.springframework.lang.NonNull Pageable pageable) {
        // O filtro por CPF roda contra o blind index, então precisa do CPF inteiro.
        String cpfHash = (dto != null && dto.cpf() != null && !dto.cpf().isBlank())
                ? cryptoService.blindIndex(CpfUtils.normalize(dto.cpf()))
                : null;

        Specification<Collaborator> spec = Specification.allOf(
                CollaboratorSpecifications.hasActive(dto != null ? dto.active() : null),
                CollaboratorSpecifications.nameContains(dto != null ? dto.name() : null),
                CollaboratorSpecifications.cpfHashEquals(cpfHash),
                CollaboratorSpecifications.phoneNumberContains(dto != null ? dto.phoneNumber() : null),
                CollaboratorSpecifications.inDepartment(dto != null ? dto.departmentId() : null));

        return collaboratorRepository.findAllScoped(spec, Objects.requireNonNull(pageable))
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
        Collaborator logged = Principals.requireCollaborator();
        return toResponse(findEntityById(logged.getId()));
    }

    /** Entidade crua do colaborador logado, para quem precisa alterá-la. */
    public Collaborator requireAuthenticatedEntity() {
        return findEntityById(Principals.requireCollaborator().getId());
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

    /**
     * Cadastro pelo RH.
     *
     * <p>
     * Com e-mail preenchido, a pessoa recebe um convite e define a própria senha;
     * sem e-mail, sai uma temporária na resposta — uma única vez, porque o banco
     * guarda só o hash. Quem decide é o {@link CredentialProvisioningService}, o
     * mesmo do cadastro de usuário do painel.
     */
    @Transactional
    public CreatedCollaboratorResponseDTO create(CreateCollaboratorRequestDTO dto) {
        String cpf = CpfUtils.normalize(dto.cpf());
        String cpfHash = cryptoService.blindIndex(cpf);

        Collaborator collaborator = collaboratorRepository
                .findByCpfHashIncludingDeleted(currentTenant.companyId(), cpfHash)
                .map(existing -> {
                    if (!existing.isDeleted()) {
                        throw new BusinessException("Já existe um colaborador cadastrado com este CPF");
                    }
                    // Reativa o removido: a constraint uq_collaborators_company_cpf
                    // não ignora soft delete, então inserir outro estouraria.
                    existing.restore();
                    return existing;
                })
                .orElseGet(() -> {
                    Collaborator created = new Collaborator();
                    created.setCompany(currentTenant.reference());
                    created.setCpfEncrypted(cryptoService.encrypt(cpf));
                    created.setCpfHash(cpfHash);
                    return created;
                });

        collaborator.setDepartment(findDepartment(dto.departmentId()));
        collaborator.setName(dto.name());
        applyPhone(collaborator, dto.phoneNumber());
        applyEmail(collaborator, dto.email());

        // Vale tanto para o cadastro novo quanto para a reativação: quem volta não
        // pode voltar com a credencial que tinha antes de ser removido.
        provisioningService.initialize(collaborator);
        Collaborator saved = collaboratorRepository.save(collaborator);

        CredentialProvisioning provisioning = provisioningService.provision(saved);

        return new CreatedCollaboratorResponseDTO(
                toResponse(saved), CredentialDeliveryDTO.from(provisioning));
    }

    /**
     * Grava o e-mail, em branco vira nulo.
     *
     * <p>
     * Não é mais checado contra os demais colaboradores: só o CPF identifica de
     * forma única dentro da empresa. Duas pessoas podem compartilhar e-mail — a
     * recuperação de senha resolve o mais recente entre os ativos nesse caso.
     */
    private void applyEmail(Collaborator collaborator, String rawEmail) {
        String email = rawEmail == null ? "" : rawEmail.trim().toLowerCase();
        collaborator.setEmail(email.isEmpty() ? null : email);
    }

    /** Grava o telefone, em branco vira nulo — mesma reconciliação do e-mail. */
    private void applyPhone(Collaborator collaborator, String rawPhone) {
        collaborator.setPhoneNumber(rawPhone == null || rawPhone.isBlank()
                ? null
                : PhoneUtils.normalize(rawPhone));
    }

    @Transactional
    public CollaboratorResponseDTO update(Long id, UpdateCollaboratorDTO dto) {
        Collaborator collaborator = findEntityById(id);
        Objects.requireNonNull(dto);

        collaborator.setDepartment(findDepartment(dto.departmentId()));
        collaborator.setName(dto.name());
        applyPhone(collaborator, dto.phoneNumber());
        collaborator.setActive(dto.active());
        applyEmail(collaborator, dto.email());
        applyCpfChange(collaborator, dto.cpf());

        return toResponse(collaboratorRepository.save(collaborator));
    }

    /**
     * Redefinição pelo RH: gera uma temporária, devolve uma única vez e obriga o
     * colaborador a trocá-la no próximo acesso.
     *
     * @return a senha temporária em claro, para o RH repassar
     */
    @Transactional
    public String resetPassword(Long id) {
        Collaborator collaborator = findEntityById(id);
        String temporaryPassword = credentialService.resetToTemporaryPassword(collaborator);
        collaboratorRepository.save(collaborator);
        return temporaryPassword;
    }

    /**
     * Corrige um CPF cadastrado errado. Campo ausente ou em branco mantém o atual;
     * um CPF igual ao que já está gravado também é ignorado.
     */
    private void applyCpfChange(Collaborator collaborator, String rawCpf) {
        if (rawCpf == null || rawCpf.isBlank()) {
            return;
        }
        String cpf = CpfUtils.normalize(rawCpf);
        String cpfHash = cryptoService.blindIndex(cpf);
        if (cpfHash.equals(collaborator.getCpfHash())) {
            return;
        }

        collaboratorRepository.findByCpfHashIncludingDeleted(currentTenant.companyId(), cpfHash)
                .filter(other -> !other.getId().equals(collaborator.getId()))
                .ifPresent(other -> {
                    // Inclui soft-deletados: a constraint unique não os ignora.
                    throw new BusinessException(other.isDeleted()
                            ? "Existe um colaborador removido com este CPF; reative-o em vez de duplicar"
                            : "Já existe um colaborador cadastrado com este CPF");
                });

        collaborator.setCpfEncrypted(cryptoService.encrypt(cpf));
        collaborator.setCpfHash(cpfHash);
    }

    @Transactional
    public void softDelete(Long id) {
        Collaborator collaborator = findEntityById(id);
        collaborator.markDeleted();
        collaboratorRepository.save(collaborator);
        refreshTokenService.revokeAllFor(RefreshToken.SubjectType.COLLABORATOR, collaborator.getId());
    }

    /** Escopada: um id de outra empresa responde 404, como se não existisse. */
    private Collaborator findEntityById(Long id) {
        return collaboratorRepository.findByIdScoped(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
    }

    /**
     * Também escopado: sem isto, um {@code departmentId} de outro cliente ligaria
     * um colaborador a um departamento que não é da empresa dele.
     */
    private Department findDepartment(Long departmentId) {
        return departmentRepository.findByIdScoped(Objects.requireNonNull(departmentId))
                .orElseThrow(() -> new ResourceNotFoundException("Departamento não encontrado"));
    }

    private CollaboratorResponseDTO toResponse(Collaborator collaborator) {
        return new CollaboratorResponseDTO(collaborator, cryptoService.decrypt(collaborator.getCpfEncrypted()));
    }
}

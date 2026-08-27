package br.rafaeros.fastrelax_api.features.users;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.dto.CredentialDeliveryDTO;
import br.rafaeros.fastrelax_api.core.security.CredentialProvisioning;
import br.rafaeros.fastrelax_api.core.security.CredentialProvisioningService;
import br.rafaeros.fastrelax_api.core.security.Principals;
import br.rafaeros.fastrelax_api.core.tenancy.TenantContext;
import br.rafaeros.fastrelax_api.features.auth.RefreshToken;
import br.rafaeros.fastrelax_api.features.auth.RefreshTokenService;
import br.rafaeros.fastrelax_api.features.companies.Company;
import br.rafaeros.fastrelax_api.features.companies.CompanyRepository;
import br.rafaeros.fastrelax_api.features.users.dtos.CreateUserRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.CreatedUserResponseDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.UpdateUserRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.UserResponseDTO;
import lombok.RequiredArgsConstructor;

/**
 * Usuários do painel, em dois planos.
 *
 * <p>
 * O SYSADMIN cadastra a equipe da plataforma e o gestor de cada cliente,
 * informando a empresa. O gestor do cliente cadastra o RH da própria empresa, e
 * a empresa vem do contexto — nunca do corpo da requisição.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CredentialProvisioningService provisioningService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Cadastra e entrega o acesso.
     *
     * <p>
     * Quem decide entre convite por e-mail e senha temporária é o
     * {@link CredentialProvisioningService}, o mesmo usado pelo cadastro de
     * colaborador — a regra é do produto, não de cada tela.
     */
    @Transactional
    public CreatedUserResponseDTO createUser(CreateUserRequestDTO dto) {
        assertEmailAvailable(dto.email());
        assertCanAssign(dto.role());

        User newUser = new User();
        newUser.setName(dto.name());
        newUser.setEmail(dto.email());
        newUser.setRole(dto.role());
        newUser.setCompany(resolveCompany(dto));
        // Placeholder inutilizável: a coluna é NOT NULL e o id só existe depois do
        // save, mas o token de convite precisa desse id para ser emitido.
        provisioningService.initialize(newUser);

        User savedUser = userRepository.save(newUser);
        CredentialProvisioning provisioning = provisioningService.provision(savedUser);

        return new CreatedUserResponseDTO(
                new UserResponseDTO(savedUser), CredentialDeliveryDTO.from(provisioning));
    }

    public Page<UserResponseDTO> findAllUsers(@org.springframework.lang.NonNull Pageable pageable) {
        Specification<User> spec = UserSpecifications.visibleToCurrentTenant();
        return userRepository.findAll(spec, Objects.requireNonNull(pageable)).map(UserResponseDTO::new);
    }

    public UserResponseDTO findUserById(Long id) {
        return new UserResponseDTO(findManageableEntity(id));
    }

    @Transactional
    public UserResponseDTO updateUser(Long id, UpdateUserRequestDTO dto) {
        User existingUser = findManageableEntity(id);
        if (dto.getEmail() != null && !dto.getEmail().equals(existingUser.getEmail())) {
            assertEmailAvailable(dto.getEmail());
            existingUser.setEmail(dto.getEmail());
        }
        if (dto.getName() != null) {
            existingUser.setName(dto.getName());
        }
        return new UserResponseDTO(userRepository.save(existingUser));
    }

    /** Dados do usuário logado. */
    public UserResponseDTO findAuthenticated() {
        Long id = Principals.requireUser().getId();
        return new UserResponseDTO(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!")));
    }

    @Transactional
    public UserResponseDTO toggleActive(Long id) {
        User user = findManageableEntity(id);
        user.setActive(!user.isActive());
        // Desativar precisa cortar as sessões abertas, senão o token continua
        // valendo até expirar.
        if (!user.isActive()) {
            refreshTokenService.revokeAllFor(RefreshToken.SubjectType.USER, user.getId());
        }
        return new UserResponseDTO(userRepository.save(user));
    }

    @Transactional
    public void softDelete(Long id) {
        User user = findManageableEntity(id);
        user.setActive(false);
        user.setDeletedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
        refreshTokenService.revokeAllFor(RefreshToken.SubjectType.USER, user.getId());
    }

    /**
     * Usuário que quem está logado tem alcance para administrar.
     *
     * <p>
     * Resolvido por {@code Specification} em vez de {@code findById}: a busca pela
     * chave primária ignoraria o filtro de empresa, e com id sequencial isso é uma
     * porta aberta para mexer no usuário de outro cliente. Responde 404 quando o
     * alvo está fora do alcance — 403 confirmaria que o id existe.
     */
    public User findManageableEntity(Long id) {
        Specification<User> spec = Specification.allOf(
                UserSpecifications.visibleToCurrentTenant(),
                (root, query, cb) -> cb.equal(root.get("id"), Objects.requireNonNull(id)));
        return userRepository.findOne(spec)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado!"));
    }

    /**
     * A empresa do usuário novo.
     *
     * <p>
     * Quem opera dentro de uma empresa não escolhe: é a dele, tirada do contexto.
     * Só o escopo de plataforma informa a empresa, e é o que permite ao SYSADMIN
     * cadastrar o gestor de um cliente sem pertencer àquele cliente.
     */
    private Company resolveCompany(CreateUserRequestDTO dto) {
        if (dto.role().isPlatform()) {
            return null;
        }
        Long companyId = TenantContext.currentCompanyId().orElse(dto.companyId());
        if (companyId == null) {
            throw new BusinessException("Informe a empresa do usuário");
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
    }

    /**
     * Quem pode criar cada papel.
     *
     * <p>
     * Sem isto, um gestor de cliente poderia se promover a SYSADMIN criando um
     * usuário com esse papel — escalada de privilégio por cadastro comum.
     */
    private void assertCanAssign(UserRole role) {
        if (role.isPlatform() && !TenantContext.isPlatform()) {
            throw new AccessDeniedException("Apenas a equipe da plataforma cadastra usuários SYSADMIN");
        }
    }

    /**
     * O email é único no banco, e a constraint não conhece soft delete: um
     * usuário removido continua ocupando o endereço. Checar apenas a listagem
     * visível deixava o insert estourar como violação de integridade — 409 com
     * a mensagem genérica de duplicidade, que não dizia o que fazer.
     */
    private void assertEmailAvailable(String email) {
        if (Boolean.TRUE.equals(userRepository.existsByEmail(email))) {
            throw new BusinessException("Email já está em uso!");
        }
        if (userRepository.existsByEmailIncludingDeleted(email)) {
            throw new BusinessException(
                    "Este email pertence a um usuário removido e não pode ser reutilizado. Informe outro email.");
        }
    }
}

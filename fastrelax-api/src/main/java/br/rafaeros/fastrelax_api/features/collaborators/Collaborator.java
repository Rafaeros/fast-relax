package br.rafaeros.fastrelax_api.features.collaborators;

import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.rafaeros.fastrelax_api.core.security.CredentialHolder;
import br.rafaeros.fastrelax_api.core.tenancy.SoftDeletableCompanyEntity;
import br.rafaeros.fastrelax_api.core.tenancy.TenantPrincipal;
import br.rafaeros.fastrelax_api.features.auth.RefreshToken;
import br.rafaeros.fastrelax_api.features.departments.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Pessoa atendida pelas cadeiras, dentro de uma empresa.
 *
 * <p>
 * O CPF é guardado de duas formas com propósitos distintos:
 * <ul>
 * <li>{@code cpfEncrypted} — AES-GCM reversível, para o RH consultar. IV
 * aleatório, então o mesmo CPF gera ciphertext diferente a cada gravação: não
 * serve para busca nem para unicidade.</li>
 * <li>{@code cpfHash} — HMAC determinístico (blind index). Carrega a constraint
 * de unicidade e é por onde o login procura.</li>
 * </ul>
 *
 * <p>
 * O CPF identifica; quem autentica é a senha. Antes o blind index fazia os dois
 * papéis — encontrar a pessoa <em>era</em> provar quem ela é —, e isso deixava
 * o acesso valendo o que vale um CPF, que circula em qualquer cadastro. A
 * unicidade do CPF é por empresa, não global: a mesma pessoa pode ser
 * colaboradora de dois clientes.
 */
@Entity
@Table(name = "collaborators")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor
@Getter
@Setter
public class Collaborator extends SoftDeletableCompanyEntity
        implements UserDetails, TenantPrincipal, CredentialHolder {

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 120)
    private String name;

    /** Ciphertext AES-GCM. IV aleatório, então nunca é igual para CPFs iguais — não consulte por ele. */
    @Column(name = "cpf_encrypted", nullable = false, columnDefinition = "TEXT")
    private String cpfEncrypted;

    /** HMAC determinístico do CPF normalizado. Carrega a unicidade e toda busca. */
    @Column(name = "cpf_hash", nullable = false, columnDefinition = "TEXT")
    private String cpfHash;

    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    /**
     * Opcional: parte do quadro não tem e-mail corporativo, e exigir um travaria
     * o cadastro de quem trabalha no chão de fábrica.
     *
     * <p>
     * Quem tem recebe convite de primeiro acesso e recupera a senha sozinho; quem
     * não tem depende do RH gerar uma temporária. Único dentro da empresa, como o
     * CPF — a mesma pessoa pode ser colaboradora de dois clientes.
     */
    @Column(length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Verdadeiro enquanto vale a senha temporária entregue pelo RH. Até a troca,
     * o acesso fica restrito à própria definição de senha.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Override
    public Long tenantCompanyId() {
        return companyId();
    }

    @Override
    public RefreshToken.SubjectType subjectType() {
        return RefreshToken.SubjectType.COLLABORATOR;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_COLLABORATOR"));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    /**
     * O blind index do CPF. Não é o que autentica — é o identificador dentro da
     * empresa, e é o que o Spring Security usa como nome do principal.
     */
    @Override
    public String getUsername() {
        return this.cpfHash;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Empresa suspensa desliga todos os colaboradores dela de uma vez, sem
     * precisar percorrer registro a registro.
     */
    @Override
    public boolean isEnabled() {
        return isActive()
                && !isDeleted()
                && getCompany() != null
                && getCompany().isEnabled();
    }
}

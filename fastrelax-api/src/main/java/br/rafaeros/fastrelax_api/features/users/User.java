package br.rafaeros.fastrelax_api.features.users;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import br.rafaeros.fastrelax_api.core.security.CredentialHolder;
import br.rafaeros.fastrelax_api.core.tenancy.TenantPrincipal;
import br.rafaeros.fastrelax_api.features.auth.RefreshToken;
import br.rafaeros.fastrelax_api.features.companies.Company;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Usuário do painel.
 *
 * <p>
 * É a única entidade cujo vínculo com empresa é opcional, e por isso não herda
 * de {@code CompanyScopedEntity}: o SYSADMIN é da Physical e não pertence a
 * cliente nenhum. A constraint {@code chk_users_company_role} garante no banco
 * que os dois casos não se misturem — plataforma sem empresa, cliente com.
 */
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor
@Getter
@Setter
public class User implements UserDetails, TenantPrincipal, CredentialHolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nulo apenas para {@link UserRole#SYSADMIN}. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "company_id", updatable = false)
    private Company company;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(unique = true, nullable = false, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    /**
     * Verdadeiro enquanto o usuário ainda usa a senha temporária definida por quem
     * o cadastrou. Enquanto for verdadeiro, o acesso fica limitado à troca de senha.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Override
    public Long tenantCompanyId() {
        return company == null ? null : company.getId();
    }

    @Override
    public RefreshToken.SubjectType subjectType() {
        return RefreshToken.SubjectType.USER;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @Override
    public String getUsername() {
        return this.email;
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
     * Empresa suspensa derruba quem trabalha nela: sem esta checagem, o gestor de
     * um contrato encerrado continuaria operando o painel até o token expirar.
     */
    @Override
    public boolean isEnabled() {
        return active
                && deletedAt == null
                && (company == null || company.isEnabled());
    }
}

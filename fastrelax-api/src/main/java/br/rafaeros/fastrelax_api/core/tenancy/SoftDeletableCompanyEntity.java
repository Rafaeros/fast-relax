package br.rafaeros.fastrelax_api.core.tenancy;

import java.time.LocalDateTime;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Cadastro de empresa que pode ser desativado e removido sem sumir do banco.
 *
 * <p>
 * Os dois estados são distintos e ambos são necessários:
 * <ul>
 * <li>{@code active = false} — suspensão reversível, o registro continua
 * visível para o RH e pode voltar;</li>
 * <li>{@code deletedAt != null} — sumiu das listagens, mas continua ocupando as
 * constraints de unicidade (CPF, MAC, nome do departamento), o que é o que
 * permite reativar em vez de duplicar.</li>
 * </ul>
 *
 * <p>
 * Quem herda daqui precisa declarar {@code @SQLRestriction("deleted_at IS
 * NULL")}: o filtro é por entidade, não por superclasse.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class SoftDeletableCompanyEntity extends CompanyScopedEntity {

    @ColumnDefault("true")
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

    /** Desativa e marca a remoção no mesmo gesto — separá-los deixaria linha removida "ativa". */
    public void markDeleted() {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.active = true;
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}

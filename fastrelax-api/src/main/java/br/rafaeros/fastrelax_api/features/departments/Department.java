package br.rafaeros.fastrelax_api.features.departments;

import org.hibernate.annotations.SQLRestriction;

import br.rafaeros.fastrelax_api.core.tenancy.SoftDeletableCompanyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Setor da empresa a que o colaborador pertence.
 *
 * <p>
 * O nome é único dentro da empresa, não globalmente: "Recursos Humanos" existe
 * em todos os clientes, e exigir unicidade global obrigaria o segundo a inventar
 * outro nome.
 */
@Entity
@Table(name = "departments")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor
@Getter
@Setter
public class Department extends SoftDeletableCompanyEntity {

    @Column(nullable = false, length = 100)
    private String name;
}

package br.rafaeros.fastrelax_api.core.tenancy;

import br.rafaeros.fastrelax_api.features.companies.Company;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base das entidades que existem dentro de uma empresa.
 *
 * <p>
 * Concentra o id e o vínculo com o tenant para que nenhuma entidade nova precise
 * lembrar de declarar {@code company_id} — esquecer essa coluna é exatamente o
 * tipo de omissão que só aparece como vazamento em produção.
 *
 * <p>
 * {@code updatable = false}: uma linha nunca troca de empresa. Se isso fosse
 * possível, um update mal-intencionado moveria dados de um cliente para outro
 * sem violar constraint nenhuma.
 *
 * <p>
 * A associação é {@code EAGER} de propósito. O estado da empresa é consultado em
 * lugares que rodam fora de qualquer transação — o {@code SecurityFilter}, ao
 * decidir se a credencial ainda vale — e um proxy preguiçoso ali estoura com
 * {@code LazyInitializationException}. É uma linha por consulta, por uma FK
 * indexada; o custo não paga o risco de um caminho novo esbarrar nisso.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class CompanyScopedEntity implements CompanyOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id", nullable = false, updatable = false)
    private Company company;
}

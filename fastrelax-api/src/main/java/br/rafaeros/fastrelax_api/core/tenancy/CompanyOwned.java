package br.rafaeros.fastrelax_api.core.tenancy;

import br.rafaeros.fastrelax_api.features.companies.Company;

/**
 * Contrato de tudo que pertence a uma empresa.
 *
 * <p>
 * É o que permite ao {@link CompanyScopedRepository} e ao {@link CurrentTenant}
 * aplicarem o isolamento sem conhecer nenhuma entidade em particular: quem
 * implementa entra no filtro de graça.
 */
public interface CompanyOwned {

    Company getCompany();

    void setCompany(Company company);

    /**
     * Atalho para o id do tenant.
     *
     * <p>
     * <b>Não renomeie para {@code getCompanyId()}.</b> O Spring Data resolve os
     * nomes de query derivada pelo modelo de bean property, e um getter com esse
     * nome faz {@code companyId} parecer uma propriedade real da entidade: em vez
     * de quebrar o nome em {@code company.id}, ele o trata como atributo único e
     * o Hibernate estoura com {@code Could not resolve attribute 'companyId'} —
     * derrubando a aplicação na criação dos repositórios, longe daqui.
     *
     * <p>
     * Sem o prefixo {@code get} não há propriedade aparente, e os
     * {@code findByCompanyId...} voltam a ser traduzidos para a associação.
     */
    default Long companyId() {
        Company company = getCompany();
        return company == null ? null : company.getId();
    }
}

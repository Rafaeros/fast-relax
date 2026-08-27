package br.rafaeros.fastrelax_api.features.collaborators;

import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.core.tenancy.TenantContext;
import br.rafaeros.fastrelax_api.features.companies.Company;
import br.rafaeros.fastrelax_api.features.companies.CompanyRepository;
import lombok.RequiredArgsConstructor;

/**
 * Passa a expiração por todas as empresas.
 *
 * <p>
 * As rotinas de fundo não têm requisição e, portanto, não têm tenant. Em vez de
 * uma segunda versão da regra que ignora empresa — e que aplicaria a tolerância
 * de um cliente às sessões de outro —, este varredor entra no escopo de cada
 * empresa e reaproveita exatamente a mesma
 * {@link SessionExpirationService#expireAbandonedSessions()} que o fluxo de
 * requisição usa.
 *
 * <p>
 * Uma empresa que falhe não interrompe as demais: o erro é do cliente dela, e
 * abortar a varredura deixaria todo o resto com sessão presa.
 */
@Component
@RequiredArgsConstructor
public class SessionExpirationSweeper {

    private final CompanyRepository companyRepository;
    private final SessionExpirationService sessionExpirationService;

    /**
     * @return quantas sessões foram encerradas, somando todas as empresas
     */
    public int sweepAllCompanies() {
        int total = 0;
        for (Company company : companyRepository.findAll()) {
            if (!company.isEnabled()) {
                // Contrato suspenso: ninguém daquela empresa está agendando nem
                // iniciando nada, e mexer nas sessões dela agora só produziria
                // avisos para quem já perdeu o acesso.
                continue;
            }
            total += TenantContext.callAsCompany(company.getId(),
                    sessionExpirationService::expireAbandonedSessions);
        }
        return total;
    }
}

package br.rafaeros.fastrelax_api.core.tenancy;

import java.util.Objects;

import org.springframework.stereotype.Service;

import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.companies.Company;
import br.rafaeros.fastrelax_api.features.companies.CompanyRepository;
import lombok.RequiredArgsConstructor;

/**
 * Ponte entre o {@link TenantContext}, que só guarda um id, e a entidade
 * {@link Company}, que é o que as entidades novas precisam receber.
 *
 * <p>
 * Existe para que nenhum serviço precise injetar o {@code CompanyRepository} só
 * para carimbar o dono de um registro que está criando.
 */
@Service
@RequiredArgsConstructor
public class CurrentTenant {

    private final CompanyRepository companyRepository;

    public Long companyId() {
        return TenantContext.requireCompanyId();
    }

    /**
     * Referência gerenciada da empresa do contexto, para gravar em entidades
     * novas. Não vai ao banco: só a FK é necessária no insert.
     */
    public Company reference() {
        return companyRepository.getReferenceById(companyId());
    }

    /** A empresa de fato carregada, para quem precisa de nome, CNPJ ou estado. */
    public Company load() {
        return companyRepository.findById(companyId())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
    }

    /**
     * Barreira para as leituras que escapam do {@code Specification}: um
     * {@code findById} cru, uma associação carregada a partir de outra entidade.
     *
     * <p>
     * Responde 404, não 403: dizer "sem permissão" confirmaria que o id existe em
     * alguma empresa, e o id é sequencial. Para quem está de fora, o registro de
     * outro cliente simplesmente não existe.
     */
    public void assertOwned(CompanyOwned entity) {
        Objects.requireNonNull(entity, "entity");
        if (TenantContext.isPlatform()) {
            return;
        }
        Long current = TenantContext.currentCompanyId().orElse(null);
        if (current == null || !current.equals(entity.companyId())) {
            throw new ResourceNotFoundException("Registro não encontrado");
        }
    }
}

package br.rafaeros.fastrelax_api.features.collaborators;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.rafaeros.fastrelax_api.core.tenancy.CompanyScopedRepository;

public interface CollaboratorRepository extends CompanyScopedRepository<Collaborator> {

    /**
     * Busca do login: empresa mais blind index do CPF.
     *
     * <p>
     * Roda antes de haver tenant no contexto — é ela quem descobre a empresa —,
     * então o {@code companyId} vem por parâmetro em vez de vir do
     * {@code TenantContext}.
     */
    Optional<Collaborator> findByCompanyIdAndCpfHash(Long companyId, String cpfHash);

    /**
     * Busca da recuperação de senha. Roda sem tenant no contexto — quem pede
     * ainda não está logado —, então o {@code companyId} vem do CNPJ informado na
     * tela.
     *
     * <p>
     * E-mail não é mais único dentro da empresa — só o CPF é. Pode haver mais de
     * um resultado; quem chama decide qual usar (o mais recente entre os ativos).
     */
    List<Collaborator> findByCompanyIdAndEmailIgnoreCaseOrderByCreatedAtDesc(Long companyId, String email);

    /**
     * Enxerga também os removidos, para reativar em vez de violar
     * {@code uq_collaborators_company_cpf} — a constraint não conhece soft delete.
     *
     * <p>
     * Nativa de propósito: é o único jeito de escapar do
     * {@code @SQLRestriction}. Por não passar por {@code Specification}, o
     * {@code company_id} vai explícito.
     */
    @Query(value = "SELECT * FROM collaborators WHERE company_id = :companyId AND cpf_hash = :cpfHash",
            nativeQuery = true)
    Optional<Collaborator> findByCpfHashIncludingDeleted(@Param("companyId") Long companyId,
            @Param("cpfHash") String cpfHash);
}

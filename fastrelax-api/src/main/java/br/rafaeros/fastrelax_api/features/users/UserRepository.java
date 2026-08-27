package br.rafaeros.fastrelax_api.features.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Não estende {@code CompanyScopedRepository} porque o vínculo com empresa é
 * opcional aqui: o SYSADMIN não pertence a nenhuma. O escopo é aplicado no
 * serviço, via {@link UserSpecifications#visibleToCurrentTenant()}.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    /**
     * Busca da recuperação de senha, que roda sem autenticação.
     *
     * <p>
     * Ignora caixa: quem digita o próprio e-mail para recuperar a senha não
     * lembra se cadastrou com maiúscula, e recusar por isso mandaria a pessoa
     * para o suporte por nada.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    Boolean existsByEmail(String email);

    /**
     * Enxerga também os usuários removidos.
     *
     * <p>
     * A entidade carrega {@code @SQLRestriction("deleted_at IS NULL")}, então
     * {@link #existsByEmail(String)} não vê quem sofreu soft delete — mas a
     * constraint {@code UNIQUE} da coluna vê. Sem esta consulta, recadastrar o
     * e-mail de um usuário removido passava pela checagem de negócio e estourava
     * como violação de integridade (409) no meio do insert. Nativa de propósito:
     * é o único jeito de escapar do filtro da entidade.
     *
     * <p>
     * O e-mail é único no sistema inteiro, não por empresa: é ele que identifica
     * o login do painel, e dois clientes com o mesmo endereço tornariam a
     * autenticação ambígua.
     */
    @Query(value = "SELECT COUNT(*) > 0 FROM users WHERE email = :email", nativeQuery = true)
    boolean existsByEmailIncludingDeleted(@Param("email") String email);
}

package br.rafaeros.fastrelax_api.core.tenancy;

import java.util.Objects;

/**
 * A quem pertence a requisição em curso.
 *
 * <p>
 * {@code companyId} nulo significa escopo de plataforma: a equipe Physical
 * (SYSADMIN) e as rotinas de fundo, que atravessam empresas por natureza. Todo
 * o resto opera dentro de uma empresa e nunca enxerga fora dela.
 *
 * <p>
 * A ausência de identidade — nenhum {@code TenantIdentity} no contexto — é
 * diferente das duas: é rota pública ou requisição anônima, e nesse caso
 * qualquer consulta que dependa de empresa deve falhar em vez de assumir uma.
 */
public record TenantIdentity(Long companyId) {

    public static TenantIdentity ofCompany(Long companyId) {
        return new TenantIdentity(Objects.requireNonNull(companyId, "companyId"));
    }

    public static TenantIdentity platform() {
        return new TenantIdentity(null);
    }

    public boolean isPlatform() {
        return companyId == null;
    }
}

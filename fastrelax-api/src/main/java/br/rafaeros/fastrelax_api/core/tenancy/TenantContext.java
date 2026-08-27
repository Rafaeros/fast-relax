package br.rafaeros.fastrelax_api.core.tenancy;

import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.security.access.AccessDeniedException;

/**
 * Guarda a empresa da requisição em curso.
 *
 * <p>
 * É {@code ThreadLocal} porque o isolamento precisa alcançar código que não tem
 * como receber a empresa por parâmetro — {@code Specification}s, listeners de
 * evento, o {@code toResponse} de um serviço. Passar {@code companyId} de mão em
 * mão por toda a pilha seria a alternativa, e bastaria um método esquecer o
 * argumento para o vazamento acontecer sem ninguém perceber.
 *
 * <p>
 * Quem popula é o {@link TenantContextFilter}, uma vez por requisição, e ele
 * limpa no {@code finally} — o pool reaproveita threads, e uma identidade
 * vazada aqui seria lida pela requisição seguinte como se fosse dela.
 *
 * <p>
 * Rotinas de fundo não passam por filtro nenhum: elas declaram o escopo com
 * {@link #runAsPlatform(Runnable)} ou {@link #callAsCompany(Long, Supplier)}.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantIdentity> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(TenantIdentity identity) {
        CURRENT.set(identity);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static Optional<TenantIdentity> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    /** Vazio tanto para escopo de plataforma quanto para requisição sem identidade. */
    public static Optional<Long> currentCompanyId() {
        TenantIdentity identity = CURRENT.get();
        return identity == null ? Optional.empty() : Optional.ofNullable(identity.companyId());
    }

    /**
     * Empresa do contexto, para operações que só fazem sentido dentro de uma.
     *
     * @throws AccessDeniedException quando quem chama é da plataforma ou anônimo
     */
    public static Long requireCompanyId() {
        return currentCompanyId().orElseThrow(() -> new AccessDeniedException(
                "Esta operação exige um usuário vinculado a uma empresa."));
    }

    /** Verdadeiro só para a equipe Physical e para as rotinas de fundo. */
    public static boolean isPlatform() {
        TenantIdentity identity = CURRENT.get();
        return identity != null && identity.isPlatform();
    }

    public static void runAsPlatform(Runnable action) {
        callAs(TenantIdentity.platform(), () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callAsPlatform(Supplier<T> action) {
        return callAs(TenantIdentity.platform(), action);
    }

    public static void runAsCompany(Long companyId, Runnable action) {
        callAs(TenantIdentity.ofCompany(companyId), () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callAsCompany(Long companyId, Supplier<T> action) {
        return callAs(TenantIdentity.ofCompany(companyId), action);
    }

    /**
     * Restaura o que havia antes em vez de limpar: um job que percorre empresas
     * troca de escopo várias vezes dentro da mesma thread, e limpar no fim de
     * cada volta apagaria o escopo do laço externo.
     */
    private static <T> T callAs(TenantIdentity identity, Supplier<T> action) {
        TenantIdentity previous = CURRENT.get();
        CURRENT.set(identity);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}

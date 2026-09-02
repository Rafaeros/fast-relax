package br.rafaeros.fastrelax_api.core.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;

/**
 * Slug da empresa: o que o colaborador digita na tela de login em vez do
 * CNPJ.
 *
 * <p>
 * Deriva da primeira palavra do nome — "Lanx Cables" vira "lanx" — porque é o
 * que a pessoa de fato vai digitar num teclado de celular; o nome inteiro
 * normalizado ("lanx-cables-industria-e-comercio-ltda") resolveria a colisão
 * sozinho, mas é justamente o que a mudança tenta evitar.
 */
public final class SlugUtils {

    private static final int MAX_LENGTH = 60;
    private static final Pattern VALID = Pattern.compile("^[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+|-+$)");

    private SlugUtils() {
    }

    /** Primeira palavra do nome, normalizada — o candidato natural quando ninguém digitou um slug. */
    public static String deriveFromName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Não é possível derivar o slug sem um nome de empresa");
        }
        String firstWord = name.trim().split("\\s+")[0];
        String slug = sanitize(firstWord);
        return slug.isEmpty() ? "empresa" : slug;
    }

    /** Minúsculas, sem acento, só `a-z0-9-`. É o que compara igual no login, sem depender de capitalização. */
    public static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        String withoutDiacritics = DIACRITICS.matcher(Normalizer.normalize(raw, Normalizer.Form.NFD)).replaceAll("");
        String lowered = withoutDiacritics.toLowerCase(Locale.ROOT);
        String dashed = NON_ALPHANUMERIC.matcher(lowered).replaceAll("-");
        return EDGE_DASHES.matcher(dashed).replaceAll("");
    }

    /** Formato aceito: 2 a 60 caracteres, começando e terminando em letra ou número. */
    public static boolean isValid(String slug) {
        return slug != null && slug.length() >= 2 && slug.length() <= MAX_LENGTH && VALID.matcher(slug).matches();
    }
}

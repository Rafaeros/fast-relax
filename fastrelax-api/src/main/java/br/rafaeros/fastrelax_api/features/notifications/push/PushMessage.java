package br.rafaeros.fastrelax_api.features.notifications.push;

import java.util.Map;

/**
 * Conteúdo que chega no aparelho.
 *
 * <p>
 * Deliberadamente independente de {@code Notification}: o que se entrega é
 * sempre menor do que o que se guarda, e manter os dois separados impede que
 * um campo novo do histórico vaze para dentro do payload de push, que tem
 * limite de tamanho apertado (4KB no Web Push).
 *
 * @param url    rota do app aberta no clique — relativa, resolvida pelo cliente
 * @param tag    agrupa avisos do mesmo assunto: um aviso com a mesma tag
 *               substitui o anterior na bandeja em vez de empilhar
 * @param data   pares extras entregues ao cliente junto do aviso
 */
public record PushMessage(
        String title,
        String body,
        String url,
        String tag,
        Map<String, String> data) {

    public PushMessage(String title, String body, String url, String tag) {
        this(title, body, url, tag, Map.of());
    }
}

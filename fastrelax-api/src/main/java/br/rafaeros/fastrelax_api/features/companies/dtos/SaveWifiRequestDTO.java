package br.rafaeros.fastrelax_api.features.companies.dtos;

import jakarta.validation.constraints.Size;

/**
 * Rede em que as cadeiras da própria empresa entram. Autoatendimento do
 * RH/gestor — a Physical não cadastra nem vê este valor, só o aplica ao
 * dispositivo através do push de rede.
 */
public record SaveWifiRequestDTO(
    /** Em branco apaga o Wi-Fi cadastrado (SSID e senha). */
    @Size(max = 64, message = "O SSID deve ter no máximo 64 caracteres")
    String wifiSsid,

    /**
     * Senha do Wi-Fi. Write-only: nunca volta da API.
     *
     * <p>
     * Campo ausente ou vazio mantém a senha atual — é assim que se troca o SSID
     * sem redigitar a senha. Para trocar a senha, informe a nova.
     */
    @Size(max = 128, message = "A senha deve ter no máximo 128 caracteres")
    String wifiPassword
) {}

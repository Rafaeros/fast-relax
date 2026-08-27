package br.rafaeros.fastrelax_api.features.companies.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Cadastro e edição de empresa cliente, feito pela equipe da plataforma. */
public record SaveCompanyRequestDTO(
    @NotBlank(message = "O CNPJ é obrigatório")
    String cnpj,

    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 255, message = "O nome deve ter entre 2 e 255 caracteres")
    String name,

    @NotBlank(message = "O email é obrigatório")
    @Email(message = "O email deve ser válido")
    String email,

    @NotBlank(message = "O telefone é obrigatório")
    String phone,

    @NotNull(message = "O endereço é obrigatório")
    @Valid
    SaveAddressRequestDTO address,

    /**
     * Rede em que as cadeiras desta empresa entram. Opcional: a empresa é
     * cadastrada antes de existir equipamento instalado.
     */
    @Size(max = 64, message = "O SSID deve ter no máximo 64 caracteres")
    String wifiSsid,

    /**
     * Senha do Wi-Fi. Write-only: nunca volta da API.
     *
     * <p>
     * Campo ausente ou vazio mantém a senha atual — é assim que se edita o
     * cadastro sem redigitar a senha da rede do cliente. Para trocar, informe a
     * nova.
     */
    @Size(max = 128, message = "A senha deve ter no máximo 128 caracteres")
    String wifiPassword
) {}

package br.rafaeros.fastrelax_api.features.chairs.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Edição de cadeira pelo RH/gestor da empresa: só o nome.
 *
 * <p>
 * MAC, IP, porta, firmware e BSSID são propriedade física do equipamento — quem
 * instala e substitui hardware é a equipe da plataforma, não o cliente. Restringir
 * pelo formato do DTO em vez de por uma checagem de campo no serviço evita que um
 * corpo com os outros campos pareça aceito e seja ignorado em silêncio.
 */
public record RenameChairRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    @Size(min = 2, max = 100, message = "O nome deve ter entre 2 e 100 caracteres")
    String name
) {}

package br.rafaeros.fastrelax_api.features.locations.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cadastro de município pela equipe da plataforma.
 *
 * <p>
 * Os mais de cinco mil municípios do país não são carregados na inicialização:
 * cada cliente usa um, e a lista inteira só encheria o banco. Quem cadastra a
 * empresa cria o município na hora, se ainda não existir.
 */
public record SaveCityRequestDTO(
    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
    String name,

    /** Código do IBGE do município: 7 dígitos, e é ele que garante a unicidade. */
    @NotBlank(message = "O código do IBGE é obrigatório")
    @Pattern(regexp = "\\d{7}", message = "O código do IBGE do município tem 7 dígitos")
    String ibgeCode
) {}

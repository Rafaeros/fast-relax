package br.rafaeros.fastrelax_api.core.validation;

import br.rafaeros.fastrelax_api.core.util.CpfUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Ausência é responsabilidade do @NotBlank; aqui só o formato importa.
        if (value == null || value.isBlank()) {
            return true;
        }
        return CpfUtils.hasValidCheckDigits(value);
    }
}

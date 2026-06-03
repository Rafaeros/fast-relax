package br.rafaeros.fastrelax_api.core.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponseDTO<T>(
        boolean success,
        String message,
        T data,
        List<String> warnings,
        List<String> errors,
        LocalDateTime timestamp
) {
    // Retorno de Sucesso com Dados
    public static <T> ApiResponseDTO<T> success(T data, String message) {
        return new ApiResponseDTO<>(true, message, data, null, null, LocalDateTime.now());
    }

    // Retorno de Sucesso sem Dados (ex: deleção)
    public static <T> ApiResponseDTO<T> success(String message) {
        return new ApiResponseDTO<>(true, message, null, null, null, LocalDateTime.now());
    }

    // Retorno com Avisos (Sucesso parcial ou regras específicas)
    public static <T> ApiResponseDTO<T> warning(T data, String message, List<String> warnings) {
        return new ApiResponseDTO<>(true, message, data, warnings, null, LocalDateTime.now());
    }

    // Retorno de Erro Simples
    public static <T> ApiResponseDTO<T> error(String message) {
        return new ApiResponseDTO<>(false, message, null, null, null, LocalDateTime.now());
    }

    // Retorno de Erro com Lista de Detalhes (ex: erros de validação)
    public static <T> ApiResponseDTO<T> error(String message, List<String> errors) {
        return new ApiResponseDTO<>(false, message, null, null, errors, LocalDateTime.now());
    }
}
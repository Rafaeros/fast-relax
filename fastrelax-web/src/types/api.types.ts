// O 'T' é um Generic. Ele permite que você diga qual é o formato 
// esperado dentro do "data" para cada requisição específica.
export interface BaseApiResponse<T = unknown> {
  status: "success" | "error" | "warning" | "info";
  message: string;
  data: T;
  timestamp: string;
}

// Uma classe customizada para podermos lançar erros com a mensagem da API
export class ApiError extends Error {
  public status: string;
  
  constructor(message: string, status: string) {
    super(message);
    this.status = status;
    this.name = "ApiError";
  }
}
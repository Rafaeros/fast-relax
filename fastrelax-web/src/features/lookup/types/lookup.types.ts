/**
 * Consultas a bases públicas usadas para preencher cadastro.
 *
 * <p>
 * Os tipos aqui são o que a aplicação usa, não o que a API de origem devolve:
 * cada serviço traduz o payload externo para este formato. É o que permite
 * trocar de provedor — a Receita tem várias fachadas gratuitas, e todas caem —
 * sem tocar em formulário nenhum.
 */

/** Endereço vindo de consulta externa, no vocabulário do nosso cadastro. */
export type LookupAddress = {
  /** Só dígitos. */
  cep: string;
  street: string;
  /** Ausente no CEP (que não conhece o número) e presente no CNPJ. */
  number: string;
  complement: string;
  district: string;
  cityName: string;
  /** Sigla da UF, para casar com `State.acronym`. */
  stateAcronym: string;
  /**
   * Código do IBGE do município, 7 dígitos.
   *
   * É a chave que liga a consulta ao nosso cadastro de cidades: nome de
   * município se repete entre estados e muda de grafia, o código não.
   */
  ibgeCode: string;
};

/** Dados de um CNPJ na Receita Federal. */
export type CnpjLookup = {
  taxId: string;
  /** Razão social. */
  name: string;
  /** Nome fantasia, quando houver. */
  alias: string;
  /** Texto da situação cadastral ("Ativa", "Baixada"...). */
  status: string;
  /** Situação diferente de ativa: o formulário avisa, mas não bloqueia. */
  active: boolean;
  email: string;
  /** Só dígitos, com DDD. */
  phone: string;
  address: LookupAddress;
};

/** Dados de um CEP nos Correios. */
export type CepLookup = {
  address: LookupAddress;
};

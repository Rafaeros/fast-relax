/** Espelha `LoginRequestDTO` do fastrelax-api. */
export type LoginCredentials = {
  email: string;
  password: string;
};

/** Espelha `LoginResponseDTO`. */
export type AuthSession = {
  token: string;
  refreshToken: string;
  expiresInSeconds: number;
  mustChangePassword: boolean;
};

/**
 * Espelha o enum `UserRole` do backend.
 *
 * `SYSADMIN` é a equipe da Physical: administra empresas, firmwares e os
 * gestores de cada cliente, e por decisão de produto não enxerga dado
 * operacional (colaborador, sessão, notificação). Os dois papéis `COMPANY_*`
 * vivem dentro de uma empresa e só veem o que é dela.
 */
export type UserRole = "SYSADMIN" | "COMPANY_ADMIN" | "COMPANY_RH";

/** Espelha `UserResponseDTO` (`GET /users/me`). */
export type AuthUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  /** Perfil em português, enviado pela API para exibição. */
  roleLabel?: string;
  /** Nulo para a equipe da plataforma, que não pertence a nenhuma empresa. */
  companyId: number | null;
  companyName: string | null;
  mustChangePassword: boolean;
  active: boolean;
};

/** Campos do formulário que podem receber erro individual. */
export type LoginFieldErrors = Partial<Record<keyof LoginCredentials, string>>;

/**
 * Estado devolvido pela Server Action para o `useActionState`.
 *
 * <p>
 * `email` volta preenchido no erro e serve de `defaultValue`: o React limpa os
 * campos não controlados depois de uma action, e sem isso a pessoa redigitava o
 * endereço inteiro por ter errado a senha.
 *
 * <p>
 * A senha nunca volta — nem para reexibir. Ela atravessaria a rede de novo, no
 * corpo da resposta, e ficaria no payload do RSC guardado pelo navegador.
 */
export type LoginFormState = {
  status: "idle" | "error";
  message?: string;
  fieldErrors?: LoginFieldErrors;
  email?: string;
};

export const LOGIN_INITIAL_STATE: LoginFormState = { status: "idle" };

export type PasswordFieldErrors = Partial<
  Record<"currentPassword" | "newPassword" | "confirmNewPassword", string>
>;

/**
 * Estado dos formulários de senha.
 *
 * <p>
 * Um tipo só para painel e app do colaborador: desde que o colaborador ganhou
 * senha própria, os dois seguem exatamente o mesmo ciclo — o mesmo que o
 * `CredentialService` do backend implementa uma vez para ambos.
 */
export type PasswordFormState = {
  status: "idle" | "success" | "error";
  message?: string;
  fieldErrors?: PasswordFieldErrors;
};

export const PASSWORD_INITIAL_STATE: PasswordFormState = { status: "idle" };

/** Espelha `CredentialToken.Purpose` do backend. */
export type RecoveryPurpose = "INVITE" | "RESET";

/**
 * Espelha `RecoveryTargetResponseDTO`.
 *
 * <p>
 * Só o necessário para a tela se apresentar: e-mail e papel ficam de fora
 * porque o link pode ter sido aberto por quem não é o dono.
 *
 * @param audience USER ou COLLABORATOR — decide para qual login voltar no fim
 */
export type RecoveryTarget = {
  name: string;
  purpose: RecoveryPurpose;
  audience: "USER" | "COLLABORATOR";
};

/**
 * Espelha `CredentialDeliveryDTO`.
 *
 * <p>
 * Como o acesso de uma conta recém-criada foi entregue. Formato único para os
 * três cadastros que criam credencial — usuário do painel, colaborador e
 * importação de planilha —, porque a pergunta da tela é a mesma nos três:
 * mostro a senha para copiar, ou aviso que o convite saiu?
 */
export type CredentialDelivery = {
  kind: "INVITE_SENT" | "TEMPORARY_PASSWORD";
  /** Em claro e só nesta resposta; nulo quando houve convite. */
  temporaryPassword: string | null;
  /** Para onde o convite foi; nulo quando houve senha. */
  email: string | null;
};

export type RecoveryFieldErrors = Partial<Record<"companySlug" | "email", string>>;

/** Estado do formulário de "esqueci minha senha". */
export type RecoveryFormState = {
  status: "idle" | "sent" | "error";
  message?: string;
  fieldErrors?: RecoveryFieldErrors;
  /** Devolvidos no erro para o formulário não perder o que foi digitado. */
  companySlug?: string;
  email?: string;
};

export const RECOVERY_INITIAL_STATE: RecoveryFormState = { status: "idle" };

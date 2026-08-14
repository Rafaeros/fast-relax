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

export type UserRole = "ADMIN" | "RH";

/** Espelha `UserResponseDTO` (`GET /users/me`). */
export type AuthUser = {
  id: number;
  name: string;
  email: string;
  role: UserRole;
  /** Perfil em português, enviado pela API para exibição. */
  roleLabel?: string;
  mustChangePassword: boolean;
  active: boolean;
};

/** Campos do formulário que podem receber erro individual. */
export type LoginFieldErrors = Partial<Record<keyof LoginCredentials, string>>;

/** Estado devolvido pela Server Action para o `useActionState`. */
export type LoginFormState = {
  status: "idle" | "error";
  message?: string;
  fieldErrors?: LoginFieldErrors;
};

export const LOGIN_INITIAL_STATE: LoginFormState = { status: "idle" };

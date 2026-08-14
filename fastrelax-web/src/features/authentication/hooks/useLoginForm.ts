"use client";

import { useActionState } from "react";
import { loginAction } from "@/features/authentication/actions/login.action";
import {
  LOGIN_INITIAL_STATE,
  type LoginFormState,
} from "@/features/authentication/types/auth.types";

export type UseLoginFormReturn = {
  state: LoginFormState;
  formAction: (formData: FormData) => void;
  pending: boolean;
};

/**
 * Liga o formulário à Server Action e expõe estado/pending prontos para a UI.
 * Concentrar isto num hook mantém `LoginForm` só com marcação.
 */
export function useLoginForm(): UseLoginFormReturn {
  const [state, formAction, pending] = useActionState(loginAction, LOGIN_INITIAL_STATE);

  return { state, formAction, pending };
}

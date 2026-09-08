"use client";

import { useState } from "react";
import { Icon } from "@/components/ui/Icon";
import { IconButton } from "@/components/ui/IconButton";
import { Input, type InputProps } from "@/components/ui/Input";

export type PasswordInputProps = Omit<InputProps, "type" | "trailing">;

/**
 * Campo de senha com o botão de mostrar/ocultar.
 *
 * <p>
 * Senha digitada às cegas é o que mais produz "senha inválida" em quem acertou
 * a senha — no celular, com teclado que troca de layout e maiúscula automática,
 * ver o que foi escrito é a diferença entre entrar e pedir uma nova ao RH.
 *
 * <p>
 * Começa oculto e volta a ocultar a cada montagem: revelar é uma escolha
 * deliberada de quem está na frente da tela, e guardá-la entre visitas deixaria
 * a senha exposta para quem passa atrás da pessoa.
 */
export function PasswordInput(props: PasswordInputProps) {
  const [visible, setVisible] = useState(false);

  return (
    <Input
      {...props}
      type={visible ? "text" : "password"}
      trailing={
        <IconButton
          label={visible ? "Ocultar senha" : "Mostrar senha"}
          aria-pressed={visible}
          // `tabIndex={-1}`: o Tab da senha vai para o botão de entrar, que é
          // para onde a pessoa está indo. O olho continua clicável e acessível
          // pelo rótulo.
          tabIndex={-1}
          disabled={props.disabled}
          onClick={() => setVisible((current) => !current)}
          icon={<Icon name={visible ? "eyeOff" : "eye"} className="h-4 w-4" />}
        />
      }
    />
  );
}

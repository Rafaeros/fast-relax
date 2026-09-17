"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Card, CardBody, CardDescription, CardTitle, Icon, Input, useToast } from "@/components/ui";
import { formatLongDate } from "@/lib/format";
import { updateMyWifiAction } from "@/features/companies/actions/company.actions";
import { WIFI_INITIAL_STATE, type Company } from "@/features/companies/types/company.types";
import { hasFieldErrors } from "@/lib/forms";

export type CompanyWifiFormProps = {
  company: Company;
};

/**
 * Rede das cadeiras — autoatendimento do RH/gestor.
 *
 * <p>
 * A Physical não cadastra nem enxerga este valor (nem o SSID): ela só aplica
 * a rede ao dispositivo pela ação "Enviar configuração de rede", na lista de
 * cadeiras. O BSSID fica em cada cadeira, para fixar o ponto de acesso quando
 * houver mais de um no mesmo SSID.
 */
export function CompanyWifiForm({ company }: CompanyWifiFormProps) {
  const [state, setState] = useState(WIFI_INITIAL_STATE);
  const [pending, startTransition] = useTransition();
  const toast = useToast();

  // A tela reflete o que a API confirmou salvar, não o rascunho local — assim
  // o placeholder da senha ("guardada"/"da rede") acompanha o SSID atual
  // mesmo sem recarregar a página.
  const current = state.company ?? company;
  const fieldErrors = state.fieldErrors ?? {};

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await updateMyWifiAction(state, formData);
      setState(result);

      if (result.status === "success") {
        if (result.message) toast.success(result.message);
        return;
      }

      if (result.message && !hasFieldErrors(result.fieldErrors)) toast.error(result.message);
    });
  };

  return (
    <Card padding="lg">
      <CardBody>
        <CardTitle>Rede das cadeiras</CardTitle>
        <CardDescription>
          SSID e senha do Wi-Fi em que as cadeiras desta empresa entram. Depois de salvar, peça
          à Physical para reenviar a configuração — é ela quem aplica ao dispositivo.
        </CardDescription>

        <form onSubmit={handleSubmit} className="mt-4 flex flex-col gap-5" noValidate>
          <Input
            key={`ssid-${current.wifiSsid ?? ""}`}
            name="wifiSsid"
            label="SSID"
            placeholder="Nome da rede Wi-Fi"
            maxLength={64}
            disabled={pending}
            defaultValue={current.wifiSsid ?? ""}
            hint="Limpar o SSID apaga também a senha guardada."
            error={fieldErrors.wifiSsid}
            leadingIcon={<Icon name="wrench" />}
          />

          <Input
            key={`password-${current.wifiConfigured}`}
            name="wifiPassword"
            type="password"
            label="Senha do Wi-Fi"
            // Sem `defaultValue`: a senha não volta da API por decisão de
            // projeto, e preencher com asteriscos falsos faria o campo mentir
            // sobre o que será enviado.
            placeholder={
              current.wifiConfigured ? "Senha guardada — deixe em branco para manter" : "Senha da rede"
            }
            maxLength={128}
            autoComplete="new-password"
            disabled={pending}
            hint={
              current.wifiConfigured
                ? "Preencha só para trocar a senha. Em branco, a atual é mantida."
                : "Fica cifrada no banco e só sai daqui para o ESP32, no push da Physical."
            }
            leadingIcon={<Icon name="lock" />}
          />

          <div className="flex items-center justify-between gap-4 border-t border-line pt-5">
            <span className="text-xs text-ink-tertiary">
              {current.wifiUpdatedAt
                ? `Última alteração em ${formatLongDate(current.wifiUpdatedAt)}`
                : "Ainda sem rede cadastrada"}
            </span>

            <Button
              type="submit"
              size="sm"
              disabled={pending}
              trailingIcon={
                pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
              }
            >
              {pending ? "Salvando..." : "Salvar rede"}
            </Button>
          </div>
        </form>
      </CardBody>
    </Card>
  );
}

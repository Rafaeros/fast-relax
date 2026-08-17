import { Button, Icon } from "@/components/ui";
import { logoutAction, type LogoutOrigin } from "@/features/authentication/actions/logout.action";

export type LogoutButtonProps = {
  /** Define para qual tela de login voltar. Painel é o padrão. */
  origin?: LogoutOrigin;
};

/** Formulário mínimo: sem JS no cliente, o logout continua funcionando. */
export function LogoutButton({ origin = "panel" }: LogoutButtonProps) {
  return (
    <form action={logoutAction}>
      <input type="hidden" name="origin" value={origin} />
      <Button
        type="submit"
        variant="ghost"
        size="sm"
        leadingIcon={<Icon name="logout" className="h-4 w-4" />}
      >
        Sair
      </Button>
    </form>
  );
}

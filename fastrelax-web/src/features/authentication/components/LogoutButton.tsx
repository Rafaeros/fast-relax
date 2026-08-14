import { Button, Icon } from "@/components/ui";
import { logoutAction } from "@/features/authentication/actions/logout.action";

/** Formulário mínimo: sem JS no cliente, o logout continua funcionando. */
export function LogoutButton() {
  return (
    <form action={logoutAction}>
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

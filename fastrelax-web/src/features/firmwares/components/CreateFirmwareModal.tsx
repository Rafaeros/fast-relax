"use client";

import { useState, useTransition, type FormEvent } from "react";
import { Button, Icon, Modal, useToast } from "@/components/ui";
import {
  createFirmwareAction,
  uploadFirmwareFileAction,
} from "@/features/firmwares/actions/firmware.actions";
import { FirmwareFilePicker } from "@/features/firmwares/components/FirmwareFilePicker";
import { FirmwareFormFields } from "@/features/firmwares/components/FirmwareFormFields";
import { FIRMWARE_INITIAL_STATE } from "@/features/firmwares/types/firmware.types";
import { hasFieldErrors } from "@/lib/forms";

export type CreateFirmwareModalProps = {
  /** Disparado após a publicação dar certo — a tabela recarrega a partir daqui. */
  onCreated: () => void;
};

/**
 * Publica uma versão e, opcionalmente, já anexa o binário.
 *
 * <p>
 * O upload é por id, e o id só nasce no insert — então o arquivo fica retido no
 * cliente e sobe logo depois da publicação, em dois passos encadeados. Foi o
 * jeito de dar um gesto só ao usuário sem inventar um endpoint que aceite
 * metadados e arquivo juntos.
 *
 * <p>
 * Se a publicação der certo e o upload falhar, a versão <b>permanece</b>: o
 * aviso diz o que faltou, e o binário pode ser anexado pelos detalhes. Desfazer
 * o cadastro seria pior — apagaria um registro que já está correto.
 */
export function CreateFirmwareModal({ onCreated }: CreateFirmwareModalProps) {
  const [open, setOpen] = useState(false);
  const [state, setState] = useState(FIRMWARE_INITIAL_STATE);
  const [file, setFile] = useState<File | null>(null);
  const [pending, startTransition] = useTransition();
  // Trocar a key remonta o formulário e zera os campos para a próxima versão.
  const [formKey, setFormKey] = useState(0);
  const toast = useToast();

  const fieldErrors = state.fieldErrors ?? {};

  const close = () => {
    setOpen(false);
    setState(FIRMWARE_INITIAL_STATE);
    setFile(null);
    setFormKey((current) => current + 1);
  };

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);

    startTransition(async () => {
      const result = await createFirmwareAction(state, formData);

      if (result.status !== "success") {
        if (result.message && !hasFieldErrors(result.fieldErrors)) toast.error(result.message);
        setState(result);
        return;
      }

      if (file && result.firmwareId) {
        const body = new FormData();
        body.append("file", file);
        const upload = await uploadFirmwareFileAction(result.firmwareId, body);

        if (!upload.ok) {
          // A versão já existe; só o anexo falhou.
          close();
          onCreated();
          toast.error(`Versão publicada, mas o binário não subiu: ${upload.message}`);
          return;
        }
      }

      close();
      onCreated();
      if (result.message) toast.success(result.message);
    });
  };

  return (
    <>
      <Button
        size="sm"
        leadingIcon={<Icon name="upload" className="h-4 w-4" />}
        onClick={() => setOpen(true)}
      >
        Publicar versão
      </Button>

      <Modal
        open={open}
        onClose={close}
        size="md"
        dismissible={!pending}
        title="Publicar firmware"
        description="A versão fica disponível para vincular às cadeiras de qualquer cliente."
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={close} disabled={pending}>
              Cancelar
            </Button>
            <Button
              type="submit"
              form="create-firmware-form"
              size="sm"
              disabled={pending}
              trailingIcon={
                pending ? <Icon name="loader" className="h-4 w-4 animate-spin" /> : undefined
              }
            >
              {pending ? publishingLabel(file) : "Publicar"}
            </Button>
          </>
        }
      >
        <form
          key={formKey}
          id="create-firmware-form"
          onSubmit={handleSubmit}
          className="flex flex-col gap-5"
          noValidate
        >
          <FirmwareFormFields fieldErrors={fieldErrors} disabled={pending} />

          <FirmwareFilePicker file={file} onChange={setFile} disabled={pending} />
        </form>
      </Modal>
    </>
  );
}

/** Com anexo o envio tem duas etapas; o rótulo diz que ainda há upload em curso. */
function publishingLabel(file: File | null): string {
  return file ? "Publicando e anexando..." : "Publicando...";
}

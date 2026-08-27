/**
 * Gravação do ESP32 pelo navegador, via Web Serial.
 *
 * <p>
 * O navegador conversa direto com a porta USB — não há servidor no meio, nem
 * esptool instalado na máquina. Quem faz o protocolo é o `esptool-js`, o mesmo
 * código do esptool oficial compilado para o browser.
 *
 * <p>
 * Duas restrições que a interface precisa respeitar, ambas do navegador e não
 * do nosso código:
 * <ul>
 * <li><b>Contexto seguro.</b> A Web Serial só existe em HTTPS ou em
 * {@code localhost}. Servido por HTTP num IP da rede, o objeto nem aparece.</li>
 * <li><b>Chromium.</b> Firefox e Safari não implementam a API.</li>
 * </ul>
 */

export type FlashProgress = {
  /** 0 a 100, já arredondado. */
  percent: number;
  message: string;
};

export type FlashSupport =
  | { supported: true }
  | { supported: false; reason: string };

/** Endereço padrão de uma imagem de aplicação do ESP32 gerada pelo PlatformIO. */
export const DEFAULT_FLASH_ADDRESS = 0x10000;

export function checkFlashSupport(): FlashSupport {
  if (typeof window === "undefined") {
    return { supported: false, reason: "Indisponível no servidor." };
  }
  if (!window.isSecureContext) {
    return {
      supported: false,
      reason:
        "A gravação pelo navegador exige HTTPS. Neste endereço em HTTP o navegador bloqueia o acesso à porta serial.",
    };
  }
  if (!("serial" in navigator)) {
    return {
      supported: false,
      reason: "Este navegador não suporta Web Serial. Use Chrome ou Edge.",
    };
  }
  return { supported: true };
}

/**
 * Abre o seletor de portas do navegador.
 *
 * <p>
 * A escolha é sempre do usuário, numa janela do próprio navegador — a página
 * não consegue listar nem abrir portas sozinha. É por isso que este passo é
 * separado da gravação: ele precisa nascer de um clique.
 *
 * @returns a porta escolhida, ou `null` se o usuário fechou o seletor
 */
export async function requestSerialPort(): Promise<SerialPort | null> {
  try {
    return await navigator.serial.requestPort();
  } catch {
    // O navegador lança quando o usuário cancela — não é erro para reportar.
    return null;
  }
}

/**
 * Grava a imagem na placa.
 *
 * @param port    porta escolhida em {@link requestSerialPort}
 * @param content bytes do `.bin`
 * @param onProgress chamado ao longo da escrita, para a barra andar
 */
export async function flashFirmware(
  port: SerialPort,
  content: ArrayBuffer,
  onProgress: (progress: FlashProgress) => void,
): Promise<void> {
  // Import dinâmico: o esptool-js só existe no cliente e pesa bem mais que a
  // tela que o hospeda. Carregar sob demanda mantém o bundle do painel leve
  // para quem nunca vai gravar nada.
  const { ESPLoader, Transport } = await import("esptool-js");

  const transport = new Transport(port, true);

  const loader = new ESPLoader({
    transport,
    baudrate: 921600,
    // O esptool narra o processo por este terminal; sem ele, escreveria no
    // console do navegador e o usuário não veria nada.
    terminal: {
      clean: () => {},
      writeLine: (line: string) => onProgress({ percent: 0, message: line }),
      write: () => {},
    },
  });

  try {
    onProgress({ percent: 0, message: "Conectando à placa..." });
    const chip = await loader.main();

    onProgress({ percent: 0, message: `Placa detectada: ${chip}` });

    await loader.writeFlash({
      fileArray: [{ data: new Uint8Array(content), address: DEFAULT_FLASH_ADDRESS }],
      // "keep" preserva o que já está gravado no cabeçalho da imagem: o binário
      // do PlatformIO já traz modo, frequência e tamanho corretos para a placa,
      // e sobrescrever aqui é como se costuma inutilizar um dispositivo.
      flashSize: "keep",
      flashMode: "keep",
      flashFreq: "keep",
      eraseAll: false,
      compress: true,
      reportProgress: (_fileIndex: number, written: number, total: number) => {
        onProgress({
          percent: total > 0 ? Math.round((written / total) * 100) : 0,
          message: "Gravando...",
        });
      },
    });

    onProgress({ percent: 100, message: "Gravação concluída. Reiniciando a placa..." });

    // Sem o reset a placa fica parada no modo de gravação até alguém apertar o
    // botão — quem gravou pelo navegador não tem por que saber disso.
    await loader.after();
  } finally {
    // Solta a porta em qualquer desfecho: mantida aberta, uma segunda tentativa
    // (ou o monitor serial do PlatformIO) encontraria o dispositivo ocupado.
    try {
      await transport.disconnect();
    } catch {
      // A porta já pode ter sumido se o cabo foi removido no meio.
    }
  }
}

/**
 * Motivo pelo qual a gravação não está disponível, ou `null` se estiver.
 *
 * <p>
 * Assíncrono mesmo respondendo na hora: assim o resultado chega por callback e
 * a tela não precisa de um `setState` síncrono dentro do efeito, que dispara
 * renderização em cascata.
 */
export async function resolveFlashUnavailableReason(): Promise<string | null> {
  const support = checkFlashSupport();
  return support.supported ? null : support.reason;
}

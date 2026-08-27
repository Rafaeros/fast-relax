package br.rafaeros.fastrelax_api.features.chairs;

/**
 * Desfecho de um comando enviado ao ESP32.
 *
 * <p>
 * Substitui o {@code boolean} que existia antes. Um booleano juntava num mesmo
 * "false" coisas que exigem reações opostas: a cadeira que <em>respondeu</em>
 * recusando porque está se estabilizando — situação normal, com prazo conhecido,
 * que só pede espera — e a cadeira que <em>não respondeu nada</em>, que é rede
 * caída ou firmware travado e precisa de alguém indo olhar o equipamento.
 *
 * <p>
 * A distinção vem do tipo da falha na chamada: uma resposta HTTP de erro traz
 * um estado conhecido do dispositivo; um timeout ou recusa de conexão traz
 * silêncio, e silêncio é {@link Outcome#UNREACHABLE}.
 *
 * @param outcome           o que aconteceu
 * @param retryAfterSeconds só em {@link Outcome#COOLING_DOWN}: quanto falta para
 *                          a cadeira aceitar comandos de novo
 */
public record ChairCommandResult(Outcome outcome, int retryAfterSeconds) {

    public enum Outcome {
        /** O ESP32 confirmou. */
        ACCEPTED,
        /** Respondeu 409: já existe sessão em andamento no dispositivo. */
        BUSY,
        /** Respondeu 409: estabilizando após a sessão anterior. */
        COOLING_DOWN,
        /** Respondeu 400: o corpo do comando não fazia sentido para o firmware. */
        INVALID_REQUEST,
        /** Respondeu 401: o token do backend não bate com o do config.h. */
        UNAUTHORIZED,
        /** Não respondeu: timeout, recusa de conexão ou firmware travado. */
        UNREACHABLE,
        /** Nem chegou a sair: a cadeira nunca informou um IP. */
        NO_ADDRESS
    }

    public static ChairCommandResult accepted() {
        return new ChairCommandResult(Outcome.ACCEPTED, 0);
    }

    public static ChairCommandResult coolingDown(int retryAfterSeconds) {
        return new ChairCommandResult(Outcome.COOLING_DOWN, Math.max(0, retryAfterSeconds));
    }

    public static ChairCommandResult of(Outcome outcome) {
        return new ChairCommandResult(outcome, 0);
    }

    public boolean isAccepted() {
        return outcome == Outcome.ACCEPTED;
    }

    /**
     * Verdadeiro quando o dispositivo respondeu, qualquer que tenha sido a
     * resposta. O oposto é o silêncio, único caso em que o estado da cadeira é
     * desconhecido.
     */
    public boolean deviceAnswered() {
        return outcome != Outcome.UNREACHABLE && outcome != Outcome.NO_ADDRESS;
    }

    /** Mensagem pronta para quem está em frente à cadeira. */
    public String message() {
        return switch (outcome) {
            case ACCEPTED -> "Comando aceito pela cadeira.";
            case BUSY -> "A cadeira está em uso por outra sessão.";
            case COOLING_DOWN -> "A cadeira está se estabilizando após a sessão anterior. "
                    + "Tente novamente em " + retryAfterSeconds + " segundos.";
            case INVALID_REQUEST -> "A cadeira recusou o comando. Procure o RH.";
            case UNAUTHORIZED -> "A cadeira recusou o comando por divergência de token. Procure o RH.";
            case UNREACHABLE -> "A cadeira não respondeu. Verifique se está ligada e na rede, "
                    + "ou procure o RH.";
            case NO_ADDRESS -> "A cadeira ainda não informou o endereço de rede. "
                    + "Ligue o dispositivo e aguarde o primeiro sinal.";
        };
    }
}

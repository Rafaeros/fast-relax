package br.rafaeros.fastrelax_api.features.chairs;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairNetworkResultDTO;
import br.rafaeros.fastrelax_api.features.companies.Company;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Entrega a rede da empresa às cadeiras dela.
 *
 * <p>
 * Separado do {@link ChairService}, que cuida do cadastro: aqui a
 * responsabilidade é falar com o dispositivo e registrar o que aconteceu. São
 * dois motivos de mudança diferentes — o formulário do RH e o protocolo do
 * ESP32.
 *
 * <p>
 * A senha só é decifrada dentro deste serviço, no instante do envio. Não passa
 * por DTO, não vai para log e não volta em resposta nenhuma.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChairNetworkService {

    private final ChairRepository chairRepository;
    private final ChairClient chairClient;
    private final CryptoService cryptoService;

    /**
     * Empurra a configuração para uma cadeira.
     *
     * <p>
     * Sem escopo de empresa: quem chama é o SYSADMIN, que configura o parque de
     * todos os clientes. A barreira de papel está no controller.
     */
    @Transactional
    public ChairNetworkResultDTO push(Long chairId) {
        Chair chair = chairRepository.findById(Objects.requireNonNull(chairId))
                .orElseThrow(() -> new ResourceNotFoundException("Cadeira não encontrada"));

        return push(chair);
    }

    /**
     * Empurra para todas as cadeiras ativas de uma empresa.
     *
     * <p>
     * É o gesto que interessa depois de trocar a senha do Wi-Fi: uma a uma, o
     * SYSADMIN esqueceria alguma, e a cadeira esquecida some da rede sem
     * ninguém perceber até alguém tentar agendar.
     *
     * <p>
     * Uma cadeira que não responde não interrompe as demais — cada uma tem o
     * próprio desfecho na lista devolvida.
     */
    @Transactional
    public List<ChairNetworkResultDTO> pushToCompany(Long companyId) {
        List<Chair> chairs = chairRepository.findByCompanyIdAndActiveTrue(
                Objects.requireNonNull(companyId));

        return chairs.stream().map(this::push).toList();
    }

    private ChairNetworkResultDTO push(Chair chair) {
        Company company = chair.getCompany();

        if (company == null || !company.hasWifi()) {
            throw new BusinessException(
                    "Configure o Wi-Fi da empresa antes de enviar para as cadeiras");
        }

        // Decifrada aqui e em nenhum outro lugar: o valor vive o tempo desta
        // chamada e vai direto para a NVS do dispositivo.
        String password = cryptoService.decrypt(company.getWifiPasswordEncrypted());

        ChairCommandResult result = chairClient.pushNetwork(
                chair, company.getWifiSsid(), password, chair.getWifiBssid());

        if (result.isAccepted()) {
            // Marca o envio, não a aplicação. Que a cadeira entrou mesmo na rede
            // só se sabe pelo SSID que ela relata no heartbeat seguinte —
            // `isOnConfiguredNetwork`.
            chair.setNetworkSyncedAt(LocalDateTime.now());
            chairRepository.save(chair);
        } else {
            log.warn("Configuração de rede não entregue à cadeira {}: {}",
                    chair.getName(), result.outcome());
        }

        return new ChairNetworkResultDTO(
                chair.getId(),
                chair.getName(),
                result.isAccepted(),
                result.outcome().name(),
                messageFor(result));
    }

    /**
     * Mensagem para quem está configurando, não para o log.
     *
     * <p>
     * "UNREACHABLE" não diz o que fazer; "a cadeira não respondeu" seguido do
     * motivo provável, sim. Quem lê isto está em pé na planta com o notebook.
     */
    private String messageFor(ChairCommandResult result) {
        return switch (result.outcome()) {
            case ACCEPTED -> "Configuração gravada. A cadeira vai reconectar na rede nova.";
            case NO_ADDRESS -> "A cadeira nunca se anunciou: sem IP conhecido, não há como alcançá-la.";
            case UNREACHABLE -> "A cadeira não respondeu. Confira se ela está ligada e na rede atual.";
            default -> "A cadeira recusou a configuração.";
        };
    }
}

package br.rafaeros.fastrelax_api.features.chairs;

import java.util.List;
import java.util.Objects;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairFilterDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairHeartbeatRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairMqttResultDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairNetworkResultDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.ChairResponseDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.CreateChairRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.RenameChairRequestDTO;
import br.rafaeros.fastrelax_api.features.chairs.dtos.SaveChairRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/chairs")
@RequiredArgsConstructor
@Tag(name = "Cadeiras")
public class ChairController {

    private final ChairService chairService;
    private final ChairNetworkService networkService;
    private final ChairMqttConfigService mqttConfigService;
    private final ChairCommandService chairCommandService;

    /**
     * Batida periódica do ESP32. Autenticada pelo token de dispositivo, não por
     * JWT — o hardware não faz login.
     *
     * <p>
     * O token é da própria cadeira (gerada por ela no primeiro boot), não um
     * segredo único compartilhado: o primeiro heartbeat pareia, os seguintes
     * exigem que bata — ver {@code ChairService#registerHeartbeat}.
     */
    @PostMapping("/heartbeat")
    @Operation(summary = "Heartbeat do ESP32: informa que está online e atualiza o IP")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> heartbeat(
            @RequestHeader(value = "X-Device-Token", required = false) String deviceToken,
            @RequestBody @Valid ChairHeartbeatRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.registerHeartbeat(dto, deviceToken),
                "Heartbeat registrado"));
    }

    @GetMapping
    // A equipe da plataforma enxerga o parque de todos os clientes: é ela que
    // instala e configura o equipamento. Cadeira é ativo da Physical, não dado
    // pessoal — colaborador e sessão continuam fora do alcance dela.
    @PreAuthorize("@access.isPlatformTeam() or @access.operatesCompany()")
    @Operation(summary = "Lista as cadeiras e o estado de conexão de cada uma")
    public ResponseEntity<ApiResponseDTO<Page<ChairResponseDTO>>> listAll(
            @ParameterObject ChairFilterDTO filter,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(ApiResponseDTO.success(
                chairService.findAll(filter, Objects.requireNonNull(pageable)),
                "Cadeiras listadas com sucesso"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam() or @access.operatesCompany()")
    @Operation(summary = "Busca uma cadeira por id")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.findById(id), "Cadeira encontrada"));
    }

    /**
     * Exclusivo da equipe da plataforma: é quem instala o equipamento e decide a
     * empresa dona dele. O RH não cadastra cadeira.
     */
    @PostMapping
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Cadastra uma cadeira pelo MAC address do ESP32 (somente Physical)")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> create(
            @RequestBody @Valid CreateChairRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(chairService.create(dto), "Cadeira cadastrada com sucesso"));
    }

    /** Edição completa (MAC, IP, porta, firmware, BSSID): somente Physical. */
    @PutMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Atualiza os dados físicos de uma cadeira (somente Physical)")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> update(@PathVariable Long id,
            @RequestBody @Valid SaveChairRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.update(id, dto),
                "Cadeira atualizada com sucesso"));
    }

    /**
     * Edição do RH/gestor da empresa: só o nome. O restante do cadastro é
     * propriedade física do equipamento, fora do alcance do painel do cliente.
     */
    @PatchMapping("/{id}/name")
    @PreAuthorize("@access.operatesCompany()")
    @Operation(summary = "Renomeia uma cadeira da própria empresa")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> rename(@PathVariable Long id,
            @RequestBody @Valid RenameChairRequestDTO dto) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.rename(id, dto),
                "Cadeira renomeada com sucesso"));
    }

    /**
     * Aciona o relé por alguns segundos, sem sessão.
     *
     * <p>
     * Restrito a ADMIN: liga fisicamente a cadeira e não passa por nenhuma das
     * regras de agendamento.
     */
    @PostMapping("/{id}/relay-test")
    @PreAuthorize("@access.isPlatformTeam() or @access.administersCompany()")
    @Operation(summary = "Liga o relé por alguns segundos para testar a instalação (somente ADMIN)")
    public ResponseEntity<ApiResponseDTO<Void>> testRelay(@PathVariable Long id,
            @RequestParam(name = "durationSeconds", defaultValue = "10") int durationSeconds) {
        chairCommandService.testRelay(chairService.findEntity(id), durationSeconds);
        return ResponseEntity.ok(ApiResponseDTO.success(
                "Relé acionado por " + durationSeconds + "s. Verifique a cadeira."));
    }

    /**
     * Ativar/desativar é decisão da Physical, não do cliente: reflete o contrato
     * comercial (equipamento pago, inadimplência, manutenção), não algo que o RH
     * decida sobre o próprio parque.
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Ativa ou desativa uma cadeira (somente Physical)")
    public ResponseEntity<ApiResponseDTO<ChairResponseDTO>> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponseDTO.success(chairService.toggleActive(id),
                "Status da cadeira alterado com sucesso"));
    }

    /** Mesma razão do toggle: remover equipamento do parque é decisão da Physical. */
    @DeleteMapping("/{id}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Remove uma cadeira (soft delete, somente Physical)")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        chairService.softDelete(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Cadeira removida com sucesso"));
    }

    /**
     * Grava a rede da empresa na memória do ESP32.
     *
     * <p>
     * Exclusivo da equipe da plataforma: é ela quem instala o equipamento e
     * conhece a rede do cliente. O RH não tem por que mexer em SSID de Wi-Fi, e
     * a senha nem passa por esta rota — sai cifrada do cadastro da empresa e é
     * decifrada só no instante do envio.
     */
    @PostMapping("/{id}/network")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Envia SSID, senha e BSSID para o ESP32 gravar na memória")
    public ResponseEntity<ApiResponseDTO<ChairNetworkResultDTO>> pushNetwork(@PathVariable Long id) {
        ChairNetworkResultDTO result = networkService.push(id);
        return ResponseEntity.ok(ApiResponseDTO.success(result, result.message()));
    }

    /**
     * Reenvia para todas as cadeiras ativas de uma empresa.
     *
     * <p>
     * É o gesto que interessa depois de trocar a senha do Wi-Fi: uma a uma, a
     * cadeira esquecida some da rede sem ninguém perceber até alguém tentar
     * agendar.
     */
    @PostMapping("/network/company/{companyId}")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Reenvia a configuração de rede para as cadeiras de uma empresa")
    public ResponseEntity<ApiResponseDTO<List<ChairNetworkResultDTO>>> pushNetworkToCompany(
            @PathVariable Long companyId) {
        List<ChairNetworkResultDTO> results = networkService.pushToCompany(companyId);

        long delivered = results.stream().filter(ChairNetworkResultDTO::delivered).count();
        return ResponseEntity.ok(ApiResponseDTO.success(results,
                delivered + " de " + results.size() + " cadeira(s) receberam a configuração"));
    }

    /**
     * Grava host, porta, usuário e senha do broker MQTT na memória do ESP32.
     *
     * <p>
     * Exclusiva da equipe da plataforma, mesma razão de /network. Diferente do
     * Wi-Fi, a maioria das cadeiras nunca precisa disto: o broker padrão já
     * bate com o que o firmware traz embutido em config.h — esta rota só
     * importa para quem configurou um override no cadastro da cadeira.
     */
    @PostMapping("/{id}/mqtt")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Envia host, porta, usuário e senha do broker MQTT para o ESP32 gravar na memória")
    public ResponseEntity<ApiResponseDTO<ChairMqttResultDTO>> pushMqtt(@PathVariable Long id) {
        ChairMqttResultDTO result = mqttConfigService.push(id);
        return ResponseEntity.ok(ApiResponseDTO.success(result, result.message()));
    }
}

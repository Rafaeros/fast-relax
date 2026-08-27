package br.rafaeros.fastrelax_api.features.locations;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.rafaeros.fastrelax_api.core.dto.ApiResponseDTO;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.features.locations.dtos.CityResponseDTO;
import br.rafaeros.fastrelax_api.features.locations.dtos.SaveCityRequestDTO;
import br.rafaeros.fastrelax_api.features.locations.dtos.StateResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Domínio do IBGE.
 *
 * <p>
 * Alimenta os selects do cadastro de empresa. Não é escopado por empresa porque
 * a lista de municípios do Brasil não é dado de cliente nenhum; escrever nele,
 * porém, é da equipe da plataforma.
 */
@RestController
@RequestMapping("/locations")
@RequiredArgsConstructor
@Tag(name = "Localidades")
public class LocationController {

    private final StateRepository stateRepository;
    private final CityRepository cityRepository;

    @GetMapping("/states")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista as unidades federativas")
    public ResponseEntity<ApiResponseDTO<List<StateResponseDTO>>> listStates() {
        List<StateResponseDTO> states = stateRepository.findAllByOrderByNameAsc().stream()
                .map(StateResponseDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponseDTO.success(states, "Estados listados com sucesso"));
    }

    @GetMapping("/states/{stateId}/cities")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista os municípios de uma unidade federativa")
    public ResponseEntity<ApiResponseDTO<List<CityResponseDTO>>> listCities(@PathVariable Long stateId) {
        List<CityResponseDTO> cities = cityRepository.findByStateIdOrderByNameAsc(stateId).stream()
                .map(CityResponseDTO::new)
                .toList();
        return ResponseEntity.ok(ApiResponseDTO.success(cities, "Cidades listadas com sucesso"));
    }

    /**
     * Cadastra o município na hora de precisar dele.
     *
     * <p>
     * A alternativa seria carregar os mais de cinco mil na inicialização, para que
     * cada cliente usasse um. Reenviar um município já cadastrado devolve o
     * existente em vez de erro: o cadastro de empresa não deve falhar porque outro
     * cliente da mesma cidade chegou antes.
     */
    @PostMapping("/states/{stateId}/cities")
    @PreAuthorize("@access.isPlatformTeam()")
    @Operation(summary = "Cadastra um município")
    public ResponseEntity<ApiResponseDTO<CityResponseDTO>> createCity(@PathVariable Long stateId,
            @RequestBody @Valid SaveCityRequestDTO dto) {
        State state = stateRepository.findById(stateId)
                .orElseThrow(() -> new ResourceNotFoundException("Estado não encontrado"));

        City city = cityRepository.findByIbgeCode(dto.ibgeCode())
                .orElseGet(() -> {
                    City created = new City();
                    created.setState(state);
                    created.setIbgeCode(dto.ibgeCode());
                    created.setName(dto.name());
                    return cityRepository.save(created);
                });

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.success(new CityResponseDTO(city), "Cidade cadastrada com sucesso"));
    }
}

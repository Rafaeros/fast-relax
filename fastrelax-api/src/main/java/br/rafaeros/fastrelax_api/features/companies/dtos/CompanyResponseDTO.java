package br.rafaeros.fastrelax_api.features.companies.dtos;

import java.time.LocalDateTime;

import br.rafaeros.fastrelax_api.features.companies.Company;
import br.rafaeros.fastrelax_api.features.locations.Address;

public record CompanyResponseDTO(
    Long id,
    String cnpj,
    String name,
    String email,
    String phone,
    boolean active,
    Long addressId,
    String cep,
    String street,
    String number,
    String complement,
    Long cityId,
    String cityName,
    String stateAcronym,
    /**
     * SSID da rede das cadeiras.
     *
     * <p>
     * A senha não acompanha, por decisão explícita: é segredo de terceiro
     * guardado por nós, e devolvê-la a colocaria em resposta HTTP, log de proxy
     * e histórico de navegador. Quem precisa dela é o ESP32, e ele a recebe no
     * push da configuração.
     */
    String wifiSsid,
    /** Se há senha gravada — o que a tela precisa saber sem receber o valor. */
    boolean wifiConfigured,
    LocalDateTime wifiUpdatedAt,
    LocalDateTime createdAt
) {
    public CompanyResponseDTO(Company entity) {
        this(entity.getId(), entity.getCnpj(), entity.getName(), entity.getEmail(), entity.getPhone(),
                entity.isActive(),
                id(entity.getAddress()),
                entity.getAddress() != null ? entity.getAddress().getCep() : null,
                entity.getAddress() != null ? entity.getAddress().getStreet() : null,
                entity.getAddress() != null ? entity.getAddress().getNumber() : null,
                entity.getAddress() != null ? entity.getAddress().getComplement() : null,
                cityId(entity.getAddress()),
                cityName(entity.getAddress()),
                stateAcronym(entity.getAddress()),
                entity.getWifiSsid(),
                entity.hasWifi(),
                entity.getWifiUpdatedAt(),
                entity.getCreatedAt());
    }

    private static Long id(Address address) {
        return address == null ? null : address.getId();
    }

    private static Long cityId(Address address) {
        return address == null || address.getCity() == null ? null : address.getCity().getId();
    }

    private static String cityName(Address address) {
        return address == null || address.getCity() == null ? null : address.getCity().getName();
    }

    private static String stateAcronym(Address address) {
        if (address == null || address.getCity() == null || address.getCity().getState() == null) {
            return null;
        }
        return address.getCity().getState().getAcronym();
    }
}

package br.rafaeros.fastrelax_api.features.companies;

import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.crypto.CryptoService;
import br.rafaeros.fastrelax_api.core.exceptions.BusinessException;
import br.rafaeros.fastrelax_api.core.exceptions.ResourceNotFoundException;
import br.rafaeros.fastrelax_api.core.tenancy.CurrentTenant;
import br.rafaeros.fastrelax_api.core.util.CnpjUtils;
import br.rafaeros.fastrelax_api.core.util.PhoneUtils;
import br.rafaeros.fastrelax_api.core.util.SlugUtils;
import br.rafaeros.fastrelax_api.features.companies.dtos.CompanyResponseDTO;
import br.rafaeros.fastrelax_api.features.companies.dtos.SaveAddressRequestDTO;
import br.rafaeros.fastrelax_api.features.companies.dtos.SaveCompanyRequestDTO;
import br.rafaeros.fastrelax_api.features.locations.Address;
import br.rafaeros.fastrelax_api.features.locations.AddressRepository;
import br.rafaeros.fastrelax_api.features.locations.CityRepository;
import br.rafaeros.fastrelax_api.features.settings.CompanySessionSettings;
import br.rafaeros.fastrelax_api.features.settings.SessionSettingsRepository;
import lombok.RequiredArgsConstructor;

/**
 * Cadastro dos clientes, operado pela equipe da plataforma.
 *
 * <p>
 * É o único serviço que atravessa empresas por natureza — criar um tenant não
 * pode acontecer de dentro de nenhum.
 */
@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final AddressRepository addressRepository;
    private final CityRepository cityRepository;
    private final SessionSettingsRepository settingsRepository;
    private final CryptoService cryptoService;
    private final CurrentTenant currentTenant;

    public Page<CompanyResponseDTO> findAll(@org.springframework.lang.NonNull Pageable pageable) {
        return companyRepository.findAll(Objects.requireNonNull(pageable)).map(CompanyResponseDTO::new);
    }

    public CompanyResponseDTO findById(Long id) {
        return new CompanyResponseDTO(findEntityById(id));
    }

    /** A própria empresa de quem está logado — o que a tela "Minha empresa" do RH mostra. */
    public CompanyResponseDTO findMine() {
        return new CompanyResponseDTO(currentTenant.load());
    }

    /**
     * Cadastro do cliente.
     *
     * <p>
     * Já cria a configuração de sessão junto: sem ela, o primeiro agendamento da
     * empresa dependeria de alguém lembrar de abrir a tela de configurações, e a
     * duração da massagem ficaria indefinida até lá.
     */
    @Transactional
    public CompanyResponseDTO create(SaveCompanyRequestDTO dto) {
        String cnpj = CnpjUtils.normalize(dto.cnpj());
        assertCnpjAvailable(cnpj, null);
        assertEmailAvailable(dto.email());

        Company company = new Company();
        company.setCnpj(cnpj);
        company.setSlug(resolveSlug(dto, null));
        company.setAddress(addressRepository.save(buildAddress(new Address(), dto.address())));
        applyFields(company, dto);

        Company saved = companyRepository.save(company);
        settingsRepository.save(new CompanySessionSettings(saved));

        return new CompanyResponseDTO(saved);
    }

    @Transactional
    public CompanyResponseDTO update(Long id, SaveCompanyRequestDTO dto) {
        Company company = findEntityById(id);
        String cnpj = CnpjUtils.normalize(dto.cnpj());
        assertCnpjAvailable(cnpj, company.getId());

        if (!company.getEmail().equals(dto.email())) {
            assertEmailAvailable(dto.email());
        }

        company.setCnpj(cnpj);
        company.setSlug(resolveSlug(dto, company.getId()));
        buildAddress(company.getAddress(), dto.address());
        applyFields(company, dto);

        return new CompanyResponseDTO(companyRepository.save(company));
    }

    /**
     * Slug explícito ou derivado do nome.
     *
     * <p>
     * Informado, precisa ser único — quem escolheu o valor decide como
     * resolver a colisão. Derivado, ganha sufixo numérico sozinho: ninguém
     * escolheu aquele texto, então não há "seu" valor para preservar diante de
     * outra empresa que já o tenha.
     */
    private String resolveSlug(SaveCompanyRequestDTO dto, Long currentId) {
        String explicit = dto.slug() == null ? "" : dto.slug().trim();

        if (!explicit.isEmpty()) {
            if (!SlugUtils.isValid(explicit)) {
                throw new BusinessException("Slug inválido: use só letras minúsculas, números e hífen");
            }
            assertSlugAvailable(explicit, currentId);
            return explicit;
        }

        String base = SlugUtils.deriveFromName(dto.name());
        String candidate = base;
        int suffix = 2;
        while (slugTaken(candidate, currentId)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private boolean slugTaken(String slug, Long currentId) {
        return companyRepository.findBySlug(slug)
                .filter(other -> !other.getId().equals(currentId))
                .isPresent()
                || companyRepository.existsBySlugIncludingDeleted(slug);
    }

    /**
     * O slug é único e a constraint não conhece soft delete — mesma lógica do
     * CNPJ, mas aqui o valor foi escolhido por alguém, então a resposta pede
     * para digitar outro em vez de gerar uma variação sozinha.
     */
    private void assertSlugAvailable(String slug, Long currentId) {
        if (slugTaken(slug, currentId)) {
            throw new BusinessException("Já existe uma empresa cadastrada com este slug");
        }
    }

    /**
     * Suspende ou reativa o contrato.
     *
     * <p>
     * Desativar derruba todo mundo da empresa de uma vez — usuários do painel e
     * colaboradores —, porque {@code isEnabled()} de ambos consulta o estado da
     * empresa. Não é preciso percorrer registro a registro.
     */
    @Transactional
    public CompanyResponseDTO toggleActive(Long id) {
        Company company = findEntityById(id);
        company.setActive(!company.isActive());
        return new CompanyResponseDTO(companyRepository.save(company));
    }

    @Transactional
    public void softDelete(Long id) {
        Company company = findEntityById(id);
        company.setActive(false);
        company.setDeletedAt(java.time.LocalDateTime.now());
        companyRepository.save(company);
    }

    private void applyFields(Company company, SaveCompanyRequestDTO dto) {
        company.setName(dto.name());
        company.setEmail(dto.email());
        company.setPhone(PhoneUtils.normalize(dto.phone()));
        applyWifi(company, dto);
    }

    /**
     * Rede das cadeiras.
     *
     * <p>
     * A senha em branco mantém a atual: é assim que se edita o cadastro sem
     * redigitar a senha da rede do cliente — que, aliás, ninguém consegue ler
     * de volta da API para conferir.
     *
     * <p>
     * Cifrada com o mesmo AES-GCM do CPF, e pelo mesmo motivo: é segredo de
     * terceiro guardado por nós.
     */
    private void applyWifi(Company company, SaveCompanyRequestDTO dto) {
        String ssid = dto.wifiSsid() == null ? "" : dto.wifiSsid().trim();
        String password = dto.wifiPassword() == null ? "" : dto.wifiPassword();

        boolean changed = false;

        if (!ssid.equals(company.getWifiSsid() == null ? "" : company.getWifiSsid())) {
            company.setWifiSsid(ssid.isEmpty() ? null : ssid);
            changed = true;
        }

        if (!password.isEmpty()) {
            company.setWifiPasswordEncrypted(cryptoService.encrypt(password));
            changed = true;
        }

        if (ssid.isEmpty()) {
            // Sem SSID a senha não serve para nada, e guardá-la seria manter um
            // segredo de terceiro sem propósito.
            company.setWifiPasswordEncrypted(null);
        }

        if (changed) {
            company.setWifiUpdatedAt(java.time.LocalDateTime.now());
        }
    }

    private Address buildAddress(Address address, SaveAddressRequestDTO dto) {
        address.setCity(cityRepository.findById(Objects.requireNonNull(dto.cityId()))
                .orElseThrow(() -> new ResourceNotFoundException("Cidade não encontrada")));
        address.setCep(dto.cep().replaceAll("\\D", ""));
        address.setStreet(dto.street());
        address.setNumber(dto.number());
        address.setComplement(dto.complement());
        return address;
    }

    /**
     * O CNPJ é único e a constraint não conhece soft delete: uma empresa removida
     * continua ocupando o número. Sem esta checagem, recadastrá-la estouraria como
     * violação de integridade no meio do insert.
     */
    private void assertCnpjAvailable(String cnpj, Long currentId) {
        companyRepository.findByCnpjIncludingDeleted(cnpj)
                .filter(other -> !other.getId().equals(currentId))
                .ifPresent(other -> {
                    throw new BusinessException(other.getDeletedAt() == null
                            ? "Já existe uma empresa cadastrada com este CNPJ"
                            : "Existe uma empresa removida com este CNPJ; reative-a em vez de duplicar");
                });
    }

    private void assertEmailAvailable(String email) {
        if (companyRepository.existsByEmail(email) || companyRepository.existsByEmailIncludingDeleted(email)) {
            throw new BusinessException("Já existe uma empresa cadastrada com este email");
        }
    }

    private Company findEntityById(Long id) {
        return companyRepository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
    }
}

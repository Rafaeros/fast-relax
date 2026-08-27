package br.rafaeros.fastrelax_api.features.locations;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByStateIdOrderByNameAsc(Long stateId);

    /** O codigo do IBGE e a identidade do municipio, e o que carrega a unicidade. */
    java.util.Optional<City> findByIbgeCode(String ibgeCode);
}

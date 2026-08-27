package br.rafaeros.fastrelax_api.features.locations;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StateRepository extends JpaRepository<State, Long> {

    List<State> findAllByOrderByNameAsc();

    Optional<State> findByAcronymIgnoreCase(String acronym);
}

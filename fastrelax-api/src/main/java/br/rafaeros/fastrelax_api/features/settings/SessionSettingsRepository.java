package br.rafaeros.fastrelax_api.features.settings;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessionSettingsRepository extends JpaRepository<SessionSettings, Long> {

    Optional<SessionSettings> findFirstByOrderByIdAsc();
}

package br.rafaeros.fastrelax_api.features.settings;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A chave primária é o {@code company_id}, então {@code findById} já é a busca
 * por empresa — não há o que escopar além disso.
 */
public interface SessionSettingsRepository extends JpaRepository<CompanySessionSettings, Long> {
}

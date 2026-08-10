package br.rafaeros.fastrelax_api.features.notifications;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    List<DeviceToken> findByCollaboratorIdAndActiveTrue(Long collaboratorId);
}

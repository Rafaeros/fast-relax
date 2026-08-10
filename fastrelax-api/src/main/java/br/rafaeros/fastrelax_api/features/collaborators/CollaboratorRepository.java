package br.rafaeros.fastrelax_api.features.collaborators;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CollaboratorRepository
        extends JpaRepository<Collaborator, Long>, JpaSpecificationExecutor<Collaborator> {

    Optional<Collaborator> findByCpfHash(String cpfHash);

    // Native query bypasses @SQLRestriction("deleted_at IS NULL") so we can find
    // soft-deleted rows and reactivate them instead of violating the unique constraint.
    @Query(value = "SELECT * FROM collaborators WHERE cpf_hash = :cpfHash", nativeQuery = true)
    Optional<Collaborator> findByCpfHashIncludingDeleted(@Param("cpfHash") String cpfHash);
}

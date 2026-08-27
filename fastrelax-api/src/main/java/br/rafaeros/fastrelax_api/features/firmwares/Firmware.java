package br.rafaeros.fastrelax_api.features.firmwares;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Versão de firmware publicada pela Physical.
 *
 * <p>
 * Não pertence a empresa nenhuma: é catálogo da plataforma, e a mesma versão é
 * instalada em cadeiras de clientes diferentes. Quem administra é o SYSADMIN; as
 * empresas só leem, para saber o que roda nos equipamentos delas.
 */
@Entity
@Table(name = "firmwares")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor
@Getter
@Setter
public class Firmware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    /** Única no catálogo: é o que identifica a versão em qualquer conversa de suporte. */
    @Column(nullable = false, unique = true, length = 50)
    private String version;

    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    /**
     * Binários da versão. {@code orphanRemoval} porque um arquivo sem firmware não
     * significa nada — não é um registro que sobrevive sozinho.
     */
    @OneToMany(mappedBy = "firmware", cascade = CascadeType.ALL, orphanRemoval = true,
            fetch = FetchType.LAZY)
    private List<FirmwareFile> files = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void addFile(FirmwareFile file) {
        file.setFirmware(this);
        files.add(file);
    }
}

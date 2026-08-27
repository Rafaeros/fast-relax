package br.rafaeros.fastrelax_api;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Recria o banco de teste do zero a cada execução da suíte.
 *
 * <p>
 * O schema ainda é pré-lançamento: V1 é reescrita, e o Flyway compara o
 * checksum do arquivo com o que ficou registrado na execução anterior. Sem
 * limpar, toda edição em V1 derrubava o build com "Migration checksum
 * mismatch" — falha de ambiente, não de código.
 *
 * <p>
 * Limpar é também o comportamento correto para teste: cada execução parte do
 * schema que está no repositório, e não do que sobrou da anterior.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestDatabaseConfig {

    /** Reconhece o banco descartável. Ver {@link #testMigrationStrategy()}. */
    private static final String EXPECTED_DATABASE_SUFFIX = "_test";

    @Bean
    FlywayMigrationStrategy testMigrationStrategy() {
        return flyway -> {
            assertDisposableDatabase(flyway.getConfiguration().getDataSource());

            flyway.clean();
            flyway.migrate();
        };
    }

    /**
     * `clean` apaga o banco inteiro. Um perfil apontado para o lugar errado —
     * um TEST_DB_URL de desenvolvimento no .env, por exemplo — levaria o banco
     * de trabalho junto, em silêncio. Falhar aqui custa uma execução da suíte;
     * não falhar custa o banco.
     */
    private static void assertDisposableDatabase(DataSource dataSource) {
        String url;

        // A URL não vem da configuração do Flyway: o Spring Boot o monta a
        // partir de um DataSource já pronto, e ali `getUrl()` é sempre nulo.
        try (Connection connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível identificar o banco de teste", e);
        }

        if (url == null || !url.endsWith(EXPECTED_DATABASE_SUFFIX)) {
            throw new IllegalStateException(
                    "O perfil de teste recria o banco do zero e só aceita um banco terminado em '"
                            + EXPECTED_DATABASE_SUFFIX + "'. URL configurada: " + url);
        }
    }
}

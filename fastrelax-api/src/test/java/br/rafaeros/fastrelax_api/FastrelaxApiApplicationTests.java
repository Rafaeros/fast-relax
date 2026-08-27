package br.rafaeros.fastrelax_api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Sobe o contexto inteiro: pega bean quebrado, injeção faltando e, sobretudo,
 * migration que não aplica.
 *
 * <p>
 * O perfil {@code test} aponta para o banco {@code fastrelax_test} e deixa o
 * Flyway recriá-lo quando o checksum não bate. Sem ele o teste rodava contra o
 * banco de desenvolvimento, e reescrever V1 — que ainda é pré-lançamento —
 * quebrava o build por diferença de ambiente, não de código.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
class FastrelaxApiApplicationTests {

	@Test
	void contextLoads() {
	}

}

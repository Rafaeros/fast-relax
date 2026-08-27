package br.rafaeros.fastrelax_api.core.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.features.locations.State;
import br.rafaeros.fastrelax_api.features.locations.StateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Popula as 27 unidades federativas.
 *
 * <p>
 * Sem elas não há município, sem município não há endereço, e sem endereço não
 * se cadastra empresa — ou seja, o sistema subiria travado no primeiro passo.
 * São 27 linhas fixas há décadas: carregá-las de um arquivo externo só
 * acrescentaria um ponto de falha na inicialização.
 *
 * <p>
 * Os municípios ficam de fora de propósito: são mais de cinco mil, e cada
 * cliente precisa de um. Eles são criados sob demanda pela equipe da plataforma
 * no cadastro da empresa.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class StateSeeder implements CommandLineRunner {

    /** Código do IBGE, sigla e nome — nessa ordem. */
    private static final List<String[]> STATES = List.of(
            new String[] { "11", "RO", "Rondônia" },
            new String[] { "12", "AC", "Acre" },
            new String[] { "13", "AM", "Amazonas" },
            new String[] { "14", "RR", "Roraima" },
            new String[] { "15", "PA", "Pará" },
            new String[] { "16", "AP", "Amapá" },
            new String[] { "17", "TO", "Tocantins" },
            new String[] { "21", "MA", "Maranhão" },
            new String[] { "22", "PI", "Piauí" },
            new String[] { "23", "CE", "Ceará" },
            new String[] { "24", "RN", "Rio Grande do Norte" },
            new String[] { "25", "PB", "Paraíba" },
            new String[] { "26", "PE", "Pernambuco" },
            new String[] { "27", "AL", "Alagoas" },
            new String[] { "28", "SE", "Sergipe" },
            new String[] { "29", "BA", "Bahia" },
            new String[] { "31", "MG", "Minas Gerais" },
            new String[] { "32", "ES", "Espírito Santo" },
            new String[] { "33", "RJ", "Rio de Janeiro" },
            new String[] { "35", "SP", "São Paulo" },
            new String[] { "41", "PR", "Paraná" },
            new String[] { "42", "SC", "Santa Catarina" },
            new String[] { "43", "RS", "Rio Grande do Sul" },
            new String[] { "50", "MS", "Mato Grosso do Sul" },
            new String[] { "51", "MT", "Mato Grosso" },
            new String[] { "52", "GO", "Goiás" },
            new String[] { "53", "DF", "Distrito Federal" });

    private final StateRepository stateRepository;

    @Override
    public void run(String... args) {
        int created = 0;
        for (String[] entry : STATES) {
            // Por sigla, não por contagem: uma carga interrompida no meio deixaria
            // a tabela parcialmente preenchida, e "já tem alguma coisa" faria o
            // seeder pular o que falta.
            if (stateRepository.findByAcronymIgnoreCase(entry[1]).isPresent()) {
                continue;
            }
            State state = new State();
            state.setIbgeCode(entry[0]);
            state.setAcronym(entry[1]);
            state.setName(entry[2]);
            stateRepository.save(state);
            created++;
        }

        if (created > 0) {
            log.info("Database Seeder: {} unidade(s) federativa(s) cadastrada(s)", created);
        }
    }
}

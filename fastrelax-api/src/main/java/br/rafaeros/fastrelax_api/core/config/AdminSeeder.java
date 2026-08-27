package br.rafaeros.fastrelax_api.core.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.features.users.User;
import br.rafaeros.fastrelax_api.features.users.UserRepository;
import br.rafaeros.fastrelax_api.features.users.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cria o primeiro SYSADMIN, sem o qual não há como cadastrar a primeira empresa
 * — e sem empresa não existe nenhum outro usuário possível.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@fastrelax.com";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (userRepository.findByEmail(ADMIN_EMAIL).isPresent()) {
            log.info("Database Seeder: SYSADMIN já existe. Nenhuma ação necessária.");
            return;
        }

        log.info("Iniciando o Database Seeder: criando SYSADMIN padrão...");

        User admin = new User();
        admin.setName("Administrador da Plataforma");
        admin.setEmail(ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode("admin123"));
        admin.setRole(UserRole.SYSADMIN);
        // Sem empresa: a equipe da plataforma não pertence a cliente nenhum, e a
        // constraint chk_users_company_role exige exatamente isso.
        admin.setCompany(null);
        // Senha do seed é pública neste arquivo: obriga a definir uma própria
        // antes de liberar qualquer outra rota.
        admin.setMustChangePassword(true);
        userRepository.save(admin);

        log.warn("SYSADMIN criado com senha temporária 'admin123'. "
                + "Defina uma nova senha no primeiro acesso.");
    }
}

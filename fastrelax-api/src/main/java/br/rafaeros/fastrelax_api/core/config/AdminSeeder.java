package br.rafaeros.fastrelax_api.core.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import br.rafaeros.fastrelax_api.features.users.User;
import br.rafaeros.fastrelax_api.features.users.UserRepository;
import br.rafaeros.fastrelax_api.features.users.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String adminEmail = "admin@fastrelax.com";
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            log.info("Iniciando o Database Seeder: Criando usuário ADMIN padrão...");

            User admin = new User();
            admin.setName("Administrador do Sistema");
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode("admin123"));
            admin.setRole(UserRole.ADMIN);
            // Senha do seed é pública neste arquivo: obriga a definir uma própria
            // antes de liberar qualquer outra rota.
            admin.setMustChangePassword(true);
            userRepository.save(admin);

            log.warn("Usuário ADMIN criado com senha temporária 'admin123'. "
                    + "Defina uma nova senha no primeiro acesso.");
        } else {
            log.info("Database Seeder: Usuário ADMIN já existe. Nenhuma ação necessária.");
        }
    }
}
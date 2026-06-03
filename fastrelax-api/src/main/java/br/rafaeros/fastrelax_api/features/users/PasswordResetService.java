package br.rafaeros.fastrelax_api.features.users;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Injeção via construtor
    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void updatePassword(String email, String novaSenha) {
        User user = userRepository.findByEmail(email).orElseThrow();
        
        // Criptografando a nova senha antes de salvar
        String novoHash = passwordEncoder.encode(novaSenha);
        user.setPasswordHash(novoHash);
        
        userRepository.save(user);
    }
}
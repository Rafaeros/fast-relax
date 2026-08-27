package br.rafaeros.fastrelax_api.features.collaborators;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.rafaeros.fastrelax_api.core.security.CredentialService;
import br.rafaeros.fastrelax_api.features.users.dtos.ChangePasswordRequestDTO;
import br.rafaeros.fastrelax_api.features.users.dtos.FirstAccessPasswordRequestDTO;
import lombok.RequiredArgsConstructor;

/**
 * Senha do colaborador.
 *
 * <p>
 * As regras — confirmação, senha diferente da atual, revogação das sessões —
 * vivem no {@link CredentialService} e são as mesmas do painel. O que sobra aqui
 * é resolver de quem é a credencial e gravar: um serviço por dono, uma regra
 * para os dois.
 */
@Service
@RequiredArgsConstructor
public class CollaboratorPasswordService {

    private final CollaboratorRepository collaboratorRepository;
    private final CollaboratorService collaboratorService;
    private final CredentialService credentialService;

    /** Primeiro acesso: troca a temporária entregue pelo RH e destrava o app. */
    @Transactional
    public void defineFirstAccessPassword(FirstAccessPasswordRequestDTO dto) {
        Collaborator collaborator = collaboratorService.requireAuthenticatedEntity();
        credentialService.defineFirstAccessPassword(collaborator, dto.newPassword(), dto.confirmNewPassword());
        collaboratorRepository.save(collaborator);
    }

    /** Troca da própria senha, conferindo a atual. */
    @Transactional
    public void changeOwnPassword(ChangePasswordRequestDTO dto) {
        Collaborator collaborator = collaboratorService.requireAuthenticatedEntity();
        credentialService.changePassword(collaborator, dto.currentPassword(), dto.newPassword(),
                dto.confirmNewPassword());
        collaboratorRepository.save(collaborator);
    }
}

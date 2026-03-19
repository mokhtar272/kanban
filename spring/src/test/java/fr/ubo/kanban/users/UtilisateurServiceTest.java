package fr.ubo.kanban.users;

import fr.ubo.kanban.common.dto.ApiResponse;
import fr.ubo.kanban.users.entity.Utilisateur;
import fr.ubo.kanban.users.repository.UtilisateurRepository;
import fr.ubo.kanban.users.service.UtilisateurService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires — UtilisateurService
 * On mocke le repository pour ne pas toucher la base de données.
 */
@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository repo;

    @InjectMocks
    private UtilisateurService service;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    private Utilisateur utilisateurTest;

    @BeforeEach
    void setUp() {
        utilisateurTest = new Utilisateur();
        utilisateurTest.setId(1);
        utilisateurTest.setPseudo("alice");
        utilisateurTest.setEmail("alice@test.fr");
        utilisateurTest.setPassword(encoder.encode("MotDePasse123!"));
        utilisateurTest.setRole(Utilisateur.Role.USER);
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register — succès avec des données valides")
    void register_succes() {
        when(repo.existsByPseudo("alice")).thenReturn(false);
        when(repo.existsByEmail("alice@test.fr")).thenReturn(false);
        when(repo.save(any())).thenReturn(utilisateurTest);

        ApiResponse<Utilisateur> res = service.register(Map.of(
            "pseudo",   "alice",
            "email",    "alice@test.fr",
            "password", "MotDePasse123!"
        ));

        assertTrue(res.isSuccess());
        assertNotNull(res.getData());
        assertEquals("alice", res.getData().getPseudo());
        verify(repo, times(1)).save(any());
    }

    @Test
    @DisplayName("register — échec si pseudo déjà utilisé")
    void register_pseudoDejaUtilise() {
        when(repo.existsByPseudo("alice")).thenReturn(true);

        ApiResponse<Utilisateur> res = service.register(Map.of(
            "pseudo",   "alice",
            "email",    "nouveau@test.fr",
            "password", "MotDePasse123!"
        ));

        assertFalse(res.isSuccess());
        assertNotNull(res.getMessage());
        assertTrue(res.getMessage().toLowerCase().contains("pseudo"));
        // Le save ne doit jamais être appelé
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("register — échec si email déjà utilisé")
    void register_emailDejaUtilise() {
        when(repo.existsByPseudo("nouveau")).thenReturn(false);
        when(repo.existsByEmail("alice@test.fr")).thenReturn(true);

        ApiResponse<Utilisateur> res = service.register(Map.of(
            "pseudo",   "nouveau",
            "email",    "alice@test.fr",
            "password", "MotDePasse123!"
        ));

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("email"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("register — le mot de passe est hashé en base (jamais en clair)")
    void register_motDePasseHashe() {
        when(repo.existsByPseudo(any())).thenReturn(false);
        when(repo.existsByEmail(any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Utilisateur> res = service.register(Map.of(
            "pseudo",   "bob",
            "email",    "bob@test.fr",
            "password", "MotDePasse123!"
        ));

        assertTrue(res.isSuccess());
        // Le mot de passe stocké ne doit PAS être en clair
        assertNotEquals("MotDePasse123!", res.getData().getPassword());
        // Il doit être un hash BCrypt valide
        assertTrue(encoder.matches("MotDePasse123!", res.getData().getPassword()));
    }

    // ── VERIFY (LOGIN) ────────────────────────────────────────────────────────

    @Test
    @DisplayName("verify — succès avec bon pseudo et bon mot de passe")
    void verify_succes() {
        when(repo.findByPseudo("alice")).thenReturn(Optional.of(utilisateurTest));

        ApiResponse<Utilisateur> res = service.verify(Map.of(
            "pseudo",   "alice",
            "password", "MotDePasse123!"
        ));

        assertTrue(res.isSuccess());
        assertEquals("alice", res.getData().getPseudo());
    }

    @Test
    @DisplayName("verify — échec avec mauvais mot de passe")
    void verify_mauvaisMotDePasse() {
        when(repo.findByPseudo("alice")).thenReturn(Optional.of(utilisateurTest));

        ApiResponse<Utilisateur> res = service.verify(Map.of(
            "pseudo",   "alice",
            "password", "mauvaisMotDePasse"
        ));

        assertFalse(res.isSuccess());
        assertNotNull(res.getMessage());
    }

    @Test
    @DisplayName("verify — échec si pseudo inexistant")
    void verify_pseudoInexistant() {
        when(repo.findByPseudo("inconnu")).thenReturn(Optional.empty());

        ApiResponse<Utilisateur> res = service.verify(Map.of(
            "pseudo",   "inconnu",
            "password", "MotDePasse123!"
        ));

        assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("verify — retourne null dans data en cas d'échec (pas de fuite d'info)")
    void verify_pasDeFuiteInfo() {
        when(repo.findByPseudo(any())).thenReturn(Optional.empty());

        ApiResponse<Utilisateur> res = service.verify(Map.of(
            "pseudo",   "hacker",
            "password", "tentative"
        ));

        assertFalse(res.isSuccess());
        assertNull(res.getData(), "data doit être null en cas d'échec d'auth");
    }
}

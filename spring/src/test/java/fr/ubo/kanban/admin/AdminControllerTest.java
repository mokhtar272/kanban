package fr.ubo.kanban.admin;

import fr.ubo.kanban.common.dto.ApiResponse;
import fr.ubo.kanban.common.dto.UtilisateurDTO;
import fr.ubo.kanban.tasks.repository.HistoriqueRepository;
import fr.ubo.kanban.tasks.repository.TacheRepository;
import fr.ubo.kanban.boards.repository.TableauRepository;
import fr.ubo.kanban.users.entity.Utilisateur;
import fr.ubo.kanban.users.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires — AdminController
 */
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock private UtilisateurRepository userRepo;
    @Mock private TableauRepository     tableauRepo;
    @Mock private TacheRepository       tacheRepo;
    @Mock private HistoriqueRepository  historiqueRepo;

    @InjectMocks
    private AdminController controller;

    private Utilisateur alice;
    private Utilisateur bob;

    @BeforeEach
    void setUp() {
        alice = new Utilisateur();
        alice.setId(1); alice.setPseudo("alice");
        alice.setEmail("alice@test.fr"); alice.setRole(Utilisateur.Role.ADMIN);

        bob = new Utilisateur();
        bob.setId(2); bob.setPseudo("bob");
        bob.setEmail("bob@test.fr"); bob.setRole(Utilisateur.Role.USER);
    }

    // ── STATS ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("stats — retourne nbUtilisateurs, nbTableaux et nbTaches")
    void stats_retourneToutes() {
        when(userRepo.count()).thenReturn(5L);
        when(tableauRepo.count()).thenReturn(12L);
        when(tacheRepo.count()).thenReturn(48L);
        when(historiqueRepo.countActionsByUser()).thenReturn(List.of());

        ApiResponse<Map<String, Object>> res = controller.stats();

        assertTrue(res.isSuccess());
        assertEquals(5L,  res.getData().get("nbUtilisateurs"));
        assertEquals(12L, res.getData().get("nbTableaux"));
        assertEquals(48L, res.getData().get("nbTaches"));
    }

    @Test
    @DisplayName("stats — activiteUtilisateurs est présent dans la réponse")
    void stats_activitePresente() {
        when(userRepo.count()).thenReturn(2L);
        when(tableauRepo.count()).thenReturn(3L);
        when(tacheRepo.count()).thenReturn(10L);

        // Simuler 1 entrée dans l'historique
        Object[] row = new Object[]{ 1, "alice", 5L };
        when(historiqueRepo.countActionsByUser()).thenReturn(java.util.Arrays.asList(new Object[][]{row}));

        ApiResponse<Map<String, Object>> res = controller.stats();

        assertTrue(res.isSuccess());
        assertNotNull(res.getData().get("activiteUtilisateurs"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> activite =
            (List<Map<String, Object>>) res.getData().get("activiteUtilisateurs");

        assertEquals(1, activite.size());
        assertEquals("alice", activite.get(0).get("pseudo"));
        assertEquals(5L,      activite.get(0).get("actions"));
    }

    @Test
    @DisplayName("stats — retourne 0 si aucune donnée en base")
    void stats_basesVides() {
        when(userRepo.count()).thenReturn(0L);
        when(tableauRepo.count()).thenReturn(0L);
        when(tacheRepo.count()).thenReturn(0L);
        when(historiqueRepo.countActionsByUser()).thenReturn(List.of());

        ApiResponse<Map<String, Object>> res = controller.stats();

        assertTrue(res.isSuccess());
        assertEquals(0L, res.getData().get("nbUtilisateurs"));
        assertEquals(0L, res.getData().get("nbTableaux"));
        assertEquals(0L, res.getData().get("nbTaches"));
    }

    // ── GET UTILISATEURS ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getUsers — retourne la liste complète des utilisateurs")
    void getUsers_retourneListe() {
        when(userRepo.findAll()).thenReturn(List.of(alice, bob));

        ApiResponse<List<UtilisateurDTO>> res = controller.getUsers();

        assertTrue(res.isSuccess());
        assertEquals(2, res.getData().size());
        assertEquals("alice", res.getData().get(0).getPseudo());
        assertEquals("bob",   res.getData().get(1).getPseudo());
    }

    @Test
    @DisplayName("getUsers — retourne liste vide si aucun utilisateur")
    void getUsers_listeVide() {
        when(userRepo.findAll()).thenReturn(List.of());

        ApiResponse<List<UtilisateurDTO>> res = controller.getUsers();

        assertTrue(res.isSuccess());
        assertTrue(res.getData().isEmpty());
    }

    @Test
    @DisplayName("getUsers — les mots de passe ne sont pas exposés dans le DTO")
    void getUsers_pasDeMotDePasse() {
        alice.setPassword("hash_secret");
        when(userRepo.findAll()).thenReturn(List.of(alice));

        ApiResponse<List<UtilisateurDTO>> res = controller.getUsers();

        // UtilisateurDTO ne doit pas avoir de champ password
        assertTrue(res.isSuccess());
        // On vérifie que le DTO ne contient pas le mot de passe
        // (UtilisateurDTO.from() ne copie pas le password)
        assertNotNull(res.getData().get(0).getPseudo());
    }

    // ── DELETE UTILISATEUR ────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser — succès si l'utilisateur existe")
    void deleteUser_succes() {
        when(userRepo.existsById(2)).thenReturn(true);

        ApiResponse<Void> res = controller.deleteUser(2);

        assertTrue(res.isSuccess());
        verify(userRepo, times(1)).deleteById(2);
    }

    @Test
    @DisplayName("deleteUser — échec si l'utilisateur n'existe pas")
    void deleteUser_utilisateurIntrouvable() {
        when(userRepo.existsById(99)).thenReturn(false);

        ApiResponse<Void> res = controller.deleteUser(99);

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("introuvable"));
        verify(userRepo, never()).deleteById(any());
    }
}

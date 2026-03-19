package fr.ubo.kanban.boards;

import fr.ubo.kanban.boards.entity.Tableau;
import fr.ubo.kanban.boards.entity.TableauMembre;
import fr.ubo.kanban.boards.repository.TableauMembreRepository;
import fr.ubo.kanban.boards.repository.TableauRepository;
import fr.ubo.kanban.boards.service.TableauService;
import fr.ubo.kanban.common.dto.ApiResponse;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires — TableauService
 */
@ExtendWith(MockitoExtension.class)
class TableauServiceTest {

    @Mock private TableauRepository      tableauRepo;
    @Mock private TableauMembreRepository membreRepo;
    @Mock private UtilisateurRepository  userRepo;

    @InjectMocks
    private TableauService service;

    private Utilisateur alice;
    private Utilisateur bob;
    private Tableau     tableau;

    @BeforeEach
    void setUp() {
        alice = new Utilisateur();
        alice.setId(1); alice.setPseudo("alice"); alice.setRole(Utilisateur.Role.USER);

        bob = new Utilisateur();
        bob.setId(2); bob.setPseudo("bob"); bob.setRole(Utilisateur.Role.USER);

        tableau = new Tableau();
        tableau.setId(10); tableau.setTitre("Mon Kanban"); tableau.setCouleur("#3B82F6");
        tableau.setCreateur(alice);
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — succès : tableau créé avec le créateur comme OWNER")
    void create_succes() {
        when(userRepo.findById(1)).thenReturn(Optional.of(alice));
        when(tableauRepo.save(any())).thenReturn(tableau);
        lenient().when(tableauRepo.findById(10)).thenReturn(Optional.of(tableau));
        when(membreRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Tableau> res = service.create(
            Map.of("titre", "Mon Kanban", "couleur", "#3B82F6"), 1
        );

        assertTrue(res.isSuccess());
        assertNotNull(res.getData());
        // Le créateur doit être sauvegardé comme OWNER
        verify(membreRepo, times(1)).save(argThat(m ->
            m.getRole() == TableauMembre.Role.OWNER
        ));
    }

    @Test
    @DisplayName("create — échec si utilisateur introuvable")
    void create_utilisateurIntrouvable() {
        when(userRepo.findById(99)).thenReturn(Optional.empty());

        ApiResponse<Tableau> res = service.create(
            Map.of("titre", "Tableau"), 99
        );

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("utilisateur"));
        verify(tableauRepo, never()).save(any());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — succès : titre et couleur modifiés")
    void update_succes() {
        lenient().when(tableauRepo.findById(10)).thenReturn(Optional.of(tableau));
        when(tableauRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Tableau> res = service.update(10, Map.of(
            "titre",   "Nouveau Titre",
            "couleur", "#EC4899"
        ));

        assertTrue(res.isSuccess());
        assertEquals("Nouveau Titre", res.getData().getTitre());
        assertEquals("#EC4899",       res.getData().getCouleur());
    }

    @Test
    @DisplayName("update — échec si tableau introuvable")
    void update_tableauIntrouvable() {
        when(tableauRepo.findById(99)).thenReturn(Optional.empty());

        ApiResponse<Tableau> res = service.update(99, Map.of("titre", "Test"));

        assertFalse(res.isSuccess());
        verify(tableauRepo, never()).save(any());
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — appelle deleteById sur le repository")
    void delete_appelleRepository() {
        service.delete(10);
        verify(tableauRepo, times(1)).deleteById(10);
    }

    // ── INVITER ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("inviter — succès : nouveau membre ajouté avec rôle EDITOR")
    void inviter_succes() {
        lenient().when(tableauRepo.findById(10)).thenReturn(Optional.of(tableau));
        when(userRepo.findByPseudo("bob")).thenReturn(Optional.of(bob));
        when(membreRepo.existsByTableauIdAndUtilisateurId(10, 2)).thenReturn(false);
        when(membreRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<TableauMembre> res = service.inviter(10, Map.of(
            "pseudo", "bob",
            "role",   "EDITOR"
        ));

        assertTrue(res.isSuccess());
        assertEquals(TableauMembre.Role.EDITOR, res.getData().getRole());
    }

    @Test
    @DisplayName("inviter — échec si l'utilisateur est déjà membre")
    void inviter_dejaMembre() {
        lenient().when(tableauRepo.findById(10)).thenReturn(Optional.of(tableau));
        when(userRepo.findByPseudo("bob")).thenReturn(Optional.of(bob));
        when(membreRepo.existsByTableauIdAndUtilisateurId(10, 2)).thenReturn(true);

        ApiResponse<TableauMembre> res = service.inviter(10, Map.of(
            "pseudo", "bob",
            "role",   "EDITOR"
        ));

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("membre"));
        verify(membreRepo, never()).save(any());
    }

    @Test
    @DisplayName("inviter — échec si pseudo introuvable")
    void inviter_pseudoInexistant() {
        lenient().when(tableauRepo.findById(10)).thenReturn(Optional.of(tableau));
        when(userRepo.findByPseudo("inconnu")).thenReturn(Optional.empty());

        ApiResponse<TableauMembre> res = service.inviter(10, Map.of(
            "pseudo", "inconnu",
            "role",   "EDITOR"
        ));

        assertFalse(res.isSuccess());
    }

    // ── GET MES TABLEAUX ──────────────────────────────────────────────────────

    @Test
    @DisplayName("getMesTableaux — retourne la liste des tableaux de l'utilisateur")
    void getMesTableaux_retourneListe() {
        when(tableauRepo.findByMembreUserId(1)).thenReturn(List.of(tableau));

        List<Tableau> result = service.getMesTableaux(1);

        assertEquals(1, result.size());
        assertEquals("Mon Kanban", result.get(0).getTitre());
    }

    @Test
    @DisplayName("getMesTableaux — retourne liste vide si aucun tableau")
    void getMesTableaux_listeVide() {
        when(tableauRepo.findByMembreUserId(99)).thenReturn(List.of());

        List<Tableau> result = service.getMesTableaux(99);

        assertTrue(result.isEmpty());
    }
}

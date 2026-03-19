package fr.ubo.kanban.tasks;

import fr.ubo.kanban.boards.entity.Tableau;
import fr.ubo.kanban.boards.repository.TableauRepository;
import fr.ubo.kanban.columns.entity.Colonne;
import fr.ubo.kanban.columns.repository.ColonneRepository;
import fr.ubo.kanban.common.dto.ApiResponse;
import fr.ubo.kanban.tasks.entity.Historique;
import fr.ubo.kanban.tasks.entity.Tache;
import fr.ubo.kanban.tasks.repository.HistoriqueRepository;
import fr.ubo.kanban.tasks.repository.TacheRepository;
import fr.ubo.kanban.tasks.service.TacheService;
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
 * Tests unitaires — TacheService
 */
@ExtendWith(MockitoExtension.class)
class TacheServiceTest {

    @Mock private TacheRepository       tacheRepo;
    @Mock private ColonneRepository     colonneRepo;
    @Mock private UtilisateurRepository userRepo;
    @Mock private HistoriqueRepository  historiqueRepo;
    @Mock private TableauRepository     tableauRepo;

    @InjectMocks
    private TacheService service;

    private Utilisateur alice;
    private Tableau     tableau;
    private Colonne     colonne;
    private Tache       tache;

    @BeforeEach
    void setUp() {
        alice = new Utilisateur();
        alice.setId(1); alice.setPseudo("alice");

        tableau = new Tableau();
        tableau.setId(1); tableau.setTitre("Kanban");

        colonne = new Colonne();
        colonne.setId(10); colonne.setTitre("À faire"); colonne.setTableau(tableau);

        tache = new Tache();
        tache.setId(100); tache.setTitre("Ma tâche");
        tache.setPriorite(Tache.Priorite.NORMALE); tache.setColonne(colonne);
        tache.setCreateur(alice); tache.setPosition(0);
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — succès avec titre et colonne valides")
    void create_succes() {
        when(colonneRepo.findById(10)).thenReturn(Optional.of(colonne));
        when(userRepo.findById(1)).thenReturn(Optional.of(alice));
        when(tacheRepo.findByColonneIdOrderByPositionAsc(10)).thenReturn(List.of());
        when(tacheRepo.save(any())).thenReturn(tache);
        when(historiqueRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Tache> res = service.create(Map.of(
            "titre",     "Ma tâche",
            "idColonne", 10
        ), 1);

        assertTrue(res.isSuccess());
        assertNotNull(res.getData());
        verify(tacheRepo, times(1)).save(any());
        // Un historique CREATION doit être enregistré
        verify(historiqueRepo, times(1)).save(argThat(h ->
            "CREATION".equals(h.getAction())
        ));
    }

    @Test
    @DisplayName("create — échec si titre vide")
    void create_titreVide() {
        ApiResponse<Tache> res = service.create(Map.of(
            "titre",     "",
            "idColonne", 10
        ), 1);

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("titre"));
        verify(tacheRepo, never()).save(any());
    }

    @Test
    @DisplayName("create — échec si colonne introuvable")
    void create_colonneIntrouvable() {
        when(colonneRepo.findById(99)).thenReturn(Optional.empty());

        ApiResponse<Tache> res = service.create(Map.of(
            "titre",     "Tâche test",
            "idColonne", 99
        ), 1);

        assertFalse(res.isSuccess());
        verify(tacheRepo, never()).save(any());
    }

    @Test
    @DisplayName("create — échec si idColonne manquant")
    void create_colonneManquante() {
        ApiResponse<Tache> res = service.create(Map.of(
            "titre", "Tâche sans colonne"
        ), 1);

        assertFalse(res.isSuccess());
    }

    @Test
    @DisplayName("create — priorité invalide retourne une erreur")
    void create_prioriteInvalide() {
        when(colonneRepo.findById(10)).thenReturn(Optional.of(colonne));
        when(userRepo.findById(1)).thenReturn(Optional.of(alice));

        ApiResponse<Tache> res = service.create(Map.of(
            "titre",     "Tâche",
            "idColonne", 10,
            "priorite",  "INVALIDE"
        ), 1);

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("priorité"));
    }

    @Test
    @DisplayName("create — date limite invalide retourne une erreur")
    void create_dateLimiteInvalide() {
        when(colonneRepo.findById(10)).thenReturn(Optional.of(colonne));
        when(userRepo.findById(1)).thenReturn(Optional.of(alice));

        ApiResponse<Tache> res = service.create(Map.of(
            "titre",      "Tâche",
            "idColonne",  10,
            "dateLimite", "pas-une-date"
        ), 1);

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("date"));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — succès : titre modifié")
    void update_titreModifie() {
        when(tacheRepo.findById(100)).thenReturn(Optional.of(tache));
        when(tacheRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Tache> res = service.update(100, Map.of("titre", "Nouveau titre"), 1);

        assertTrue(res.isSuccess());
        assertEquals("Nouveau titre", res.getData().getTitre());
    }

    @Test
    @DisplayName("update — déplacement de colonne enregistre un historique DEPLACEMENT")
    void update_deplacementEnregistreHistorique() {
        Colonne colonneDestination = new Colonne();
        colonneDestination.setId(20); colonneDestination.setTitre("En cours");
        colonneDestination.setTableau(tableau);

        when(tacheRepo.findById(100)).thenReturn(Optional.of(tache));
        when(colonneRepo.findById(20)).thenReturn(Optional.of(colonneDestination));
        when(userRepo.findById(1)).thenReturn(Optional.of(alice));
        when(tacheRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(historiqueRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Tache> res = service.update(100, Map.of("idColonne", 20), 1);

        assertTrue(res.isSuccess());
        assertEquals(colonneDestination, res.getData().getColonne());
        verify(historiqueRepo, times(1)).save(argThat(h ->
            "DEPLACEMENT".equals(h.getAction())
        ));
    }

    @Test
    @DisplayName("update — titre vide retourne une erreur")
    void update_titreVide() {
        when(tacheRepo.findById(100)).thenReturn(Optional.of(tache));

        ApiResponse<Tache> res = service.update(100, Map.of("titre", ""), 1);

        assertFalse(res.isSuccess());
        verify(tacheRepo, never()).save(any());
    }

    @Test
    @DisplayName("update — tâche introuvable retourne une erreur")
    void update_tacheIntrouvable() {
        when(tacheRepo.findById(999)).thenReturn(Optional.empty());

        ApiResponse<Tache> res = service.update(999, Map.of("titre", "Test"), 1);

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("introuvable"));
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — appelle deleteById sur le repository")
    void delete_appelleRepository() {
        service.delete(100);
        verify(tacheRepo, times(1)).deleteById(100);
    }

    // ── SEARCH ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search — appelle le repository avec le bon tableau et le bon mot-clé")
    void search_appelleRepository() {
        when(tacheRepo.search(1, "bug")).thenReturn(List.of(tache));

        List<Tache> result = service.search(1, "bug");

        assertEquals(1, result.size());
        verify(tacheRepo, times(1)).search(1, "bug");
    }

    @Test
    @DisplayName("search — query vide retourne toutes les tâches du tableau")
    void search_queryVide() {
        when(tacheRepo.findByTableauId(1)).thenReturn(List.of(tache));

        List<Tache> result = service.search(1, "");

        assertEquals(1, result.size());
        verify(tacheRepo, times(1)).findByTableauId(1);
        verify(tacheRepo, never()).search(any(), any());
    }

    // ── FILTER ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("filter — par priorité HAUTE retourne les bonnes tâches")
    void filter_parPriorite() {
        tache.setPriorite(Tache.Priorite.HAUTE);
        when(tacheRepo.findByTableauAndPriorite(1, Tache.Priorite.HAUTE))
            .thenReturn(List.of(tache));

        List<Tache> result = service.filter(1, "HAUTE", null);

        assertEquals(1, result.size());
        assertEquals(Tache.Priorite.HAUTE, result.get(0).getPriorite());
    }

    @Test
    @DisplayName("filter — par assigné retourne les tâches assignées à cet utilisateur")
    void filter_parAssigne() {
        tache.setAssigne(alice);
        when(tacheRepo.findByTableauAndAssigne(1, 1)).thenReturn(List.of(tache));

        List<Tache> result = service.filter(1, null, 1);

        assertEquals(1, result.size());
        assertEquals(alice, result.get(0).getAssigne());
    }

    @Test
    @DisplayName("filter — priorité invalide lève une IllegalArgumentException")
    void filter_prioriteInvalide() {
        assertThrows(IllegalArgumentException.class, () ->
            service.filter(1, "INEXISTANTE", null)
        );
    }

    // ── HISTORIQUE ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getHistorique — retourne l'historique du tableau trié par date")
    void getHistorique_retourneListe() {
        Historique h = new Historique();
        h.setAction("CREATION"); h.setDescription("Tâche créée"); h.setUser(alice);
        when(historiqueRepo.findByTableauIdOrderByCreatedAtDesc(1)).thenReturn(List.of(h));

        List<Historique> result = service.getHistorique(1);

        assertEquals(1, result.size());
        assertEquals("CREATION", result.get(0).getAction());
    }
}

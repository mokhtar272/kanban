package fr.ubo.kanban.columns;

import fr.ubo.kanban.boards.entity.Tableau;
import fr.ubo.kanban.boards.repository.TableauRepository;
import fr.ubo.kanban.columns.entity.Colonne;
import fr.ubo.kanban.columns.repository.ColonneRepository;
import fr.ubo.kanban.columns.service.ColonneService;
import fr.ubo.kanban.common.dto.ApiResponse;
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
 * Tests unitaires — ColonneService
 */
@ExtendWith(MockitoExtension.class)
class ColonneServiceTest {

    @Mock private ColonneRepository  colonneRepo;
    @Mock private TableauRepository  tableauRepo;

    @InjectMocks
    private ColonneService service;

    private Tableau tableau;
    private Colonne colonne;

    @BeforeEach
    void setUp() {
        tableau = new Tableau();
        tableau.setId(1); tableau.setTitre("Mon tableau");

        colonne = new Colonne();
        colonne.setId(10); colonne.setTitre("À faire");
        colonne.setCouleur("#3B82F6"); colonne.setPosition(0);
        colonne.setTableau(tableau);
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — succès avec titre et couleur")
    void create_succes() {
        when(tableauRepo.findById(1)).thenReturn(Optional.of(tableau));
        when(colonneRepo.countByTableauId(1)).thenReturn(0L);
        when(colonneRepo.save(any())).thenReturn(colonne);

        ApiResponse<Colonne> res = service.create(1, Map.of(
            "titre",   "À faire",
            "couleur", "#3B82F6"
        ));

        assertTrue(res.isSuccess());
        assertNotNull(res.getData());
        assertEquals("À faire", res.getData().getTitre());
        verify(colonneRepo, times(1)).save(any());
    }

    @Test
    @DisplayName("create — la position est calculée automatiquement (nb colonnes existantes)")
    void create_positionAutomatique() {
        when(tableauRepo.findById(1)).thenReturn(Optional.of(tableau));
        when(colonneRepo.countByTableauId(1)).thenReturn(3L); // 3 colonnes déjà présentes
        when(colonneRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(1, Map.of("titre", "Nouvelle colonne"));

        verify(colonneRepo).save(argThat(c -> c.getPosition() == 3));
    }

    @Test
    @DisplayName("create — échec si tableau introuvable")
    void create_tableauIntrouvable() {
        when(tableauRepo.findById(99)).thenReturn(Optional.empty());

        ApiResponse<Colonne> res = service.create(99, Map.of("titre", "Test"));

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("tableau"));
        verify(colonneRepo, never()).save(any());
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("update — succès : titre et couleur modifiés")
    void update_succes() {
        when(colonneRepo.findById(10)).thenReturn(Optional.of(colonne));
        when(colonneRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Colonne> res = service.update(10, Map.of(
            "titre",   "En cours",
            "couleur", "#10B981"
        ));

        assertTrue(res.isSuccess());
        assertEquals("En cours", res.getData().getTitre());
        assertEquals("#10B981",  res.getData().getCouleur());
    }

    @Test
    @DisplayName("update — échec si colonne introuvable")
    void update_colonneIntrouvable() {
        when(colonneRepo.findById(99)).thenReturn(Optional.empty());

        ApiResponse<Colonne> res = service.update(99, Map.of("titre", "Test"));

        assertFalse(res.isSuccess());
        verify(colonneRepo, never()).save(any());
    }

    @Test
    @DisplayName("update — seul le titre est modifié si couleur absente")
    void update_seulementTitre() {
        when(colonneRepo.findById(10)).thenReturn(Optional.of(colonne));
        when(colonneRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Colonne> res = service.update(10, Map.of("titre", "Terminé"));

        assertTrue(res.isSuccess());
        assertEquals("Terminé",   res.getData().getTitre());
        assertEquals("#3B82F6",   res.getData().getCouleur()); // couleur inchangée
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — appelle deleteById sur le repository")
    void delete_appelleRepository() {
        service.delete(10);
        verify(colonneRepo, times(1)).deleteById(10);
    }

    // ── GET BY TABLEAU ────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByTableau — retourne les colonnes triées par position")
    void getByTableau_retourneListe() {
        Colonne c2 = new Colonne();
        c2.setId(11); c2.setTitre("En cours"); c2.setPosition(1);

        when(colonneRepo.findByTableauIdOrderByPositionAsc(1))
            .thenReturn(List.of(colonne, c2));

        List<Colonne> result = service.getByTableau(1);

        assertEquals(2, result.size());
        assertEquals("À faire",  result.get(0).getTitre());
        assertEquals("En cours", result.get(1).getTitre());
    }
}

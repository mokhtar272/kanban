package fr.ubo.kanban.comments;

import fr.ubo.kanban.comments.document.Commentaire;
import fr.ubo.kanban.comments.repository.CommentaireRepository;
import fr.ubo.kanban.comments.service.CommentaireService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires — CommentaireService (MongoDB)
 */
@ExtendWith(MockitoExtension.class)
class CommentaireServiceTest {

    @Mock
    private CommentaireRepository repo;

    @InjectMocks
    private CommentaireService service;

    private Commentaire commentaireTest;

    @BeforeEach
    void setUp() {
        commentaireTest = new Commentaire();
        commentaireTest.setId("mongo_id_123");
        commentaireTest.setTacheId(7);
        commentaireTest.setAuteurPseudo("alice");
        commentaireTest.setContenu("Commentaire de test");
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("create — succès avec contenu et auteur valides")
    void create_succes() {
        when(repo.save(any())).thenReturn(commentaireTest);

        ApiResponse<Commentaire> res = service.create(7, Map.of(
            "contenu",      "Commentaire de test",
            "auteurPseudo", "alice"
        ));

        assertTrue(res.isSuccess());
        assertNotNull(res.getData());
        assertEquals("alice",               res.getData().getAuteurPseudo());
        assertEquals("Commentaire de test", res.getData().getContenu());
        verify(repo, times(1)).save(any());
    }

    @Test
    @DisplayName("create — échec si contenu vide")
    void create_contenuVide() {
        ApiResponse<Commentaire> res = service.create(7, Map.of(
            "contenu",      "",
            "auteurPseudo", "alice"
        ));

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("contenu"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("create — échec si contenu absent")
    void create_contenuAbsent() {
        ApiResponse<Commentaire> res = service.create(7, Map.of(
            "auteurPseudo", "alice"
        ));

        assertFalse(res.isSuccess());
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("create — échec si auteurPseudo absent")
    void create_auteurAbsent() {
        ApiResponse<Commentaire> res = service.create(7, Map.of(
            "contenu", "Commentaire sans auteur"
        ));

        assertFalse(res.isSuccess());
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("create — avec pièces jointes valides, les pièces sont stockées")
    void create_avecPiecesJointes() {
        Commentaire avecPJ = new Commentaire();
        avecPJ.setId("abc"); avecPJ.setContenu("Voir le fichier");
        avecPJ.setAuteurPseudo("alice");
        avecPJ.setPiecesJointes(List.of(
            new Commentaire.PieceJointe("doc.pdf", "application/pdf", 1024L, "base64data==")
        ));
        when(repo.save(any())).thenReturn(avecPJ);

        ApiResponse<Commentaire> res = service.create(7, Map.of(
            "contenu",      "Voir le fichier",
            "auteurPseudo", "alice",
            "piecesJointes", List.of(Map.of(
                "nom",    "doc.pdf",
                "type",   "application/pdf",
                "taille", 1024,
                "data",   "base64data=="
            ))
        ));

        assertTrue(res.isSuccess());
        assertEquals(1, res.getData().getPiecesJointes().size());
        assertEquals("doc.pdf", res.getData().getPiecesJointes().get(0).getNom());
    }

    @Test
    @DisplayName("create — pièce jointe trop grande (> 3 Mo) retourne une erreur")
    void create_pieceJointeTropGrande() {
        // Générer une chaîne base64 de plus de 4.1 millions de caractères
        String dataTropGrosse = "A".repeat(4_200_000);

        ApiResponse<Commentaire> res = service.create(7, Map.of(
            "contenu",      "Test",
            "auteurPseudo", "alice",
            "piecesJointes", List.of(Map.of(
                "nom",    "gros_fichier.zip",
                "type",   "application/zip",
                "taille", 4_200_000,
                "data",   dataTropGrosse
            ))
        ));

        assertFalse(res.isSuccess());
        assertTrue(res.getMessage().toLowerCase().contains("volumineux")
                || res.getMessage().toLowerCase().contains("taille"));
        verify(repo, never()).save(any());
    }

    @Test
    @DisplayName("create — pièce jointe sans nom est ignorée silencieusement")
    void create_pieceJointeSansNom() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApiResponse<Commentaire> res = service.create(7, Map.of(
            "contenu",      "Test",
            "auteurPseudo", "alice",
            "piecesJointes", List.of(Map.of(
                "type",   "image/png",
                "taille", 100,
                "data",   "abc123"
                // nom absent → ignorée
            ))
        ));

        assertTrue(res.isSuccess());
        assertTrue(res.getData().getPiecesJointes().isEmpty(),
            "Une pièce jointe sans nom doit être ignorée");
    }

    // ── GET BY TACHE ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getByTache — retourne les commentaires de la tâche")
    void getByTache_retourneListe() {
        when(repo.findByTacheIdOrderByCreatedAtDesc(7))
            .thenReturn(List.of(commentaireTest));

        List<Commentaire> result = service.getByTache(7);

        assertEquals(1, result.size());
        assertEquals("alice", result.get(0).getAuteurPseudo());
    }

    @Test
    @DisplayName("getByTache — retourne liste vide si aucun commentaire")
    void getByTache_listeVide() {
        when(repo.findByTacheIdOrderByCreatedAtDesc(99)).thenReturn(List.of());

        List<Commentaire> result = service.getByTache(99);

        assertTrue(result.isEmpty());
    }

    // ── COUNT ─────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("countByTache — retourne le bon nombre de commentaires")
    void countByTache_retourneNombre() {
        when(repo.countByTacheId(7)).thenReturn(5L);

        long count = service.countByTache(7);

        assertEquals(5L, count);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("delete — appelle deleteById sur le repository MongoDB")
    void delete_appelleRepository() {
        service.delete("mongo_id_123");
        verify(repo, times(1)).deleteById("mongo_id_123");
    }
}

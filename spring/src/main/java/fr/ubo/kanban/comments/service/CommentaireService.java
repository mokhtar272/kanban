package fr.ubo.kanban.comments.service;
import fr.ubo.kanban.comments.document.Commentaire;
import fr.ubo.kanban.comments.repository.CommentaireRepository;
import fr.ubo.kanban.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class CommentaireService {
    private final CommentaireRepository repo;

    public List<Commentaire> getByTache(Integer tacheId) {
        return repo.findByTacheIdOrderByCreatedAtDesc(tacheId);
    }

    public long countByTache(Integer tacheId) {
        return repo.countByTacheId(tacheId);
    }

    @SuppressWarnings("unchecked")
    public ApiResponse<Commentaire> create(Integer tacheId, Map<String, Object> body) {
        Object contenuObj = body.get("contenu");
        if (contenuObj == null || contenuObj.toString().isBlank())
            return ApiResponse.error("Le contenu du commentaire ne peut pas être vide");

        Object auteurObj = body.get("auteurPseudo");
        if (auteurObj == null || auteurObj.toString().isBlank())
            return ApiResponse.error("Auteur manquant");

        Commentaire c = new Commentaire();
        c.setTacheId(tacheId);
        c.setAuteurPseudo(auteurObj.toString().trim());
        c.setContenu(contenuObj.toString().trim());

        // Pièces jointes (base64)
        Object pjObj = body.get("piecesJointes");
        if (pjObj instanceof List<?> rawList) {
            List<Commentaire.PieceJointe> pieces = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof Map<?, ?> pjMap) {
                    String nom    = getStr(pjMap, "nom");
                    String type   = getStr(pjMap, "type");
                    String data   = getStr(pjMap, "data");
                    Object tailleObj = pjMap.get("taille");
                    if (nom == null || nom.isBlank() || data == null || data.isBlank()) continue;
                    long taille = tailleObj instanceof Number n ? n.longValue() : 0L;
                    // Limite 3 Mo en base64
                    if (data.length() > 4_100_000)
                        return ApiResponse.error("Fichier trop volumineux (max 3 Mo) : " + nom);
                    pieces.add(new Commentaire.PieceJointe(nom, type, taille, data));
                }
            }
            c.setPiecesJointes(pieces);
        }

        return ApiResponse.ok("Commentaire ajouté", repo.save(c));
    }

    public void delete(String id) { repo.deleteById(id); }

    private String getStr(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }
}

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

    public ApiResponse<Commentaire> create(Integer tacheId, Map<String, String> body) {
        String contenu = body.get("contenu");
        if (contenu == null || contenu.isBlank())
            return ApiResponse.error("Contenu vide");

        Commentaire c = new Commentaire();
        c.setTacheId(tacheId);
        c.setAuteurPseudo(body.get("auteurPseudo"));
        c.setContenu(contenu.trim());
        return ApiResponse.ok("Commentaire ajouté", repo.save(c));
    }

    public void delete(String id) { repo.deleteById(id); }
}

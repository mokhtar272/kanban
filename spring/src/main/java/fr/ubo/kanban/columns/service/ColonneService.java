package fr.ubo.kanban.columns.service;
import fr.ubo.kanban.boards.repository.TableauRepository;
import fr.ubo.kanban.columns.entity.Colonne;
import fr.ubo.kanban.columns.repository.ColonneRepository;
import fr.ubo.kanban.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class ColonneService {
    private final ColonneRepository colonneRepo;
    private final TableauRepository tableauRepo;

    public List<Colonne> getByTableau(Integer tableauId) {
        return colonneRepo.findByTableauIdOrderByPositionAsc(tableauId);
    }

    public ApiResponse<Colonne> create(Integer tableauId, Map<String, Object> body) {
        return tableauRepo.findById(tableauId).map(tableau -> {
            Colonne c = new Colonne();
            c.setTitre((String) body.get("titre"));
            c.setTableau(tableau);
            if (body.get("couleur") != null) c.setCouleur((String) body.get("couleur"));
            int pos = body.get("position") != null
                ? (Integer) body.get("position")
                : (int) colonneRepo.countByTableauId(tableauId);
            c.setPosition(pos);
            return ApiResponse.ok("Colonne créée", colonneRepo.save(c));
        }).orElse(ApiResponse.error("Tableau introuvable"));
    }

    public ApiResponse<Colonne> update(Integer id, Map<String, Object> body) {
        return colonneRepo.findById(id).map(c -> {
            if (body.get("titre")    != null) c.setTitre((String) body.get("titre"));
            if (body.get("couleur")  != null) c.setCouleur((String) body.get("couleur"));
            if (body.get("position") != null) c.setPosition((Integer) body.get("position"));
            return ApiResponse.ok(colonneRepo.save(c));
        }).orElse(ApiResponse.error("Colonne introuvable"));
    }

    public void delete(Integer id) { colonneRepo.deleteById(id); }
}

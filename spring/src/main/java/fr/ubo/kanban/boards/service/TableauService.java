package fr.ubo.kanban.boards.service;
import fr.ubo.kanban.boards.entity.*;
import fr.ubo.kanban.boards.repository.*;
import fr.ubo.kanban.common.dto.ApiResponse;
import fr.ubo.kanban.users.entity.Utilisateur;
import fr.ubo.kanban.users.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class TableauService {
    private final TableauRepository tableauRepo;
    private final TableauMembreRepository membreRepo;
    private final UtilisateurRepository userRepo;

    public List<Tableau> getMesTableaux(Integer userId) {
        return tableauRepo.findByMembreUserId(userId);
    }

    public Optional<Tableau> getById(Integer id) {
        return tableauRepo.findById(id);
    }

    @Transactional
    public ApiResponse<Tableau> create(Map<String, Object> body, Integer userId) {
        Utilisateur user = userRepo.findById(userId).orElse(null);
        if (user == null) return ApiResponse.error("Utilisateur introuvable");

        Tableau t = new Tableau();
        t.setTitre((String) body.get("titre"));
        t.setDescription((String) body.get("description"));
        if (body.get("couleur") != null) t.setCouleur((String) body.get("couleur"));
        t.setCreateur(user);
        tableauRepo.save(t);

        TableauMembre m = new TableauMembre();
        m.setTableau(t); m.setUtilisateur(user); m.setRole(TableauMembre.Role.OWNER);
        membreRepo.save(m);
        return ApiResponse.ok("Tableau créé", tableauRepo.findById(t.getId()).orElse(t));
    }

    public ApiResponse<Tableau> update(Integer id, Map<String, Object> body) {
        return tableauRepo.findById(id).map(t -> {
            if (body.get("titre")       != null) t.setTitre((String) body.get("titre"));
            if (body.get("description") != null) t.setDescription((String) body.get("description"));
            if (body.get("couleur")     != null) t.setCouleur((String) body.get("couleur"));
            t.setUpdatedAt(LocalDateTime.now());
            return ApiResponse.ok(tableauRepo.save(t));
        }).orElse(ApiResponse.error("Tableau introuvable"));
    }

    public void delete(Integer id) { tableauRepo.deleteById(id); }

    @Transactional
    public ApiResponse<TableauMembre> inviter(Integer tableauId, Map<String, String> body) {
        Tableau t = tableauRepo.findById(tableauId).orElse(null);
        Utilisateur u = userRepo.findByPseudo(body.get("pseudo")).orElse(null);
        if (t == null || u == null)
            return ApiResponse.error("Tableau ou utilisateur introuvable");
        if (membreRepo.existsByTableauIdAndUtilisateurId(tableauId, u.getId()))
            return ApiResponse.error("Déjà membre");
        TableauMembre m = new TableauMembre();
        m.setTableau(t); m.setUtilisateur(u);
        String role = body.getOrDefault("role", "EDITOR");
        m.setRole(TableauMembre.Role.valueOf(role));
        return ApiResponse.ok("Invité", membreRepo.save(m));
    }

    @Transactional
    public void retirerMembre(Integer tableauId, Integer userId) {
        membreRepo.deleteByTableauIdAndUtilisateurId(tableauId, userId);
    }

    public boolean isMembre(Integer tableauId, Integer userId) {
        return membreRepo.existsByTableauIdAndUtilisateurId(tableauId, userId);
    }
}

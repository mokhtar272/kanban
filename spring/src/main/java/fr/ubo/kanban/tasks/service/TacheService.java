package fr.ubo.kanban.tasks.service;
import fr.ubo.kanban.boards.entity.Tableau;
import fr.ubo.kanban.boards.repository.TableauRepository;
import fr.ubo.kanban.columns.entity.Colonne;
import fr.ubo.kanban.columns.repository.ColonneRepository;
import fr.ubo.kanban.common.dto.ApiResponse;
import fr.ubo.kanban.tasks.entity.*;
import fr.ubo.kanban.tasks.repository.*;
import fr.ubo.kanban.users.entity.Utilisateur;
import fr.ubo.kanban.users.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class TacheService {
    private final TacheRepository        tacheRepo;
    private final ColonneRepository      colonneRepo;
    private final UtilisateurRepository  userRepo;
    private final HistoriqueRepository   historiqueRepo;
    private final TableauRepository      tableauRepo;

    public List<Tache> getByColonne(Integer colonneId) {
        return tacheRepo.findByColonneIdOrderByPositionAsc(colonneId);
    }

    public Optional<Tache> getById(Integer id) { return tacheRepo.findById(id); }

    @Transactional
    public ApiResponse<Tache> create(Map<String, Object> body, Integer userId) {
        Integer colonneId = getInt(body, "idColonne");
        Colonne colonne   = colonneRepo.findById(colonneId).orElse(null);
        Utilisateur user  = userRepo.findById(userId).orElse(null);
        if (colonne == null || user == null) return ApiResponse.error("Données invalides");

        Tache t = new Tache();
        t.setTitre((String) body.get("titre"));
        t.setDescription((String) body.get("description"));
        t.setColonne(colonne);
        t.setCreateur(user);
        if (body.get("priorite")   != null) t.setPriorite(Tache.Priorite.valueOf((String) body.get("priorite")));
        if (body.get("dateLimite") != null) t.setDateLimite(LocalDate.parse((String) body.get("dateLimite")));
        if (body.get("idAssigne")  != null) userRepo.findById(getInt(body,"idAssigne")).ifPresent(t::setAssigne);
        t.setPosition((int) tacheRepo.findByColonneIdOrderByPositionAsc(colonneId).size());
        tacheRepo.save(t);

        log("CREATION", "Tâche \"" + t.getTitre() + "\" créée", t, colonne.getTableau(), user);
        return ApiResponse.ok("Tâche créée", t);
    }

    @Transactional
    public ApiResponse<Tache> update(Integer id, Map<String, Object> body, Integer userId) {
        return tacheRepo.findById(id).map(t -> {
            String ancienneColonne = t.getColonne().getTitre();
            if (body.get("titre")       != null) t.setTitre((String) body.get("titre"));
            if (body.get("description") != null) t.setDescription((String) body.get("description"));
            if (body.get("priorite")    != null) t.setPriorite(Tache.Priorite.valueOf((String) body.get("priorite")));
            if (body.get("dateLimite")  != null) {
                String dl = (String) body.get("dateLimite");
                t.setDateLimite(dl.isBlank() ? null : LocalDate.parse(dl));
            }
            if (body.get("idAssigne") != null) {
                Integer aId = getInt(body, "idAssigne");
                if (aId == 0) t.setAssigne(null);
                else userRepo.findById(aId).ifPresent(t::setAssigne);
            }
            if (body.get("position") != null) t.setPosition(getInt(body, "position"));
            if (body.get("idColonne") != null) {
                Integer newColId = getInt(body, "idColonne");
                colonneRepo.findById(newColId).ifPresent(newCol -> {
                    if (!newCol.getId().equals(t.getColonne().getId())) {
                        userRepo.findById(userId).ifPresent(u ->
                            log("DEPLACEMENT", "\"" + t.getTitre() + "\" : " + ancienneColonne + " → " + newCol.getTitre(),
                                t, newCol.getTableau(), u));
                    }
                    t.setColonne(newCol);
                });
            }
            t.setUpdatedAt(LocalDateTime.now());
            return ApiResponse.ok(tacheRepo.save(t));
        }).orElse(ApiResponse.error("Tâche introuvable"));
    }

    public void delete(Integer id) { tacheRepo.deleteById(id); }

    public List<Tache> search(Integer tableauId, String q) {
        return tacheRepo.search(tableauId, q);
    }

    public List<Historique> getHistorique(Integer tableauId) {
        return historiqueRepo.findByTableauIdOrderByCreatedAtDesc(tableauId);
    }

    private void log(String action, String desc, Tache tache, Tableau tableau, Utilisateur user) {
        Historique h = new Historique();
        h.setAction(action); h.setDescription(desc);
        h.setTache(tache); h.setTableau(tableau); h.setUser(user);
        historiqueRepo.save(h);
    }

    private Integer getInt(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v == null) return null;
        if (v instanceof Integer i) return i;
        return Integer.parseInt(v.toString());
    }
}

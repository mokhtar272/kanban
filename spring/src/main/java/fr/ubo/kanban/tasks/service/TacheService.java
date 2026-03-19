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
        String titre = (String) body.get("titre");
        if (titre == null || titre.isBlank())
            return ApiResponse.error("Le titre de la tâche est obligatoire");

        Integer colonneId = getInt(body, "idColonne");
        if (colonneId == null)
            return ApiResponse.error("Colonne non spécifiée");

        Colonne colonne = colonneRepo.findById(colonneId).orElse(null);
        if (colonne == null) return ApiResponse.error("Colonne introuvable (id=" + colonneId + ")");

        Utilisateur user = userRepo.findById(userId).orElse(null);
        if (user == null) return ApiResponse.error("Utilisateur introuvable");

        Tache t = new Tache();
        t.setTitre(titre.trim());
        t.setDescription(body.get("description") != null ? ((String) body.get("description")).trim() : null);
        t.setColonne(colonne);
        t.setCreateur(user);

        if (body.get("priorite") != null) {
            try { t.setPriorite(Tache.Priorite.valueOf((String) body.get("priorite"))); }
            catch (IllegalArgumentException e) { return ApiResponse.error("Priorité invalide : " + body.get("priorite")); }
        }
        if (body.get("dateLimite") != null && !((String)body.get("dateLimite")).isBlank()) {
            try { t.setDateLimite(LocalDate.parse((String) body.get("dateLimite"))); }
            catch (Exception e) { return ApiResponse.error("Format de date invalide (attendu : YYYY-MM-DD)"); }
        }
        if (body.get("idAssigne") != null) {
            Integer aId = getInt(body, "idAssigne");
            if (aId != null && aId > 0)
                userRepo.findById(aId).ifPresent(t::setAssigne);
        }
        t.setPosition((int) tacheRepo.findByColonneIdOrderByPositionAsc(colonneId).size());
        tacheRepo.save(t);
        log("CREATION", "Tâche \"" + t.getTitre() + "\" créée dans \"" + colonne.getTitre() + "\"", t, colonne.getTableau(), user);
        return ApiResponse.ok("Tâche créée", t);
    }

    @Transactional
    public ApiResponse<Tache> update(Integer id, Map<String, Object> body, Integer userId) {
        return tacheRepo.findById(id).map(t -> {
            String ancienneColonne = t.getColonne().getTitre();

            if (body.containsKey("titre")) {
                String titre = (String) body.get("titre");
                if (titre == null || titre.isBlank()) return ApiResponse.<Tache>error("Le titre ne peut pas être vide");
                t.setTitre(titre.trim());
            }
            if (body.containsKey("description"))
                t.setDescription(body.get("description") != null ? ((String) body.get("description")).trim() : null);

            if (body.containsKey("priorite")) {
                try { t.setPriorite(Tache.Priorite.valueOf((String) body.get("priorite"))); }
                catch (IllegalArgumentException e) { return ApiResponse.<Tache>error("Priorité invalide"); }
            }
            if (body.containsKey("dateLimite")) {
                String dl = (String) body.get("dateLimite");
                if (dl == null || dl.isBlank()) { t.setDateLimite(null); }
                else {
                    try { t.setDateLimite(LocalDate.parse(dl)); }
                    catch (Exception e) { return ApiResponse.<Tache>error("Format de date invalide"); }
                }
            }
            if (body.containsKey("idAssigne")) {
                Integer aId = getInt(body, "idAssigne");
                if (aId == null || aId == 0) t.setAssigne(null);
                else userRepo.findById(aId).ifPresent(t::setAssigne);
            }
            if (body.containsKey("position")) t.setPosition(getInt(body, "position"));
            if (body.containsKey("idColonne")) {
                Integer newColId = getInt(body, "idColonne");
                if (newColId != null) {
                    Colonne newCol = colonneRepo.findById(newColId).orElse(null);
                    if (newCol == null) return ApiResponse.<Tache>error("Colonne de destination introuvable");
                    if (!newCol.getId().equals(t.getColonne().getId())) {
                        userRepo.findById(userId).ifPresent(u ->
                            log("DEPLACEMENT", "\"" + t.getTitre() + "\" : " + ancienneColonne + " → " + newCol.getTitre(),
                                t, newCol.getTableau(), u));
                    }
                    t.setColonne(newCol);
                }
            }
            t.setUpdatedAt(LocalDateTime.now());
            return ApiResponse.ok(tacheRepo.save(t));
        }).orElse(ApiResponse.error("Tâche introuvable (id=" + id + ")"));
    }

    public void delete(Integer id) { tacheRepo.deleteById(id); }

    public List<Tache> search(Integer tableauId, String q) {
        if (q == null || q.isBlank()) return tacheRepo.findByTableauId(tableauId);
        return tacheRepo.search(tableauId, q.trim());
    }

    public List<Tache> filter(Integer tableauId, String priorite, Integer assigneId) {
        if (priorite != null && !priorite.isBlank()) {
            try {
                Tache.Priorite p = Tache.Priorite.valueOf(priorite.toUpperCase());
                List<Tache> results = tacheRepo.findByTableauAndPriorite(tableauId, p);
                // Filtrer aussi par assigné si fourni
                if (assigneId != null && assigneId > 0)
                    results = results.stream().filter(t -> t.getAssigne() != null && t.getAssigne().getId().equals(assigneId)).toList();
                return results;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Priorité invalide : " + priorite);
            }
        }
        if (assigneId != null && assigneId > 0)
            return tacheRepo.findByTableauAndAssigne(tableauId, assigneId);
        return tacheRepo.findByTableauId(tableauId);
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
        try { return Integer.parseInt(v.toString()); }
        catch (NumberFormatException e) { return null; }
    }
}

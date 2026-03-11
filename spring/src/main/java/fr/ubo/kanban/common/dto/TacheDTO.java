package fr.ubo.kanban.common.dto;
import fr.ubo.kanban.tasks.entity.Tache;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TacheDTO {
    private Integer id;
    private String titre;
    private String description;
    private String priorite;
    private String statut;
    private Integer position;
    private LocalDate dateLimite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ColonneDTO colonne;
    private UtilisateurDTO assigne;
    private UtilisateurDTO createur;

    public static TacheDTO from(Tache t) {
        if (t == null) return null;
        TacheDTO d = new TacheDTO();
        d.setId(t.getId());
        d.setTitre(t.getTitre());
        d.setDescription(t.getDescription());
        d.setPriorite(t.getPriorite().name());
        d.setStatut(t.getStatut().name());
        d.setPosition(t.getPosition());
        d.setDateLimite(t.getDateLimite());
        d.setCreatedAt(t.getCreatedAt());
        d.setUpdatedAt(t.getUpdatedAt());
        d.setColonne(ColonneDTO.from(t.getColonne()));
        d.setAssigne(UtilisateurDTO.from(t.getAssigne()));
        d.setCreateur(UtilisateurDTO.from(t.getCreateur()));
        return d;
    }
}

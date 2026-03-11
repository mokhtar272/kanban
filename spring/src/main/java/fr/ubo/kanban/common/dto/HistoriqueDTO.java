package fr.ubo.kanban.common.dto;
import fr.ubo.kanban.tasks.entity.Historique;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistoriqueDTO {
    private Integer id;
    private String action;
    private String description;
    private LocalDateTime createdAt;
    private UtilisateurDTO user;

    public static HistoriqueDTO from(Historique h) {
        if (h == null) return null;
        HistoriqueDTO d = new HistoriqueDTO();
        d.setId(h.getId());
        d.setAction(h.getAction());
        d.setDescription(h.getDescription());
        d.setCreatedAt(h.getCreatedAt());
        d.setUser(UtilisateurDTO.from(h.getUser()));
        return d;
    }
}

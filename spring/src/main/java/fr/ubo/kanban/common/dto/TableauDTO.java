package fr.ubo.kanban.common.dto;
import fr.ubo.kanban.boards.entity.Tableau;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class TableauDTO {
    private Integer id;
    private String titre;
    private String description;
    private String couleur;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UtilisateurDTO createur;
    private List<MembreDTO> membres;

    public static TableauDTO from(Tableau t) {
        if (t == null) return null;
        TableauDTO d = new TableauDTO();
        d.setId(t.getId());
        d.setTitre(t.getTitre());
        d.setDescription(t.getDescription());
        d.setCouleur(t.getCouleur());
        d.setCreatedAt(t.getCreatedAt());
        d.setUpdatedAt(t.getUpdatedAt());
        d.setCreateur(UtilisateurDTO.from(t.getCreateur()));
        d.setMembres(t.getMembres().stream()
            .map(MembreDTO::from)
            .collect(Collectors.toList()));
        return d;
    }
}

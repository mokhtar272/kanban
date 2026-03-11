package fr.ubo.kanban.common.dto;
import fr.ubo.kanban.columns.entity.Colonne;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ColonneDTO {
    private Integer id;
    private String titre;
    private Integer position;
    private String couleur;
    private Integer tableauId;
    private LocalDateTime createdAt;

    public static ColonneDTO from(Colonne c) {
        if (c == null) return null;
        ColonneDTO d = new ColonneDTO();
        d.setId(c.getId());
        d.setTitre(c.getTitre());
        d.setPosition(c.getPosition());
        d.setCouleur(c.getCouleur());
        d.setCreatedAt(c.getCreatedAt());
        if (c.getTableau() != null) d.setTableauId(c.getTableau().getId());
        return d;
    }
}

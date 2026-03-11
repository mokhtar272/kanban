package fr.ubo.kanban.common.dto;
import fr.ubo.kanban.boards.entity.TableauMembre;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MembreDTO {
    private Integer id;
    private String role;
    private LocalDateTime joinedAt;
    private UtilisateurDTO utilisateur;

    public static MembreDTO from(TableauMembre m) {
        if (m == null) return null;
        MembreDTO d = new MembreDTO();
        d.setId(m.getId());
        d.setRole(m.getRole().name());
        d.setJoinedAt(m.getJoinedAt());
        d.setUtilisateur(UtilisateurDTO.from(m.getUtilisateur()));
        return d;
    }
}

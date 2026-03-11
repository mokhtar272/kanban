package fr.ubo.kanban.common.dto;
import fr.ubo.kanban.users.entity.Utilisateur;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class UtilisateurDTO {
    private Integer id;
    private String pseudo;
    private String email;
    private String nom;
    private String prenom;
    private String avatar;
    private String role;
    private LocalDateTime createdAt;

    public static UtilisateurDTO from(Utilisateur u) {
        if (u == null) return null;
        UtilisateurDTO d = new UtilisateurDTO();
        d.setId(u.getId());
        d.setPseudo(u.getPseudo());
        d.setEmail(u.getEmail());
        d.setNom(u.getNom());
        d.setPrenom(u.getPrenom());
        d.setAvatar(u.getAvatar());
        d.setRole(u.getRole().name());
        d.setCreatedAt(u.getCreatedAt());
        return d;
    }
}

package fr.ubo.kanban.boards.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.ubo.kanban.users.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor
@Table(name = "tableau_membre",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_tableau","id_utilisateur"}))
public class TableauMembre {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tableau")
    @ToString.Exclude
    @JsonIgnoreProperties({"membres","createur","hibernateLazyInitializer"})
    private Tableau tableau;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_utilisateur")
    @JsonIgnoreProperties({"password","hibernateLazyInitializer"})
    private Utilisateur utilisateur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.EDITOR;

    private LocalDateTime joinedAt = LocalDateTime.now();

    public enum Role { OWNER, EDITOR, VIEWER }
}

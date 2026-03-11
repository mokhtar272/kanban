package fr.ubo.kanban.tasks.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.ubo.kanban.boards.entity.Tableau;
import fr.ubo.kanban.users.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor
@Table(name = "historique")
public class Historique {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tache")
    @ToString.Exclude
    @JsonIgnoreProperties({"colonne","createur","assigne","hibernateLazyInitializer"})
    private Tache tache;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tableau")
    @JsonIgnoreProperties({"membres","createur","hibernateLazyInitializer"})
    private Tableau tableau;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_user")
    @JsonIgnoreProperties({"password","hibernateLazyInitializer"})
    private Utilisateur user;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}

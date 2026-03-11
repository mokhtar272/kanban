package fr.ubo.kanban.boards.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.ubo.kanban.users.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.*;

@Entity @Data @NoArgsConstructor
@Table(name = "tableau")
public class Tableau {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 7)
    private String couleur = "#3B82F6";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_createur", nullable = false)
    @JsonIgnoreProperties({"password","hibernateLazyInitializer"})
    private Utilisateur createur;

    @OneToMany(mappedBy = "tableau", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"tableau","hibernateLazyInitializer"})
    private List<TableauMembre> membres = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}

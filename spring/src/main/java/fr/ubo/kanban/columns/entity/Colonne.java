package fr.ubo.kanban.columns.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.ubo.kanban.boards.entity.Tableau;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor
@Table(name = "colonne")
public class Colonne {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 100)
    private String titre;

    private Integer position = 0;

    @Column(length = 7)
    private String couleur = "#6B7280";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tableau", nullable = false)
    @ToString.Exclude
    @JsonIgnoreProperties({"membres","createur","colonnes","hibernateLazyInitializer"})
    private Tableau tableau;

    private LocalDateTime createdAt = LocalDateTime.now();
}

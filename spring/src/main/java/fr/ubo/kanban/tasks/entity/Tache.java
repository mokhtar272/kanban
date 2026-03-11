package fr.ubo.kanban.tasks.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import fr.ubo.kanban.columns.entity.Colonne;
import fr.ubo.kanban.users.entity.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity @Data @NoArgsConstructor
@Table(name = "tache")
public class Tache {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priorite priorite = Priorite.NORMALE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ACTIVE;

    @Column(nullable = false)
    private Integer position = 0;

    private LocalDate dateLimite;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_colonne", nullable = false)
    @JsonIgnoreProperties({"taches","tableau","hibernateLazyInitializer"})
    private Colonne colonne;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_assigne")
    @JsonIgnoreProperties({"password","hibernateLazyInitializer"})
    private Utilisateur assigne;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_createur", nullable = false)
    @JsonIgnoreProperties({"password","hibernateLazyInitializer"})
    private Utilisateur createur;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Priorite { BASSE, NORMALE, HAUTE, URGENTE }
    public enum Statut   { ACTIVE, ARCHIVEE }
}

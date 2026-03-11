package fr.ubo.kanban.users.entity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Data @NoArgsConstructor @AllArgsConstructor
@Table(name = "utilisateur")
public class Utilisateur {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique = true, nullable = false, length = 50)
    private String pseudo;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private String password;

    private String nom;
    private String prenom;
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Role { USER, ADMIN }
}

package fr.ubo.kanban.users.repository;
import fr.ubo.kanban.users.entity.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByPseudo(String pseudo);
    Optional<Utilisateur> findByEmail(String email);
    boolean existsByPseudo(String pseudo);
    boolean existsByEmail(String email);
}

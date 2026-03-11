package fr.ubo.kanban.boards.repository;
import fr.ubo.kanban.boards.entity.TableauMembre;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TableauMembreRepository extends JpaRepository<TableauMembre, Integer> {
    Optional<TableauMembre> findByTableauIdAndUtilisateurId(Integer tableauId, Integer userId);
    boolean existsByTableauIdAndUtilisateurId(Integer tableauId, Integer userId);
    void deleteByTableauIdAndUtilisateurId(Integer tableauId, Integer userId);
}

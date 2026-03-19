package fr.ubo.kanban.tasks.repository;
import fr.ubo.kanban.tasks.entity.Historique;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface HistoriqueRepository extends JpaRepository<Historique, Integer> {
    List<Historique> findByTableauIdOrderByCreatedAtDesc(Integer tableauId);

    @Query("SELECT h.user.id, h.user.pseudo, COUNT(h) as cnt " +
           "FROM Historique h WHERE h.user IS NOT NULL " +
           "GROUP BY h.user.id, h.user.pseudo ORDER BY cnt DESC")
    List<Object[]> countActionsByUser();
}

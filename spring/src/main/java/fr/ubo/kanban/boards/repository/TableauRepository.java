package fr.ubo.kanban.boards.repository;
import fr.ubo.kanban.boards.entity.Tableau;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TableauRepository extends JpaRepository<Tableau, Integer> {
    @Query("SELECT DISTINCT t FROM Tableau t JOIN t.membres m WHERE m.utilisateur.id = :userId ORDER BY t.createdAt DESC")
    List<Tableau> findByMembreUserId(@Param("userId") Integer userId);
}

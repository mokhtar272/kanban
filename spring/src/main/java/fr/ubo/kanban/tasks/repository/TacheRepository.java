package fr.ubo.kanban.tasks.repository;
import fr.ubo.kanban.tasks.entity.Tache;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface TacheRepository extends JpaRepository<Tache, Integer> {
    List<Tache> findByColonneIdOrderByPositionAsc(Integer colonneId);
    long countByColonneTableauId(Integer tableauId);

    @Query("SELECT t FROM Tache t WHERE t.colonne.tableau.id = :tableauId " +
           "AND (LOWER(t.titre) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(t.description) LIKE LOWER(CONCAT('%',:q,'%')))")
    List<Tache> search(@Param("tableauId") Integer tableauId, @Param("q") String q);

    @Query("SELECT t FROM Tache t WHERE t.colonne.tableau.id = :tableauId AND t.assigne.id = :userId")
    List<Tache> findByTableauAndAssigne(@Param("tableauId") Integer tableauId, @Param("userId") Integer userId);

    @Query("SELECT t FROM Tache t WHERE t.colonne.tableau.id = :tableauId AND t.priorite = :p")
    List<Tache> findByTableauAndPriorite(@Param("tableauId") Integer tableauId, @Param("p") Tache.Priorite p);
}

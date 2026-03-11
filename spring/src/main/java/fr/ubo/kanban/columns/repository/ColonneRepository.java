package fr.ubo.kanban.columns.repository;
import fr.ubo.kanban.columns.entity.Colonne;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ColonneRepository extends JpaRepository<Colonne, Integer> {
    List<Colonne> findByTableauIdOrderByPositionAsc(Integer tableauId);
    long countByTableauId(Integer tableauId);
}

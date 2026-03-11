package fr.ubo.kanban.tasks.repository;
import fr.ubo.kanban.tasks.entity.Historique;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HistoriqueRepository extends JpaRepository<Historique, Integer> {
    List<Historique> findByTableauIdOrderByCreatedAtDesc(Integer tableauId);
}

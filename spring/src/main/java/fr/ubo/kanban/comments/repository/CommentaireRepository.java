package fr.ubo.kanban.comments.repository;
import fr.ubo.kanban.comments.document.Commentaire;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface CommentaireRepository extends MongoRepository<Commentaire, String> {
    List<Commentaire> findByTacheIdOrderByCreatedAtDesc(Integer tacheId);
    long             countByTacheId(Integer tacheId);
    void             deleteByTacheId(Integer tacheId);
}

package fr.ubo.kanban.comments.document;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.*;

@Data @NoArgsConstructor
@Document(collection = "commentaires")
public class Commentaire {
    @Id
    private String id;

    @Indexed
    private Integer tacheId;

    private String auteurPseudo;
    private String contenu;

    private List<PieceJointe> piecesJointes = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class PieceJointe {
        private String nom;
        private String type;
        private Long   taille;
        /** Contenu du fichier encodé en Base64 */
        private String data;
    }
}

package fr.ubo.kanban.comments.controller;
import fr.ubo.kanban.comments.document.Commentaire;
import fr.ubo.kanban.comments.service.CommentaireService;
import fr.ubo.kanban.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/commentaires")
@RequiredArgsConstructor
public class CommentaireController {
    private final CommentaireService service;

    @GetMapping("/tache/{tacheId}")
    public ApiResponse<List<Commentaire>> getByTache(@PathVariable Integer tacheId) {
        return ApiResponse.ok(service.getByTache(tacheId));
    }

    @GetMapping("/tache/{tacheId}/count")
    public ApiResponse<Long> count(@PathVariable Integer tacheId) {
        return ApiResponse.ok(service.countByTache(tacheId));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Commentaire>> create(
            @RequestParam Integer tacheId,
            @RequestBody Map<String, String> body) {
        ApiResponse<Commentaire> res = service.create(tacheId, body);
        return res.isSuccess() ? ResponseEntity.ok(res) : ResponseEntity.badRequest().body(res);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}

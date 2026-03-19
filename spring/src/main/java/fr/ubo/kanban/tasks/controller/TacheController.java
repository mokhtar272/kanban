package fr.ubo.kanban.tasks.controller;
import fr.ubo.kanban.common.dto.*;
import fr.ubo.kanban.tasks.service.TacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/taches")
@RequiredArgsConstructor
public class TacheController {
    private final TacheService service;

    @GetMapping("/colonne/{colonneId}")
    public ApiResponse<List<TacheDTO>> getByColonne(@PathVariable Integer colonneId) {
        return ApiResponse.ok(
            service.getByColonne(colonneId).stream()
                .map(TacheDTO::from).collect(Collectors.toList())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TacheDTO>> getById(@PathVariable Integer id) {
        return service.getById(id)
            .map(t -> ResponseEntity.ok(ApiResponse.ok(TacheDTO.from(t))))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TacheDTO>> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-User-Id") Integer userId) {
        if (userId == null || userId <= 0)
            return ResponseEntity.badRequest().body(ApiResponse.error("Utilisateur non authentifié"));
        var res = service.create(body, userId);
        if (!res.isSuccess()) return ResponseEntity.badRequest().body(ApiResponse.error(res.getMessage()));
        return ResponseEntity.ok(ApiResponse.ok(res.getMessage(), TacheDTO.from(res.getData())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TacheDTO>> update(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) Integer userId) {
        var res = service.update(id, body, userId != null ? userId : 0);
        if (!res.isSuccess()) return ResponseEntity.badRequest().body(ApiResponse.error(res.getMessage()));
        return ResponseEntity.ok(ApiResponse.ok(TacheDTO.from(res.getData())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    /** Recherche plein texte (titre + description) */
    @GetMapping("/search")
    public ApiResponse<List<TacheDTO>> search(
            @RequestParam Integer tableauId,
            @RequestParam(required = false, defaultValue = "") String q) {
        if (tableauId == null || tableauId <= 0)
            return ApiResponse.error("Tableau non spécifié");
        return ApiResponse.ok(
            service.search(tableauId, q).stream()
                .map(TacheDTO::from).collect(Collectors.toList())
        );
    }

    /** Filtre par priorité et/ou utilisateur assigné */
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<TacheDTO>>> filter(
            @RequestParam Integer tableauId,
            @RequestParam(required = false) String priorite,
            @RequestParam(required = false) Integer assigneId) {
        if (tableauId == null || tableauId <= 0)
            return ResponseEntity.badRequest().body(ApiResponse.error("Tableau non spécifié"));
        var results = service.filter(tableauId, priorite, assigneId);
        return ResponseEntity.ok(ApiResponse.ok(
            results.stream().map(TacheDTO::from).collect(Collectors.toList())
        ));
    }

    @GetMapping("/historique/{tableauId}")
    public ApiResponse<List<HistoriqueDTO>> historique(@PathVariable Integer tableauId) {
        return ApiResponse.ok(
            service.getHistorique(tableauId).stream()
                .map(HistoriqueDTO::from).collect(Collectors.toList())
        );
    }
}

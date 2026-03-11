package fr.ubo.kanban.columns.controller;
import fr.ubo.kanban.columns.service.ColonneService;
import fr.ubo.kanban.common.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/colonnes")
@RequiredArgsConstructor
public class ColonneController {
    private final ColonneService service;

    @GetMapping("/tableau/{tableauId}")
    public ApiResponse<List<ColonneDTO>> getByTableau(@PathVariable Integer tableauId) {
        return ApiResponse.ok(
            service.getByTableau(tableauId).stream()
                .map(ColonneDTO::from)
                .collect(Collectors.toList())
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ColonneDTO>> create(
            @RequestParam Integer tableauId,
            @RequestBody Map<String, Object> body) {
        var res = service.create(tableauId, body);
        if (!res.isSuccess()) return ResponseEntity.badRequest().body(ApiResponse.error(res.getMessage()));
        return ResponseEntity.ok(ApiResponse.ok(res.getMessage(), ColonneDTO.from(res.getData())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ColonneDTO>> update(
            @PathVariable Integer id, @RequestBody Map<String, Object> body) {
        var res = service.update(id, body);
        if (!res.isSuccess()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(ApiResponse.ok(ColonneDTO.from(res.getData())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }
}

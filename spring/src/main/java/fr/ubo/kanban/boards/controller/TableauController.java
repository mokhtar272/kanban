package fr.ubo.kanban.boards.controller;
import fr.ubo.kanban.boards.entity.TableauMembre;
import fr.ubo.kanban.boards.service.TableauService;
import fr.ubo.kanban.common.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tableaux")
@RequiredArgsConstructor
public class TableauController {
    private final TableauService service;

    @GetMapping("/mes/{userId}")
    public ApiResponse<List<TableauDTO>> getMes(@PathVariable Integer userId) {
        return ApiResponse.ok(
            service.getMesTableaux(userId).stream()
                .map(TableauDTO::from)
                .collect(Collectors.toList())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TableauDTO>> getById(@PathVariable Integer id) {
        return service.getById(id)
            .map(t -> ResponseEntity.ok(ApiResponse.ok(TableauDTO.from(t))))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TableauDTO>> create(
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-User-Id") Integer userId) {
        var res = service.create(body, userId);
        if (!res.isSuccess()) return ResponseEntity.badRequest().body(ApiResponse.error(res.getMessage()));
        return ResponseEntity.ok(ApiResponse.ok(res.getMessage(), TableauDTO.from(res.getData())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TableauDTO>> update(
            @PathVariable Integer id, @RequestBody Map<String, Object> body) {
        var res = service.update(id, body);
        if (!res.isSuccess()) return ResponseEntity.badRequest().body(ApiResponse.error(res.getMessage()));
        return ResponseEntity.ok(ApiResponse.ok(TableauDTO.from(res.getData())));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ApiResponse.ok("Supprimé", null);
    }

    @PostMapping("/{id}/inviter")
    public ResponseEntity<ApiResponse<MembreDTO>> inviter(
            @PathVariable Integer id, @RequestBody Map<String, String> body) {
        var res = service.inviter(id, body);
        if (!res.isSuccess()) return ResponseEntity.badRequest().body(ApiResponse.error(res.getMessage()));
        return ResponseEntity.ok(ApiResponse.ok(res.getMessage(), MembreDTO.from(res.getData())));
    }

    @DeleteMapping("/{id}/membres/{userId}")
    public ApiResponse<Void> retirerMembre(@PathVariable Integer id, @PathVariable Integer userId) {
        service.retirerMembre(id, userId);
        return ApiResponse.ok(null);
    }
}

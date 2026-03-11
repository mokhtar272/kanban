package fr.ubo.kanban.users.controller;
import fr.ubo.kanban.common.dto.ApiResponse;
import fr.ubo.kanban.common.dto.UtilisateurDTO;
import fr.ubo.kanban.users.entity.Utilisateur;
import fr.ubo.kanban.users.service.UtilisateurService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
public class UtilisateurController {

    private final UtilisateurService service;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Utilisateur>> register(@RequestBody Map<String, String> body) {
        ApiResponse<Utilisateur> res = service.register(body);
        return res.isSuccess()
            ? ResponseEntity.ok(res)
            : ResponseEntity.badRequest().body(res);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Utilisateur>> verify(@RequestBody Map<String, String> body) {
        ApiResponse<Utilisateur> res = service.verify(body);
        return res.isSuccess()
            ? ResponseEntity.ok(res)
            : ResponseEntity.status(401).body(res);
    }

    @GetMapping
    public ApiResponse<List<UtilisateurDTO>> getAll() {
        return ApiResponse.ok(
            service.findAll().stream()
                .map(UtilisateurDTO::from).collect(Collectors.toList())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UtilisateurDTO>> getById(@PathVariable Integer id) {
        return service.findById(id)
            .map(u -> ResponseEntity.ok(ApiResponse.ok(UtilisateurDTO.from(u))))
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        service.deleteById(id);
        return ApiResponse.ok(null);
    }
}

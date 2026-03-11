package fr.ubo.kanban.admin;
import fr.ubo.kanban.boards.repository.TableauRepository;
import fr.ubo.kanban.common.dto.ApiResponse;
import fr.ubo.kanban.common.dto.UtilisateurDTO;
import fr.ubo.kanban.tasks.repository.TacheRepository;
import fr.ubo.kanban.users.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UtilisateurRepository userRepo;
    private final TableauRepository     tableauRepo;
    private final TacheRepository       tacheRepo;

    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nbUtilisateurs", userRepo.count());
        m.put("nbTableaux",     tableauRepo.count());
        m.put("nbTaches",       tacheRepo.count());
        return ApiResponse.ok(m);
    }

    @GetMapping("/utilisateurs")
    public ApiResponse<List<UtilisateurDTO>> getUsers() {
        return ApiResponse.ok(
            userRepo.findAll().stream()
                .map(UtilisateurDTO::from).collect(Collectors.toList())
        );
    }

    @DeleteMapping("/utilisateurs/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Integer id) {
        userRepo.deleteById(id);
        return ApiResponse.ok(null);
    }
}

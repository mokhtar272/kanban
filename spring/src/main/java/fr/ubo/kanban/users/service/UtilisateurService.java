package fr.ubo.kanban.users.service;
import fr.ubo.kanban.common.dto.ApiResponse;
import fr.ubo.kanban.users.entity.Utilisateur;
import fr.ubo.kanban.users.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;

@Service @RequiredArgsConstructor
public class UtilisateurService {
    private final UtilisateurRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public ApiResponse<Utilisateur> register(Map<String, String> body) {
        if (repo.existsByPseudo(body.get("pseudo")))
            return ApiResponse.error("Pseudo déjà utilisé");
        if (repo.existsByEmail(body.get("email")))
            return ApiResponse.error("Email déjà utilisé");

        Utilisateur u = new Utilisateur();
        u.setPseudo(body.get("pseudo"));
        u.setEmail(body.get("email"));
        u.setPassword(encoder.encode(body.get("password")));
        u.setNom(body.getOrDefault("nom", ""));
        u.setPrenom(body.getOrDefault("prenom", ""));
        return ApiResponse.ok("Compte créé", repo.save(u));
    }

    public ApiResponse<Utilisateur> verify(Map<String, String> body) {
        return repo.findByPseudo(body.get("pseudo"))
            .filter(u -> encoder.matches(body.get("password"), u.getPassword()))
            .map(u -> ApiResponse.ok(u))
            .orElse(ApiResponse.error("Identifiants incorrects"));
    }

    public List<Utilisateur> findAll()                          { return repo.findAll(); }
    public Optional<Utilisateur> findById(Integer id)           { return repo.findById(id); }
    public Optional<Utilisateur> findByPseudo(String pseudo)    { return repo.findByPseudo(pseudo); }
    public void deleteById(Integer id)                          { repo.deleteById(id); }
    public long count()                                         { return repo.count(); }
}

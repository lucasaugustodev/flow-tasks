package com.projectmanagement.controller;

import com.projectmanagement.service.GitHubOAuthService;
import com.projectmanagement.service.UserService;
import com.projectmanagement.model.User;
import com.projectmanagement.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpSession;
import java.util.*;

/**
 * Controlador para autenticação via GitHub OAuth
 */
@RestController
@RequestMapping("/api/auth/github")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GitHubAuthController {

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    /**
     * Inicia o processo de autenticação OAuth com GitHub
     */
    @GetMapping("/login")
    public ResponseEntity<?> initiateGitHubLogin(HttpSession session) {
        try {
            if (!gitHubOAuthService.isConfigured()) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "error", "GitHub OAuth não está configurado. Configure GITHUB_CLIENT_ID e GITHUB_CLIENT_SECRET."
                    ));
            }

            String state = gitHubOAuthService.generateState();
            session.setAttribute("github_oauth_state", state);

            String authUrl = gitHubOAuthService.getAuthorizationUrl(state);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "authUrl", authUrl,
                "message", "Redirecionando para autenticação GitHub..."
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Erro ao iniciar autenticação GitHub: " + e.getMessage()
                ));
        }
    }

    /**
     * Callback do GitHub OAuth
     */
    @GetMapping("/callback")
    public RedirectView handleGitHubCallback(
            @RequestParam String code,
            @RequestParam String state,
            HttpSession session) {
        
        try {
            // Verificar estado para segurança
            String sessionState = (String) session.getAttribute("github_oauth_state");
            if (sessionState == null || !sessionState.equals(state)) {
                return new RedirectView("http://localhost:4200/login?error=invalid_state");
            }

            // Trocar código por token
            Map<String, Object> tokenResult = gitHubOAuthService.exchangeCodeForToken(code, state);
            if (!(Boolean) tokenResult.get("success")) {
                return new RedirectView("http://localhost:4200/login?error=token_exchange_failed");
            }

            String accessToken = (String) tokenResult.get("accessToken");

            // Obter informações do usuário
            Map<String, Object> userResult = gitHubOAuthService.getUserInfo(accessToken);
            if (!(Boolean) userResult.get("success")) {
                return new RedirectView("http://localhost:4200/login?error=user_info_failed");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> githubUser = (Map<String, Object>) userResult.get("user");

            // Obter emails do usuário
            Map<String, Object> emailsResult = gitHubOAuthService.getUserEmails(accessToken);
            String primaryEmail = extractPrimaryEmail(emailsResult);

            // Criar ou atualizar usuário no sistema
            User user = createOrUpdateUser(githubUser, primaryEmail, accessToken);

            // Gerar JWT token
            String jwtToken = jwtUtils.generateJwtToken(user.getUsername());

            // Armazenar informações na sessão
            session.setAttribute("user", user);
            session.setAttribute("github_token", accessToken);
            session.setAttribute("jwt_token", jwtToken);

            // Redirecionar para frontend com token
            return new RedirectView("http://localhost:4200/login/success?token=" + jwtToken);

        } catch (Exception e) {
            System.err.println("Erro no callback GitHub: " + e.getMessage());
            return new RedirectView("http://localhost:4200/login?error=callback_failed");
        }
    }

    /**
     * Obtém informações do usuário GitHub autenticado
     */
    @GetMapping("/user")
    public ResponseEntity<?> getGitHubUser(HttpSession session) {
        try {
            String accessToken = (String) session.getAttribute("github_token");
            if (accessToken == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "error", "Usuário não autenticado via GitHub"
                    ));
            }

            Map<String, Object> userResult = gitHubOAuthService.getUserInfo(accessToken);
            if (!(Boolean) userResult.get("success")) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "error", "Erro ao obter informações do usuário GitHub"
                    ));
            }

            return ResponseEntity.ok(userResult);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Erro ao obter usuário GitHub: " + e.getMessage()
                ));
        }
    }

    /**
     * Obtém repositórios do usuário GitHub
     */
    @GetMapping("/repositories")
    public ResponseEntity<?> getGitHubRepositories(
            HttpSession session,
            @RequestParam(defaultValue = "30") int per_page,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "updated") String sort) {
        
        try {
            String accessToken = (String) session.getAttribute("github_token");
            if (accessToken == null) {
                return ResponseEntity.badRequest()
                    .body(Map.of(
                        "success", false,
                        "error", "Usuário não autenticado via GitHub"
                    ));
            }

            Map<String, Object> params = Map.of(
                "per_page", per_page,
                "page", page,
                "sort", sort
            );

            Map<String, Object> reposResult = gitHubOAuthService.getUserRepositories(accessToken, params);
            return ResponseEntity.ok(reposResult);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Erro ao obter repositórios: " + e.getMessage()
                ));
        }
    }

    /**
     * Desconecta do GitHub
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutGitHub(HttpSession session) {
        try {
            String accessToken = (String) session.getAttribute("github_token");
            
            if (accessToken != null) {
                // Opcional: revogar token (comentado pois pode afetar outras aplicações)
                // gitHubOAuthService.revokeToken(accessToken);
            }

            // Limpar sessão
            session.removeAttribute("github_token");
            session.removeAttribute("github_oauth_state");

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Desconectado do GitHub com sucesso"
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Erro ao desconectar do GitHub: " + e.getMessage()
                ));
        }
    }

    /**
     * Verifica status da autenticação GitHub
     */
    @GetMapping("/status")
    public ResponseEntity<?> getGitHubAuthStatus(HttpSession session) {
        try {
            String accessToken = (String) session.getAttribute("github_token");
            boolean isAuthenticated = accessToken != null && gitHubOAuthService.validateToken(accessToken);

            Map<String, Object> status = new HashMap<>();
            status.put("success", true);
            status.put("authenticated", isAuthenticated);
            status.put("configured", gitHubOAuthService.isConfigured());

            if (isAuthenticated) {
                Map<String, Object> userResult = gitHubOAuthService.getUserInfo(accessToken);
                if ((Boolean) userResult.get("success")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> githubUser = (Map<String, Object>) userResult.get("user");
                    status.put("user", Map.of(
                        "login", githubUser.get("login"),
                        "name", githubUser.get("name"),
                        "avatar_url", githubUser.get("avatar_url")
                    ));
                }
            }

            return ResponseEntity.ok(status);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of(
                    "success", false,
                    "error", "Erro ao verificar status: " + e.getMessage()
                ));
        }
    }

    /**
     * Extrai email primário da lista de emails do GitHub
     */
    private String extractPrimaryEmail(Map<String, Object> emailsResult) {
        if (!(Boolean) emailsResult.get("success")) {
            return null;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> emails = (List<Map<String, Object>>) emailsResult.get("emails");
        
        for (Map<String, Object> email : emails) {
            if (Boolean.TRUE.equals(email.get("primary"))) {
                return (String) email.get("email");
            }
        }

        // Se não encontrar primário, retorna o primeiro
        return emails.isEmpty() ? null : (String) emails.get(0).get("email");
    }

    /**
     * Cria ou atualiza usuário no sistema baseado nos dados do GitHub
     */
    private User createOrUpdateUser(Map<String, Object> githubUser, String email, String accessToken) {
        String githubLogin = (String) githubUser.get("login");
        String name = (String) githubUser.get("name");
        String avatarUrl = (String) githubUser.get("avatar_url");

        // Verificar se usuário já existe
        Optional<User> existingUser = userService.findByUsername(githubLogin);
        
        User user;
        if (existingUser.isPresent()) {
            // Atualizar usuário existente
            user = existingUser.get();
            user.setEmail(email != null ? email : user.getEmail());
            user.setGithubToken(accessToken);
            user.setGithubAvatarUrl(avatarUrl);
        } else {
            // Criar novo usuário
            user = new User();
            user.setUsername(githubLogin);
            user.setEmail(email != null ? email : githubLogin + "@github.local");
            user.setPassword(""); // Senha vazia para usuários OAuth
            user.setGithubToken(accessToken);
            user.setGithubAvatarUrl(avatarUrl);
        }

        return userService.save(user);
    }
}

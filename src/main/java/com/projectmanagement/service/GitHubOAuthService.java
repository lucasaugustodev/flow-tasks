package com.projectmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

/**
 * Serviço para autenticação OAuth com GitHub
 * Permite login via GitHub e obtenção de tokens de acesso
 */
@Service
public class GitHubOAuthService {

    private static final String GITHUB_OAUTH_URL = "https://github.com/login/oauth";
    private static final String GITHUB_API_URL = "https://api.github.com";
    
    @Value("${github.oauth.client-id:}")
    private String clientId;
    
    @Value("${github.oauth.client-secret:}")
    private String clientSecret;
    
    @Value("${github.oauth.redirect-uri:http://localhost:8080/auth/github/callback}")
    private String redirectUri;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GitHubOAuthService() {
        this.webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gera URL de autorização do GitHub
     */
    public String getAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(GITHUB_OAUTH_URL + "/authorize")
            .queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", "repo,user,read:org")
            .queryParam("state", state)
            .queryParam("allow_signup", "true")
            .build()
            .toUriString();
    }

    /**
     * Troca código de autorização por token de acesso
     */
    public Map<String, Object> exchangeCodeForToken(String code, String state) {
        try {
            Map<String, String> requestBody = Map.of(
                "client_id", clientId,
                "client_secret", clientSecret,
                "code", code,
                "redirect_uri", redirectUri,
                "state", state
            );

            String response = webClient.post()
                .uri(GITHUB_OAUTH_URL + "/access_token")
                .header("Accept", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> tokenData = objectMapper.readValue(response, Map.class);
            
            if (tokenData.containsKey("access_token")) {
                return Map.of(
                    "success", true,
                    "accessToken", tokenData.get("access_token"),
                    "tokenType", tokenData.getOrDefault("token_type", "bearer"),
                    "scope", tokenData.getOrDefault("scope", "")
                );
            } else {
                return Map.of(
                    "success", false,
                    "error", tokenData.getOrDefault("error_description", "Failed to get access token")
                );
            }

        } catch (Exception e) {
            System.err.println("Erro ao trocar código por token: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Obtém informações do usuário usando o token de acesso
     */
    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            String response = webClient.get()
                .uri(GITHUB_API_URL + "/user")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> userData = objectMapper.readValue(response, Map.class);
            
            return Map.of(
                "success", true,
                "user", userData
            );

        } catch (Exception e) {
            System.err.println("Erro ao obter informações do usuário: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Obtém emails do usuário
     */
    public Map<String, Object> getUserEmails(String accessToken) {
        try {
            String response = webClient.get()
                .uri(GITHUB_API_URL + "/user/emails")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            List<Map<String, Object>> emails = objectMapper.readValue(response, List.class);
            
            return Map.of(
                "success", true,
                "emails", emails
            );

        } catch (Exception e) {
            System.err.println("Erro ao obter emails do usuário: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Valida se o token de acesso ainda é válido
     */
    public boolean validateToken(String accessToken) {
        try {
            Map<String, Object> result = getUserInfo(accessToken);
            return (Boolean) result.get("success");
        } catch (Exception e) {
            System.err.println("Erro ao validar token: " + e.getMessage());
            return false;
        }
    }

    /**
     * Revoga o token de acesso
     */
    public Map<String, Object> revokeToken(String accessToken) {
        try {
            Map<String, String> requestBody = Map.of(
                "access_token", accessToken
            );

            // Para revogar token, usamos POST ao invés de DELETE
            webClient.post()
                .uri(GITHUB_API_URL + "/applications/{client_id}/token", clientId)
                .header("Authorization", "Basic " +
                    Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()))
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            return Map.of(
                "success", true,
                "message", "Token revogado com sucesso"
            );

        } catch (Exception e) {
            System.err.println("Erro ao revogar token: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Gera um estado aleatório para segurança OAuth
     */
    public String generateState() {
        return UUID.randomUUID().toString();
    }

    /**
     * Verifica se as configurações OAuth estão definidas
     */
    public boolean isConfigured() {
        return clientId != null && !clientId.isEmpty() && 
               clientSecret != null && !clientSecret.isEmpty();
    }

    /**
     * Obtém organizações do usuário
     */
    public Map<String, Object> getUserOrganizations(String accessToken) {
        try {
            String response = webClient.get()
                .uri(GITHUB_API_URL + "/user/orgs")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            List<Map<String, Object>> orgs = objectMapper.readValue(response, List.class);
            
            return Map.of(
                "success", true,
                "organizations", orgs
            );

        } catch (Exception e) {
            System.err.println("Erro ao obter organizações do usuário: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Obtém repositórios do usuário com token OAuth
     */
    public Map<String, Object> getUserRepositories(String accessToken, Map<String, Object> params) {
        try {
            String visibility = (String) params.getOrDefault("visibility", "all");
            String affiliation = (String) params.getOrDefault("affiliation", "owner,collaborator,organization_member");
            String sort = (String) params.getOrDefault("sort", "updated");
            String direction = (String) params.getOrDefault("direction", "desc");
            Integer perPage = (Integer) params.getOrDefault("per_page", 30);
            Integer page = (Integer) params.getOrDefault("page", 1);

            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .scheme("https")
                    .host("api.github.com")
                    .path("/user/repos")
                    .queryParam("visibility", visibility)
                    .queryParam("affiliation", affiliation)
                    .queryParam("sort", sort)
                    .queryParam("direction", direction)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .build())
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            List<Map<String, Object>> repos = objectMapper.readValue(response, List.class);
            
            return Map.of(
                "success", true,
                "repositories", repos,
                "count", repos.size()
            );

        } catch (Exception e) {
            System.err.println("Erro ao obter repositórios do usuário: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Cria um Personal Access Token (para desenvolvimento)
     */
    public Map<String, Object> createPersonalAccessToken(String accessToken, String note, List<String> scopes) {
        try {
            Map<String, Object> requestBody = Map.of(
                "note", note,
                "scopes", scopes
            );

            String response = webClient.post()
                .uri(GITHUB_API_URL + "/authorizations")
                .header("Authorization", "Bearer " + accessToken)
                .header("Accept", "application/vnd.github.v3+json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> tokenData = objectMapper.readValue(response, Map.class);
            
            return Map.of(
                "success", true,
                "token", tokenData
            );

        } catch (Exception e) {
            System.err.println("Erro ao criar Personal Access Token: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }
}

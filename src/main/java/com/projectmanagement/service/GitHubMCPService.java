package com.projectmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Serviço para integração com GitHub MCP Server oficial
 * Permite acesso a repositórios, issues, PRs, actions e mais
 */
@Service
public class GitHubMCPService {

    private static final String GITHUB_API_URL = "https://api.github.com";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${github.api.token:}")
    private String githubToken;

    public GitHubMCPService() {
        this.webClient = WebClient.builder()
            .baseUrl(GITHUB_API_URL)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Lista repositórios do usuário autenticado
     */
    public Map<String, Object> listRepositories(Map<String, Object> params) {
        try {
            String type = (String) params.getOrDefault("type", "owner");
            String sort = (String) params.getOrDefault("sort", "updated");
            String direction = (String) params.getOrDefault("direction", "desc");
            Integer perPage = (Integer) params.getOrDefault("per_page", 30);
            Integer page = (Integer) params.getOrDefault("page", 1);

            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/user/repos")
                    .queryParam("type", type)
                    .queryParam("sort", sort)
                    .queryParam("direction", direction)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .build())
                .header("Authorization", "Bearer " + githubToken)
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
            System.err.println("Erro ao listar repositórios: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Obtém detalhes de um repositório específico
     */
    public Map<String, Object> getRepository(String owner, String repo) {
        try {
            String response = webClient.get()
                .uri("/repos/{owner}/{repo}", owner, repo)
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> repository = objectMapper.readValue(response, Map.class);
            
            return Map.of(
                "success", true,
                "repository", repository
            );

        } catch (Exception e) {
            System.err.println("Erro ao obter repositório: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Lista issues de um repositório
     */
    public Map<String, Object> listIssues(String owner, String repo, Map<String, Object> params) {
        try {
            String state = (String) params.getOrDefault("state", "open");
            String sort = (String) params.getOrDefault("sort", "created");
            String direction = (String) params.getOrDefault("direction", "desc");
            Integer perPage = (Integer) params.getOrDefault("per_page", 30);
            Integer page = (Integer) params.getOrDefault("page", 1);

            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/repos/{owner}/{repo}/issues")
                    .queryParam("state", state)
                    .queryParam("sort", sort)
                    .queryParam("direction", direction)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .build(owner, repo))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            List<Map<String, Object>> issues = objectMapper.readValue(response, List.class);
            
            return Map.of(
                "success", true,
                "issues", issues,
                "count", issues.size()
            );

        } catch (Exception e) {
            System.err.println("Erro ao listar issues: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Cria uma nova issue
     */
    public Map<String, Object> createIssue(String owner, String repo, Map<String, Object> issueData) {
        try {
            String requestBody = objectMapper.writeValueAsString(issueData);

            String response = webClient.post()
                .uri("/repos/{owner}/{repo}/issues", owner, repo)
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> issue = objectMapper.readValue(response, Map.class);
            
            return Map.of(
                "success", true,
                "issue", issue,
                "issueNumber", issue.get("number")
            );

        } catch (Exception e) {
            System.err.println("Erro ao criar issue: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Lista pull requests de um repositório
     */
    public Map<String, Object> listPullRequests(String owner, String repo, Map<String, Object> params) {
        try {
            String state = (String) params.getOrDefault("state", "open");
            String sort = (String) params.getOrDefault("sort", "created");
            String direction = (String) params.getOrDefault("direction", "desc");
            Integer perPage = (Integer) params.getOrDefault("per_page", 30);
            Integer page = (Integer) params.getOrDefault("page", 1);

            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/repos/{owner}/{repo}/pulls")
                    .queryParam("state", state)
                    .queryParam("sort", sort)
                    .queryParam("direction", direction)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .build(owner, repo))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            List<Map<String, Object>> pullRequests = objectMapper.readValue(response, List.class);
            
            return Map.of(
                "success", true,
                "pullRequests", pullRequests,
                "count", pullRequests.size()
            );

        } catch (Exception e) {
            System.err.println("Erro ao listar pull requests: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Obtém conteúdo de um arquivo
     */
    public Map<String, Object> getFileContent(String owner, String repo, String path, String ref) {
        try {
            String response = webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/repos/{owner}/{repo}/contents/{path}");
                    if (ref != null && !ref.isEmpty()) {
                        builder.queryParam("ref", ref);
                    }
                    return builder.build(owner, repo, path);
                })
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> fileData = objectMapper.readValue(response, Map.class);
            
            // Decodificar conteúdo base64 se for um arquivo
            if ("file".equals(fileData.get("type"))) {
                String content = (String) fileData.get("content");
                if (content != null) {
                    String decodedContent = new String(Base64.getDecoder().decode(content.replaceAll("\\s", "")));
                    fileData.put("decoded_content", decodedContent);
                }
            }
            
            return Map.of(
                "success", true,
                "file", fileData
            );

        } catch (Exception e) {
            System.err.println("Erro ao obter conteúdo do arquivo: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Lista commits de um repositório
     */
    public Map<String, Object> listCommits(String owner, String repo, Map<String, Object> params) {
        try {
            String sha = (String) params.get("sha");
            String path = (String) params.get("path");
            String author = (String) params.get("author");
            Integer perPage = (Integer) params.getOrDefault("per_page", 30);
            Integer page = (Integer) params.getOrDefault("page", 1);

            String response = webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                        .path("/repos/{owner}/{repo}/commits")
                        .queryParam("per_page", perPage)
                        .queryParam("page", page);
                    
                    if (sha != null) builder.queryParam("sha", sha);
                    if (path != null) builder.queryParam("path", path);
                    if (author != null) builder.queryParam("author", author);
                    
                    return builder.build(owner, repo);
                })
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            List<Map<String, Object>> commits = objectMapper.readValue(response, List.class);
            
            return Map.of(
                "success", true,
                "commits", commits,
                "count", commits.size()
            );

        } catch (Exception e) {
            System.err.println("Erro ao listar commits: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Busca repositórios
     */
    public Map<String, Object> searchRepositories(String query, Map<String, Object> params) {
        try {
            String sort = (String) params.getOrDefault("sort", "stars");
            String order = (String) params.getOrDefault("order", "desc");
            Integer perPage = (Integer) params.getOrDefault("per_page", 30);
            Integer page = (Integer) params.getOrDefault("page", 1);

            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/search/repositories")
                    .queryParam("q", query)
                    .queryParam("sort", sort)
                    .queryParam("order", order)
                    .queryParam("per_page", perPage)
                    .queryParam("page", page)
                    .build())
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> searchResult = objectMapper.readValue(response, Map.class);
            
            return Map.of(
                "success", true,
                "searchResult", searchResult
            );

        } catch (Exception e) {
            System.err.println("Erro ao buscar repositórios: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Obtém informações do usuário autenticado
     */
    public Map<String, Object> getCurrentUser() {
        try {
            String response = webClient.get()
                .uri("/user")
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .retrieve()
                .bodyToMono(String.class)
                .block();

            Map<String, Object> user = objectMapper.readValue(response, Map.class);
            
            return Map.of(
                "success", true,
                "user", user
            );

        } catch (Exception e) {
            System.err.println("Erro ao obter usuário atual: " + e.getMessage());
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * Testa a conectividade com a API do GitHub
     */
    public boolean testConnection() {
        try {
            Map<String, Object> result = getCurrentUser();
            return (Boolean) result.get("success");
        } catch (Exception e) {
            System.err.println("Teste de conexão GitHub falhou: " + e.getMessage());
            return false;
        }
    }
}

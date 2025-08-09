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
 * Serviço para integração com Zep Memory
 * Gerencia contexto conversacional e memória de longo prazo
 */
@Service
public class ZepMemoryService {

    private static final String ZEP_API_URL = "https://api.getzep.com";
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    @Value("${zep.api.key:z_1dWlkIjoiNzRhN2JiNmMtNmQ0Ni00M2U4LTg0NDUtNGE1ODU3ZGM5NWUxIn0.-baBYGTUaFNPkBk7qo26FNYN9sPhf8iWWnWskhrmiUytR17fPBBtbJ1NYclIdqD_EGbYsUg232Z16ZkIUJngQw}")
    private String zepApiKey;

    public ZepMemoryService() {
        this.webClient = WebClient.builder()
            .baseUrl(ZEP_API_URL)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Adiciona uma mensagem à sessão de memória
     */
    public void addMessage(String sessionId, String role, String content, Map<String, Object> metadata) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("role", role);
            message.put("content", content);
            message.put("metadata", metadata != null ? metadata : new HashMap<>());

            Map<String, Object> request = new HashMap<>();
            request.put("messages", Arrays.asList(message));

            webClient.post()
                .uri("/v2/sessions/{sessionId}/memory", sessionId)
                .header("Authorization", "Bearer " + zepApiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    System.out.println("✅ Zep Memory: Mensagem adicionada para sessão " + sessionId);
                })
                .doOnError(error -> {
                    System.err.println("❌ Erro Zep Memory: " + error.getMessage());
                })
                .subscribe();

        } catch (Exception e) {
            System.err.println("Erro ao adicionar mensagem ao Zep: " + e.getMessage());
        }
    }

    /**
     * Adiciona contexto específico sobre tarefas e projetos
     */
    public void addTaskContext(String sessionId, String action, Map<String, Object> taskData) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "task_action");
        metadata.put("action", action);
        metadata.put("task_data", taskData);
        metadata.put("timestamp", System.currentTimeMillis());

        String content = String.format("Ação realizada: %s. Tarefa: %s (ID: %s)", 
            action, 
            taskData.getOrDefault("title", "N/A"),
            taskData.getOrDefault("id", "N/A"));

        addMessage(sessionId, "system", content, metadata);
    }

    /**
     * Adiciona contexto específico sobre projetos
     */
    public void addProjectContext(String sessionId, String action, Map<String, Object> projectData) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", "project_action");
        metadata.put("action", action);
        metadata.put("project_data", projectData);
        metadata.put("timestamp", System.currentTimeMillis());

        String content = String.format("Ação realizada: %s. Projeto: %s (ID: %s)", 
            action, 
            projectData.getOrDefault("name", "N/A"),
            projectData.getOrDefault("id", "N/A"));

        addMessage(sessionId, "system", content, metadata);
    }

    /**
     * Obtém o contexto relevante para a conversa atual
     */
    public String getRelevantContext(String sessionId, String query) {
        try {
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v2/sessions/{sessionId}/search")
                    .queryParam("text", query)
                    .queryParam("limit", 10)
                    .build(sessionId))
                .header("Authorization", "Bearer " + zepApiKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response != null) {
                return processSearchResults(response);
            }

        } catch (Exception e) {
            System.err.println("Erro ao buscar contexto no Zep: " + e.getMessage());
        }

        return "";
    }

    /**
     * Obtém o resumo da sessão
     */
    public String getSessionSummary(String sessionId) {
        try {
            String response = webClient.get()
                .uri("/v2/sessions/{sessionId}/summary", sessionId)
                .header("Authorization", "Bearer " + zepApiKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response != null) {
                Map<String, Object> summary = objectMapper.readValue(response, Map.class);
                return (String) summary.getOrDefault("content", "");
            }

        } catch (Exception e) {
            System.err.println("Erro ao obter resumo da sessão: " + e.getMessage());
        }

        return "";
    }

    /**
     * Obtém as últimas mensagens da sessão
     */
    public List<Map<String, Object>> getRecentMessages(String sessionId, int limit) {
        try {
            String response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                    .path("/v2/sessions/{sessionId}/memory")
                    .queryParam("limit", limit)
                    .build(sessionId))
                .header("Authorization", "Bearer " + zepApiKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response != null) {
                Map<String, Object> data = objectMapper.readValue(response, Map.class);
                return (List<Map<String, Object>>) data.getOrDefault("messages", new ArrayList<>());
            }

        } catch (Exception e) {
            System.err.println("Erro ao obter mensagens recentes: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    /**
     * Processa os resultados da busca e extrai contexto relevante
     */
    private String processSearchResults(String searchResponse) {
        try {
            Map<String, Object> data = objectMapper.readValue(searchResponse, Map.class);
            List<Map<String, Object>> results = (List<Map<String, Object>>) data.getOrDefault("results", new ArrayList<>());

            StringBuilder context = new StringBuilder();
            context.append("CONTEXTO RELEVANTE DA CONVERSA:\n");

            for (Map<String, Object> result : results) {
                Map<String, Object> message = (Map<String, Object>) result.get("message");
                Map<String, Object> metadata = (Map<String, Object>) message.getOrDefault("metadata", new HashMap<>());
                
                String type = (String) metadata.get("type");
                if ("task_action".equals(type)) {
                    Map<String, Object> taskData = (Map<String, Object>) metadata.get("task_data");
                    context.append("- Tarefa: ").append(taskData.get("title"))
                           .append(" (ID: ").append(taskData.get("id")).append(")\n");
                } else if ("project_action".equals(type)) {
                    Map<String, Object> projectData = (Map<String, Object>) metadata.get("project_data");
                    context.append("- Projeto: ").append(projectData.get("name"))
                           .append(" (ID: ").append(projectData.get("id")).append(")\n");
                }
            }

            return context.toString();

        } catch (Exception e) {
            System.err.println("Erro ao processar resultados da busca: " + e.getMessage());
            return "";
        }
    }

    /**
     * Cria ou atualiza uma sessão
     */
    public void createOrUpdateSession(String sessionId, String userId) {
        try {
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("session_id", sessionId);
            sessionData.put("user_id", userId);
            sessionData.put("metadata", Map.of(
                "created_at", System.currentTimeMillis(),
                "application", "project-management-mcp"
            ));

            webClient.post()
                .uri("/v2/sessions")
                .header("Authorization", "Bearer " + zepApiKey)
                .bodyValue(sessionData)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    System.out.println("✅ Zep Memory: Sessão criada/atualizada " + sessionId);
                })
                .doOnError(error -> {
                    System.err.println("❌ Erro ao criar sessão Zep: " + error.getMessage());
                })
                .subscribe();

        } catch (Exception e) {
            System.err.println("Erro ao criar/atualizar sessão: " + e.getMessage());
        }
    }

    /**
     * Adiciona fatos estruturados sobre o contexto atual
     */
    public void addFact(String sessionId, String fact, Map<String, Object> metadata) {
        try {
            Map<String, Object> factData = new HashMap<>();
            factData.put("fact", fact);
            factData.put("metadata", metadata);

            webClient.post()
                .uri("/v2/sessions/{sessionId}/facts", sessionId)
                .header("Authorization", "Bearer " + zepApiKey)
                .bodyValue(factData)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> {
                    System.out.println("✅ Zep Memory: Fato adicionado - " + fact);
                })
                .doOnError(error -> {
                    System.err.println("❌ Erro ao adicionar fato: " + error.getMessage());
                })
                .subscribe();

        } catch (Exception e) {
            System.err.println("Erro ao adicionar fato: " + e.getMessage());
        }
    }
}

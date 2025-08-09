package com.projectmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouterClient {

    // TODO: mover para variável de ambiente. Por hora, hardcoded conforme solicitado.
    // AVISO: não comitar chaves reais em repositórios públicos.
    private static final String API_KEY = "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";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> chatCompletion(List<Map<String, Object>> messages,
                                              List<Map<String, Object>> tools,
                                              String preferredModel) {
        // Lista de modelos para tentar em ordem de preferência
        String[] modelsToTry = {
            "anthropic/claude-3.5-sonnet",
            "x-ai/grok-beta",
            "openai/gpt-4o",
            "openai/gpt-4o-mini",
            "anthropic/claude-3-haiku",
            "openai/gpt-3.5-turbo",
            "meta-llama/llama-3.1-8b-instruct:free"
        };

        // Se um modelo específico foi solicitado, tenta ele primeiro
        if (preferredModel != null && !preferredModel.isEmpty()) {
            try {
                return tryModel(preferredModel, messages, tools);
            } catch (Exception e) {
                System.out.println("Modelo preferido " + preferredModel + " falhou: " + e.getMessage());
            }
        }

        // Tenta cada modelo da lista até encontrar um que funcione
        for (String model : modelsToTry) {
            try {
                System.out.println("Tentando modelo: " + model);
                return tryModel(model, messages, tools);
            } catch (Exception e) {
                System.out.println("Modelo " + model + " falhou: " + e.getMessage());
                // Se o erro for relacionado a ferramentas e temos ferramentas, tenta sem elas
                if (tools != null && !tools.isEmpty() && e.getMessage().contains("tool")) {
                    try {
                        System.out.println("Tentando " + model + " sem ferramentas...");
                        return tryModel(model, messages, null);
                    } catch (Exception e2) {
                        System.out.println("Modelo " + model + " falhou mesmo sem ferramentas: " + e2.getMessage());
                    }
                }
            }
        }

        throw new RuntimeException("Nenhum modelo disponível funcionou. Verifique sua conta OpenRouter.");
    }

    private Map<String, Object> tryModel(String model, List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + API_KEY);
        headers.set("X-Title", "Project Management AI Chat");

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            body.put("tools", tools);
        }
        body.put("stream", false);

        HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
        ResponseEntity<Map> resp = restTemplate.postForEntity(API_URL, req, Map.class);

        System.out.println("Sucesso com modelo: " + model);
        return resp.getBody();
    }
}


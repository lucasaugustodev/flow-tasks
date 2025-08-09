package com.projectmanagement.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Service
public class AnthropicClient {

    @Value("${anthropic.api.key:}")
    private String apiKey;
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-3-5-haiku-20241022"; // Usando Claude 3.5 Haiku (mais recente disponível)

    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> chatCompletion(List<Map<String, Object>> messages,
                                              List<Map<String, Object>> tools,
                                              String preferredModel) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> body = new HashMap<>();
            body.put("model", preferredModel != null ? preferredModel : MODEL);
            body.put("max_tokens", 4000);
            body.put("messages", messages);

            // Adicionar tools se fornecidas
            if (tools != null && !tools.isEmpty()) {
                List<Map<String, Object>> anthropicTools = convertToAnthropicTools(tools);
                body.put("tools", anthropicTools);
            }

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(API_URL, req, Map.class);

            // Converter resposta para formato compatível
            return convertAnthropicResponse(resp.getBody());
            
        } catch (Exception e) {
            System.err.println("Erro na API Anthropic: " + e.getMessage());
            e.printStackTrace();
            
            // Retornar resposta de erro em formato compatível
            Map<String, Object> errorResponse = new HashMap<>();
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "assistant");
            message.put("content", "Desculpe, ocorreu um erro ao processar sua solicitação. Erro: " + e.getMessage());
            choice.put("message", message);
            errorResponse.put("choices", List.of(choice));
            return errorResponse;
        }
    }

    /**
     * Converte resposta da API Anthropic para formato compatível com OpenAI
     */
    private Map<String, Object> convertAnthropicResponse(Map<String, Object> anthropicResponse) {
        Map<String, Object> openAIResponse = new HashMap<>();
        
        // Extrair conteúdo da resposta
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) anthropicResponse.get("content");
        
        if (content != null && !content.isEmpty()) {
            Map<String, Object> firstContent = content.get(0);
            String type = (String) firstContent.get("type");
            
            Map<String, Object> choice = new HashMap<>();
            Map<String, Object> message = new HashMap<>();
            message.put("role", "assistant");
            
            if ("text".equals(type)) {
                // Resposta de texto simples
                message.put("content", firstContent.get("text"));
            } else if ("tool_use".equals(type)) {
                // Resposta com chamada de ferramenta
                List<Map<String, Object>> toolCalls = new ArrayList<>();
                
                for (Map<String, Object> contentItem : content) {
                    if ("tool_use".equals(contentItem.get("type"))) {
                        Map<String, Object> toolCall = new HashMap<>();
                        toolCall.put("id", contentItem.get("id"));
                        toolCall.put("type", "function");
                        
                        Map<String, Object> function = new HashMap<>();
                        function.put("name", contentItem.get("name"));
                        function.put("arguments", contentItem.get("input"));
                        toolCall.put("function", function);
                        
                        toolCalls.add(toolCall);
                    }
                }
                
                message.put("tool_calls", toolCalls);
                
                // Adicionar texto se houver
                String textContent = extractTextFromContent(content);
                if (!textContent.isEmpty()) {
                    message.put("content", textContent);
                }
            }
            
            choice.put("message", message);
            openAIResponse.put("choices", List.of(choice));
        }
        
        return openAIResponse;
    }

    /**
     * Extrai texto de uma lista de conteúdo misto
     */
    private String extractTextFromContent(List<Map<String, Object>> content) {
        StringBuilder text = new StringBuilder();
        for (Map<String, Object> item : content) {
            if ("text".equals(item.get("type"))) {
                text.append(item.get("text"));
            }
        }
        return text.toString();
    }

    /**
     * Converte ferramentas do formato OpenAI para Anthropic
     */
    private List<Map<String, Object>> convertToAnthropicTools(List<Map<String, Object>> tools) {
        List<Map<String, Object>> anthropicTools = new ArrayList<>();
        
        for (Map<String, Object> tool : tools) {
            @SuppressWarnings("unchecked")
            Map<String, Object> function = (Map<String, Object>) tool.get("function");
            if (function != null) {
                Map<String, Object> anthropicTool = new HashMap<>();
                anthropicTool.put("name", function.get("name"));
                anthropicTool.put("description", function.get("description"));
                anthropicTool.put("input_schema", function.get("parameters"));
                anthropicTools.add(anthropicTool);
            }
        }
        
        return anthropicTools;
    }

    /**
     * Método para testar conectividade com a API
     */
    public boolean testConnection() {
        try {
            List<Map<String, Object>> testMessages = new ArrayList<>();
            Map<String, Object> testMessage = new HashMap<>();
            testMessage.put("role", "user");
            testMessage.put("content", "Hello");
            testMessages.add(testMessage);
            
            Map<String, Object> response = chatCompletion(testMessages, null, null);
            return response != null && response.containsKey("choices");
        } catch (Exception e) {
            System.err.println("Teste de conexão falhou: " + e.getMessage());
            return false;
        }
    }
}

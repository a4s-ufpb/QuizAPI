package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.openai.ChatGptRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.openai.ChatGptResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ChatGptService {
    @Value("${openai.gpt.model}")
    private String model;
    @Value("${openai.gpt.url}")
    private String url;

    private final RestTemplate restTemplate;

    public ChatGptService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ChatGptResponse createQuestionByPrompt(String themeName){
        String prompt = generatePrompt(themeName);

        ChatGptRequest request = ChatGptRequest.getInstance(model, prompt);

        return restTemplate.postForObject(url, request, ChatGptResponse.class);
    }

    private String generatePrompt(String themeName) {
        return String.format("""
                Gere para mim uma questão sobre o tema %s, nesse formato:
                { "title": "", "alternatives": [ { "text": "", "correct": false },
                { "text": "", "correct": true }, { "text": "", "correct": false },
                { "text": "", "correct": false } ] } Onde o title precisa ter entre 4 e 170 caracteres;
                O text de cada alternativa deve ter entre 1 e 100 caracteres;
                E quero que alterne o lugar da alternativa correta, podendo ser a primeira, ou segunda, ou terceira, ou última;""", themeName)
                .trim();
    }
}

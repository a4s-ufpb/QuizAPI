package br.ufpb.dcx.apps4society.quizapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ChatGPTConfig {
    @Value("${openai.gpt.key}")
    private String apiKey;

    @Bean
    RestTemplate template(){
        RestTemplate restTemplate =  new RestTemplate();
        restTemplate
                .getInterceptors()
                .add((request, body, execution) -> {
                    request.getHeaders().add("Authorization", "Bearer " + apiKey);
                    return execution.execute(request, body);
                });
        return  restTemplate;
    }
}

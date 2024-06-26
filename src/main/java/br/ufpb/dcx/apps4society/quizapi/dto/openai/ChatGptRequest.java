package br.ufpb.dcx.apps4society.quizapi.dto.openai;

import java.util.ArrayList;
import java.util.List;

public record ChatGptRequest(
        String model,
        List<GPTMessage> messages
) {

    public static ChatGptRequest getInstance(String model, String prompt) {
        GPTMessage gptMessage = new GPTMessage("user", prompt);
        List<GPTMessage> messages = new ArrayList<>();
        messages.add(gptMessage);
        return new ChatGptRequest(model, messages);
    }
}

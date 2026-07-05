package br.ufpb.dcx.apps4society.quizapi.mock;

import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionRequest;
import br.ufpb.dcx.apps4society.quizapi.entity.*;

import java.util.ArrayList;
import java.util.List;

public class MockResponse {
    public Response mockResponseDTO(Integer num) {
        return new Response(
                new User("User", "user@gmail.com", "12345678"),
                new Question(new QuestionRequest("title", "http://image.com", null, null, null), new Theme(), new User(), null, null),
                new Alternative(new AlternativeRequest("response", true), new Question()));
    }

    public List<Response> mockList(Integer size) {
        List<Response> responseDTOList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            responseDTOList.add(mockResponseDTO(i));
        }
        return responseDTOList;
    }

}

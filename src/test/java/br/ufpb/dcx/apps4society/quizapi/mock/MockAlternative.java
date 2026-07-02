package br.ufpb.dcx.apps4society.quizapi.mock;

import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeUpdate;
import br.ufpb.dcx.apps4society.quizapi.entity.Alternative;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;

public class MockAlternative implements MockInterface<Alternative, AlternativeRequest>{

    public Alternative mockEntity(Integer num, Boolean correct, Question question) {
        return new Alternative(num.longValue(),
                "Alternative",
                correct,
                question);
    }
    @Override
    public Alternative mockEntity(Integer num) {
        return new Alternative(num.longValue(),
                "Alternative",
                false,
                new Question());
    }

    @Override
    public AlternativeRequest mockRequest(Integer num) {
        return new AlternativeRequest("Alternative", false);
    }

    public AlternativeRequest mockAlternativeRequest(Boolean correct){
        return new AlternativeRequest("Alternative", correct);
    }

    public AlternativeUpdate mockAlternativeUpdate(){
        return new AlternativeUpdate("Nova Alternativa");
    }

    public AlternativeRequest[] mockAlternativesRequestList() {
        return new AlternativeRequest[]{
                new AlternativeRequest("alt 1", false),
                new AlternativeRequest("alt 2", false),
                new AlternativeRequest("alt 3", false),
                new AlternativeRequest("alt 4", true),
        };
    }

    public AlternativeRequest[] mockAlternativesFalsesRequestList() {
        return new AlternativeRequest[]{
                new AlternativeRequest("alt 1", false),
                new AlternativeRequest("alt 2", false),
                new AlternativeRequest("alt 3", false),
                new AlternativeRequest("alt 4", false),
                new AlternativeRequest("alt 5", false),
                new AlternativeRequest("alt 6", false),
        };
    }

    public AlternativeRequest[] mockAlternativesRequestListMoreThanTheLimit() {
        return new AlternativeRequest[]{
                new AlternativeRequest("alt 1", false),
                new AlternativeRequest("alt 2", false),
                new AlternativeRequest("alt 3", false),
                new AlternativeRequest("alt 4", false),
                new AlternativeRequest("alt 5", false),
                new AlternativeRequest("alt 6", true),
                new AlternativeRequest("alt 7", false)
        };
    }

    public AlternativeRequest[] mockAlternativesTrueDuplicateRequestList() {
        return new AlternativeRequest[]{
                new AlternativeRequest("alt 1", false),
                new AlternativeRequest("alt 2", false),
                new AlternativeRequest("alt 3", true),
                new AlternativeRequest("alt 4", true),
        };
    }
}

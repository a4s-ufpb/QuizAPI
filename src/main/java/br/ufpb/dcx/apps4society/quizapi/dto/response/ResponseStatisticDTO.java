package br.ufpb.dcx.apps4society.quizapi.dto.response;

import br.ufpb.dcx.apps4society.quizapi.entity.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseStatisticDTO {
    Long questionId;
    String questionTitle;
    Integer totalOfAnswers;
    Integer totalOfCorrectAnswers;
    Integer totalOfIncorrectAnswers;
    Double percentageOfAnswersCorrect;
    Double percentageOfAnswersIncorrect;

    public ResponseStatisticDTO(){

    }

    public ResponseStatisticDTO(Long questionId, String questionTitle, Integer totalOfAnswers, Integer totalOfCorrectAnswers,
                                Integer totalOfIncorrectAnswers, Double percentageOfAnswersCorrect, Double percentageOfAnswersIncorrect) {
        this.questionId = questionId;
        this.questionTitle = questionTitle;
        this.totalOfAnswers = totalOfAnswers;
        this.totalOfCorrectAnswers = totalOfCorrectAnswers;
        this.totalOfIncorrectAnswers = totalOfIncorrectAnswers;
        this.percentageOfAnswersCorrect = percentageOfAnswersCorrect;
        this.percentageOfAnswersIncorrect = percentageOfAnswersIncorrect;
    }

    public static List<ResponseStatisticDTO> convertResponseToResponseStatistic(List<Response> responses) {
        Map<Long, ResponseStatisticDTO> responseStatisticMap = new HashMap<>();

        for (Response response : responses) {
            Long questionId = response.getQuestion().getId();
            String questionTitle = response.getQuestion().getTitle();

            if (responseStatisticMap.containsKey(questionId)) {
                ResponseStatisticDTO responseStatisticDTO = responseStatisticMap.get(questionId);

                responseStatisticDTO.setTotalOfAnswers(responseStatisticDTO.getTotalOfAnswers() + 1);

                if (response.getAlternative().getCorrect()) {
                    responseStatisticDTO.setTotalOfCorrectAnswers(responseStatisticDTO.getTotalOfCorrectAnswers() + 1);
                } else {
                    responseStatisticDTO.setTotalOfIncorrectAnswers(responseStatisticDTO.getTotalOfIncorrectAnswers() + 1);
                }

                responseStatisticDTO.setPercentageOfAnswersCorrect(calculatePercentage(responseStatisticDTO.getTotalOfCorrectAnswers(), responseStatisticDTO.getTotalOfAnswers()));
                responseStatisticDTO.setPercentageOfAnswersIncorrect(calculatePercentage(responseStatisticDTO.getTotalOfIncorrectAnswers(), responseStatisticDTO.getTotalOfAnswers()));

            } else {
                ResponseStatisticDTO responseStatisticDTO = new ResponseStatisticDTO(
                        questionId,
                        questionTitle,
                        1,
                        response.getAlternative().getCorrect() ? 1 : 0,
                        response.getAlternative().getCorrect() ? 0 : 1,
                        response.getAlternative().getCorrect() ? 100.0 : 0.0,
                        response.getAlternative().getCorrect() ? 0.0 : 100.0
                );
                responseStatisticMap.put(questionId, responseStatisticDTO);
            }
        }

        return new ArrayList<>(responseStatisticMap.values());
    }

    private static double calculatePercentage(int quantityAnswers, int quantityAllAnswers) {
        if (quantityAllAnswers != 0) {
            return ((double) quantityAnswers / quantityAllAnswers) * 100;
        } else {
            return 0.0;
        }
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public Double getPercentageOfAnswersCorrect() {
        return percentageOfAnswersCorrect;
    }

    public Double getPercentageOfAnswersIncorrect() {
        return percentageOfAnswersIncorrect;
    }

    public Integer getTotalOfAnswers() {
        return totalOfAnswers;
    }

    public void setTotalOfAnswers(Integer totalOfAnswers) {
        this.totalOfAnswers = totalOfAnswers;
    }

    public Integer getTotalOfCorrectAnswers() {
        return totalOfCorrectAnswers;
    }

    public void setTotalOfCorrectAnswers(Integer totalOfCorrectAnswers) {
        this.totalOfCorrectAnswers = totalOfCorrectAnswers;
    }

    public Integer getTotalOfIncorrectAnswers() {
        return totalOfIncorrectAnswers;
    }

    public void setTotalOfIncorrectAnswers(Integer totalOfIncorrectAnswers) {
        this.totalOfIncorrectAnswers = totalOfIncorrectAnswers;
    }

    public void setPercentageOfAnswersCorrect(Double percentageOfAnswersCorrect) {
        this.percentageOfAnswersCorrect = percentageOfAnswersCorrect;
    }

    public void setPercentageOfAnswersIncorrect(Double percentageOfAnswersIncorrect) {
        this.percentageOfAnswersIncorrect = percentageOfAnswersIncorrect;
    }
}

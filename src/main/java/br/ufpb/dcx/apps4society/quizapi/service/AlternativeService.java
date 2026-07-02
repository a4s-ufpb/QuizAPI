package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeUpdate;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.service.exception.*;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeResponse;
import br.ufpb.dcx.apps4society.quizapi.entity.Alternative;
import br.ufpb.dcx.apps4society.quizapi.repository.AlternativeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AlternativeService {
    private AlternativeRepository alternativeRepository;
    private QuestionRepository questionRepository;
    private UserService userService;

    @Autowired
    public AlternativeService(AlternativeRepository alternativeRepository, QuestionRepository questionRepository,
                              UserService userService) {
        this.alternativeRepository = alternativeRepository;
        this.questionRepository = questionRepository;
        this.userService = userService;
    }

    public AlternativeResponse insertAlternative(AlternativeRequest alternativeRequest, Long idQuestion)
            throws FalseAlternativesOnlyException, AlternativeCorrectDuplicateException, LimitOfAlternativesException {
        Question question = questionRepository.findById(idQuestion)
                .orElseThrow(() -> new QuestionNotFoundException("A questão não foi encontrada"));

        Alternative alternative = new Alternative(alternativeRequest, question);
        addAlternativeToQuestion(alternative,question);

        alternativeRepository.save(alternative);
        return alternative.entityToResponse();
    }

    private void addAlternativeToQuestion(Alternative alternative, Question question)
            throws LimitOfAlternativesException, AlternativeCorrectDuplicateException, FalseAlternativesOnlyException {
        if (question.isFullListOfAlternatives()){
            throw new LimitOfAlternativesException("Limite máximo de alternativas");
        }
        if (isAlternativeCorrectDuplicate(alternative, question)) {
            throw new AlternativeCorrectDuplicateException("Já existe uma alternativa verdadeira");
        }
        if (isFalseAlternativesOnly(alternative, question)){
            throw new FalseAlternativesOnlyException("Não é possível cadastrar apenas alternativas falsas");
        }

        question.addAlternative(alternative);
    }

    private boolean isAlternativeCorrectDuplicate(Alternative alternative, Question question){
        for (Alternative a : question.getAlternatives()){
            if (a.getCorrect() && alternative.getCorrect()){
                return true;
            }
        }
        return false;
    }

    private boolean isFalseAlternativesOnly(Alternative alternative, Question question){
        int falseAlternatives = 0;
        for (Alternative a : question.getAlternatives()){
            if (!a.getCorrect() && !alternative.getCorrect()){
                falseAlternatives++;
            }
        }
        return falseAlternatives == Question.MAXIMUM_NUMBER_OF_ALTERNATIVES - 1;
    }

    public void removeAlternative(Long id, String token) throws UserNotHavePermissionException {
        User loggedUser = userService.findUserByToken(token);

        Alternative alternative = alternativeRepository.findById(id)
                .orElseThrow(() -> new AlternativeNotFoundException("Alternativa não encontrada"));

        if (loggedUser.userNotHavePermission(alternative.getQuestionCreator())){
            throw new UserNotHavePermissionException("Usuário não tem permissão para remover essa alternativa");
        }

        alternativeRepository.delete(alternative);
    }

    public AlternativeResponse updateAlternative(Long id, AlternativeUpdate alternativeUpdate, String token) throws UserNotHavePermissionException {
        User user = userService.findUserByToken(token);
        Alternative alternative = alternativeRepository.findById(id)
                .orElseThrow(() -> new AlternativeNotFoundException("Alternativa não encontrada"));

        if (user.userNotHavePermission(alternative.getQuestionCreator())){
            throw new UserNotHavePermissionException("Usuário não tem permissão para atualizar essa alternativa");
        }

        updateData(alternative, alternativeUpdate);
        alternativeRepository.save(alternative);

        return alternative.entityToResponse();
    }

    private void updateData(Alternative alternative, AlternativeUpdate alternativeUpdate) {
        alternative.setText(alternativeUpdate.text());
    }
}

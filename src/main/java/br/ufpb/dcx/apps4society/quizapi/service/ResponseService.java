package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseStatisticDTO;
import br.ufpb.dcx.apps4society.quizapi.dto.response.Themes;
import br.ufpb.dcx.apps4society.quizapi.dto.response.Usernames;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.repository.AlternativeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.UserRepository;
import br.ufpb.dcx.apps4society.quizapi.security.TokenProvider;
import br.ufpb.dcx.apps4society.quizapi.service.exception.*;
import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseDTO;
import br.ufpb.dcx.apps4society.quizapi.entity.Alternative;
import br.ufpb.dcx.apps4society.quizapi.entity.Response;
import br.ufpb.dcx.apps4society.quizapi.repository.ResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class ResponseService {
    private ResponseRepository responseRepository;
    private UserRepository userRepository;
    private QuestionRepository questionRepository;
    private AlternativeRepository alternativeRepository;
    private TokenProvider tokenProvider;

    @Autowired
    public ResponseService(ResponseRepository responseRepository, UserRepository userRepository,
                           QuestionRepository questionRepository, AlternativeRepository alternativeRepository,
                           TokenProvider tokenProvider) {
        this.responseRepository = responseRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.alternativeRepository = alternativeRepository;
        this.tokenProvider = tokenProvider;
    }

    public ResponseDTO insertResponse(UUID userId, Long idQuestion, Long idAlternative){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        Question question = questionRepository.findById(idQuestion)
                .orElseThrow(() -> new QuestionNotFoundException("A questão não foi encontrada"));

        Alternative alternative = alternativeRepository.findById(idAlternative)
                .orElseThrow(() -> new AlternativeNotFoundException("Alternativa não encontrada"));

        Response response = new Response(user,question,alternative);
        user.addResponse(response);

        responseRepository.save(response);

        return response.entityToResponse();
    }

    public void removeResponse(Long idResponse, String token) throws UserNotHavePermissionException {
        User loggedUser = findUserByToken(token);

        Response response = responseRepository.findById(idResponse)
                .orElseThrow(() -> new ResponseNotFoundException("Resposta não encontrada"));

        if (loggedUser.userNotHavePermission(response.getUser())){
            throw new UserNotHavePermissionException("Usuário não tem permissão para remover essa resposta");
        }

        responseRepository.delete(response);
    }

    public Page<ResponseDTO> findResponsesByUser(Pageable pageable, String token){
        User loggedUser = findUserByToken(token);

        Page<Response> responses = responseRepository.findByUser(pageable, loggedUser);

        if (responses.isEmpty()){
            throw new ResponseNotFoundException("Esse usuário ainda não possui nenhuma resposta cadastrada");
        }

        return responses.map(Response::entityToResponse);
    }

    public Page<ResponseDTO> findResponsesByQuestionCreator(Pageable pageable, String token){
        User loggedUser = findUserByToken(token);

        Page<Response> responses = responseRepository.findByQuestionCreator(pageable, loggedUser);

        if (responses.isEmpty()){
            throw new ResponseNotFoundException("Essa questão ainda não possui resposta cadastrada");
        }

        return responses.map(Response::entityToResponse);
    }

    public Page<ResponseDTO> findResponsesByUserNameOrDateOrThemeName(Pageable pageable, String token, String name, String themeName,
                                                                      LocalDate currentDate, LocalDate finalDate) {
        User loggedUser = findUserByToken(token);
        UUID creatorUuid = loggedUser.getUuid();
        Page<Response> responses;

        if (!name.isBlank() && currentDate == null && finalDate == null && themeName.isBlank()) {
            // Busca apenas pelo nome de usuário
            responses = responseRepository.findByQuestionCreatorAndUserName(pageable, creatorUuid, name);
        } else if (name.isBlank() && currentDate != null && finalDate != null && themeName.isBlank()) {
            // Busca apenas pelo intervalo de datas
            responses = responseRepository.findByDateTime(pageable, creatorUuid, currentDate, finalDate);
        } else if (!name.isBlank() && currentDate != null && finalDate != null && themeName.isBlank()) {
            // Busca pelo intervalo de datas e nome de usuário
            responses = responseRepository.findByDateTimeAndUserName(pageable, creatorUuid, name, currentDate, finalDate);
        } else if (name.isBlank() && currentDate == null && finalDate == null && !themeName.isBlank()) {
            // Busca apenas pelo nome do tema
            responses = responseRepository.findByQuestionCreatorAndThemeName(pageable, creatorUuid, themeName);
        } else if (!name.isBlank() && !themeName.isBlank() && currentDate == null && finalDate == null) {
            // Busca pelo nome de usuário e nome do tema
            responses = responseRepository.findByQuestionCreatorUserNameAndThemeName(pageable, creatorUuid, name, themeName);
        } else if (name.isBlank() && !themeName.isBlank() && currentDate != null && finalDate != null) {
            // Busca pelo intervalo de datas e nome do tema
            responses = responseRepository.findByDateTimeAndThemeName(pageable, creatorUuid, themeName, currentDate, finalDate);
        } else if (!name.isBlank() && !themeName.isBlank() && currentDate != null && finalDate != null) {
            // Busca pelo intervalo de datas, nome de usuário e nome do tema
            responses = responseRepository.findByDateTimeAndUserNameAndThemeName(pageable, creatorUuid, name, themeName, currentDate, finalDate);
        } else {
            // Busca apenas pelo criador (caso nenhum filtro específico seja preenchido)
            responses = responseRepository.findByQuestionCreator(pageable, loggedUser);
        }

        if (responses.isEmpty()) {
            throw new ResponseNotFoundException("Nenhuma resposta cadastrada!");
        }

        return responses.map(Response::entityToResponse);
    }

    public List<ResponseStatisticDTO> findStatisticResponse(String token, String themeName, UUID userId) {
        User loggedUser = findUserByToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        List<Response> responses;

        if (loggedUser.userNotHavePermission(user)){
            responses = responseRepository.findByQuestionCreatorAndQuestionThemeName(loggedUser, themeName);
        } else {
            responses = responseRepository.findByQuestionThemeName(themeName);
        }

        List<ResponseStatisticDTO> responseStatisticDTOS = ResponseStatisticDTO.convertResponseToResponseStatistic(responses);

        if (responseStatisticDTOS.isEmpty()){
            throw new ResponseNotFoundException("Nenhuma resposta cadastrada");
        }

        return responseStatisticDTOS;
    }

    public List<Usernames> findUsernamesByCreator(UUID creatorId) {
        userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado!"));

        Set<Usernames> usernames = new HashSet<>();

        return responseRepository
                .findByQuestionCreatorUuid(creatorId)
                .stream()
                .map(response -> new Usernames(
                        response.getUser().getName()
                ))
                .filter(usernames::add)
                .sorted(Comparator.comparing(username -> username.username().toLowerCase()))
                .toList();
    }

    public List<Themes> findThemesByCreator(UUID creatorId) {
        userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado!"));

        Set<Themes> themes = new HashSet<>();

        return responseRepository
                .findByQuestionCreatorUuid(creatorId)
                .stream()
                .map(response -> new Themes(
                        response.getQuestion().getTheme().getName()
                ))
                .filter(themes::add)
                .sorted(Comparator.comparing(theme -> theme.themeName().toLowerCase()))
                .toList();
    }

    public User findUserByToken(String token) {
        if (token != null && token.startsWith("Bearer ")){
            token = token.substring("Bearer ".length());
        }

        String email = tokenProvider.getSubjectByToken(token);

        User user = (User) userRepository.findByEmail(email);

        if (user == null){
            throw new InvalidUserException("Usuário inválido, pode ter sido removido do BD e utilizado o token");
        }

        return user;
    }
}

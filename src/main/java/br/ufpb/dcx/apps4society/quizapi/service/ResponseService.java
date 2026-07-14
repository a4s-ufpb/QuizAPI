package br.ufpb.dcx.apps4society.quizapi.service;

import br.ufpb.dcx.apps4society.quizapi.dto.response.MySummaryDTO;
import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseItemRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseStatisticDTO;
import br.ufpb.dcx.apps4society.quizapi.dto.response.Themes;
import br.ufpb.dcx.apps4society.quizapi.dto.response.Usernames;
import br.ufpb.dcx.apps4society.quizapi.entity.Question;
import br.ufpb.dcx.apps4society.quizapi.entity.User;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.GameMode;
import br.ufpb.dcx.apps4society.quizapi.entity.enums.Role;
import br.ufpb.dcx.apps4society.quizapi.repository.AlternativeRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.QuestionRepository;
import br.ufpb.dcx.apps4society.quizapi.repository.StatisticRepository;
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
    private StatisticRepository statisticRepository;
    private TokenProvider tokenProvider;

    @Autowired
    public ResponseService(ResponseRepository responseRepository, UserRepository userRepository,
                           QuestionRepository questionRepository, AlternativeRepository alternativeRepository,
                           StatisticRepository statisticRepository, TokenProvider tokenProvider) {
        this.responseRepository = responseRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.alternativeRepository = alternativeRepository;
        this.statisticRepository = statisticRepository;
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

    // Salva em lote as respostas do modo multiplayer todos-contra-todos, de uma
    // vez ao final da partida (só chamado pelo front para usuários logados).
    public List<ResponseDTO> insertMultiplayerResponses(String token, List<ResponseItemRequest> items) {
        User user = findUserByToken(token);

        List<Response> responses = items.stream().map(item -> {
            Question question = questionRepository.findById(item.questionId())
                    .orElseThrow(() -> new QuestionNotFoundException("A questão não foi encontrada"));
            Alternative alternative = alternativeRepository.findById(item.alternativeId())
                    .orElseThrow(() -> new AlternativeNotFoundException("Alternativa não encontrada"));

            Response response = new Response(user, question, alternative, GameMode.MULTIPLAYER);
            user.addResponse(response);
            return response;
        }).toList();

        responseRepository.saveAll(responses);

        return responses.stream().map(Response::entityToResponse).toList();
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

    public Page<ResponseDTO> findMyResponses(Pageable pageable, String token, String themeName,
                                              LocalDate startDate, LocalDate endDate, GameMode gameMode){
        User loggedUser = findUserByToken(token);
        Page<Response> responses = responseRepository.findByUserAndFilters(
                pageable, loggedUser, themeName, startDate, endDate, gameMode);
        return responses.map(Response::entityToResponse);
    }

    public MySummaryDTO findMySummary(String token, String themeName, LocalDate startDate, LocalDate endDate, GameMode gameMode){
        User loggedUser = findUserByToken(token);

        long totalCorrect = responseRepository.countByUserAndCorrectAndFilters(
                loggedUser, true, themeName, startDate, endDate, gameMode);
        long totalWrong = responseRepository.countByUserAndCorrectAndFilters(
                loggedUser, false, themeName, startDate, endDate, gameMode);
        long totalQuizzesFinished = statisticRepository.countByStudentNameAndFilters(
                loggedUser.getName(), themeName, startDate, endDate);

        return new MySummaryDTO(totalQuizzesFinished, totalCorrect, totalWrong);
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
                                                                      LocalDate currentDate, LocalDate finalDate, GameMode gameMode) {
        User loggedUser = findUserByToken(token);

        // ADMIN enxerga as respostas de todas as questões (igual à tela de
        // estatísticas por questão); os demais só as das próprias questões.
        Page<Response> responses = loggedUser.getRole() == Role.ADMIN
                ? responseRepository.findByFiltersForAdmin(pageable, name, themeName, currentDate, finalDate, gameMode)
                : responseRepository.findByCreatorAndFilters(pageable, loggedUser.getUuid(), name, themeName, currentDate, finalDate, gameMode);

        if (responses.isEmpty()) {
            throw new ResponseNotFoundException("Nenhuma resposta cadastrada!");
        }

        return responses.map(Response::entityToResponse);
    }

    public List<ResponseDTO> findResponsesByUserNameOrDateOrThemeNameForChart(String token, String name, String themeName,
                                                                               LocalDate currentDate, LocalDate finalDate, GameMode gameMode) {
        Pageable unpaged = Pageable.unpaged();
        try {
            return findResponsesByUserNameOrDateOrThemeName(unpaged, token, name, themeName, currentDate, finalDate, gameMode)
                    .getContent();
        } catch (ResponseNotFoundException e) {
            return List.of();
        }
    }

    public List<ResponseStatisticDTO> findStatisticResponse(String token, String themeName, UUID userId, GameMode gameMode) {
        User loggedUser = findUserByToken(token);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado"));

        List<Response> responses;

        if (loggedUser.userNotHavePermission(user)){
            responses = responseRepository.findByQuestionCreatorAndQuestionThemeNameAndGameMode(loggedUser, themeName, gameMode);
        } else {
            responses = responseRepository.findByQuestionThemeNameAndGameMode(themeName, gameMode);
        }

        List<ResponseStatisticDTO> responseStatisticDTOS = ResponseStatisticDTO.convertResponseToResponseStatistic(responses);

        if (responseStatisticDTOS.isEmpty()){
            throw new ResponseNotFoundException("Nenhuma resposta cadastrada");
        }

        return responseStatisticDTOS;
    }

    public List<Usernames> findUsernamesByCreator(UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado!"));

        Set<Usernames> usernames = new HashSet<>();

        // ADMIN vê os usuários de todas as respostas, não só das próprias questões.
        List<Response> source = creator.getRole() == Role.ADMIN
                ? responseRepository.findAll()
                : responseRepository.findByQuestionCreatorUuid(creatorId);

        return source
                .stream()
                .map(response -> new Usernames(
                        response.getUser().getName()
                ))
                .filter(usernames::add)
                .sorted(Comparator.comparing(username -> username.username().toLowerCase()))
                .toList();
    }

    public List<Themes> findThemesByCreator(UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("Usuário não encontrado!"));

        Set<Themes> themes = new HashSet<>();

        // ADMIN vê os temas de todas as respostas, não só das próprias questões.
        List<Response> source = creator.getRole() == Role.ADMIN
                ? responseRepository.findAll()
                : responseRepository.findByQuestionCreatorUuid(creatorId);

        return source
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

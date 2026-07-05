package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.alternative.AlternativeResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.response.ResponseDTO;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserLogin;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.mock.MockAlternative;
import br.ufpb.dcx.apps4society.quizapi.mock.MockQuestion;
import br.ufpb.dcx.apps4society.quizapi.mock.MockTheme;
import br.ufpb.dcx.apps4society.quizapi.util.*;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static br.ufpb.dcx.apps4society.quizapi.util.ResponseRequestUtil.BASE_PATH_RESPONSE;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ResponseControllerTest extends QuizApplicationTests {
    MockAlternative mockAlternative = new MockAlternative();
    MockQuestion mockQuestion = new MockQuestion();
    MockTheme mockTheme = new MockTheme();
    @Test
    void insertResponseByUserQuestionAlternative_shouldReturn201Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseDTO responseDTO = given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post(baseURI+":"+port+basePath+ BASE_PATH_RESPONSE+"/"+userResponse.uuid()+"/"+questionResponse.id()+"/"+alternativeResponse.id())
                .then()
                .statusCode(201)
                .extract()
                .as(ResponseDTO.class);

        assertNotNull(responseDTO);
        assertNotNull(responseDTO.id());
        assertNotNull(responseDTO.user());
        assertNotNull(responseDTO.question());
        assertNotNull(responseDTO.alternative());
        assertNotNull(responseDTO.dateTime());

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void insertResponseByUserNotFound_shouldReturn404Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        UUID userIdNotFound = UUID.randomUUID();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/" + userIdNotFound + "/" + questionResponse.id() + "/" + alternativeResponse.id())
                .then()
                .assertThat()
                .statusCode(404);

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void insertResponseByQuestionNotFound_shouldReturn404Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());


        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        long questionIdNotFound = 107676;

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/" + userResponse.uuid() + "/" + questionIdNotFound + "/" + alternativeResponse.id())
                .then()
                .assertThat()
                .statusCode(404);

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void insertResponseByAlternativeNotFound_shouldReturn404Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        long alternativeIdNotFound = 107878;

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .post(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/" + userResponse.uuid() + "/" + questionResponse.id() + "/" + alternativeIdNotFound)
                .then()
                .assertThat()
                .statusCode(404);

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeResponseById_shouldReturn204Test(){
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseDTO responseDTO = ResponseRequestUtil.post(userResponse.uuid(),questionResponse.id(), alternativeResponse.id(), token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_RESPONSE+"/"+responseDTO.id())
                .then()
                .statusCode(204)
                .extract()
                .response();

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeResponseNotFound_shouldReturn404Test(){
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseRequestUtil.post(userResponse.uuid(),questionResponse.id(), alternativeResponse.id(), token);

        long responseIdNotFound = 10;

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_RESPONSE+"/"+responseIdNotFound)
                .then()
                .statusCode(404)
                .extract()
                .response();

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeResponseByInvalidToken_shouldReturn403Test(){
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseDTO responseDTO = ResponseRequestUtil.post(userResponse.uuid(),questionResponse.id(), alternativeResponse.id(), token);

        given()
                .header("Authorization", "Bearer " + INVALID_TOKEN)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_RESPONSE+"/"+responseDTO.id())
                .then()
                .statusCode(403)
                .extract()
                .response();

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeResponseByUserNotHavePermission_shouldReturn403Test(){
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        UserRequest anotherUser = new UserRequest("another", "another@gmail.com","another12345");
        UserResponse anotherUserResponse = UserRequestUtil.post(anotherUser);
        UserLogin anotherUserLogin = new UserLogin(anotherUser.email(), anotherUser.password());
        String anotherUserToken = UserRequestUtil.login(anotherUserLogin);

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseDTO responseDTO = ResponseRequestUtil.post(userResponse.uuid(),questionResponse.id(), alternativeResponse.id(), token);

        given()
                .header("Authorization", "Bearer " + anotherUserToken)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_RESPONSE+"/"+responseDTO.id())
                .then()
                .statusCode(403)
                .extract()
                .response();

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
        UserRequestUtil.delete(anotherUserResponse.uuid(), anotherUserToken);
    }

    @Test
    void findResponsesByUser_shouldReturn200Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseRequestUtil.post(userResponse.uuid(), questionResponse.id(),alternativeResponse.id(),token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/user")
                .then()
                .assertThat()
                .statusCode(200);

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findResponsesByInvalidToken_shouldReturn403Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseRequestUtil.post(userResponse.uuid(), questionResponse.id(),alternativeResponse.id(),token);

        given()
                .header("Authorization", "Bearer " + INVALID_TOKEN)
                .when()
                .get(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/user")
                .then()
                .assertThat()
                .statusCode(403);

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findResponsesByListIsEmpty_shouldReturn200WithEmptyContentTest() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        // Endpoint paginado: lista vazia retorna 200 com "content" vazio, não 404
        // (convenção de listagem paginada, igual às demais páginas do app).
        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/user")
                .then()
                .assertThat()
                .statusCode(200)
                .body("content.size()", equalTo(0));

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findResponsesByQuestionCreator_shouldReturn200Test(){
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        UserRequest anotherUser = new UserRequest("Another", "another@gmail.com","12345678");
        UserResponse anotherUserResponse = UserRequestUtil.post(anotherUser);
        UserLogin anotherUserLogin = new UserLogin(anotherUser.email(), anotherUser.password());
        String anotherUserToken = UserRequestUtil.login(anotherUserLogin);

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseRequestUtil.post(anotherUserResponse.uuid(), questionResponse.id(),alternativeResponse.id(), anotherUserToken);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/question/creator")
                .then()
                .assertThat()
                .statusCode(200);

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
        UserRequestUtil.delete(anotherUserResponse.uuid(), anotherUserToken);
    }

    @Test
    void findResponsesByQuestionCreatorWithInvalidToken_shouldReturn403Test(){
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        UserRequest anotherUser = new UserRequest("Another", "another@gmail.com","12345678");
        UserResponse anotherUserResponse = UserRequestUtil.post(anotherUser);
        UserLogin anotherUserLogin = new UserLogin(anotherUser.email(), anotherUser.password());
        String anotherUserToken = UserRequestUtil.login(anotherUserLogin);

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeResponse alternativeResponse = AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        ResponseRequestUtil.post(anotherUserResponse.uuid(), questionResponse.id(),alternativeResponse.id(), anotherUserToken);

        given()
                .header("Authorization", "Bearer " + INVALID_TOKEN)
                .when()
                .get(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/question/creator")
                .then()
                .assertThat()
                .statusCode(403);

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
        UserRequestUtil.delete(anotherUserResponse.uuid(), anotherUserToken);
    }

    @Test
    void findResponsesQuestionCreatorByListIsEmpty_shouldReturn404Test(){
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        UserRequest anotherUser = new UserRequest("Another", "another@gmail.com","12345678");
        UserResponse anotherUserResponse = UserRequestUtil.post(anotherUser);
        UserLogin anotherUserLogin = new UserLogin(anotherUser.email(), anotherUser.password());
        String anotherUserToken = UserRequestUtil.login(anotherUserLogin);

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionResponse questionResponse = QuestionRequestUtil.post(questionRequest, token, themeResponse.id());

        AlternativeRequest alternativeRequest = mockAlternative.mockRequest(1);
        AlternativeRequestUtil.post(alternativeRequest, token, questionResponse.id());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI + ":" + port + basePath + BASE_PATH_RESPONSE + "/question/creator")
                .then()
                .assertThat()
                .statusCode(404);

        QuestionRequestUtil.delete(questionResponse.id(), token);
        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
        UserRequestUtil.delete(anotherUserResponse.uuid(), anotherUserToken);
    }
}
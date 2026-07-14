package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeUpdate;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserLogin;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.mock.MockQuestion;
import br.ufpb.dcx.apps4society.quizapi.mock.MockTheme;
import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.question.QuestionResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeRequest;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import br.ufpb.dcx.apps4society.quizapi.util.QuestionRequestUtil;
import br.ufpb.dcx.apps4society.quizapi.util.ThemeRequestUtil;
import br.ufpb.dcx.apps4society.quizapi.util.UserRequestUtil;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static br.ufpb.dcx.apps4society.quizapi.util.ThemeRequestUtil.BASE_PATH_THEME;
import static io.restassured.RestAssured.*;
import static org.junit.jupiter.api.Assertions.*;

class ThemeControllerTest extends QuizApplicationTests {
    MockTheme mockTheme = new MockTheme();
    MockQuestion mockQuestion = new MockQuestion();
    @Test
    void insertThemeByNameAndToken_shouldReturn201Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);

        ThemeResponse themeResponse = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(themeRequest)
                .when()
                .post(baseURI+":"+port+basePath+BASE_PATH_THEME)
                .then()
                .statusCode(201)
                .extract()
                .as(ThemeResponse.class);

        assertNotNull(themeResponse);
        assertNotNull(themeResponse.id());
        assertNotNull(themeResponse.name());

        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void insertThemeWithDescriptionAndMaterials_shouldPersistTest() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        java.util.Map<String, Object> body = java.util.Map.of(
                "name", "Tema com material",
                "imageUrl", "http://imagem.com",
                "description", "Conteúdos: frações e porcentagem",
                "materials", java.util.List.of(
                        java.util.Map.of("name", "Aula 1", "link", "https://youtu.be/abc", "type", "VIDEO"),
                        java.util.Map.of("name", "PDF", "link", "https://x.com/a.pdf", "type", "FILE")
                )
        );

        ThemeResponse themeResponse = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(baseURI + ":" + port + basePath + BASE_PATH_THEME)
                .then()
                .statusCode(201)
                .body("description", org.hamcrest.Matchers.equalTo("Conteúdos: frações e porcentagem"))
                .body("materials.size()", org.hamcrest.Matchers.equalTo(2))
                .body("materials[0].type", org.hamcrest.Matchers.equalTo("VIDEO"))
                .extract()
                .as(ThemeResponse.class);

        assertNotNull(themeResponse.materials());
        assertEquals(2, themeResponse.materials().size());

        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void insertThemeByNameInvalid_shouldReturn400Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = new ThemeRequest("", "http://imagem.com");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(themeRequest)
                .when()
                .post(baseURI + ":" + port + basePath + BASE_PATH_THEME)
                .then()
                .statusCode(400)
                .assertThat();

        UserRequestUtil.delete(userResponse);
    }

    @Test
    void insertThemeByNameAlreadyExists_shouldReturn400Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);

        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(themeRequest)
                .when()
                .post(baseURI + ":" + port + basePath + BASE_PATH_THEME)
                .then()
                .statusCode(400)
                .assertThat();

        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void insertThemeByInvalidToken_shouldReturn403Test() {
        ThemeRequest themeRequest = mockTheme.mockRequest(1);

        given()
                .header("Authorization", "Bearer " + INVALID_TOKEN)
                .contentType(ContentType.JSON)
                .body(themeRequest)
                .when()
                .post(baseURI + ":" + port + basePath + BASE_PATH_THEME)
                .then()
                .statusCode(403)
                .assertThat();
    }

    @Test
    void removeThemeByIdAndToken_shouldReturn204Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_THEME+"/"+themeResponse.id())
                .then()
                .statusCode(204);

        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeThemeWithLinkedQuestions_shouldReturn204Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        QuestionRequest questionRequest = mockQuestion.mockRequest(1);
        QuestionRequestUtil.post(questionRequest,token, themeResponse.id());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_THEME+"/"+themeResponse.id())
                .then()
                .statusCode(204);

        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeThemeByIdIsNull_shouldReturn403Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_THEME+"/"+null)
                .then()
                .statusCode(403);

        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeThemeNotFound_shouldReturn404Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        ThemeRequestUtil.delete(themeResponse.id(), token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_THEME+"/"+themeResponse.id())
                .then()
                .statusCode(404);

        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeThemeWithInvalidToken_shouldReturn403Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        given()
                .header("Authorization", "Bearer " + INVALID_TOKEN)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_THEME+"/"+themeResponse.id())
                .then()
                .statusCode(403);

        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void removeThemeByUserNotHavePermission_shouldReturn403Test() {
        UserResponse userResponse = UserRequestUtil.post(mockUser.mockRequest(1));
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        UserRequest anotherUserRequest = new UserRequest("another", "another@gmail.com", "12345678");
        UserResponse anotherUser = UserRequestUtil.post(anotherUserRequest);
        UserLogin anotherUserLogin = new UserLogin(anotherUserRequest.email(), anotherUserRequest.password());
        String anotherToken = UserRequestUtil.login(anotherUserLogin);

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);

        given()
                .header("Authorization", "Bearer " + anotherToken)
                .when()
                .delete(baseURI+":"+port+basePath+BASE_PATH_THEME+"/"+themeResponse.id())
                .then()
                .statusCode(403);

        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
        UserRequestUtil.delete(anotherUser.uuid(), anotherToken);
    }

    @Test
    void findAllThemes_shouldReturn200Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);

        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest,token);

        given()
                .get(baseURI+":"+port+basePath+BASE_PATH_THEME)
                .then()
                .statusCode(200)
                .assertThat();

        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findAllThemesNotFound_shouldReturn404Test() {
        given()
                .get(baseURI+":"+port+basePath+BASE_PATH_THEME)
                .then()
                .statusCode(404)
                .assertThat();
    }

    @Test
    void findThemeByIdAndToken_shouldReturn200Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);

        ThemeResponse postTheme = ThemeRequestUtil.post(themeRequest,token);

        ThemeResponse themeResponse =  given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI+":"+port+basePath+BASE_PATH_THEME+"/"+ postTheme.id())
                .then()
                .statusCode(200)
                .extract()
                .as(ThemeResponse.class);

        assertNotNull(themeResponse);
        assertNotNull(themeResponse.id());
        assertNotNull(themeResponse.name());

        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findThemeByIdIsNull_shouldReturn403Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI + ":" + port + basePath + BASE_PATH_THEME + "/" + null)
                .then()
                .statusCode(403)
                .assertThat();

        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findThemeNotFound_shouldReturn404Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest, token);
        ThemeRequestUtil.delete(themeResponse.id(), token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI + ":" + port + basePath + BASE_PATH_THEME + "/" + themeResponse.id())
                .then()
                .statusCode(404)
                .assertThat();

        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findThemesByName_shouldReturn200Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);

        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest,token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI+":"+port+basePath+BASE_PATH_THEME+"?name="+themeRequest.name())
                .then()
                .statusCode(200)
                .assertThat();


        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findThemesByNameIsEmpty_shouldReturn200Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);

        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest,token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI+":"+port+basePath+BASE_PATH_THEME+"?name=")
                .then()
                .statusCode(200)
                .assertThat();


        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void findThemesByNameNotFound_shouldReturn404Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);

        ThemeResponse themeResponse = ThemeRequestUtil.post(themeRequest,token);
        ThemeRequestUtil.delete(themeResponse.id(), token);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(baseURI+":"+port+basePath+BASE_PATH_THEME+"?name="+themeRequest.name())
                .then()
                .statusCode(404)
                .assertThat();

        UserRequestUtil.delete(userResponse);
    }

    @Test
    void updateThemeByIdAndToken_shouldReturn200Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse postTheme = ThemeRequestUtil.post(themeRequest,token);
        ThemeUpdate themeUpdate = mockTheme.mockThemeUpdate();

        ThemeResponse themeResponse = given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(themeUpdate)
                .when()
                .patch(baseURI+":"+port+basePath+BASE_PATH_THEME+"/"+ postTheme.id())
                .then()
                .statusCode(200)
                .extract()
                .as(ThemeResponse.class);

        assertNotNull(themeResponse);
        assertNotNull(themeResponse.id());
        assertNotNull(themeResponse.name());
        assertEquals("Novo tema", themeResponse.name());

        ThemeRequestUtil.delete(themeResponse.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void updateThemeByInvalidName_shouldReturn400Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse postTheme = ThemeRequestUtil.post(themeRequest, token);
        ThemeUpdate themeUpdate = new ThemeUpdate("", "http://imagem.com");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(themeUpdate)
                .when()
                .patch(baseURI + ":" + port + basePath + BASE_PATH_THEME + "/" + postTheme.id())
                .then()
                .statusCode(400)
                .assertThat();

        ThemeRequestUtil.delete(postTheme.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void updateThemeByNameAlreadyExists_shouldReturn400Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest cineTheme = new ThemeRequest("Cine", "http://imagem.com");
        ThemeRequest gameTheme = new ThemeRequest("Game", "http://imagem.com");

        ThemeResponse postCineTheme = ThemeRequestUtil.post(cineTheme, token);
        ThemeResponse postGameTheme = ThemeRequestUtil.post(gameTheme, token);
        ThemeUpdate themeUpdate = new ThemeUpdate("Game", "http://imagem.com");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(themeUpdate)
                .when()
                .patch(baseURI + ":" + port + basePath + BASE_PATH_THEME + "/" + postCineTheme.id())
                .then()
                .statusCode(400)
                .assertThat();

        ThemeRequestUtil.delete(postCineTheme.id(), token);
        ThemeRequestUtil.delete(postGameTheme.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void updateThemeByInvalidToken_shouldReturn403Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse postTheme = ThemeRequestUtil.post(themeRequest, token);
        ThemeUpdate themeUpdate = mockTheme.mockThemeUpdate();

        given()
                .header("Authorization", "Bearer " + INVALID_TOKEN)
                .contentType(ContentType.JSON)
                .body(themeUpdate)
                .when()
                .patch(baseURI + ":" + port + basePath + BASE_PATH_THEME + "/" + postTheme.id())
                .then()
                .statusCode(403)
                .assertThat();

        ThemeRequestUtil.delete(postTheme.id(), token);
        UserRequestUtil.delete(userResponse);
    }

    @Test
    void updateThemeByUserNotHavePermission_shouldReturn403Test() {
        UserRequest userRequest = mockUser.mockRequest(1);
        UserResponse userResponse = UserRequestUtil.post(userRequest);
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        UserRequest anotherUserRequest = new UserRequest("another", "another@gmail.com", "12345678");
        UserResponse anotherUser = UserRequestUtil.post(anotherUserRequest);
        UserLogin anotherUserLogin = new UserLogin(anotherUserRequest.email(), anotherUserRequest.password());
        String anotherToken = UserRequestUtil.login(anotherUserLogin);

        ThemeRequest themeRequest = mockTheme.mockRequest(1);
        ThemeResponse postTheme = ThemeRequestUtil.post(themeRequest, token);
        ThemeUpdate themeUpdate = mockTheme.mockThemeUpdate();

        given()
                .header("Authorization", "Bearer " + anotherToken)
                .contentType(ContentType.JSON)
                .body(themeUpdate)
                .when()
                .patch(baseURI + ":" + port + basePath + BASE_PATH_THEME + "/" + postTheme.id())
                .then()
                .statusCode(403)
                .assertThat();

        ThemeRequestUtil.delete(postTheme.id(), token);
        UserRequestUtil.delete(userResponse);
        UserRequestUtil.delete(anotherUser.uuid(), anotherToken);
    }
}
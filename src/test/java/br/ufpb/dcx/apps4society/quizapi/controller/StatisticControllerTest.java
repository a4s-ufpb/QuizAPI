package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.mock.MockTheme;
import br.ufpb.dcx.apps4society.quizapi.util.ThemeRequestUtil;
import br.ufpb.dcx.apps4society.quizapi.util.UserRequestUtil;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.*;

class StatisticControllerTest extends QuizApplicationTests {
    MockTheme mockTheme = new MockTheme();

    private String statisticPath() {
        return baseURI + ":" + port + basePath + "/statistic";
    }

    @Test
    void insertStatistic_shouldReturn201() {
        UserRequestUtil.post(mockUser.mockRequest(1));
        String token = UserRequestUtil.login(mockUser.mockUserLogin());
        // A estatística referencia um tema existente pelo nome.
        ThemeRequestUtil.post(mockTheme.mockRequest(1), token);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("studentName", "Aluno Teste", "themeName", "Tema", "percentagemOfHits", 80.0))
                .when()
                .post(statisticPath())
                .then()
                .statusCode(201);
    }

    @Test
    void findByCreator_afterInsert_shouldReturn200() {
        UserResponse user = UserRequestUtil.post(mockUser.mockRequest(1));
        String token = UserRequestUtil.login(mockUser.mockUserLogin());
        ThemeRequestUtil.post(mockTheme.mockRequest(1), token);

        given()
                .contentType(ContentType.JSON)
                .body(Map.of("studentName", "Aluno Teste", "themeName", "Tema", "percentagemOfHits", 80.0))
                .when()
                .post(statisticPath())
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(statisticPath() + "/" + user.uuid())
                .then()
                .statusCode(200);
    }

    @Test
    void findDistinctThemeNames_shouldReturn200() {
        UserResponse user = UserRequestUtil.post(mockUser.mockRequest(1));
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(statisticPath() + "/theme/" + user.uuid())
                .then()
                .statusCode(200);
    }
}

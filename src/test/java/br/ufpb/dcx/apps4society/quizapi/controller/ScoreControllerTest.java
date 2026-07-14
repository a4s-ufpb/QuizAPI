package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import br.ufpb.dcx.apps4society.quizapi.dto.theme.ThemeResponse;
import br.ufpb.dcx.apps4society.quizapi.dto.user.UserResponse;
import br.ufpb.dcx.apps4society.quizapi.mock.MockTheme;
import br.ufpb.dcx.apps4society.quizapi.util.ThemeRequestUtil;
import br.ufpb.dcx.apps4society.quizapi.util.UserRequestUtil;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.notNullValue;

class ScoreControllerTest extends QuizApplicationTests {
    MockTheme mockTheme = new MockTheme();

    private String scorePath() {
        return baseURI + ":" + port + basePath + "/score";
    }

    @Test
    void insertScore_shouldReturn201() {
        UserResponse user = UserRequestUtil.post(mockUser.mockRequest(1));
        String token = UserRequestUtil.login(mockUser.mockUserLogin());
        ThemeResponse theme = ThemeRequestUtil.post(mockTheme.mockRequest(1), token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("numberOfHits", 5, "totalTime", 30))
                .when()
                .post(scorePath() + "/" + user.uuid() + "/" + theme.id())
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }

    @Test
    void findRankingByTheme_afterInsert_shouldReturn200() {
        UserResponse user = UserRequestUtil.post(mockUser.mockRequest(1));
        String token = UserRequestUtil.login(mockUser.mockUserLogin());
        ThemeResponse theme = ThemeRequestUtil.post(mockTheme.mockRequest(1), token);

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("numberOfHits", 5, "totalTime", 30))
                .when()
                .post(scorePath() + "/" + user.uuid() + "/" + theme.id())
                .then()
                .statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(scorePath() + "/" + theme.id())
                .then()
                .statusCode(200);
    }

    @Test
    void findRankingByTheme_noScores_shouldReturn404() {
        UserRequestUtil.post(mockUser.mockRequest(1));
        String token = UserRequestUtil.login(mockUser.mockUserLogin());

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(scorePath() + "/99999")
                .then()
                .statusCode(404);
    }

    @Test
    void insertScore_withoutToken_shouldReturn403() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("numberOfHits", 5, "totalTime", 30))
                .when()
                .post(scorePath() + "/" + java.util.UUID.randomUUID() + "/1")
                .then()
                .statusCode(403);
    }
}

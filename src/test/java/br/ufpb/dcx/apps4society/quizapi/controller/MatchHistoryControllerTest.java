package br.ufpb.dcx.apps4society.quizapi.controller;

import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import br.ufpb.dcx.apps4society.quizapi.util.UserRequestUtil;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.*;

class MatchHistoryControllerTest extends QuizApplicationTests {

    private String path() {
        return baseURI + ":" + port + basePath + "/match-history";
    }

    private String authenticate() {
        UserRequestUtil.post(mockUser.mockRequest(1));
        return UserRequestUtil.login(mockUser.mockUserLogin());
    }

    @Test
    void recordMatch_shouldReturn201() {
        String token = authenticate();

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body(Map.of("mode", "SINGLE_PLAYER", "themeName", "Geografia", "score", 4, "total", 10))
                .when()
                .post(path())
                .then()
                .statusCode(201);
    }

    @Test
    void findMyHistory_shouldReturn200() {
        String token = authenticate();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(path() + "/mine")
                .then()
                .statusCode(200);
    }

    @Test
    void findMyAchievements_shouldReturn200() {
        String token = authenticate();

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get(path() + "/achievements")
                .then()
                .statusCode(200);
    }

    @Test
    void recordMatch_withoutToken_shouldReturn403() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("mode", "SINGLE_PLAYER", "themeName", "Geografia", "score", 4, "total", 10))
                .when()
                .post(path())
                .then()
                .statusCode(403);
    }
}

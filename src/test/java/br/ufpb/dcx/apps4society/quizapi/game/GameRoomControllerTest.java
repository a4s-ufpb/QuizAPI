package br.ufpb.dcx.apps4society.quizapi.game;

import br.ufpb.dcx.apps4society.quizapi.QuizApplicationTests;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/** Integração dos endpoints REST públicos do modo multiplayer (/v1/game/**). */
class GameRoomControllerTest extends QuizApplicationTests {

    private String gamePath() {
        return baseURI + ":" + port + basePath + "/game";
    }

    @Test
    void createRoom_shouldReturn201AndLobbyState() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("hostId", "host-1", "hostName", "Prof"))
                .when()
                .post(gamePath() + "/room")
                .then()
                .statusCode(201)
                .body("code", notNullValue())
                .body("status", equalTo("LOBBY"))
                .body("hostId", equalTo("host-1"));
    }

    @Test
    void getRoom_existingCode_shouldReturn200() {
        String code = given()
                .contentType(ContentType.JSON)
                .body(Map.of("hostId", "host-2", "hostName", "Prof"))
                .when()
                .post(gamePath() + "/room")
                .then()
                .statusCode(201)
                .extract()
                .path("code");

        given()
                .when()
                .get(gamePath() + "/room/" + code)
                .then()
                .statusCode(200)
                .body("code", equalTo(code));
    }

    @Test
    void getRoom_unknownCode_shouldReturn404() {
        given()
                .when()
                .get(gamePath() + "/room/ZZZZZZ")
                .then()
                .statusCode(404);
    }
}

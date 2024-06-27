package br.ufpb.dcx.apps4society.quizapi;

import br.ufpb.dcx.apps4society.quizapi.mock.MockUser;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import static io.restassured.RestAssured.port;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.basePath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class QuizApplicationTests {
	public static final String INVALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSIsImlhTcxMDI3Mzg0MCwiZXhwIjoxNzEwMjc3NDQwfQ.tIr6mbb-LmAbQQxYIOTSk1ZctMAijDcQKp2M";
	public static MockUser mockUser;
	@BeforeAll
	public static void setUp(){
		port = 8080;
		baseURI = "http://localhost";
		basePath = "/v1";
		mockUser = new MockUser();
	}

}

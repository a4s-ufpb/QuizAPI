package br.ufpb.dcx.apps4society.quizapi;

import br.ufpb.dcx.apps4society.quizapi.mock.MockUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static io.restassured.RestAssured.port;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.basePath;

// Testes de integração sobem um Postgres real via Testcontainers (Docker) —
// mesmas migrations do Flyway usadas em dev/prod rodam aqui, sem precisar de
// um dialeto separado (H2) que divergia sutilmente da sintaxe do Postgres.
//
// Container em padrão singleton (start() manual, sem @Testcontainers/@Container):
// essas anotações param o container ao final de CADA classe de teste, mas o
// Spring reaproveita o mesmo ApplicationContext (e portanto a mesma conexão já
// aberta para a porta antiga) entre as classes — resultando em "connection
// refused" a partir da 2ª classe. Um único container vivo por toda a JVM de
// teste (encerrado pelo Ryuk só ao final) evita essa dessincronia.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
public class QuizApplicationTests {
	public static final String INVALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyQGdtYWlsLmNvbSIsImlhTcxMDI3Mzg0MCwiZXhwIjoxNzEwMjc3NDQwfQ.tIr6mbb-LmAbQQxYIOTSk1ZctMAijDcQKp2M";
	public static MockUser mockUser;

	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	static {
		postgres.start();
	}

	@DynamicPropertySource
	static void registerPostgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeAll
	public static void setUp(){
		port = 8080;
		baseURI = "http://localhost";
		basePath = "/v1";
		mockUser = new MockUser();
	}

	// O container é compartilhado por toda a suíte (ver comentário acima), então
	// os dados de uma classe de teste vazam pra próxima se não forem limpos —
	// cada teste assume partir de um banco vazio (ex.: "lista deve estar vazia").
	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute("""
				TRUNCATE TABLE
					tb_response, tb_score, tb_statistic, tb_alternative, tb_question,
					tb_room_players, tb_room, tb_match_history, tb_friendship, tb_theme, tb_user
				RESTART IDENTITY CASCADE
				""");
	}

}

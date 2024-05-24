package br.ufpb.dcx.apps4society.quizapi;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class QuizApplication {
	@Value("${app.version}")
	private String version;

	public static void main(String[] args) {
		SpringApplication.run(QuizApplication.class, args);
	}

	@GetMapping("/")
	public String welcome(){
		return "Bem-vindo ao QuizAPI";
	}

}

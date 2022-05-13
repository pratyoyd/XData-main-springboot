package xdata;


import java.io.IOException;

import org.json.simple.parser.ParseException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import test.RegressionTests;


import io.swagger.annotations.ApiOperation;


@RestController
@RequestMapping("/test")
public class TestController {

	@GetMapping("/hello")
	public String getGreetings() {
		return "Hello";
	}
	
	@GetMapping("/hello/{id}")
	@ApiOperation(value = "Greeting Message",
    notes = "Say Hello with user message")
	public String sayHello(@RequestParam("gitReadMe")  String msg) throws IOException, ParseException {
		String basePath="src/main/java/test/universityTest";
		RegressionTests.readFromJsonAPI(basePath);
		return "Hello " + msg;
	}
	
}

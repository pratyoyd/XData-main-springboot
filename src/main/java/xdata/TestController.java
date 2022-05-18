package xdata;


import java.io.IOException;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
		
		
		return "Hello " + msg;
	}
	
	@PostMapping(value ="/getxdataoutput", consumes = {MediaType.APPLICATION_JSON_VALUE},
	        produces = {MediaType.APPLICATION_JSON_VALUE})
	public JSONObject InvokeXDATA(@RequestBody String jsonInput) throws ParseException, IOException {
		JSONParser parser = new JSONParser();
		JSONObject jsonobj = (JSONObject)parser.parse(jsonInput);
		JSONObject jsonoutput = RegressionTests.readFromJsonAPI(jsonobj);
		return jsonoutput;
	}
	
}

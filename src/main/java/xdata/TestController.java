package xdata;


import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import test.RegressionTests;


import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;


@RestController
@RequestMapping("/test")
public class TestController {
	private final Logger logger = LoggerFactory.getLogger(TestController.class);
	
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
		org.json.JSONObject jsonOutput = RegressionTests.readFromJsonAPI(jsonobj,"");
		System.out.println(jsonOutput);
		String mutdbsStr = (String) jsonOutput.toString();	
		
		JSONObject jsonSimpleOutput = (JSONObject) parser.parse(mutdbsStr);
		return jsonSimpleOutput;
	}
	
	@PostMapping(value = "/rest/upload", consumes = {
		      "multipart/form-data"
		   })
	 @Operation(summary = "Upload a single File")
	   public ResponseEntity < ? > uploadFile(@RequestParam("file") MultipartFile uploadfile) {
	      logger.debug("Single file upload!");
	       String uploadedFolder = "/tmp/uploads/";
	      if (uploadfile.isEmpty()) {
	         return new ResponseEntity("You must select a file!", HttpStatus.OK);
	      }
	      try {
	         saveUploadedFiles(Arrays.asList(uploadfile), uploadedFolder);
	      } catch (IOException e) {
	         return new ResponseEntity < > (HttpStatus.BAD_REQUEST);
	      }
	      return new ResponseEntity("Successfully uploaded - " + uploadfile.getOriginalFilename(), new HttpHeaders(), HttpStatus.OK);
		   }
	
	@PostMapping(value = "/rest/uploadXDATA", consumes = { "multipart/form-data"},produces = {MediaType.APPLICATION_JSON_VALUE}
		)
	 @Operation(summary = "Upload a single File and inputs in JSON form")
	public JSONObject uploadFile2(@RequestParam("file") MultipartFile uploadfile,@RequestParam String jsonInput) throws ParseException, IOException {
	      logger.debug("Single file upload!");
	      JSONParser parser = new JSONParser();
	      
			
			JSONObject jsonObj = (JSONObject)parser.parse(jsonInput);
			JSONObject inputQueryJSON = (JSONObject)jsonObj.get("hiddenQuery(Student)");
			String uploadedFolder = (String)inputQueryJSON.get("executablePath");
			
			if (uploadfile.isEmpty()) {
		         return (JSONObject) parser.parse("You must select a file!");
		      }
		      try {
		         saveUploadedFiles(Arrays.asList(uploadfile), uploadedFolder);
		      } catch (IOException e) {
		         return (JSONObject) parser.parse (e.toString());
		      }
		      
			org.json.JSONObject jsonOutput = RegressionTests.readFromJsonAPI(jsonObj,uploadfile.getOriginalFilename());
			System.out.println(jsonOutput);
			String mutdbsStr = (String) jsonOutput.toString();	
			
			JSONObject jsonSimpleOutput = (JSONObject) parser.parse(mutdbsStr);
			return jsonSimpleOutput;
	      //return new ResponseEntity(RegressionTests.readFromJsonAPI(jsonobj,uploadfile.getOriginalFilename()), new HttpHeaders(), HttpStatus.OK);
		   }
	
	private void saveUploadedFiles(List < MultipartFile > files, String uploadedFolder) throws IOException {
		File folder = new File(uploadedFolder);
		if (!folder.exists()) {
			folder.mkdir();
		}
		for (MultipartFile file: files) {
			if (file.isEmpty()) {
				continue;
				// next pls
			}
			byte[] bytes = file.getBytes();
			Path path = Paths.get(uploadedFolder + file.getOriginalFilename());
			Files.write(path, bytes);
		}
	
}
}

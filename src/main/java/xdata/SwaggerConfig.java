/**
 * 
 */
package xdata;

/**
 * @author manish
 *
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import static springfox.documentation.builders.PathSelectors.regex;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableSwagger2
public class SwaggerConfig {

  public static final Contact DEFAULT_CONTACT = new Contact(
      "Manish Kesarwani", "https://dsl.cds.iisc.ac.in/", "manishkesar@iisc.ac.in");
  
  public static final ApiInfo DEFAULT_API_INFO = new ApiInfo(
      "XData API", "API to interfacte with Xdata", "1.0",
      "urn:tos", DEFAULT_CONTACT, 
      "Apache 2.0", "http://www.apache.org/licenses/LICENSE-2.0", Arrays.asList());

  private static final Set<String> DEFAULT_PRODUCES_AND_CONSUMES = 
      new HashSet<String>(Arrays.asList("application/json",
          "application/xml"));

  @Bean
  public Docket api() {
	  return new Docket(DocumentationType.SWAGGER_2)
			  .select()
              .apis(RequestHandlerSelectors.basePackage("xdata"))
              .paths(regex("/test.*"))
              .build()
              .apiInfo(DEFAULT_API_INFO)
              .produces(DEFAULT_PRODUCES_AND_CONSUMES)
            .consumes(DEFAULT_PRODUCES_AND_CONSUMES);
	  
  }
  
  
}



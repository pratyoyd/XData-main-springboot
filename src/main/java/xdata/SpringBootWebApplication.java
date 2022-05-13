/**
 * 
 */
package xdata;


/**
 * @author manish
 *
 */
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;




@SpringBootApplication
public class SpringBootWebApplication{
    public static void main(String[] args) {
//    	SpringApplication app = new SpringApplication(SpringBootWebApplication.class);
//        app.setDefaultProperties(Collections
//          .singletonMap("server.port", "8084"));
//        app.run(args);
    	SpringApplication.run(SpringBootWebApplication.class, args);
    }
}

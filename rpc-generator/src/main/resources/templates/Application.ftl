package ${info.rootPackage};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ${info.serviceClass?cap_first}Application {

    public static void main(String[] args) {
        SpringApplication.run(${info.serviceClass?cap_first}Application.class, args);
    }

}
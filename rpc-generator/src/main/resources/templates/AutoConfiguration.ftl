package ${info.rootPackage}.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration("${info.service}")
@EnableConfigurationProperties(Properties.class)
@ComponentScan(basePackages = "${info.rootPackage}")
public class AutoConfiguration {
}
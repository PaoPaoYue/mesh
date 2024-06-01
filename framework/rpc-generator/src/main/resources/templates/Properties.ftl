package ${info.rootPackage}.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "${info.configPrefix}")
public class Properties {

}
package com.sigae.api;

import com.sigae.api.config.DotenvPropertiesLoader;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
@EnableJpaAuditing
public class SigaeApiApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(SigaeApiApplication.class);
		application.setDefaultProperties(
				DotenvPropertiesLoader.loadDefaultProperties(Path.of(".env"))
		);
		application.run(args);
	}

}

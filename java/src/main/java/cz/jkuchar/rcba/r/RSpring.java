package cz.jkuchar.rcba.r;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import cz.jkuchar.rcba.Application;

@Configuration
@EnableAutoConfiguration(exclude = { Application.class })
@ComponentScan(basePackages = { "cz.jkuchar.rcba.pruning", "cz.jkuchar.rcba.r",
		"cz.jkuchar.rcba.rules" })
public class RSpring {

	public static RPruning initializePruning() {
		ApplicationContext context = new AnnotationConfigApplicationContext(
				RSpring.class);
		RPruning pruning = (RPruning) context.getBean("RPruning");
		return pruning;
	}

}

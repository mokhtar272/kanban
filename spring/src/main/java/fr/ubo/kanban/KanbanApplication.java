package fr.ubo.kanban;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
public class KanbanApplication {
    public static void main(String[] args) {
        SpringApplication.run(KanbanApplication.class, args);
    }
}

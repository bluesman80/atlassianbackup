package nl.ymor.config;

import nl.ymor.backup.JiraBackup;
import nl.ymor.model.Arguments;
import nl.ymor.service.BackupService;
import nl.ymor.service.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class ApplicationConfiguration {

    @Bean
    @Order(10)
    public Arguments arguments() {
        return new Arguments();
    }

    @Bean
    @Order(30)
    public RestClient restClient(Arguments arguments) {
        return new RestClient(arguments);
    }

    @Bean
    @Order(20)
    public BackupService backupService(Arguments arguments, RestClient restClient) {
        return new BackupService(arguments, restClient);
    }

    @Bean
    @Order(40)
    public JiraBackup jiraBackup(BackupService backupService) {
        return new JiraBackup(backupService);
    }
}

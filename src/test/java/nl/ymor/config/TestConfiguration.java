package nl.ymor.config;

import nl.ymor.backup.JiraBackup;
import nl.ymor.model.Arguments;
import nl.ymor.service.BackupService;
import nl.ymor.service.RestClient;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@Configuration
@ActiveProfiles("test")
@EnableAutoConfiguration
public class TestConfiguration {
    @Bean
    public Arguments arguments() {
        return new Arguments();
    }

    @Bean
    public RestClient restClient(Arguments arguments) {
        return new RestClient(arguments);
    }

    @Bean
    public BackupService backupService(Arguments arguments, RestClient restClient) {
        return new BackupService(arguments, restClient);
    }

    @Bean
    public JiraBackup jiraBackup(BackupService backupService) {
        return new JiraBackup(backupService);
    }
}


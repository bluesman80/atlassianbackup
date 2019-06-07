package nl.ymor.backup;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.ymor.config.ApplicationConfiguration;
import nl.ymor.service.BackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static java.lang.System.exit;

@SpringBootApplication
public class JiraBackup implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(JiraBackup.class);
    private static final String TIMEZONE = "Europe/Amsterdam";
    private BackupService backupService;

    public JiraBackup(BackupService backupService) {
        this.backupService = backupService;
    }

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(ApplicationConfiguration.class);
        springApplication.setBannerMode(Banner.Mode.OFF);
        springApplication.run(args);
    }

    @SuppressWarnings("squid:S2142")
    @Override
    public void run(String... args) {
        if (!backupService.setArguments(args)) exit(1);

        boolean isBackupSuccessful = false;
        // Save the start time
        final Instant start = Instant.now();

        LOG.info("Starting the backup script: {} {}",
                start.atZone(ZoneId.of(TIMEZONE)).toLocalDateTime(), TIMEZONE);

        try {
            // Gotta first get the string response, because response can also be HTML (e.g. if the user is unauthorized)
            final HttpResponse<String> backupStringResponse = backupService.requestBackup();

            if (!backupStringResponse.getBody().isEmpty() && backupStringResponse.getStatus() == 200) {
                // If response code is 200, we have a json response for sure, let's get it
                final JsonNode backupJsonResponse = new JsonNode(backupStringResponse.getBody());

                if (backupJsonResponse.getObject().has(backupService.KEY_TASK_ID)) {
                    final String taskID = backupJsonResponse.getObject().get(backupService.KEY_TASK_ID).toString();

                    LOG.info("Backup task ID is {}", taskID);

                    final Optional<String> backupFileUrl = backupService.getBackupFileUrl(taskID);

                    if (backupFileUrl.isPresent()) {
                        backupService.downloadBackupFile(backupFileUrl.get());
                        isBackupSuccessful = true;
                    }
                }
            } else {
                LOG.error("Backup request returned with HTTP code: {}\n Response Body:\n{}",
                        backupStringResponse.getStatus(), backupStringResponse.getBody());
            }
        } catch (final UnirestException | InterruptedException e) {
            LOG.error("Error processing request: ", e);
        } catch (final IOException e) { // This one only comes from the downloadBackupFile method
            LOG.error("Cannot save backup archive due to IOException: ", e);
        }

        // Log the result with duration
        LOG.info("Backup job {} and it took {} seconds", isBackupSuccessful ?
                "succeeded" : "failed", Duration.between(start, Instant.now()));

        if (!isBackupSuccessful) exit(1);
    }

}

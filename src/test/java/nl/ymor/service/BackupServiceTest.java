package nl.ymor.service;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.ymor.config.TestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = TestConfiguration.class,
        initializers = ConfigFileApplicationContextInitializer.class)
@TestPropertySource(locations = "classpath:test.properties")
public class BackupServiceTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private RestClient restClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpResponse<JsonNode> httpResponse;

    @Autowired
    @InjectMocks
    private BackupService instance;

    @Before
    public void initMocks() throws UnirestException {
        MockitoAnnotations.initMocks(this);
        when(restClient.doProgressCheckRequest(anyString())).thenReturn(httpResponse);
    }

    @Test
    public void testRequestBackup() {
        //TODO write
        assertThat(true).isTrue();
    }

    @Test
    public void shouldGetBackupFileUrlByRequestingProgressTwice() throws UnirestException, InterruptedException {
        // get Logback Logger
        Logger fooLogger = (Logger) LoggerFactory.getLogger(BackupService.class);

        // create and start a ListAppender
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();

        // add the appender to the logger
        fooLogger.addAppender(listAppender);

        final String backupFileURL = "export/download/?fileId=11f959da-1c70-4519-8e6f-633748f7b832";

        final JsonNode progressResponseIncomplete =
                new JsonNode("{\"status\":\"InProgress\",\"description\":\"Cloud Export task\"," +
                        "\"message\":\"Preparing database for export (it may take up to 30 minutes)\"," +
                        "\"progress\":0}");
        final JsonNode progressResponseComplete =
                new JsonNode("{\"status\":\"Success\",\"description\":\"Cloud Export task\",\"message\":\"Completed " +
                        "export\",\"result\":\"" + backupFileURL + "\"," +
                        "\"progress\":100}");

        when(httpResponse.getStatus()).thenReturn(200);
        when(httpResponse.getBody()).thenReturn(progressResponseIncomplete, progressResponseComplete);

        Optional<String> backupFileURLOptional = instance.getBackupFileUrl(instance.KEY_TASK_ID);

        assertThat(backupFileURLOptional).isNotEmpty();
        assertThat(backupFileURLOptional.get()).isEqualTo(backupFileURL);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Backup progress: 0");
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(1).getFormattedMessage()).contains("Backup progress: 100");
        assertThat(logsList.get(2).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(2).getFormattedMessage()).contains("Backup finished successfully.");

    }

    @Test
    public void testDownloadBackupFile() {
        //TODO write
        assertThat(true).isTrue();
    }
}

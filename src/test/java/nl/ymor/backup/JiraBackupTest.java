package nl.ymor.backup;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.ymor.config.TestConfiguration;
import nl.ymor.service.BackupService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles("test")
@ContextConfiguration(classes = TestConfiguration.class,
        initializers = ConfigFileApplicationContextInitializer.class)
@TestPropertySource(locations = "classpath:test.properties")
public class JiraBackupTest {

    @Mock
    private BackupService backupService;

    @Mock
    private HttpResponse<String> stringHttpResponse;

    @InjectMocks
    @Autowired
    private JiraBackup instance;

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    // Get Logback Logger (will be used to assert logs)
    private final Logger backupServiceLogger = (Logger) LoggerFactory.getLogger(BackupService.class);
    // Create a ListAppender
    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @Before
    public void setup() {
        // Initialize mocks
        MockitoAnnotations.initMocks(this);

        // Start the ListAppender
        listAppender.start();
        // Add the appender to the logger
        backupServiceLogger.addAppender(listAppender);
    }

    @Test
    public void shouldSucceedBackup() throws UnirestException, InterruptedException, IOException {
        final String responseBody = "{\"taskId\":\"10023\"}";

        when(stringHttpResponse.getBody()).thenReturn(responseBody);
        when(stringHttpResponse.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);

        when(backupService.setArguments(anyString())).thenReturn(true);
        when(backupService.requestBackup()).thenReturn(stringHttpResponse);
        when(backupService.getBackupFileUrl(anyString())).thenReturn(Optional.of("some/url"));
        doNothing().when(backupService).downloadBackupFile(anyString());

        instance.run("args");
    }

    @Test
    public void shouldNotRunDueToWrongArguments() {
        when(backupService.setArguments(anyString())).thenReturn(false);

        exit.expectSystemExitWithStatus(1);

        instance.run("fakenews");
    }

    @Test
    public void shouldFailBecauseOfUnauthorizedUser() throws UnirestException {
        exit.expectSystemExitWithStatus(1);

        final String responseBody = "HTML response body";

        when(stringHttpResponse.getBody()).thenReturn(responseBody);
        when(stringHttpResponse.getStatus()).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED);

        when(backupService.setArguments(anyString())).thenReturn(true);
        when(backupService.requestBackup()).thenReturn(stringHttpResponse);

        instance.run("args");

        // Assert the log messages
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Backup request returned with HTTP code")
                .contains(String.valueOf(HttpURLConnection.HTTP_UNAUTHORIZED)).contains(responseBody);
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(1).getFormattedMessage()).contains("Backup job failed!");
    }

    @Test
    public void shouldFailBecauseBackupRequestFails() throws UnirestException {
        exit.expectSystemExitWithStatus(1);

        when(backupService.setArguments(anyString())).thenReturn(true);
        when(backupService.requestBackup()).thenThrow(UnirestException.class);

        instance.run("args");

        // Assert the log messages
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Error processing request: ");
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(1).getFormattedMessage()).contains("Backup job failed!");
    }

    @Test
    public void shouldFailBecauseFileDownloadFails() throws UnirestException, InterruptedException {
        exit.expectSystemExitWithStatus(1);

        final String responseBody = "{\"taskId\":\"10023\"}";

        when(stringHttpResponse.getBody()).thenReturn(responseBody);
        when(stringHttpResponse.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);

        when(backupService.setArguments(anyString())).thenReturn(true);
        when(backupService.requestBackup()).thenReturn(stringHttpResponse);
        when(backupService.getBackupFileUrl(anyString())).thenReturn(Optional.empty());

        instance.run("args");

        // Assert the log messages
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Backup task ID is 10023");
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(1).getFormattedMessage()).contains("Backup job failed!");
    }

    @Test
    public void shouldFailBecauseFileSavingFails() throws UnirestException, InterruptedException, IOException {
        exit.expectSystemExitWithStatus(1);

        final String responseBody = "{\"taskId\":\"10023\"}";

        when(stringHttpResponse.getBody()).thenReturn(responseBody);
        when(stringHttpResponse.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);

        when(backupService.setArguments(anyString())).thenReturn(true);
        when(backupService.requestBackup()).thenReturn(stringHttpResponse);
        when(backupService.getBackupFileUrl(anyString())).thenReturn(Optional.of("some/url"));
        doThrow(IOException.class).when(backupService).downloadBackupFile(anyString());

        instance.run("args");

        // Assert the log messages
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Backup task ID is 10023");
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(1).getFormattedMessage()).contains("Cannot save backup archive due to IOException");
        assertThat(logsList.get(2).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(2).getFormattedMessage()).contains("Backup job failed!");
    }
}
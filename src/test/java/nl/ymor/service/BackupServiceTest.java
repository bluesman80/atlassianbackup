package nl.ymor.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class BackupServiceTest {

    @Mock(answer = Answers.RETURNS_MOCKS)
    private RestClient restClient;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private HttpResponse<JsonNode> httpResponse;

    @Mock
    private HttpResponse<InputStream> downloadRequest;

    @Autowired
    @InjectMocks
    private BackupService instance;

    // Get Logback Logger (will be used to assert logs)
    private final Logger backupServiceLogger = (Logger) LoggerFactory.getLogger(BackupService.class);
    // Create a ListAppender
    private final ListAppender<ILoggingEvent> listAppender = new ListAppender<>();

    @Before
    public void setup() throws UnirestException {
        // Initialize mocks
        MockitoAnnotations.initMocks(this);

        when(restClient.doProgressCheckRequest(anyString())).thenReturn(httpResponse);

        // Start the ListAppender
        listAppender.start();
        // Add the appender to the logger
        backupServiceLogger.addAppender(listAppender);
    }

    @Test
    public void shouldGetBackupFileUrlByRequestingProgressTwice() throws UnirestException, InterruptedException {
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

        assertThat(backupFileURLOptional).isPresent().isEqualTo(backupFileURL);

        // assert the log messages
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Backup progress: 0");
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(1).getFormattedMessage()).contains("Backup progress: 100");
        assertThat(logsList.get(2).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(2).getFormattedMessage()).contains("Backup finished successfully.");
    }

    @Test
    public void shouldGetAnEmptyBackupFileUrlDueToInsufficientProgressCheckNumber() throws UnirestException,
            InterruptedException {
        final JsonNode progressResponseIncomplete =
                new JsonNode("{\"status\":\"InProgress\",\"description\":\"Cloud Export task\"," +
                        "\"message\":\"Preparing database for export (it may take up to 30 minutes)\"," +
                        "\"progress\":0}");

        when(httpResponse.getStatus()).thenReturn(200);
        when(httpResponse.getBody()).thenReturn(progressResponseIncomplete);

        Optional<String> backupFileURLOptional = instance.getBackupFileUrl(instance.KEY_TASK_ID);

        assertThat(backupFileURLOptional).isEmpty();

        // assert the log messages
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(0).getFormattedMessage()).contains("Backup progress: 0");
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(1).getFormattedMessage()).contains("Backup progress: 0");
    }

    @Test(expected = UnirestException.class)
    public void shouldFailBackupFileUrlDueToServerResponse() throws InterruptedException, UnirestException {
        when(httpResponse.getStatus()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(httpResponse.getBody()).thenReturn(new JsonNode(""));

        instance.getBackupFileUrl(instance.KEY_TASK_ID);
    }

    @Test
    public void shouldValidateArguments() {
        assertThat(instance.setArguments(
                "-u", "bluesman80@ymor.com",
                "-i", "blackdog.atlassian.net",
                "-t", "token",
                "-d", "./target/download")).isTrue();

        assertThat(instance.setArguments(
                "--username", "bluesman80@ymor.com",
                "--instance", "blackdog.atlassian.net",
                "--token", "token",
                "--dir", "./target/download")).isTrue();
    }

    @Test
    public void shouldNotValidateArgumentsAndPrintUsage() {
        assertThat(instance.setArguments(
                "-u", "bluesman80@ymor.com",
                "-i", "blackdog.atlassian.net",
                "-d", "./target/download")).isFalse();

        // Assert the log messages
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(0).getFormattedMessage()).contains("The following option is required: [-t | --token]");
    }

    @Test
    public void shouldDownloadBackupFile() throws UnirestException, IOException {
        final String downloadDirectory = "src/test/data";
        final Path path = Paths.get(downloadDirectory, instance.archiveFileName);

        if (path.toFile().exists()) {
            assertThat(path.toFile().delete()).isTrue();
        } else {
            if (!new File(downloadDirectory).exists())
                assertThat(new File(downloadDirectory).mkdirs()).isTrue();
        }

        InputStream backupFileInputStream = new ByteArrayInputStream("This is a backup file".getBytes());

        when(restClient.doDownloadRequest(anyString(), anyString())).thenReturn(downloadRequest);
        when(downloadRequest.getStatus()).thenReturn(HttpURLConnection.HTTP_OK);
        when(downloadRequest.getRawBody()).thenReturn(backupFileInputStream);

        instance.setArguments(
                "--username", "bluesman80@ymor.com",
                "--token", "token",
                "--dir", new File(downloadDirectory).getAbsolutePath());

        instance.downloadBackupFile("partial/URL/Of/The/Backup/File/To/Download");

        assertThat(path.toFile()).exists().isFile().hasExtension("zip");
    }

    @Test(expected = UnirestException.class)
    public void shouldFailDownloadingBackupFile() throws UnirestException, IOException {
        when(restClient.doDownloadRequest(anyString(), anyString())).thenReturn(downloadRequest);
        when(downloadRequest.getStatus()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        instance.downloadBackupFile("partial/URL/Of/The/Backup/File/To/Download");
    }

    @Test
    public void testRequestBackup() throws UnirestException {
        instance.requestBackup();
        verify(restClient, times(1)).doBackupRequest();
    }
}

package nl.ymor.service;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.http.utils.Base64Coder;
import nl.ymor.model.Arguments;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

@SuppressWarnings({"squid:S1192", "squid:S00116", "squid:S1170"})
@Service
public class BackupService {

    private static final Logger LOG = LoggerFactory.getLogger(BackupService.class);
    private static final String CHECK_PROGRESS_ENDPOINT = "https://%s/rest/backup/1/export/getProgress?taskId=%s";

    @Value("${archive.file.name.extension}")
    private String archiveFileName;
    @Value("${maximum.progress.check.times}")
    protected String progressChecks;
    @Value("${pause.milliseconds.between.progress.checks}")
    protected String sleepTimeMillis;

    private static final String KEY_PROGRESS = "progress";
    private static final String KEY_BACKUP_FILE_URL = "result";
    public final String KEY_TASK_ID = "taskId";
    private Arguments arguments;
    private RestClient restClient;

    public BackupService(final Arguments arguments, final RestClient restClient) {
        this.arguments = arguments;
        this.restClient = restClient;
    }

    public boolean areArgumentsValid(final String... args) {
        final JCommander argumentParser = JCommander.newBuilder().addObject(arguments).build();

        try {
            argumentParser.parse(args);

            arguments.setAuthorization(
                    "Basic " + Base64Coder.encodeString(arguments.getUser() + ":" + arguments.getApiToken()));
        } catch (final ParameterException e) {
            LOG.error(e.getMessage());
            argumentParser.usage();

            return false;
        }

        return true;
    }

    public HttpResponse<String> requestBackup() throws UnirestException {
        return restClient.doBackupRequest();
    }

    @SuppressWarnings("squid:ForLoopCounterChangedCheck")
    public Optional<String> getBackupFileUrl(final String taskID) throws UnirestException, InterruptedException {
        final int progressCheckCount = Integer.parseInt(progressChecks);
        final int sleepTime = Integer.parseInt(sleepTimeMillis);

        for (int i = 0, progressPercentage = 0; i < progressCheckCount && progressPercentage < 100; i++) {
            final String progressCheckUrl = String.format(CHECK_PROGRESS_ENDPOINT, arguments.getInstanceUrl(), taskID);

            final HttpResponse<JsonNode> progressResponse = restClient.doProgressCheckRequest(progressCheckUrl);
            final JSONObject responseBody = progressResponse.getBody().getObject();

            if (progressResponse.getStatus() == 200 && responseBody.has(KEY_PROGRESS)) {
                progressPercentage =
                        Integer.valueOf(responseBody.get(KEY_PROGRESS).toString());

                LOG.info("Backup progress: {}", progressPercentage);

                if (progressPercentage == 100) {
                    LOG.info("Backup finished successfully.");

                    return Optional.of(responseBody.get(KEY_BACKUP_FILE_URL).toString());
                    // Backup File URL example: export/download/?fileId=11f959da-1c70-4519-8e6f-633748f7b832
                }

                Thread.sleep(sleepTime);
            } else {
                throw new UnirestException(String.format("Cannot check progress. Response from server: %s",
                        progressResponse.getStatus()));
            }
        }

        return Optional.empty();
    }

    public void downloadBackupFile(final String backupFilePartialURL) throws UnirestException, IOException {
        final String instance = arguments.getInstanceUrl();
        final String downloadDirectory = arguments.getDirectory();

        LOG.info("Downloading now: {} (this may take a while)", backupFilePartialURL);
        final HttpResponse<InputStream> downloadRequest = restClient.doDownloadRequest(backupFilePartialURL, instance);

        if (downloadRequest.getStatus() == 200) {
            LOG.info("Downloaded the backup file. Saving to {}", downloadDirectory);

            final InputStream inputStream = downloadRequest.getRawBody();
            Files.copy(inputStream, Paths.get(downloadDirectory, archiveFileName), StandardCopyOption.REPLACE_EXISTING);

            LOG.info("Saved the backup file to {}", downloadDirectory);

            inputStream.close();
        } else {
            throw new UnirestException(String.format("Cannot download file: %s%nDownload response: %s",
                    backupFilePartialURL, downloadRequest.getStatus()));
        }
    }

}

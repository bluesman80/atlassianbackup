package nl.ymor.service;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import nl.ymor.model.Arguments;

import java.io.InputStream;

public class RestClient {
    private final Arguments arguments;
    private static final String DOWNLOAD_END_POINT = "https://%s/plugins/servlet/%s";
    private static final String BACKUP_ENDPOINT = "https://%s/rest/backup/1/export/runbackup";

    public RestClient(final Arguments arguments) {
        this.arguments = arguments;
        Unirest.setDefaultHeader("accept-encoding", "gzip, deflate, br");
        Unirest.setDefaultHeader("Authorization", this.arguments.getAuthorization());
    }

    HttpResponse<String> doBackupRequest() throws UnirestException {
        // curl
        // -H 'origin: https://${INSTANCE}'
        // -H 'accept-encoding: gzip, deflate, br'
        // -H 'content-type: application/json'
        // -H 'accept: application/json, text/javascript, */*; q=0.01'
        // -H 'x-requested-with: XMLHttpRequest'
        // --data-binary '{"cbAttachments":"true", "exportToCloud":"true"}'
        // --request POST "https://${INSTANCE}/rest/backup/1/export/runbackup"
        // --user ${EMAIL}:${API_TOKEN}
        return Unirest.post(String.format(BACKUP_ENDPOINT, arguments.getInstanceUrl()))
                .header("origin", arguments.getInstanceUrl())
                .header("content-type", "application/json")
                .header("accept", "application/json, text/javascript, */*; q=0.01")
                .header("x-requested-with", "XMLHttpRequest")
                .body("{\"cbAttachments\":\"true\", \"exportToCloud\":\"true\"}")
                .asString();
    }

    HttpResponse<JsonNode> doProgressCheckRequest(final String progressCheckUrl) throws UnirestException {
        // curl
        // -H 'accept-encoding: gzip, deflate, br'
        // -H 'accept: application/json, text/javascript, */*; q=0.01'
        // --user ${EMAIL}:${API_TOKEN}
        // "https://${INSTANCE}/rest/backup/1/export/getProgress?taskId=${TASK_ID}"
        return Unirest.get(progressCheckUrl)
                .header("accept", "application/json, text/javascript, */*; q=0.01")
                .asJson();
    }

    HttpResponse<InputStream> doDownloadRequest(final String backupFileUrl, final String instance)
            throws UnirestException {
        // curl 'https://${INSTANCE}/plugins/servlet/export/download/?fileId=${FILE_ID}'
        //      -H 'accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0
        //      .8,application/signed-exchange;v=b3'
        //      -H 'accept-encoding: gzip, deflate, br'
        //      --user ${EMAIL}:${API_TOKEN}
        return Unirest.get(String.format(DOWNLOAD_END_POINT, instance, backupFileUrl))
                .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng," +
                        "*/*;q=0.8,application/signed-exchange;v=b3")
                .asBinary();
    }
}

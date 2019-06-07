package nl.ymor.model;

import com.beust.jcommander.Parameter;
import org.springframework.stereotype.Component;

@Component
public class Arguments {
    //<user email> <API token> <instance> <download directory>

    @Parameter(names = {"-u", "--username"}, description = "User to execute backup job", required = true)
    private String user;

    @Parameter(names = {"-t", "--token"}, description = "API token to authenticate the user", required = true)
    private String apiToken;

    @Parameter(names = {"-i", "--instance"}, description = "Jira instance URL")
    private String instanceUrl = "userhappiness.atlassian.net";

    @Parameter(names = {"-d", "--dir"}, description = "Directory path to save the downloaded backup file")
    private String directory = ".";

    @Parameter(hidden = true)
    private String authorization;

    public String getUser() {
        return user;
    }

    public void setUser(final String user) {
        this.user = user;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(final String apiToken) {
        this.apiToken = apiToken;
    }

    public String getInstanceUrl() {
        return instanceUrl;
    }

    public void setInstanceUrl(final String instanceUrl) {
        this.instanceUrl = instanceUrl;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    public String getAuthorization() {
        return authorization;
    }

    public void setAuthorization(final String authorization) {
        this.authorization = authorization;
    }
}

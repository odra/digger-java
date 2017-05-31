package org.aerogear.digger.client;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.aerogear.digger.client.model.BuildTriggerStatus;
import org.aerogear.digger.client.services.ArtifactsService;
import org.aerogear.digger.client.services.BuildService;
import org.aerogear.digger.client.services.JobService;
import org.aerogear.digger.client.util.DiggerClientException;
import org.aerogear.digger.client.util.JenkinsAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Digger Java Client interact with Digger Jenkins api.
 */
public class DiggerClient {

    private static final Logger LOG = LoggerFactory.getLogger(DiggerClient.class);

    public static final long DEFAULT_BUILD_TIMEOUT = 60 * 1000;

    private JenkinsServer jenkinsServer;

    private JobService jobService;
    private BuildService buildService;
    private ArtifactsService artifactsService;

    private DiggerClient() {
    }

    /**
     * Create a client with defaults using provided url and credentials.
     * <p>
     * This client will use the defaults for the services. This is perfectly fine for majorith of the cases.
     *
     * @param url      Jenkins url
     * @param user     Jenkins user
     * @param password Jenkins password
     * @return client instance
     * @throws DiggerClientException if something goes wrong
     */
    public static DiggerClient createDefaultWithAuth(String url, String user, String password) throws DiggerClientException {
        BuildService buildService = new BuildService(BuildService.DEFAULT_FIRST_CHECK_DELAY, BuildService.DEFAULT_POLL_PERIOD);
        JobService jobService = new JobService();
        ArtifactsService artifactsService = new ArtifactsService();
        return DiggerClient.builder()
            .createJobService(jobService)
            .triggerBuildService(buildService)
            .artifactsService(artifactsService)
            .withAuth(url, user, password)
            .build();
    }

    public static DiggerClientBuilder builder() {
        return new DiggerClientBuilder();
    }

    public static class DiggerClientBuilder {
        private JenkinsAuth auth;
        private JobService jobService;
        private BuildService buildService;
        private ArtifactsService artifactsService;

        public DiggerClientBuilder withAuth(String url, String user, String password) {
            this.auth = new JenkinsAuth(url, user, password);
            return this;
        }

        public DiggerClientBuilder createJobService(JobService jobService) {
            this.jobService = jobService;
            return this;
        }

        public DiggerClientBuilder triggerBuildService(BuildService buildService) {
            this.buildService = buildService;
            return this;
        }

        public DiggerClientBuilder artifactsService(ArtifactsService artifactsService) {
            this.artifactsService = artifactsService;
            return this;
        }

        public DiggerClient build() throws DiggerClientException {
            final DiggerClient client = new DiggerClient();
            try {
                client.jenkinsServer = new JenkinsServer(new URI(auth.getUrl()), auth.getUser(), auth.getPassword());
                client.jobService = this.jobService;
                client.buildService = this.buildService;
                client.artifactsService = this.artifactsService;
                return client;
            } catch (URISyntaxException e) {
                throw new DiggerClientException("Invalid jenkins url format.");
            }
        }
    }


    /**
     * Create new Digger job on Jenkins platform
     *
     * @param name      job name that can be used later to reference job
     * @param gitRepo   git repository url (full git repository url. e.g git@github.com:wtrocki/helloworld-android-gradle.git
     * @param gitBranch git repository branch (default branch used to checkout source code)
     * @throws DiggerClientException if something goes wrong
     */
    public void createJob(String name, String gitRepo, String gitBranch) throws DiggerClientException {
        try {
            jobService.create(this.jenkinsServer, name, gitRepo, gitBranch);
        } catch (Throwable e) {
            throw new DiggerClientException(e);
        }
    }

    /**
     * Triggers a build for the given job and waits until it leaves the queue and actually starts.
     * <p>
     * Jenkins puts the build requests in a queue and once there is a slave available, it starts building
     * it and a build number is assigned to the build.
     * <p>
     * This method will block until there is a build number, or the given timeout period is passed. If the build is still in the queue
     * after the given timeout period, a {@code BuildStatus} is returned with state {@link BuildTriggerStatus.State#TIMED_OUT}.
     * <p>
     * Please note that timeout period is never meant to be very precise. It has the resolution of {@link BuildService#DEFAULT_POLL_PERIOD} because
     * timeout is checked before every pull.
     * <p>
     * Similarly, {@link BuildTriggerStatus.State#CANCELLED_IN_QUEUE} is returned if the build is cancelled on Jenkins side and
     * {@link BuildTriggerStatus.State#STUCK_IN_QUEUE} is returned if the build is stuck.
     *
     * @param jobName name of the job to trigger the build
     * @param timeout how many milliseconds should this call block before returning {@link BuildTriggerStatus.State#TIMED_OUT}.
     *                Should be larger than {@link BuildService#DEFAULT_FIRST_CHECK_DELAY}
     * @return the build status
     * @throws DiggerClientException if connection problems occur during connecting to Jenkins
     */
    public BuildTriggerStatus build(String jobName, long timeout) throws DiggerClientException {
        try {
            return buildService.build(this.jenkinsServer, jobName, timeout);
        } catch (IOException e) {
            LOG.debug("Exception while connecting to Jenkins", e);
            throw new DiggerClientException(e);
        } catch (InterruptedException e) {
            LOG.debug("Exception while waiting on Jenkins", e);
            throw new DiggerClientException(e);
        } catch (Throwable e) {
            LOG.debug("Exception while triggering a build", e);
            throw new DiggerClientException(e);
        }
    }

    /**
     * Triggers a build for the given job and waits until it leaves the queue and actually starts.
     * <p>
     * Calls {@link #build(String, long)} with a default timeout of {@link #DEFAULT_BUILD_TIMEOUT}.
     *
     * @param jobName name of the job
     * @return the build status
     * @throws DiggerClientException if connection problems occur during connecting to Jenkins
     * @see #build(String, long)
     */
    public BuildTriggerStatus build(String jobName) throws DiggerClientException {
        return this.build(jobName, DEFAULT_BUILD_TIMEOUT);
    }

    /**
     * Fetch artifacts urls for specific job and build number
     *
     * @param jobName      name of the job
     * @param buildNumber  job build number
     * @param artifactName - name of the artifact to fetch - can be regexp
     * @return InputStream with file contents
     * @throws DiggerClientException - when problem with fetching artifacts from jenkins
     */
    public InputStream fetchArtifact(String jobName, int buildNumber, String artifactName) throws DiggerClientException {
        return artifactsService.streamArtifact(jenkinsServer, jobName, buildNumber, artifactName);
    }

    /**
     * Save artifact for specified location for specific job, build number and artifact name.
     * If name would be an regular expression method would return stream for the first match.
     *
     * @param jobName      name of the job
     * @param buildNumber  job build number
     * @param artifactName name of the artifact to fetch - can be regexp for example *.apk
     * @param outputFile   file (location) used to save artifact
     * @throws DiggerClientException when problem with fetching artifacts from jenkins
     * @throws IOException           when one of the files cannot be saved
     */
    public void saveArtifact(String jobName, int buildNumber, String artifactName, File outputFile) throws DiggerClientException, IOException {
        artifactsService.saveArtifact(jenkinsServer, jobName, buildNumber, artifactName, outputFile);
    }

    /**
     * Get build logs for specific job and build number
     *
     * @param jobName     name of the job
     * @param buildNumber job build number
     * @return String with file contents that can be saved or piped to socket
     * @throws DiggerClientException when problem with fetching artifacts from jenkins
     */
    public String getBuildLogs(String jobName, int buildNumber) throws DiggerClientException {
        return buildService.getBuildLogs(jenkinsServer, jobName, buildNumber);
    }

    /**
     * Returns the build history for a job. As reported by {@link JobWithDetails#getBuilds()} it will return max 100 most-recent builds.
     * <p>
     * Please note that this approach will take some time since we first fetch the builds in 1 call, then fetch build details in 1 call per build.
     *
     * @param jobName name of the job
     * @return the build history
     * @throws DiggerClientException if connection problems occur
     */
    public List<BuildWithDetails> getBuildHistory(String jobName) throws DiggerClientException {
        return buildService.getBuildHistory(jenkinsServer, jobName);
    }
}

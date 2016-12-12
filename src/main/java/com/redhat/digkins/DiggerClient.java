package com.redhat.digkins;

import com.offbytwo.jenkins.JenkinsServer;
import com.redhat.digkins.model.BuildStatus;
import com.redhat.digkins.services.CreateJobService;
import com.redhat.digkins.services.TriggerBuildService;
import com.redhat.digkins.util.DiggerClientException;
import com.redhat.digkins.util.JenkinsAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Digger Java Client interact with Digger Jenkins api.
 */
public class DiggerClient {

  private static final Logger LOG = LoggerFactory.getLogger(DiggerClient.class);

  public static final long DEFAULT_BUILD_TIMEOUT = 60 * 1000;

  private JenkinsServer jenkinsServer;

  private CreateJobService createJobService;
  private TriggerBuildService triggerBuildService;

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
    return DiggerClient.builder()
      .createJobService(new CreateJobService())
      .triggerBuildService(new TriggerBuildService(TriggerBuildService.DEFAULT_FIRST_CHECK_DELAY, TriggerBuildService.DEFAULT_POLL_PERIOD))
      .withAuth(url, user, password)
      .build();
  }

  public static DiggerClientBuilder builder() {
    return new DiggerClientBuilder();
  }

  public static class DiggerClientBuilder {
    private JenkinsAuth auth;
    private CreateJobService createJobService;
    private TriggerBuildService triggerBuildService;

    public DiggerClientBuilder withAuth(String url, String user, String password) {
      this.auth = new JenkinsAuth(url, user, password);
      return this;
    }

    public DiggerClientBuilder createJobService(CreateJobService createJobService) {
      this.createJobService = createJobService;
      return this;
    }

    public DiggerClientBuilder triggerBuildService(TriggerBuildService triggerBuildService) {
      this.triggerBuildService = triggerBuildService;
      return this;
    }

    public DiggerClient build() throws DiggerClientException {
      final DiggerClient client = new DiggerClient();
      try {
        client.jenkinsServer = new JenkinsServer(new URI(auth.getUrl()), auth.getUser(), auth.getPassword());
        client.createJobService = this.createJobService;
        client.triggerBuildService = this.triggerBuildService;
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
      createJobService.create(this.jenkinsServer, name, gitRepo, gitBranch);
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
   * after the given timeout period, a {@code BuildStatus} is returned with state {@link BuildStatus.State#TIMED_OUT}.
   * <p>
   * Please note that timeout period is never meant to be very precise. It has the resolution of {@link TriggerBuildService#DEFAULT_POLL_PERIOD} because
   * timeout is checked before every pull.
   * <p>
   * Similarly, {@link BuildStatus.State#CANCELLED_IN_QUEUE} is returned if the build is cancelled on Jenkins side and
   * {@link BuildStatus.State#STUCK_IN_QUEUE} is returned if the build is stuck.
   *
   * @param jobName name of the job to trigger the build
   * @param timeout how many milliseconds should this call block before returning {@link BuildStatus.State#TIMED_OUT}.
   *                Should be larger than {@link TriggerBuildService#DEFAULT_FIRST_CHECK_DELAY}
   * @return the build status
   * @throws DiggerClientException if connection problems occur during connecting to Jenkins
   */
  public BuildStatus build(String jobName, long timeout) throws DiggerClientException {
    try {
      return triggerBuildService.build(this.jenkinsServer, jobName, timeout);
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
  public BuildStatus build(String jobName) throws DiggerClientException {
    return this.build(jobName, DEFAULT_BUILD_TIMEOUT);
  }

}

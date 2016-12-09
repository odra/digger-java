package com.redhat.digkins;

import com.offbytwo.jenkins.JenkinsServer;
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

  private final JenkinsServer jenkins;

  public DiggerClient(JenkinsAuth auth) throws URISyntaxException {
    this.jenkins = new JenkinsServer(new URI(auth.getUrl()), auth.getUser(), auth.getPassword());
  }

  /**
   * Create client using provided url and credentials
   *
   * @param url      Jenkins url
   * @param user     Jenkins user
   * @param password Jenkins password
   * @return client instance
   * @throws DiggerClientException if something goes wrong
   */
  public static DiggerClient from(String url, String user, String password) throws DiggerClientException {
    try {
      JenkinsAuth jenkinsAuth = new JenkinsAuth(url, user, password);
      return new DiggerClient(jenkinsAuth);
    } catch (URISyntaxException e) {
      throw new DiggerClientException("Invalid jenkins url format.");
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
    CreateJobService service = new CreateJobService(this.jenkins);
    try {
      service.create(name, gitRepo, gitBranch);
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
   * after the given timeout period, -1 is returned as the build number.
   * <p>
   * Please note that timeout period is never meant to be very precise. It has the resolution of {@link TriggerBuildService#POLL_PERIOD} because
   * timeout is checked before every pull.
   * <p>
   * Similarly, -1 is returned if the build is stuck or cancelled on Jenkins side.
   *
   * @param jobName name of the job to trigger the build
   * @param timeout how many milliseconds should this call block before returning -1. Should be larger than {@link TriggerBuildService#FIRST_CHECK_DELAY}
   * @return the build number. -1 if build is cancelled, stuck or in queue more than the given timeout period
   * @throws DiggerClientException if connection problems occur during connecting to Jenkins
   */
  public long build(String jobName, long timeout) throws DiggerClientException {
    final TriggerBuildService triggerBuildService = new TriggerBuildService(jenkins);
    try {
      return triggerBuildService.build(jobName, timeout);
    } catch (IOException e) {
      LOG.debug("Exception while connecting to Jenkins", e);
      throw new DiggerClientException(e);
    } catch (InterruptedException e) {
      LOG.debug("Exception while sleeping between checks", e);
      throw new DiggerClientException(e);
    }
  }

  /**
   * Triggers a build for the given job and waits until it leaves the queue and actually starts.
   * <p>
   * Calls {@link #build(String, long)} with a default timeout of {@link #DEFAULT_BUILD_TIMEOUT}.
   *
   * @param jobName name of the job
   * @return the build number
   * @throws DiggerClientException if connection problems occur during connecting to Jenkins
   * @see #build(String, long)
   */
  public long build(String jobName) throws DiggerClientException {
    return this.build(jobName, DEFAULT_BUILD_TIMEOUT);
  }

}

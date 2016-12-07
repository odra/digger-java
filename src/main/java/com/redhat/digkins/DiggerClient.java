package com.redhat.digkins;

import com.offbytwo.jenkins.JenkinsServer;
import com.redhat.digkins.services.CreateJobService;
import com.redhat.digkins.util.JenkinsAuth;
import com.redhat.digkins.util.DiggerClientException;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Digger Java Client interact with Digger Jenkins api.
 */
public class DiggerClient {

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

}

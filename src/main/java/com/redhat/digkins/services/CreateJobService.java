package com.redhat.digkins.services;

import com.offbytwo.jenkins.JenkinsServer;
import org.apache.commons.io.FileUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.io.File;
import java.io.IOException;

/**
 * Create digger job on jenkins platform
 */
public class CreateJobService {

  private JenkinsServer jenkins;

  private final static String GIT_REPO_URL = "GIT_REPO_URL", GIT_REPO_BRANCH = "GIT_REPO_BRANCH";

  /**
   * @param jenkins - jenkins api instance
   */
  public CreateJobService(JenkinsServer jenkins) {
    this.jenkins = jenkins;
  }

  /**
   * Create new digger job on jenkins platform
   *
   * @param name      - job name that can be used later to reference job
   * @param gitRepo   - git repository url (full git repository url. e.g git@github.com:digger/helloworld.git
   * @param gitBranch - git repository branch (default branch used to checkout source code)
   */
  public void create(String name, String gitRepo, String gitBranch) throws IOException {
    JtwigTemplate template = JtwigTemplate.classpathTemplate("templates/job.xml");
    JtwigModel model = JtwigModel.newModel().with(GIT_REPO_URL, gitRepo).with(GIT_REPO_BRANCH, gitBranch);
    jenkins.createJob(name, template.render(model));
  }

}

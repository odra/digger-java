package org.aerogear.digger.client.services;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Artifact;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.aerogear.digger.client.util.DiggerClientException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.List;

/**
 * Service used to retrieve artifacts
 */
public class ArtifactsService {

  /**
   */
  public ArtifactsService() {
  }

  private static final Logger LOG = LoggerFactory.getLogger(ArtifactsService.class);
  private static int DEFAULT_BUFFER = 8 * 1024;

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
  public void saveArtifact(JenkinsServer jenkins, String jobName, int buildNumber, String artifactName, File outputFile) throws DiggerClientException, IOException {
    InputStream inputStream = streamArtifact(jenkins, jobName, buildNumber, artifactName);
    if (inputStream != null) {
      OutputStream outStream = new FileOutputStream(outputFile);
      byte[] buffer = new byte[DEFAULT_BUFFER];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outStream.write(buffer, 0, bytesRead);
      }
      IOUtils.closeQuietly(inputStream);
      IOUtils.closeQuietly(outStream);
    } else {
      throw new DiggerClientException("Cannot fetch artifacts from jenkins");
    }
  }

  /**
   * Get artifact inputstream for specific job, build number and artifact name.
   * If name would be an regular expression method would return stream for the first match.
   *
   * @param jobName      name of the job
   * @param buildNumber  job build number
   * @param artifactName name of the artifact to fetch - can be regexp for example *.apk
   * @return InputStream with file contents that can be saved or piped to socket
   * @throws DiggerClientException when problem with fetching artifacts from jenkins
   */
  public InputStream streamArtifact(JenkinsServer jenkins, String jobName, int buildNumber, String artifactName) throws DiggerClientException {
    try {
      JobWithDetails job = jenkins.getJob(jobName);
      if (job == null) {
        LOG.error("Cannot fetch job from jenkins {0}", jobName);
        throw new DiggerClientException("Cannot fetch job from jenkins");
      }
      Build build = job.getBuildByNumber(buildNumber);
      BuildWithDetails buildWithDetails = build.details();
      List<Artifact> artifacts = buildWithDetails.getArtifacts();
      for (Artifact artifact : artifacts) {
        if (artifact.getFileName().matches(artifactName)) {
          LOG.debug("Streaming artifact {0}", artifactName);
          return buildWithDetails.downloadArtifact(artifact);
        }
      }
    } catch (URISyntaxException e) {
      LOG.error("Invalid job name {0}", jobName, e);
      throw new DiggerClientException(e);
    } catch (IOException e) {
      LOG.error("Problem when fetching artifacts for {0} {1} {2}", jobName, buildNumber, artifactName, e);
      throw new DiggerClientException(e);
    }
    LOG.debug("Cannot find build for ", jobName, buildNumber, artifactName);
    return null;
  }

}

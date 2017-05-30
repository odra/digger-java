package org.aerogear.digger.client.services;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactsServiceTests {

  @Mock
  private JenkinsServer server;
  private ArtifactsService artifactsService;

  @Before
  public void beforeTests() {
    artifactsService = new ArtifactsService();
  }

  @Test
  public void shouldFetchArtifactsWithNoResults() throws Exception {
    BuildWithDetails build = mock(BuildWithDetails.class);
    JobWithDetails job = mock(JobWithDetails.class);
    when(server.getJob(anyString())).thenReturn(job);
    when(job.getBuildByNumber(anyInt())).thenReturn(build);
    when(build.details()).thenReturn(build);
    InputStream artifact = artifactsService.streamArtifact(server, "artifact", 1, "test");

    assertNull(artifact);
  }

  @Test
  public void shouldFetchArtifactsWithResults() throws Exception {
    Artifact artifact = mock(Artifact.class);
    JobWithDetails job = mock(JobWithDetails.class);
    BuildWithDetails build = mock(BuildWithDetails.class);
    FileInputStream fs = mock(FileInputStream.class);
    when(server.getJob(anyString())).thenReturn(job);
    when(job.getBuildByNumber(anyInt())).thenReturn(build);
    when(build.details()).thenReturn(build);
    when(build.getArtifacts()).thenReturn(Arrays.asList(artifact));
    when(build.downloadArtifact(artifact)).thenReturn(fs);
    String artifactName = "test";
    when(artifact.getFileName()).thenReturn(artifactName);
    InputStream artifactStream = artifactsService.streamArtifact(server, "artifact", 1, artifactName);

    assertThat(artifactStream).isNotNull();
  }


  @Test
  public void shouldSaveFileArtifactWithResults() throws Exception {
    Artifact artifact = mock(Artifact.class);
    FileInputStream fs = mock(FileInputStream.class);
    JobWithDetails job = mock(JobWithDetails.class);
    BuildWithDetails build = mock(BuildWithDetails.class);
    String artifactName = "test";
    when(server.getJob(anyString())).thenReturn(job);
    when(job.getBuildByNumber(anyInt())).thenReturn(build);
    when(build.details()).thenReturn(build);
    when(build.getArtifacts()).thenReturn(Arrays.asList(artifact));
    when(artifact.getFileName()).thenReturn(artifactName);
    when(build.downloadArtifact(artifact)).thenReturn(fs);
    when(fs.read(Matchers.<byte[]>anyObject())).thenReturn(-1);
    when(build.details()).thenReturn(build);
    File outputFile = new File("test.out");
    artifactsService.saveArtifact(server, "artifact", 1, artifactName, outputFile);

    assertThat(outputFile.exists()).isTrue();
    outputFile.delete();
  }
}

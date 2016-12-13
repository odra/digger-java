package com.redhat.digkins.services;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class JobServiceTests {

  @Mock
  private JenkinsServer server;
  private JobService jobService;

  @Before
  public void beforeTests() {
    jobService = new JobService();
  }

  @Test
  public void shouldCreateJob() throws Exception {
    JobWithDetails job = mock(JobWithDetails.class);
    BuildWithDetails build = mock(BuildWithDetails.class);
    when(job.getBuildByNumber(anyInt())).thenReturn(build);
    when(build.details()).thenReturn(build);
    jobService.create(server, "name", "repo", "branch");
    verify(server, times(1)).createJob(anyString(), anyString());
  }

}

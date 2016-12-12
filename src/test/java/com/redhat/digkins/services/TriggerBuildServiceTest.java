package com.redhat.digkins.services;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Executable;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import com.redhat.digkins.model.BuildStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(MockitoJUnitRunner.class)
public class TriggerBuildServiceTest {

  private TriggerBuildService service;

  @Mock
  JenkinsServer jenkinsServer;

  @Mock
  JobWithDetails mockJob;

  QueueReference queueReference = new QueueReference("https://jenkins.example.com/queue/item/123/");

  @Before
  public void setUp() throws Exception {
    service = new TriggerBuildService(300, 50);   // wait for 300 msecs for initial build, check every 50 msecs

    Mockito.when(jenkinsServer.getJob("TEST")).thenReturn(mockJob);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldThrowExceptionIfJobCannotBeFound() throws Exception {
    service.build(jenkinsServer, "UNKNOWN", 10000);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionIfJenkinsDoesNotReturnQueueReference() throws Exception {
    Mockito.when(mockJob.build()).thenReturn(null);
    service.build(jenkinsServer, "TEST", 10000);
  }

  @Test(expected = IllegalStateException.class)
  public void shouldThrowExceptionIfQueueItemIsNullForReference() throws Exception {
    Mockito.when(mockJob.build()).thenReturn(queueReference);
    Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(null);
    service.build(jenkinsServer, "TEST", 10000);
  }

  @Test
  public void shouldReturnCancelledStatus() throws Exception {
    final QueueItem queueItem = new QueueItem();
    queueItem.setCancelled(true);

    Mockito.when(mockJob.build()).thenReturn(queueReference);
    Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItem);

    final BuildStatus buildStatus = service.build(jenkinsServer, "TEST", 10000);
    assertThat(buildStatus).isNotNull();
    assertThat(buildStatus.getState()).isEqualTo(BuildStatus.State.CANCELLED_IN_QUEUE);
  }

  @Test
  public void shouldReturnStuckStatus() throws Exception {
    final QueueItem queueItem = new QueueItem();
    queueItem.setStuck(true);

    Mockito.when(mockJob.build()).thenReturn(queueReference);
    Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItem);

    final BuildStatus buildStatus = service.build(jenkinsServer, "TEST", 10000);
    assertThat(buildStatus).isNotNull();
    assertThat(buildStatus.getState()).isEqualTo(BuildStatus.State.STUCK_IN_QUEUE);
  }

  @Test
  public void shouldReturnBuildNumber() throws Exception {
    final QueueItem queueItem = new QueueItem();
    final Executable executable = new Executable();
    executable.setNumber(98L);
    queueItem.setExecutable(executable);

    Mockito.when(mockJob.build()).thenReturn(queueReference);
    Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItem);
    final BuildStatus buildStatus = service.build(jenkinsServer, "TEST", 10000);

    assertThat(buildStatus).isNotNull();
    assertThat(buildStatus.getState()).isEqualTo(BuildStatus.State.BUILDING);
    assertThat(buildStatus.getBuildNumber()).isEqualTo(98);
  }

  @Test
  public void shouldReturnBuildNumber_whenDidNotStartExecutingImmediately() throws Exception {
    final QueueItem queueItemNotBuildingYet = new QueueItem();

    final QueueItem queueItemBuilding = new QueueItem();
    queueItemBuilding.setExecutable(new Executable());
    queueItemBuilding.getExecutable().setNumber(98L);

    Mockito.when(mockJob.build()).thenReturn(queueReference);
    // return `not-building` for the first 2 checks, then return `building`
    Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItemNotBuildingYet, queueItemNotBuildingYet, queueItemBuilding);
    final BuildStatus buildStatus = service.build(jenkinsServer, "TEST", 10000L);

    assertThat(buildStatus).isNotNull();
    assertThat(buildStatus.getState()).isEqualTo(BuildStatus.State.BUILDING);
    assertThat(buildStatus.getBuildNumber()).isEqualTo(98);

    Mockito.verify(jenkinsServer, Mockito.times(3)).getQueueItem(queueReference);
  }

  @Test
  public void shouldReturnTimeout() throws Exception {
    final QueueItem queueItemNotBuildingYet = new QueueItem();

    Mockito.when(mockJob.build()).thenReturn(queueReference);
    Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItemNotBuildingYet);
    final BuildStatus buildStatus = service.build(jenkinsServer, "TEST", 500L);

    assertThat(buildStatus).isNotNull();
    assertThat(buildStatus.getState()).isEqualTo(BuildStatus.State.TIMED_OUT);

    Mockito.verify(jenkinsServer, Mockito.atLeast(2)).getQueueItem(queueReference);
  }

}

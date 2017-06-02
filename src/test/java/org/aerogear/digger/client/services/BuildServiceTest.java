/**
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aerogear.digger.client.services;

import com.google.common.collect.Lists;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.*;
import org.aerogear.digger.client.model.BuildTriggerStatus;
import org.aerogear.digger.client.util.DiggerClientException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class BuildServiceTest {

    private BuildService service;

    @Mock
    JenkinsServer jenkinsServer;

    @Mock
    JobWithDetails mockJob;

    QueueReference queueReference = new QueueReference("https://jenkins.example.com/queue/item/123/");

    @Before
    public void setUp() throws Exception {
        service = new BuildService(300, 50);   // wait for 300 msecs for initial build, check every 50 msecs

        Mockito.when(jenkinsServer.getJob("TEST")).thenReturn(mockJob);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionIfJobCannotBeFound() throws Exception {
        service.build(jenkinsServer, "UNKNOWN", 10000, null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfJenkinsDoesNotReturnQueueReference() throws Exception {
        Mockito.when(mockJob.build()).thenReturn(null);
        service.build(jenkinsServer, "TEST", 10000, null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfQueueItemIsNullForReference() throws Exception {
        Mockito.when(mockJob.build()).thenReturn(queueReference);
        Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(null);
        service.build(jenkinsServer, "TEST", 10000, null);
    }

    @Test
    public void shouldReturnCancelledStatus() throws Exception {
        final QueueItem queueItem = new QueueItem();
        queueItem.setCancelled(true);

        Mockito.when(mockJob.build()).thenReturn(queueReference);
        Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItem);

        final BuildTriggerStatus buildTriggerStatus = service.build(jenkinsServer, "TEST", 10000, null);
        assertThat(buildTriggerStatus).isNotNull();
        assertThat(buildTriggerStatus.getState()).isEqualTo(BuildTriggerStatus.State.CANCELLED_IN_QUEUE);
    }

    @Test
    public void shouldReturnStuckStatus() throws Exception {
        final QueueItem queueItem = new QueueItem();
        queueItem.setStuck(true);

        Mockito.when(mockJob.build()).thenReturn(queueReference);
        Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItem);

        final BuildTriggerStatus buildTriggerStatus = service.build(jenkinsServer, "TEST", 10000, null);
        assertThat(buildTriggerStatus).isNotNull();
        assertThat(buildTriggerStatus.getState()).isEqualTo(BuildTriggerStatus.State.STUCK_IN_QUEUE);
    }

    @Test
    public void shouldReturnBuildNumber() throws Exception {
        final QueueItem queueItem = new QueueItem();
        final Executable executable = new Executable();
        executable.setNumber(98L);
        queueItem.setExecutable(executable);

        Mockito.when(mockJob.build()).thenReturn(queueReference);
        Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItem);
        final BuildTriggerStatus buildTriggerStatus = service.build(jenkinsServer, "TEST", 10000, null);

        assertThat(buildTriggerStatus).isNotNull();
        assertThat(buildTriggerStatus.getState()).isEqualTo(BuildTriggerStatus.State.STARTED_BUILDING);
        assertThat(buildTriggerStatus.getBuildNumber()).isEqualTo(98);
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
        final BuildTriggerStatus buildTriggerStatus = service.build(jenkinsServer, "TEST", 10000L, null);

        assertThat(buildTriggerStatus).isNotNull();
        assertThat(buildTriggerStatus.getState()).isEqualTo(BuildTriggerStatus.State.STARTED_BUILDING);
        assertThat(buildTriggerStatus.getBuildNumber()).isEqualTo(98);

        Mockito.verify(jenkinsServer, Mockito.times(3)).getQueueItem(queueReference);
    }

    @Test
    public void shouldReturnTimeout() throws Exception {
        final QueueItem queueItemNotBuildingYet = new QueueItem();

        Mockito.when(mockJob.build()).thenReturn(queueReference);
        Mockito.when(jenkinsServer.getQueueItem(queueReference)).thenReturn(queueItemNotBuildingYet);
        final BuildTriggerStatus buildTriggerStatus = service.build(jenkinsServer, "TEST", 500L, null);

        assertThat(buildTriggerStatus).isNotNull();
        assertThat(buildTriggerStatus.getState()).isEqualTo(BuildTriggerStatus.State.TIMED_OUT);

        Mockito.verify(jenkinsServer, Mockito.atLeast(2)).getQueueItem(queueReference);
    }

    @Test(expected = DiggerClientException.class)
    public void shouldThrowExceptionIfJobForLogsCannotBeFound() throws Exception {
        when(jenkinsServer.getJob(anyString())).thenReturn(null);
        service.getBuildLogs(jenkinsServer, "artifact", 1);
    }

    @Test
    public void shouldFetchLogs() throws Exception {
        String expectedLogs = "test";
        JobWithDetails job = mock(JobWithDetails.class);
        BuildWithDetails build = mock(BuildWithDetails.class);
        when(jenkinsServer.getJob(anyString())).thenReturn(job);
        when(job.getBuildByNumber(anyInt())).thenReturn(build);
        when(build.details()).thenReturn(build);
        when(build.getConsoleOutputText()).thenReturn(expectedLogs);

        String logs = service.getBuildLogs(jenkinsServer, "artifact", 1);

        assertThat(logs).isEqualTo(expectedLogs);
    }

    @Test(expected = DiggerClientException.class)
    public void shouldThrowExceptionIfJobForBuildHistoryCannotBeFound() throws Exception {
        when(jenkinsServer.getJob(anyString())).thenReturn(null);
        service.getBuildHistory(jenkinsServer, "does-not-exist");
    }

    @Test
    public void shouldFetchBuildHistory() throws Exception {
        JobWithDetails job = mock(JobWithDetails.class);

        Build build1 = mock(Build.class);
        Build build2 = mock(Build.class);

        BuildWithDetails buildDetails1 = mock(BuildWithDetails.class);
        BuildWithDetails buildDetails2 = mock(BuildWithDetails.class);

        when(jenkinsServer.getJob(anyString())).thenReturn(job);

        when(job.getBuilds()).thenReturn(Lists.newArrayList(build1, build2));

        when(build1.details()).thenReturn(buildDetails1);
        when(build2.details()).thenReturn(buildDetails2);

        final List<BuildWithDetails> detailsList = service.getBuildHistory(jenkinsServer, "some-job");

        assertThat(detailsList).hasSize(2);
        assertThat(detailsList.get(0)).isSameAs(buildDetails1);
        assertThat(detailsList.get(1)).isSameAs(buildDetails2);
    }

}

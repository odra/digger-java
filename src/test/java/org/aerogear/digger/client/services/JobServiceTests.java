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

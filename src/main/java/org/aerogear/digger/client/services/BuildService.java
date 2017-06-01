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
import com.offbytwo.jenkins.model.*;
import org.aerogear.digger.client.DiggerClient;
import org.aerogear.digger.client.model.BuildTriggerStatus;
import org.aerogear.digger.client.util.DiggerClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Provides functionality to trigger a build.
 **/
public class BuildService {

    private static final Logger LOG = LoggerFactory.getLogger(BuildService.class);

    /**
     * Default value of {@link #firstCheckDelay}
     */
    public static final long DEFAULT_FIRST_CHECK_DELAY = 5 * 1000L;

    /**
     * Default value of {@link #pollPeriod}
     */
    public static final long DEFAULT_POLL_PERIOD = 2 * 1000L;


    private long firstCheckDelay;
    private long pollPeriod;

    /**
     * @param firstCheckDelay how long should we wait (in milliseconds) before we start checking the queue item status
     * @param pollPeriod      how long should we wait (in milliseconds) before checking the queue item status for next time
     */
    public BuildService(long firstCheckDelay, long pollPeriod) {
        this.firstCheckDelay = firstCheckDelay;
        this.pollPeriod = pollPeriod;
    }


    /**
     * Get build logs for specific job and build number
     *
     * @param jobName     name of the job
     * @param buildNumber job build number
     * @return String with file contents that can be saved or piped to socket
     * @throws DiggerClientException when problem with fetching artifacts from jenkins
     */
    public String getBuildLogs(JenkinsServer jenkins, String jobName, int buildNumber) throws DiggerClientException {
        try {
            JobWithDetails job = jenkins.getJob(jobName);
            if (job == null) {
                LOG.error("Cannot fetch job from jenkins {0}", jobName);
                throw new DiggerClientException("Cannot fetch job from jenkins");
            }
            Build build = job.getBuildByNumber(buildNumber);
            BuildWithDetails buildWithDetails = build.details();
            return buildWithDetails.getConsoleOutputText();
        } catch (IOException e) {
            LOG.error("Problem when fetching logs for {0} {1}", jobName, buildNumber, e);
            throw new DiggerClientException(e);
        }
    }

    /**
     * Returns the build history for a job. As reported by {@link JobWithDetails#getBuilds()} it will return max 100 most-recent builds.
     * <p>
     * Please note that this approach will take some time since we first fetch the builds in 1 call, then fetch build details in 1 call per build.
     *
     * @param jenkins Jenkins server client
     * @param jobName name of the job
     * @return the build history
     * @throws DiggerClientException if connection problems occur
     */
    public List<BuildWithDetails> getBuildHistory(JenkinsServer jenkins, String jobName) throws DiggerClientException {
        final JobWithDetails job;
        try {
            job = jenkins.getJob(jobName);
            if (job == null) {
                LOG.error("Cannot fetch job from jenkins {0}", jobName);
                throw new DiggerClientException("Cannot fetch job from jenkins");
            }
            final List<Build> builds = job.getBuilds();

            final List<BuildWithDetails> returnValue = new ArrayList<BuildWithDetails>();

            for (Build build : builds) {
                try {
                    returnValue.add(build.details());
                } catch (IOException e) {
                    LOG.error("Error while fetching the details for job {} with build number", jobName, build.getNumber(), e);
                    // re-raise it so the the outer block catches it and creates a DiggerClientException out of it
                    throw e;
                }
            }
            return returnValue;
        } catch (IOException e) {
            LOG.error("Problem when getting the build history for job {0}", jobName, e);
            throw new DiggerClientException(e);
        }
    }

    /**
     * See the documentation in {@link DiggerClient#build(String, long)}
     *
     * @param jenkinsServer Jenkins server client
     * @param jobName       name of the job
     * @param timeout       timeout
     * @return the build status
     * @throws IOException          if connection problems occur during connecting to Jenkins
     * @throws InterruptedException if a problem occurs during sleeping between checks
     * @see DiggerClient#build(String, long)
     */
    public BuildTriggerStatus build(JenkinsServer jenkinsServer, String jobName, long timeout) throws IOException, InterruptedException {
        final long whenToTimeout = System.currentTimeMillis() + timeout;

        LOG.debug("Going to build job with name: {}", jobName);
        LOG.debug("Going to timeout in {} msecs if build didn't start executing", timeout);

        JobWithDetails job = jenkinsServer.getJob(jobName);
        if (job == null) {
            LOG.debug("Unable to find job for name '{}'", jobName);
            throw new IllegalArgumentException("Unable to find job for name '" + jobName + "'");
        }

        final QueueReference queueReference = job.build();
        if (queueReference == null) {
            // this is probably an implementation problem we have here
            LOG.debug("Queue reference cannot be null!");
            throw new IllegalStateException("Queue reference cannot be null!");
        }
        LOG.debug("Build triggered; queue item reference: {}", queueReference.getQueueItemUrlPart());

        // wait for N seconds, then fetch the queue item.
        // do it until we have an executable.
        // we would have an executable when the build leaves queue and starts building.

        LOG.debug("Going to sleep {} msecs", firstCheckDelay);
        Thread.sleep(firstCheckDelay);

        QueueItem queueItem;
        while (true) {
            queueItem = jenkinsServer.getQueueItem(queueReference);
            LOG.debug("Queue item : {}", queueItem);

            if (queueItem == null) {
                // this is probably an implementation problem we have here
                LOG.debug("Queue item cannot be null!");
                throw new IllegalStateException("Queue item cannot be null!");
            }

            LOG.debug("Build item cancelled:{}, blocked:{}, buildable:{}, stuck:{}", queueItem.isCancelled(), queueItem.isBlocked(), queueItem.isBuildable(), queueItem.isStuck());

            if (queueItem.isCancelled()) {
                LOG.debug("Queue item is cancelled. Returning CANCELLED_IN_QUEUE");
                return new BuildTriggerStatus(BuildTriggerStatus.State.CANCELLED_IN_QUEUE, -1);
            } else if (queueItem.isStuck()) {
                LOG.debug("Queue item is stuck. Returning STUCK_IN_QUEUE");
                return new BuildTriggerStatus(BuildTriggerStatus.State.STUCK_IN_QUEUE, -1);
            }

            // do not return -1 if blocked.
            // we will wait until it is unblocked.

            final Executable executable = queueItem.getExecutable();

            if (executable != null) {
                LOG.debug("Build has an executable. Returning build number: {}", executable.getNumber());
                return new BuildTriggerStatus(BuildTriggerStatus.State.STARTED_BUILDING, executable.getNumber().intValue());
            } else {
                LOG.debug("Build did not start executing yet.");
                if (whenToTimeout > System.currentTimeMillis()) {
                    LOG.debug("Timeout period has not exceeded yet. Sleeping for {} msecs", pollPeriod);
                    Thread.sleep(pollPeriod);
                } else {
                    LOG.debug("Timeout period has exceeded. Returning TIMED_OUT.");
                    return new BuildTriggerStatus(BuildTriggerStatus.State.TIMED_OUT, -1);
                }
            }
        }
    }
}

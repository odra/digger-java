package com.redhat.digkins.services;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Executable;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Provides functionality to trigger a build.
 **/
public class TriggerBuildService {

  private static final Logger LOG = LoggerFactory.getLogger(TriggerBuildService.class);

  /**
   * How long should we wait before we start checking the queue item status.
   */
  public static final long FIRST_CHECK_DELAY = 5 * 1000L;

  /**
   * How long should we wait before checking the queue item status for next time.
   */
  public static final long POLL_PERIOD = 2 * 1000L;


  private JenkinsServer jenkins;

  /**
   * @param jenkins jenkins api instance
   */
  public TriggerBuildService(JenkinsServer jenkins) {
    this.jenkins = jenkins;
  }

  /**
   * See the documentation in {@link com.redhat.digkins.DiggerClient#build(String, long)}
   *
   * @param jobName name of the job
   * @param timeout timeout
   * @return the build number
   * @throws IOException          if connection problems occur during connecting to Jenkins
   * @throws InterruptedException if a problem occurs during sleeping between checks
   * @see com.redhat.digkins.DiggerClient#build(String, long)
   */
  public long build(String jobName, long timeout) throws IOException, InterruptedException {
    final long timeoutTime = System.currentTimeMillis() + timeout;

    LOG.debug("Going to build job with name: {}", jobName);
    LOG.debug("Going to timeout in {} msecs if build didn't start executing", timeout);

    JobWithDetails job = jenkins.getJob(jobName);
    if (job == null) {
      throw new IllegalArgumentException("Unable to find job for name '" + jobName + "'");
    }

    final QueueReference queueReference = job.build();
    if (queueReference == null) {
      // this is probably an implementation problem we have here
      throw new IllegalStateException("Queue reference cannot be null!");
    }
    LOG.debug("Build triggered; queue item reference: {}", queueReference.getQueueItemUrlPart());

    // wait for N seconds, then fetch the queue item.
    // do it until we have an executable.
    // we would have an executable when the build leaves queue and starts building.

    LOG.debug("Going to sleep {} msecs", FIRST_CHECK_DELAY);
    Thread.sleep(FIRST_CHECK_DELAY);

    QueueItem queueItem;
    while (true) {
      queueItem = jenkins.getQueueItem(queueReference);
      LOG.debug("Queue item : {}", queueItem);

      if (queueItem == null) {
        // this is probably an implementation problem we have here
        throw new IllegalStateException("Queue item cannot be null!");
      }

      LOG.debug("Build item cancelled:{}, blocked:{}, buildable:{}, stuck:{}", queueItem.isCancelled(), queueItem.isBlocked(), queueItem.isBuildable(), queueItem.isStuck());

      if (queueItem.isCancelled()) {
        LOG.debug("Queue item is cancelled. Returning -1");
        return -1;
      } else if (queueItem.isStuck()) {
        LOG.debug("Queue item is stuck. Returning -1");
        return -1;
      }

      // do not return -1 if blocked.
      // we will wait until it is unblocked.

      final Executable executable = queueItem.getExecutable();

      if (executable != null) {
        LOG.debug("Build has an executable. Returning build number: {}", executable.getNumber());
        return executable.getNumber();
      } else {
        LOG.debug("Build did not start executing yet.");
        if (timeoutTime < System.currentTimeMillis()) {
          LOG.debug("Timeout period has not exceeded yet. Sleeping for {} msecs", POLL_PERIOD);
          Thread.sleep(POLL_PERIOD);
        } else {
          LOG.debug("Timeout period has exceeded. Returning -1.");
          return -1;
        }
      }

    }
  }
}

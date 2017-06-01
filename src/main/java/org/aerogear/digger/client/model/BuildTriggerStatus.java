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
package org.aerogear.digger.client.model;

/**
 * Represents the status of a build that is just triggered.
 * <p>
 * The field {@link #buildNumber} will only be set if the
 * {@link #state} is {@link State#STARTED_BUILDING}.
 * <p>
 * This entity does not represent the details of a past build. It provides enough
 * information to see if a build has left the queue and started building.
 **/
public class BuildTriggerStatus {

    public enum State {
        /**
         * Build is out of the queue and it is currently being executed.
         */
        STARTED_BUILDING,

        /**
         * The max time to wait for the build get executed has passed.
         * This state doesn't have to mean build is stuck or etc.
         * It just means, the max waiting time has passed on the client side.
         */
        TIMED_OUT,

        /**
         * The build is cancelled in Jenkins before it started being executed.
         */
        CANCELLED_IN_QUEUE,

        /**
         * The build is stuck on Jenkins queue.
         */
        STUCK_IN_QUEUE
    }

    private final State state;
    private final int buildNumber;

    public BuildTriggerStatus(State state, int buildNumber) {
        this.state = state;
        this.buildNumber = buildNumber;
    }

    /**
     * @return state of the build
     */
    public State getState() {
        return state;
    }

    /**
     * This should only be valid if the
     * {@link #state} is {@link State#STARTED_BUILDING}.
     *
     * @return the build number assigned by Jenkins
     */
    public int getBuildNumber() {
        return buildNumber;
    }

    @Override
    public String toString() {
        return "BuildStatus{" +
            "state=" + state +
            ", buildNumber=" + buildNumber +
            '}';
    }
}

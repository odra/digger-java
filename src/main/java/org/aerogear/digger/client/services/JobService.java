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
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.io.IOException;

/**
 * Create digger job on jenkins platform
 */
public class JobService {

    private final static String GIT_REPO_URL = "GIT_REPO_URL";
    private final static String GIT_REPO_BRANCH = "GIT_REPO_BRANCH";
    private static final String JOB_TEMPLATE_PATH = "templates/job.xml";

    /**
     * Create new digger job on jenkins platform
     *
     * @param jenkinsServer Jenkins server client
     * @param name          job name that can be used later to reference job
     * @param gitRepo       git repository url (full git repository url. e.g git@github.com:digger/helloworld.git
     * @param gitBranch     git repository branch (default branch used to checkout source code)
     */
    public void create(JenkinsServer jenkinsServer, String name, String gitRepo, String gitBranch) throws IOException {
        JtwigTemplate template = JtwigTemplate.classpathTemplate(JOB_TEMPLATE_PATH);
        JtwigModel model = JtwigModel.newModel().with(GIT_REPO_URL, gitRepo).with(GIT_REPO_BRANCH, gitBranch);
        jenkinsServer.createJob(name, template.render(model));
    }
}

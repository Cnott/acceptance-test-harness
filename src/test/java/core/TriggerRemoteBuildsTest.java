/*
 * The MIT License
 *
 * Copyright (c) 2014 Sony Mobile Communications Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package core;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.BuildHistory;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.jenkinsci.test.acceptance.po.GlobalSecurityConfig;
import org.jenkinsci.test.acceptance.po.MatrixProject;
import org.jenkinsci.test.acceptance.po.ServletSecurityRealm;
import org.jenkinsci.test.acceptance.po.ShellBuildStep;
import org.jenkinsci.test.acceptance.po.StringParameter;
import org.junit.Test;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test to trigger builds remotely.
 * @author Orjan Percy <orjan.percy@sonymobile.com>
 */
public class TriggerRemoteBuildsTest extends AbstractJUnitTest {
    public static int NO_BUILDS = 30;

    /**
     * Tests that matrix builds can be triggered remotely from another job.
     */
    @Test
    public void triggerMatrixBuildsRemotely() {

        GlobalSecurityConfig sc = new GlobalSecurityConfig(jenkins);
        sc.open();
        sc.useRealm(ServletSecurityRealm.class);
        sc.save();

        MatrixProject job = jenkins.jobs.create(MatrixProject.class);
        job.configure();
        job.addParameter(StringParameter.class).setName("ID").setDefault("0");
        job.runSequentially.check();
        // Trigger builds remotely (e.g., from scripts)")
        jenkins.control("/pseudoRemoteTrigger").click();
        jenkins.control("/pseudoRemoteTrigger/authToken").fillIn("authToken", "TOKEN");
        job.addUserAxis("X", "1 2 3");
        job.addShellStep("#!/bin/bash\n" +
                "echo Job request ${ID}\n");
        job.save();

        FreeStyleJob job2 = jenkins.jobs.create(FreeStyleJob.class);
        job2.addBuildStep(ShellBuildStep.class);
        String s = "#!/bin/bash -x\n" +
                "for i in {1.." + NO_BUILDS + "}\n" +
                "do\n" +
                "\tcurl " + job.url.toString() + "buildWithParameters?token=TOKEN\\&ID=$i\n" +
                "done";
        jenkins.control("/builder/command").setAtOnce(s);
        job2.save();

        job2.startBuild().waitUntilFinished();
        int nrOfBuilds = jenkins.getBuildHistory().numberOfInclusions(job.name);
        assertThat("All triggered builds have not been run or put in build queue.", nrOfBuilds == NO_BUILDS);
    }
}

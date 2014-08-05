/*
 * The MIT License
 *
 * Copyright 2014 Jesse Glick.
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

package org.jenkinsci.plugins.workflow.steps;

import hudson.model.Result;
import hudson.tasks.Fingerprinter;
import hudson.tasks.junit.TestResultAction;
import java.util.List;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Rule;
import org.jvnet.hudson.test.JenkinsRule;

public class CoreStepTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    @Test public void artifactArchiver() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node {sh 'touch x.txt'; step($class: 'hudson.tasks.ArtifactArchiver', artifacts: 'x.txt', fingerprint: true)}"));
        WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        List<WorkflowRun.Artifact> artifacts = b.getArtifacts();
        assertEquals(1, artifacts.size());
        assertEquals("x.txt", artifacts.get(0).relativePath);
        Fingerprinter.FingerprintAction fa = b.getAction(Fingerprinter.FingerprintAction.class);
        assertNotNull(fa);
        assertEquals("[x.txt]", fa.getRecords().keySet().toString());
    }

    @Test public void fingerprinter() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition("node {sh 'touch x.txt'; step($class: 'hudson.tasks.Fingerprinter', targets: 'x.txt')}"));
        WorkflowRun b = r.assertBuildStatusSuccess(p.scheduleBuild2(0));
        Fingerprinter.FingerprintAction fa = b.getAction(Fingerprinter.FingerprintAction.class);
        assertNotNull(fa);
        assertEquals("[x.txt]", fa.getRecords().keySet().toString());
    }

    @Test public void junitResultArchiver() throws Exception {
        WorkflowJob p = r.jenkins.createProject(WorkflowJob.class, "p");
        p.setDefinition(new CpsFlowDefinition(
                  "node {\n"
                // Quote hell! " is Java, ''' is Groovy, ' is shell, \" is " inside XML
                + "    sh '''echo '<testsuite name=\"a\"><testcase name=\"a1\"/><testcase name=\"a2\"><error>a2 failed</error></testcase></testsuite>' > a.xml'''\n"
                + "    sh '''echo '<testsuite name=\"b\"><testcase name=\"b1\"/><testcase name=\"b2\"/></testsuite>' > b.xml'''\n"
                + "    step($class: 'hudson.tasks.junit.JUnitResultArchiver', testResults: '*.xml')\n"
                + "}"));
        WorkflowRun b = r.assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
        TestResultAction a = b.getAction(TestResultAction.class);
        assertNotNull(a);
        assertEquals(4, a.getTotalCount());
        assertEquals(1, a.getFailCount());
    }

}

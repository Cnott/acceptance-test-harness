/*
 * The MIT License
 *
 * Copyright 2015 thi.
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
package plugins;

import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.po.Build.Result;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author thi
 */
public class MemoryMapPluginTest extends AbstractJUnitTest{
    
    @Test
    public void compatibilityTest() throws Exception{
        
        //install memory-map-plugin
        jenkins.getPluginManager().installPlugin(resource("/memory_map_plugin/1.0.2.hpi").asFile());
        
        //Create a job and build to create workspace
        FreeStyleJob job = jenkins.jobs.create();
        job.startBuild().waitUntilFinished().shouldBe(Result.SUCCESS);
              
        //Configure job
        job.configure();
        {
            jenkins.clickButton("Add post-build action");
            jenkins.clickLink("Memory Map Publisher");
            jenkins.fillIn("_.configurationFile", "ti_link.cmd");
            jenkins.fillIn("_.wordSize", 8);
            jenkins.check("_.showBytesOnGraph");
            jenkins.find(by.name("_.scale")).sendKeys("Mega");
            jenkins.find(by.path("/publisher/")).sendKeys("Texas");
            jenkins.find(by.path("/publisher/repeatable-add")).click();
            jenkins.waitFor(by.name("graph.config.graphCaption"));
            jenkins.fillIn("graph.config.graphCaption", "TI graph");
            jenkins.fillIn("graph.config.graphDataList", "RAML0_L3");
            job.copyResource(resource("/memory_map_plugin/ti_map.txt"));
            job.copyResource(resource("/memory_map_plugin/ti_link.cmd"));        
            jenkins.fillIn("mapFile", "ti_map.txt");
        }
        job.save();
        
        //Run build, should be successful
         job.startBuild().waitUntilFinished().shouldBe(Result.SUCCESS);
         
        //Update memory-map-plugin
        jenkins.getPluginManager().installPlugin(resource("/memory_map_plugin/2.0.0.hpi").asFile());
        jenkins.restart();
        
        //Check configuration is still fine
        job.configure();
        {
            assertEquals("8", jenkins.getElement(by.name("_.wordSize")).getAttribute("value"));
            assertEquals("true", jenkins.getElement(by.name("_.showBytesOnGraph")).getAttribute("value"));
            assertEquals("Mega", jenkins.getElement(by.name("_.scale")).getAttribute("value"));
            assertEquals("Default", jenkins.getElement(by.name("_.parserUniqueName")).getAttribute("value"));
            assertEquals("", jenkins.getElement(by.name("_.parserTitle")).getAttribute("value"));
            assertEquals("ti_link.cmd", jenkins.getElement(by.name("configurationFile")).getAttribute("value"));
            assertEquals("ti_map.txt", jenkins.getElement(by.name("mapFile")).getAttribute("value"));
            assertEquals("TI graph", jenkins.getElement(by.name("_.graphCaption")).getAttribute("value"));
            assertEquals("RAML0_L3", jenkins.getElement(by.name("_.graphDataList")).getAttribute("value"));
        }
        
        //Run build, should be successful
         job.startBuild().waitUntilFinished().shouldBe(Result.SUCCESS);
    }
}

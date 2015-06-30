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

import java.io.File;
import java.util.List;
import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.Build.Result;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

/**
 *
 * @author thi
 */
public class MemoryMapPluginTest extends AbstractJUnitTest{
    
    @Test
    public void compatibilityTest(){
        
        //install memory-map-plugin
        File plugin = new File("/home/thi/Documents/Jenkins/1.0.2.hpi");
        jenkins.getPluginManager().installPlugin(plugin);
        jenkins.restart();
        
        //Create a job
        FreeStyleJob job = jenkins.jobs.create();
        
        //Create job workspace
        job.startBuild().waitUntilFinished().shouldBe(Result.SUCCESS);
              
        //Configure job
        job.configure();
        jenkins.clickButton("Add post-build action");
        jenkins.clickLink("Memory Map Publisher");
        jenkins.fillIn("_.configurationFile", "28069_RAM_lnk.cmd");
        jenkins.fillIn("_.wordSize", 8);
        jenkins.check("_.showBytesOnGraph");
        jenkins.find(by.name("_.scale")).sendKeys("Mega");
        jenkins.find(by.path("/publisher/")).sendKeys("Texas");
        jenkins.find(by.path("/publisher/repeatable-add")).click();
        jenkins.waitFor(by.name("graph.config.graphCaption"));
        jenkins.fillIn("graph.config.graphCaption", "TI graph");
        jenkins.fillIn("graph.config.graphDataList", "RAML0_L3");
        job.copyFile(new File("/home/thi/Documents/ti"));
        jenkins.fillIn("mapFile", "TexasInstrumentsMapFile.txt");
        job.save();
        
        //Run build, should be successful
         job.startBuild().waitUntilFinished().shouldBe(Result.SUCCESS);
         
        //Update memory-map-plugin
        plugin = new File("/home/thi/Documents/Jenkins/2.0.0.hpi");
        jenkins.getPluginManager().installPlugin(plugin);
        jenkins.restart();
        
        //Check configuration is still fine
        
        //Run build, should be successful
         job.startBuild().waitUntilFinished().shouldBe(Result.SUCCESS);
    }
}

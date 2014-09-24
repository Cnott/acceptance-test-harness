package plugins.praqma.pretested_integration;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.plugins.git.GitScm;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.junit.Test;
import org.openqa.selenium.By;

import java.io.File;
import java.io.IOException;

@WithPlugins("pretested-integration")
public class PretestedIntegrationTest extends AbstractJUnitTest {
    private final File GIT_DIR = new File("test-repo/.git");
    private final File GIT_PARENT_DIR = GIT_DIR.getParentFile().getAbsoluteFile();
    private final String README_FILE_PATH = GIT_PARENT_DIR.getPath().concat("/" + "readme");

    private final String AUTHER_NAME = "john Doe";
    private final String AUTHER_EMAIL = "Joh@praqma.net";

    private Repository repository;
    private Git git;

    @org.junit.After
    public void tearDown() throws Exception {
        if (repository != null) {
            repository.close();

            if (GIT_PARENT_DIR.exists())
                FileUtils.deleteDirectory(GIT_PARENT_DIR);
        }
    }

    public void createValidRepository() throws IOException, GitAPIException {
        if (GIT_PARENT_DIR.exists())
            FileUtils.deleteDirectory(GIT_PARENT_DIR);

        final String FEATURE_BRANCH_NAME = "ready/feature_1";

        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        repository = builder.setGitDir(GIT_DIR.getAbsoluteFile())
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        if (!repository.isBare() && repository.getBranch() == null) {
            repository.create();
        }

        git = new Git(repository);

        File readme = new File(README_FILE_PATH);
        if (!readme.exists())
            FileUtils.writeStringToFile(readme, "sample text\n");

        git.add().addFilepattern(readme.getName()).call();
        git.commit().setMessage("commit message 1").call();

        FileUtils.writeStringToFile(readme, "changed sample text\n");

        git.add().addFilepattern(readme.getName()).call();
        git.commit().setMessage("commit message 2").call();

        CreateBranchCommand createBranchCommand = git.branchCreate();
        createBranchCommand.setName(FEATURE_BRANCH_NAME);
        createBranchCommand.call();

        git.checkout().setName(FEATURE_BRANCH_NAME).call();

        FileUtils.writeStringToFile(readme, "FEATURE_1 branch commit 1\n");

        git.add().addFilepattern(readme.getName()).call();
        CommitCommand commitCommand = git.commit();
        commitCommand.setMessage("feature 1 commit 1");
        commitCommand.setAuthor(AUTHER_NAME, AUTHER_EMAIL);
        commitCommand.call();

        FileUtils.writeStringToFile(readme, "FEATURE_1 branch commit 2\n");

        git.add().addFilepattern(readme.getName()).call();
        commitCommand = git.commit();
        commitCommand.setMessage("feature 1 commit 2");
        commitCommand.setAuthor(AUTHER_NAME, AUTHER_EMAIL);
        commitCommand.call();

        git.checkout().setName("master").call();
    }

    @Test
    public void allow_user_to_use_Pretested_Integration_plugin_in_freestyle_project() {
        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        job.check("Use pretested integration");
        job.find(By.id("radio-block-5")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.choose("Squashed commit");

        job.save();
    }

    @Test
    public void merge_from_feature_branch_to_integration_branch_using_squash_commit_strategy() throws IOException, GitAPIException {
        createValidRepository();
        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        GitScm gitScm = job.useScm(GitScm.class);
        gitScm.url("file://" + GIT_DIR.getAbsolutePath());
        gitScm.branch.set("origin/ready/**");
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        job.check("Use pretested integration");
        job.find(By.id("radio-block-5")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.choose("Squashed commit");

        job.clickButton("Add post-build action");
        job.clickLink("Pretested Integration post-build");

        job.save();

        Build build = job.scheduleBuild();

        build.waitUntilFinished();

        TestCase.assertTrue(build.isSuccess());
    }

    @Test
    public void merge_from_feature_branch_to_integration_branch_using_accumulated_commit_strategy() throws IOException, GitAPIException {
        createValidRepository();

        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        GitScm gitScm = job.useScm(GitScm.class);
        gitScm.url("file://" + GIT_DIR.getAbsolutePath());
        gitScm.branch.set("origin/ready/**");
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        job.check("Use pretested integration");
        job.find(By.id("radio-block-5")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.choose("Accumulated commit");

        job.clickButton("Add post-build action");
        job.clickLink("Pretested Integration post-build");

        job.save();

        Build build = job.scheduleBuild();

        build.waitUntilFinished();
        TestCase.assertTrue(build.isSuccess());
    }
}

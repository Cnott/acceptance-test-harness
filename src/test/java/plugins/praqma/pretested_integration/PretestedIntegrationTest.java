package plugins.praqma.pretested_integration;

import junit.framework.TestCase;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jenkinsci.test.acceptance.junit.AbstractJUnitTest;
import org.jenkinsci.test.acceptance.junit.WithPlugins;
import org.jenkinsci.test.acceptance.plugins.git.GitScm;
import org.jenkinsci.test.acceptance.plugins.multiple_scms.MultipleScms;
import org.jenkinsci.test.acceptance.plugins.subversion.SubversionScm;
import org.jenkinsci.test.acceptance.po.Build;
import org.jenkinsci.test.acceptance.po.FreeStyleJob;
import org.junit.Test;
import org.openqa.selenium.By;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

@WithPlugins(value = {"pretested-integration", "multiple-scms"})
public class PretestedIntegrationTest extends AbstractJUnitTest {

    public Repository createValidRepository(final File gitDir) throws IOException, GitAPIException {
        final String AUTHER_NAME = "john Doe";
        final String AUTHER_EMAIL = "Joh@praqma.net";

        final File GIT_PARENT_DIR = gitDir.getParentFile().getAbsoluteFile();
        final String README_FILE_PATH = GIT_PARENT_DIR.getPath().concat("/" + "readme");

        if (GIT_PARENT_DIR.exists())
            FileUtils.deleteDirectory(GIT_PARENT_DIR);

        final String FEATURE_BRANCH_NAME = "ready/feature_1";

        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        Repository repository = builder.setGitDir(gitDir.getAbsoluteFile())
                .readEnvironment() // scan environment GIT_* variables
                .findGitDir() // scan up the file system tree
                .build();

        if (!repository.isBare() && repository.getBranch() == null) {
            repository.create();
        }

        Git git = new Git(repository);

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

        return repository;
    }

    private boolean branchExists(Repository repository, String branch) throws GitAPIException {
        Git git = new Git(repository);

        List<Ref> call = git.branchList().call();

        ListIterator<Ref> refListIterator = call.listIterator();

        while(refListIterator.hasNext()) {
            String branchName = refListIterator.next().getName();
            if (branchName.endsWith(branch))
                return true;
        }

        return false;
    }

    private void cleanUp(Repository... repositories) throws IOException {
        for (Repository repository : repositories) {
            final File GIT_PARENT_DIR = repository.getDirectory().getParentFile().getAbsoluteFile();

            repository.close();

            if (GIT_PARENT_DIR.exists())
                FileUtils.deleteDirectory(GIT_PARENT_DIR);
        }
    }

    @Test
    public void allowUserToUsePretestedIntegrationPluginInFreestyleProject() {
        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        job.check("Use pretested integration");
        job.find(By.id("radio-block-6")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.choose("Squashed commit");

        job.save();
    }

    @Test
    public void oneValidFeatureBranchUsingSquashedMergeStrategy_1buildTriggeredBranchGetsIntegratedBuildMarkedSuccessful() throws Exception {
        File gitDir = new File("test-repo/.git");
        Repository repository = createValidRepository(gitDir);
        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        GitScm gitScm = job.useScm(GitScm.class);
        gitScm.url("file://" + gitDir.getAbsolutePath());
        gitScm.branch.set("origin/ready/**");
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        job.check("Use pretested integration");
        job.find(By.id("radio-block-6")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.choose("Squashed commit");

        job.clickButton("Add post-build action");
        job.clickLink("Pretested Integration post-build");

        job.save();

        Build build = job.scheduleBuild();

        build.waitUntilFinished();

        TestCase.assertTrue(build.isSuccess());

        cleanUp(repository);
    }

    @Test
    public void oneValidFeatureBranchUsingAccumulatedMergeStrategy_1buildTriggeredBranchGetsIntegratedBuildMarkedSuccessful() throws Exception {
        File gitDir = new File("test-repo/.git");
        Repository repository = createValidRepository(gitDir);

        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        GitScm gitScm = job.useScm(GitScm.class);
        gitScm.url("file://" + gitDir.getAbsolutePath());
        gitScm.branch.set("origin/ready/**");
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        job.check("Use pretested integration");
        job.find(By.id("radio-block-6")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.choose("Accumulated commit");

        job.clickButton("Add post-build action");
        job.clickLink("Pretested Integration post-build");

        job.save();

        Build build = job.scheduleBuild();

        build.waitUntilFinished();
        TestCase.assertTrue(build.isSuccess());

        cleanUp(repository);
    }

    @Test
    public void multiScmWith2GitRepositories1svnFirstGitRepoIsUsedForIntegration_1BuildTriggeredBranchGetsIntegratedBuildMarkedSuccessful() throws Exception {
        final String FIRST_REPOSITORY_NAME = "john";
        File gitDir1 = new File("test-repo1/.git");
        Repository repository1 = createValidRepository(gitDir1);

        final String SECOND_REPOSITORY_NAME = "letters";
        File gitDir2 = new File("test-repo2/.git");
        Repository repository2 = createValidRepository(gitDir2);

        TestCase.assertTrue(branchExists(repository1, "ready/feature_1"));
        TestCase.assertTrue(branchExists(repository2, "ready/feature_1"));

        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        MultipleScms multiScm = job.useScm(MultipleScms.class);

        GitScm gitScm = multiScm.addScm(GitScm.class);
        gitScm.url("file://" + gitDir1.getAbsolutePath());
        gitScm.branch.set("*/ready/**");
        gitScm.remoteName(FIRST_REPOSITORY_NAME);
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        gitScm = multiScm.addScm(GitScm.class);
        gitScm.url("file://" + gitDir2.getAbsolutePath());
        gitScm.branch.set("*/ready/**");
        gitScm.remoteName(SECOND_REPOSITORY_NAME);
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        SubversionScm subversionScm = multiScm.addScm(SubversionScm.class);

        job.check("Use pretested integration");
        job.find(By.id("radio-block-6")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.find(By.name("_.repoName")).clear();
        job.find(By.name("_.repoName")).sendKeys(FIRST_REPOSITORY_NAME);
        job.choose("Squashed commit");

        job.clickButton("Add post-build action");
        job.clickLink("Pretested Integration post-build");

        job.save();

        Build build = job.scheduleBuild();
        build.waitUntilFinished();

        TestCase.assertTrue(build.isSuccess());
        TestCase.assertTrue(branchExists(repository2, "ready/feature_1"));
        TestCase.assertFalse(branchExists(repository1, "ready/feature_1"));

        cleanUp(repository1, repository2);
    }

    @Test
    public void multiScmWith2GitRepositories1svnSecondGitRepoIsUsedForIntegration_1BuildTriggeredBranchGetsIntegratedBuildMarkedSuccessful() throws Exception {
        final String FIRST_REPOSITORY_NAME = "john";
        File gitDir1 = new File("test-repo1/.git");
        Repository repository1 = createValidRepository(gitDir1);

        final String SECOND_REPOSITORY_NAME = "letters";
        File gitDir2 = new File("test-repo2/.git");
        Repository repository2 = createValidRepository(gitDir2);

        TestCase.assertTrue(branchExists(repository1, "ready/feature_1"));
        TestCase.assertTrue(branchExists(repository2, "ready/feature_1"));

        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        MultipleScms multiScm = job.useScm(MultipleScms.class);

        GitScm gitScm = multiScm.addScm(GitScm.class);
        gitScm.url("file://" + gitDir1.getAbsolutePath());
        gitScm.branch.set("*/ready/**");
        gitScm.remoteName(FIRST_REPOSITORY_NAME);
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        gitScm = multiScm.addScm(GitScm.class);
        gitScm.url("file://" + gitDir2.getAbsolutePath());
        gitScm.branch.set("*/ready/**");
        gitScm.remoteName(SECOND_REPOSITORY_NAME);
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        SubversionScm subversionScm = multiScm.addScm(SubversionScm.class);

        job.check("Use pretested integration");
        job.find(By.id("radio-block-6")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.find(By.name("_.repoName")).clear();
        job.find(By.name("_.repoName")).sendKeys(SECOND_REPOSITORY_NAME);
        job.choose("Squashed commit");

        job.clickButton("Add post-build action");
        job.clickLink("Pretested Integration post-build");

        job.save();

        Build build = job.scheduleBuild();
        build.waitUntilFinished();

        TestCase.assertTrue(build.isSuccess());
        TestCase.assertTrue(branchExists(repository1, "ready/feature_1"));
        TestCase.assertFalse(branchExists(repository2, "ready/feature_1"));

        cleanUp(repository1, repository2);
    }

    @Test
    public void multiScmWith1GitRepository1BranchForIntegration_1BuildTriggeredBranchGetsIntegratedBuildMarkedSuccessful() throws Exception {
        final String REPOSITORY_NAME = "john";
        File gitDir = new File("test-repo/.git");
        Repository repository = createValidRepository(gitDir);

        TestCase.assertTrue(branchExists(repository, "ready/feature_1"));

        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        MultipleScms multiScm = job.useScm(MultipleScms.class);

        GitScm gitScm = multiScm.addScm(GitScm.class);
        gitScm.url("file://" + gitDir.getAbsolutePath());
        gitScm.branch.set("*/ready/**");
        gitScm.remoteName(REPOSITORY_NAME);
        gitScm.addBehaviour(GitScm.PruneStaleBranch.class);
        gitScm.addBehaviour(GitScm.CleanAfterCheckout.class);

        job.check("Use pretested integration");
        job.find(By.id("radio-block-6")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.find(By.name("_.repoName")).clear();
        job.find(By.name("_.repoName")).sendKeys(REPOSITORY_NAME);
        job.choose("Squashed commit");

        job.clickButton("Add post-build action");
        job.clickLink("Pretested Integration post-build");

        job.save();

        Build build = job.scheduleBuild();
        build.waitUntilFinished();

        TestCase.assertTrue(build.isSuccess());
        TestCase.assertFalse(branchExists(repository, "ready/feature_1"));

        cleanUp(repository);
    }

    @Test
    public void multiScmWith1SvnRepository_BuildMarkedFailed() throws Exception {
        File gitDir = new File("test-repo/.git");
        Repository repository = createValidRepository(gitDir);
        FreeStyleJob job = jenkins.jobs.create();

        job.configure();

        MultipleScms multiScm = job.useScm(MultipleScms.class);

        SubversionScm subversionScm = multiScm.addScm(SubversionScm.class);

        job.check("Use pretested integration");
        job.find(By.id("radio-block-6")).click();
        job.find(By.name("_.branch")).sendKeys("master");
        job.choose("Squashed commit");

        job.clickButton("Add post-build action");
        job.clickLink("Pretested Integration post-build");

        job.save();

        Build build = job.scheduleBuild();

        build.waitUntilFinished();

        TestCase.assertTrue(!build.isSuccess());

        cleanUp(repository);
    }
}

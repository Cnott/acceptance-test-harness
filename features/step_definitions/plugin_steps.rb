def install_plugin!(plugin)
  manager = Jenkins::PluginManager.new(@base_url, nil)
  unless manager.installed?(plugin)
    manager.install_plugin plugin
    found = @runner.log_watcher.wait_until_logged(/(Installation successful: #{plugin} Plugin)|(Plugin #{plugin} dynamically installed)/i)
    found.should be true
  end
  manager.installed?(plugin).should be true
end

When /^I install the "(.*?)" plugin from the update center$/ do |plugin|
  install_plugin! plugin
end

Given /^I have installed the "(.*?)" plugin$/ do |plugin|
  install_plugin! plugin
end

Then /^the job should be able to use the "(.*?)" SCM$/ do |scm|
  page.should have_content scm
end

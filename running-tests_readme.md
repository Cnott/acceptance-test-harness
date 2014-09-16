When running the tests it is important to have a jenkins server already running.
Otherwise it will start a new instance for each test.

Start of by downloading the jenkins war file using the included script called "grab-latest-rc.sh".
Then launch the script called "jut-server.sh". 
When excecuting it will print out the port on which jenkins will run to the console.
Set a variable to point the testing framework to the correct url

export JENKINS_URL=[url] default is http://localhost:8080/

when running the command to exceute tests (mvn test) add a marker to tell it to use the already running instace. 
The command will look like so TYPE=existing mvn test

# Running the tests on a local version of the plugin
When runnning tests the framework will download the plugin from the plublic repository, in order to use an unreleased plugin for testing
you need to set a variable pointing to the local compilation.

export ldap.jpi=/path/to/your/ldap.jpi

running export pretested-integration.hpi=[path] doesn't work because variables with dashes are not allowed.
I tried running the tests with env 'pretested-integration.hpi=[path]' mvn test 
I'm not sure why, but maven downlaods the latest publicly released version.

What does work is running mvn clean install on the pretested integration plugin and installing it to the local maven repository.
Maven will just grab it and won't download it from the internet, unless it is a snapshot release.


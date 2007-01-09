This project requires Apache Maven 1.0 (http://maven.apache.org) to build.

Working in Eclipse
------------------

The Eclipse meta-data files may be generated using Maven.
Create a file in the top-level project directory named build.properties with the following property set:
maven.eclipse.workspace=/path/to/your/workspace

If this is your first time integrating Maven with Eclipse, execute the following:
maven eclipse:add-maven-repo eclipse:external-tools

Next, execute
maven eclipse

This generates a .project and .classpath.  If maven.xml or project.xml are modified, this goal
should be executed to synchronize Eclipse with the new maven configuration.



package net.madz.lifecycle.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal StaticWeave
 * @phase process-classes
 * @configurator include-project-dependencies
 * @requiresDependencyResolution compile+runtime
 * @requiresProject false
 */
public class LifecycleStaticWeaver extends AbstractMojo {

	/**
	 * @parameter expression="${project.build.directory}"
	 */
	private String buildDir;

	/**
	 * @parameter expression="${lifecycle.path}"
	 */
	private String lifecyclePath;

	/**
	 * @parameter expression="${project.runtimeClasspathElements}"
	 */
	private List<String> runtimeClasspathElements;

	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog().info(new File("./").getAbsolutePath());

		final String separator;
		if (File.separatorChar == '\\') {
			separator = "\\";
		} else {
			separator = File.separator;
		}
		final String targetClassesFolder = buildDir + separator + "classes";
		getLog().info(targetClassesFolder);
		getLog().info(lifecyclePath);
		final StringBuffer classpath = new StringBuffer(".:");
		for (String element : runtimeClasspathElements) {
			classpath.append(element).append(":");
		}
		classpath.append(targetClassesFolder).append(":");

		final File lifecycleJar = new File(lifecyclePath);
		classpath.append(lifecycleJar);
		getLog().info(classpath);

		String cmd = "java -cp " + classpath + " -javaagent:" + lifecyclePath
				+ " -Dnet.madz.bcel.save.original=true" + " "
				+ "net.madz.lifecycle.StaticWeaver " + targetClassesFolder;
		getLog().info(cmd);
		BufferedReader reader = null;
		try {
			Process exec = Runtime.getRuntime().exec(cmd);
			InputStream errorStream = exec.getErrorStream();
			reader = new BufferedReader(new InputStreamReader(errorStream));
			String line = null;
			while (null != (line = reader.readLine())) {
				getLog().info(line);
			}
			if (exec.exitValue() > 0) {
				throw new IllegalStateException("Lifecycle Cannot Compile.");
			}
		} catch (IOException e) {
			getLog().error(e);
		} finally {
			if (null != reader) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
		}
	}

}

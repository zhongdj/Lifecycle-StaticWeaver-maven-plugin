package net.madz.lifecycle.maven;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * @goal StaticWeave
 * @phase compile
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

	public void execute() throws MojoExecutionException, MojoFailureException {

		getLog().error(new File("./").getAbsolutePath());

		final String separator;
		if (File.separatorChar == '\\') {
			separator = "\\";
		} else {
			separator = File.separator;
		}
		final String targetClassesFolder = buildDir + separator + "classes";
		getLog().error(targetClassesFolder);
		getLog().error(lifecyclePath);
		final StringBuffer classpath = new StringBuffer(".:");
		classpath.append(targetClassesFolder).append(":");

		final File lifecycleJar = new File(lifecyclePath);
		classpath.append(lifecycleJar);
		getLog().error(classpath);

		String cmd = "java -cp " + classpath + " -javaagent:" + lifecyclePath
				+ " -Dnet.madz.bcel.save.original=true" + " "
				+ "net.madz.lifecycle.StaticWeaver " + targetClassesFolder;
		getLog().error(cmd);
		BufferedReader reader = null;
		try {
			Process exec = Runtime.getRuntime().exec(cmd);
			InputStream errorStream = exec.getErrorStream();
			reader = new BufferedReader(new InputStreamReader(errorStream));
			String line = null;
			while (null != (line = reader.readLine())) {
				getLog().error(line);
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

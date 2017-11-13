/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2013-2020 Madz. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License"). You
 * may not use this file except in compliance with the License. You can
 * obtain a copy of the License at
 * https://raw.github.com/zhongdj/Lifecycle-StaticWeaver-maven-plugin/master/License.txt
 * . See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 * 
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 * 
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license." If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above. However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package net.imadz.lifecycle.maven;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilderConfiguration;
import org.codehaus.plexus.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.List;

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

  private final MavenProject mavenProject() {
    return ((MavenProject) this.getPluginContext().get("project"));
  }


  private final String baseDir() {
    try {
      return projectBuilderConfiguration().getLocalRepository().getBasedir();
    } catch (Exception ex) {
      return defaultBaseDir();
    }
  }

  private String defaultBaseDir() {
    final StringBuilder sb = new StringBuilder();
    return sb.append(System.getProperty("user.home")).append(File.separator)
        .append(".m2").append(File.separator)
        .append("repository").append(File.separator)
        .toString();
  }

  private DefaultProjectBuildingRequest projectBuilderConfiguration() {
    Field configuration = null;
    try {
      configuration = MavenProject.class.getDeclaredField("projectBuilderConfiguration");
      configuration.setAccessible(true);
      return (DefaultProjectBuildingRequest) configuration.get(mavenProject());
    } catch (Throwable t) {
      throw new IllegalStateException("Cannot access Configuration");
    } finally {
      if (null != configuration) {
        configuration.setAccessible(false);
      }
    }
  }

  private final String lifecycleArtifactRelativePath(final String lifecycleVersion) {
    final StringBuilder sb = new StringBuilder();
    return sb.append(File.separator)
        .append("net").append(File.separator)
        .append("imadz").append(File.separator)
        .append("Lifecycle").append(File.separator)
        .append(lifecycleVersion).append(File.separator)
        .append("Lifecycle-").append(lifecycleVersion).append(".jar")
        .toString();
  }

  private final String lifecycleArtifactPath(final String lifecycleVersion) {
    return baseDir() + lifecycleArtifactRelativePath(lifecycleVersion);
  }

  private String lifecycleVersion() {
    final Object[] artifacts =  mavenProject().getDependencyArtifacts().toArray();
    for (Object artifact : artifacts) {
      if (artifact instanceof DefaultArtifact) {
        DefaultArtifact theArtifact = (DefaultArtifact) artifact;
        if ("net.imadz".equals(theArtifact.getGroupId()) && "Lifecycle".equals(theArtifact.getArtifactId())) {
          return theArtifact.getVersion();
        }
      }
    }
    throw new IllegalStateException("Cannot find net.imadz.Lifecycle from dependencies.");
  }

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
    getLog().info(getLifecyclePath());
    final StringBuffer classpath = new StringBuffer(".:");
    for (String element : runtimeClasspathElements) {
      classpath.append(element).append(":");
    }
    classpath.append(targetClassesFolder).append(":");
    final File lifecycleJar = new File(getLifecyclePath());
    classpath.append(lifecycleJar);
    getLog().info(classpath);
    String cmd = "java -cp " + classpath + " -javaagent:" + getLifecyclePath() + " -Dnet.imadz.bcel.save.original=true" + " "
        + "net.imadz.lifecycle.StaticWeaver " + targetClassesFolder;
    getLog().info(cmd);
    BufferedReader reader = null;
    Process exec = null;
    try {
      exec = Runtime.getRuntime().exec(cmd);
      InputStream errorStream = exec.getErrorStream();
      reader = new BufferedReader(new InputStreamReader(errorStream));
      String line = null;
      while (null != (line = reader.readLine())) {
        getLog().info(line);
      }
      while (isAlive(exec)) {
        Thread.sleep(500);
      }
      if (exec.exitValue() > 0) {
        throw new IllegalStateException("Lifecycle Cannot Compile.");
      }
    } catch (IOException e) {
      getLog().error(e);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (null != reader) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
      if (null != exec) {
        exec.destroy();
      }
    }
  }

  private Boolean isAlive(Process exec) {
    try {
      exec.exitValue();
      return false;
    } catch (Throwable t) {
      return true;
    }

  }

  private String getLifecyclePath() {
    if (StringUtils.isNotEmpty(this.lifecyclePath)) {
      return lifecyclePath;
    } else {
      return lifecycleArtifactPath(lifecycleVersion());
    }
  }
}

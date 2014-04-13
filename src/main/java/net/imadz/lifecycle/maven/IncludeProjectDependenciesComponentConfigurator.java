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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.component.configurator.AbstractComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.special.ClassRealmConverter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * A custom ComponentConfigurator which adds the project's runtime classpath
 * elements to the
 * 
 * @author Brian Jackson
 * @since Aug 1, 2008 3:04:17 PM
 * 
 * @plexus.component 
 *                   role="org.codehaus.plexus.component.configurator.ComponentConfigurator"
 *                   role-hint="include-project-dependencies"
 * @plexus.requirement role=
 *                     "org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup"
 *                     role-hint="default"
 */
public class IncludeProjectDependenciesComponentConfigurator extends
		AbstractComponentConfigurator {

	private static final Logger LOGGER = Logger
			.getLogger(IncludeProjectDependenciesComponentConfigurator.class);

	public void configureComponent(Object component,
			PlexusConfiguration configuration,
			ExpressionEvaluator expressionEvaluator, ClassRealm containerRealm,
			ConfigurationListener listener)
			throws ComponentConfigurationException {

		addProjectDependenciesToClassRealm(expressionEvaluator, containerRealm);

		converterLookup.registerConverter(new ClassRealmConverter(
				containerRealm));

		ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();

		converter.processConfiguration(converterLookup, component,
				containerRealm.getClassLoader(), configuration,
				expressionEvaluator, listener);
	}

	private void addProjectDependenciesToClassRealm(
			ExpressionEvaluator expressionEvaluator, ClassRealm containerRealm)
			throws ComponentConfigurationException {
		List<String> runtimeClasspathElements;
		try {
			// noinspection unchecked
			runtimeClasspathElements = (List<String>) expressionEvaluator
					.evaluate("${project.runtimeClasspathElements}");
		} catch (ExpressionEvaluationException e) {
			throw new ComponentConfigurationException(
					"There was a problem evaluating: ${project.runtimeClasspathElements}",
					e);
		}

		// Add the project dependencies to the ClassRealm
		final URL[] urls = buildURLs(runtimeClasspathElements);
		for (URL url : urls) {
			containerRealm.addConstituent(url);
		}
	}

	private URL[] buildURLs(List<String> runtimeClasspathElements)
			throws ComponentConfigurationException {
		// Add the projects classes and dependencies
		List<URL> urls = new ArrayList<URL>(runtimeClasspathElements.size());
		for (String element : runtimeClasspathElements) {
			try {
				final URL url = new File(element).toURI().toURL();
				urls.add(url);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Added to project class loader: " + url);
				}
			} catch (MalformedURLException e) {
				throw new ComponentConfigurationException(
						"Unable to access project dependency: " + element, e);
			}
		}

		// Add the plugin's dependencies (so Trove stuff works if Trove
		// isn't on
		return urls.toArray(new URL[urls.size()]);
	}

}

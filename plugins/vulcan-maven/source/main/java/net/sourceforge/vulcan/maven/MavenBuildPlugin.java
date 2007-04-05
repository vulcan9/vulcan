/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2006 Chris Eldredge
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sourceforge.vulcan.maven;

import static net.sourceforge.vulcan.ant.AntBuildPlugin.addSystemJavaHomeIfMissing;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import net.sourceforge.vulcan.BuildTool;
import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.ant.JavaHome;
import net.sourceforge.vulcan.dto.BuildToolConfigDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.exception.ConfigException;
import net.sourceforge.vulcan.integration.BuildToolPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.integration.support.PluginSupport;
import net.sourceforge.vulcan.maven.integration.MavenIntegration;

import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class MavenBuildPlugin extends PluginSupport
		implements BuildToolPlugin, ConfigurablePlugin, ApplicationContextAware {
	
	public static final String PLUGIN_ID = "net.sourceforge.vulcan.maven";
	public static final String PLUGIN_NAME = "Apache Maven";
	
	public static final String M2_POM_NS = "http://maven.apache.org/POM/4.0.0";
	
	MavenConfig config = new MavenConfig();
	MavenBuildToolFactory mavenBuildToolFactory = new MavenBuildToolFactory();
	
	MavenProjectConfiguratorFactory configuratorFactory;
	
	String maven2HomeDirectory;
	String maven2ProfileName;
	String defaultGoals;
	
	ApplicationContext applicationContext;
	
	public void init() {
		addSystemJavaHomeIfMissing(config, applicationContext);
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public String getId() {
		return PLUGIN_ID;
	}
	public String getName() {
		return PLUGIN_NAME;
	}
	
	public BuildTool createInstance(BuildToolConfigDto projectConfig) throws ConfigException {
		final MavenProjectConfig mavenProjectConfig = (MavenProjectConfig)projectConfig;
		
		final String javaHomeName = mavenProjectConfig.getJavaHome();
		final JavaHome javaHome;
		
		if (isNotBlank(javaHomeName) && javaHomeName.startsWith("System")) {
			javaHome = getSelectedEnvironment(
				this.config.getJavaHomes(),
				"System",
				null,
				true);
		} else {
			javaHome = getSelectedEnvironment(
				this.config.getJavaHomes(),
				javaHomeName,
				"ant.java.profile.missing");
		}

		final MavenHome mavenHome = getSelectedEnvironment(
				this.config.getMavenHomes(),
				mavenProjectConfig.getMavenHome(),
				"maven.home.profile.missing");
		
		return mavenBuildToolFactory.createMavenBuildTool(mavenProjectConfig, config, javaHome, mavenHome);
	}
	
	public ProjectBuildConfigurator createProjectConfigurator(File buildSpecFile, Document xmlDocument) throws ConfigException {
		if (xmlDocument == null) {
			return null;
		}
		
		final Element rootNode = xmlDocument.getRootElement();
		if (!M2_POM_NS.equals(rootNode.getNamespace().getURI())) {
			final Element modelVersionNode = rootNode.getChild("modelVersion");
			if (modelVersionNode == null || !"4.0.0".equals(modelVersionNode.getText())) {
				return null;
			}
		}
		
		if (configuratorFactory == null) {
			createConfigurationFactory();
		}

		return configuratorFactory.createProjectConfigurator(buildSpecFile, maven2ProfileName, defaultGoals, applicationContext);
	}
	
	public MavenProjectConfig getDefaultConfig() {
		return new MavenProjectConfig();
	}
	
	public void setDefaultGoals(String defaultGoals) {
		this.defaultGoals = defaultGoals;
	}

	public MavenConfig getConfiguration() {
		return config;
	}
	
	public void setConfiguration(PluginConfigDto config) {
		this.config = (MavenConfig) config;
		addSystemJavaHomeIfMissing(this.config, applicationContext);
	}

	void createConfigurationFactory() throws ConfigException {
		// find maven2 home
		for (MavenHome home : config.getMavenHomes()) {
			if (MavenBuildTool.isMaven2(home.getDirectory())) {
				maven2HomeDirectory = home.getDirectory();
				maven2ProfileName = home.getDescription();
				break;
			}
		}
		
		if (maven2HomeDirectory == null) {
			throw new ConfigException("maven.maven2.required", null);
		}

		// create classloader
		final URLClassLoader classLoader = 
			new PackageFilteringClassLoader(
					createURLs(maven2HomeDirectory),
					getClass().getClassLoader(),
					MavenIntegration.class.getPackage().getName());
		
		// load integration provider
		try {
			configuratorFactory = (MavenProjectConfiguratorFactory) classLoader.loadClass(MavenIntegration.class.getName()).newInstance();
		} catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException)e;
			}
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	static URL[] createURLs(String mavenHomeDirectory) throws ConfigException {
		final String[] extensions = new String[] {"jar"};

		try {
			final List<File> files = new ArrayList<File>();
		
			files.addAll(FileUtils.listFiles(new File(mavenHomeDirectory, "core"), extensions, true));
			files.addAll(FileUtils.listFiles(new File(mavenHomeDirectory, "lib"), extensions, true));
	
			final URL[] urls = new URL[files.size() + 1];
	
			for (int i=0; i<files.size(); i++) {
				urls[i] = files.get(i).toURL();
			}

			urls[urls.length - 1] = MavenBuildTool.getLocalClassPathEntry(MavenBuildPlugin.class, null).toURL();
			
			return urls;
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * ClassLoader forces classes in a specified package (or subpackage)to be loaded
	 * by this instance instead of delegating to the parent first.  This allows classes
	 * in the integration package to be loaded by the same instance that has the jars
	 * in the Maven 2 home.  This is required to avoid packaging all of those jars, which
	 * would increase the size of this plugin by nearly 2 megabytes.
	 */
	static class PackageFilteringClassLoader extends URLClassLoader {
		private final String packageName;

		public PackageFilteringClassLoader(URL[] urls, ClassLoader parent, String packageName) {
			super(urls, parent);
			this.packageName = packageName;
		}

		@Override
		protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (!name.startsWith(packageName)) {
				return super.loadClass(name, resolve);
			}
			
			// Against standard practie, do NOT delegate to parent first:
			
			Class c = findLoadedClass(name);
			if (c == null) {
				c = findClass(name);
			}
			
			if (resolve) {
				resolveClass(c);
			}

			return c;
		}
	}
}

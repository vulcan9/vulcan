/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
package net.sourceforge.vulcan.maven.integration;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.maven.MavenProjectBuildConfigurator;
import net.sourceforge.vulcan.maven.MavenProjectConfig;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.codehaus.classworlds.UrlUtils;
import org.springframework.context.ApplicationContext;

public class MavenProjectConfiguratorImpl implements MavenProjectBuildConfigurator {
	private final static Pattern SCM_URL_PREFIX = Pattern.compile("^scm:\\w+:");
	
	private final MavenProject project;
	private final String mavenHomeProfileName;
	private final String goals;
	private final ApplicationContext applicationContext;

	private String basedir;
	
	public MavenProjectConfiguratorImpl(MavenProject project, String mavenHomeProfileName, String goals, ApplicationContext applicationContext) {
		this.project = project;
		this.mavenHomeProfileName = mavenHomeProfileName;
		this.goals = goals;
		this.applicationContext = applicationContext;
	}

	public void applyConfiguration(ProjectConfigDto projectConfig, String buildSpecRelativePath, List<String> existingProjectNames, boolean createSubprojects) {
		final MavenProjectConfig mavenProjectConfig = new MavenProjectConfig();
		mavenProjectConfig.setApplicationContext(applicationContext);
		mavenProjectConfig.setTargets(goals);
		mavenProjectConfig.setMavenHome(mavenHomeProfileName);
		
		if (createSubprojects) {
			mavenProjectConfig.setNonRecursive(true);
		}
		
		projectConfig.setName(project.getArtifactId());
		projectConfig.setBuildToolConfig(mavenProjectConfig);
		
		final String[] vulcanDeps = findVulcanProjectDependencies(existingProjectNames);
		
		projectConfig.setDependencies(vulcanDeps);
		
		determineBasedir(getModules());
	}

	public String getRelativePathToProjectBasedir() {
		return basedir;
	}
	
	public boolean isStandaloneProject() {
		return "pom".equals(project.getPackaging());
	}

	public boolean shouldCreate() {
		return true;
	}

	public List<String> getSubprojectUrls() {
		final List<String> moduleList = getModules();
		
		if (moduleList.isEmpty()) {
			return Collections.emptyList();
		}
		
		final String[] moduleNames = moduleList.toArray(new String[moduleList.size()]);
		
		final String rootUrl = determineScmRootUrl();
		
		for (int i = 0; i < moduleNames.length; i++) {
			moduleNames[i] = UrlUtils.normalizeUrlPath(rootUrl + moduleNames[i] + "/pom.xml");
		}
		
		return Arrays.asList(moduleNames);
	}

	public String determineScmRootUrl() {
		final Scm scm = project.getScm();
		
		if (scm == null) {
			throw new IllegalStateException("Maven project " + project.getArtifactId() + " is missing scm information.");
		}
		
		final String url = scm.getConnection();
		return normalizeScmUrl(url);
	}
	
	public void determineBasedir(List<String> modules) {
		final Pattern p = Pattern.compile("^(\\.\\./)+");
		
		for (String modulePath : modules) {
			final Matcher matcher = p.matcher(modulePath);
			if (matcher.find()) {
				final String path = matcher.group();
				
				if (basedir == null || basedir.length() < path.length()) {
					basedir = path;
				}
			}
		}
	}

	public static String normalizeScmUrl(final String url) {
		final StringBuilder sb = new StringBuilder(url);

		if (!url.endsWith("/")) {
			sb.append('/');
		}

		final Matcher matcher = SCM_URL_PREFIX.matcher(sb);
		
		while (matcher.find()) {
			sb.delete(0, matcher.end());
			matcher.reset();
		}
		
		return sb.toString();
	}
	
	@SuppressWarnings("unchecked")
	private String[] findVulcanProjectDependencies(List<String> existingProjectNames) {
		final Set<String> vulcanDeps = new HashSet<String>();

		final List<Dependency> deps = project.getDependencies();
		for (Dependency dep : deps) {
			if (existingProjectNames.contains(dep.getArtifactId())) {
				vulcanDeps.add(dep.getArtifactId());
			}
		}
		
		final Set<Artifact> plugins = project.getPluginArtifacts();
		for (Artifact artifact : plugins) {
			if (existingProjectNames.contains(artifact.getArtifactId())) {
				vulcanDeps.add(artifact.getArtifactId());
			}
		}
		
		MavenProject parent = project.getParent();
		while (parent != null) {
			if (existingProjectNames.contains(parent.getArtifactId())) {
				vulcanDeps.add(parent.getArtifactId());
			}
			parent = parent.getParent();
		}
		
		return vulcanDeps.toArray(new String[vulcanDeps.size()]);
	}

	@SuppressWarnings("unchecked")
	private List<String> getModules() {
		return project.getModules();
	}
}

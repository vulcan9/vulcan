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
package net.sourceforge.vulcan.dotnet;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.vulcan.ProjectBuildConfigurator;
import net.sourceforge.vulcan.dotnet.dto.DotNetProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;

import org.jdom.Document;
import org.jdom.Element;
import org.springframework.context.ApplicationContext;

public class MSBuildProjectConfigurator implements ProjectBuildConfigurator {
	private ApplicationContext applicationContext;
	private String buildEnvironment;
	private Document document;
	private String url;
	
	private List<String> subprojectUrls;
	private String relativePathToProjectBasedir;
	private boolean shouldCreate = true;
	
	public void applyConfiguration(ProjectConfigDto projectConfig,
			String buildSpecRelativePath, List<String> existingProjectNames,
			boolean createSubprojects) {
		
		final DotNetProjectConfigDto dotNetConfig = new DotNetProjectConfigDto();
		dotNetConfig.setApplicationContext(applicationContext);
		dotNetConfig.setBuildScript(buildSpecRelativePath);
		dotNetConfig.setBuildEnvironment(buildEnvironment);
		
		projectConfig.setName(getProjectName(url));
		projectConfig.setBuildToolConfig(dotNetConfig);
		
		if (createSubprojects) {
			if (subprojectUrls == null) {
				findImportedProjectPaths(document);
			}
			
			final List<String> dependencyNames = new ArrayList<String>();
			
			for (Iterator<String> itr = subprojectUrls.iterator(); itr.hasNext(); ) {
				final String name = getProjectName(itr.next());
				
				dependencyNames.add(name);
			}
			
			projectConfig.setDependencies(dependencyNames.toArray(new String[dependencyNames.size()]));
		}
	}

	public String getRelativePathToProjectBasedir() {
		return relativePathToProjectBasedir;
	}

	public void setRelativePathToProjectBasedir(String relativePathToProjectBasedir) {
		this.relativePathToProjectBasedir = relativePathToProjectBasedir;
	}
	
	public List<String> getSubprojectUrls() {
		return subprojectUrls;
	}

	public void setSubprojectUrls(List<String> subprojectUrls) {
		this.subprojectUrls = subprojectUrls;
	}
	
	public boolean shouldCreate() {
		return shouldCreate;
	}
	
	public void setShouldCreate(boolean shouldCreate) {
		this.shouldCreate = shouldCreate;
	}
	
	public boolean isStandaloneProject() {
		return false;
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public String getBuildEnvironment() {
		return buildEnvironment;
	}

	public void setBuildEnvironment(String buildEnvironment) {
		this.buildEnvironment = buildEnvironment;
	}

	public Document getDocument() {
		return document;
	}
	
	public void setDocument(Document document) {
		this.document = document;
	}

	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	@SuppressWarnings("unchecked")
	private void findImportedProjectPaths(Document xmlDocument) {
		final Element root = xmlDocument.getRootElement();
		final List<Element> itemGroups = root.getChildren("ItemGroup", root.getNamespace());
		
		subprojectUrls = new ArrayList<String>();
		
		for (Element e : itemGroups) {
			final List<Element> projectReferences = e.getChildren("ProjectReference", root.getNamespace());
			
			for (Element projRef : projectReferences) {
				final String path = projRef.getAttributeValue("Include");
				subprojectUrls.add(path.replaceAll("\\\\", "/"));
			}
		}
	}

	private String getProjectName(String url) {
		final int lastSlash = url.lastIndexOf('/');
		if (lastSlash > 0) {
			url = url.substring(lastSlash + 1);
		}
		
		final int lastDot = url.lastIndexOf('.');
		if (lastDot > 0) {
			url = url.substring(0, lastDot);
		}
		
		return url;
	}
}

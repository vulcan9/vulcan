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
package net.sourceforge.vulcan.spring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.core.BeanEncoder;
import net.sourceforge.vulcan.core.support.AbstractFileStore;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.event.WarningEvent;
import net.sourceforge.vulcan.exception.CannotCreateDirectoryException;
import net.sourceforge.vulcan.exception.ResourceNotFoundException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.safehaus.uuid.UUIDGenerator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class SpringFileStore extends AbstractFileStore implements BeanFactoryAware {
	private BeanFactory beanFactory;
	private BeanEncoder beanEncoder;
	
	public synchronized StateManagerConfigDto loadConfiguration() throws StoreException {
		final Resource configResource = getConfigurationResource();
		
		final BeanFactory beanFactory = new XmlBeanFactory(configResource, this.beanFactory);
		
		return (StateManagerConfigDto) beanFactory.getBean("configuration");
	}
	public synchronized void exportConfiguration(OutputStream os) throws IOException, StoreException {
		final InputStream is = getConfigurationResource().getInputStream();
		
		try {
			IOUtils.copy(is, os);
		} finally {
			is.close();
		}
	}
	public String getExportMimeType() {
		return "application/xml";
	}
	public synchronized void importConfiguration(InputStream is) throws StoreException, IOException {
		final OutputStream os = new FileOutputStream(getConfigFile());
		
		try {
			IOUtils.copy(is, os);
		} finally {
			try {
				is.close();
			} finally {
				os.close();
			}
		}
	}
	public synchronized void storeConfiguration(StateManagerConfigDto config) throws StoreException {
		beanEncoder.reset();
		beanEncoder.addBean("configuration", config);
		
		final File configFile = getConfigFile();
		final File tmp;
		
		try {
			tmp = File.createTempFile("config", "xml", configFile.getParentFile());
		} catch (IOException e) {
			throw new StoreException("Cannot create temp file in " + configFile.getParent(), e);
		}
		
		writeBeanConfig(tmp);
		
		try {
			if (FileUtils.contentEquals(configFile, tmp)) {
				tmp.delete();
			} else {
				if (tmp.renameTo(configFile) == false) {
					// MS Windows work-around:
					FileUtils.copyFile(tmp, configFile, true);
					tmp.delete();
				}
			}
		} catch (IOException e) {
			throw new StoreException("Error comparing config files", e);
		}
	}
	public synchronized UUID storeBuildOutcome(ProjectStatusDto outcome) throws StoreException {
		UUID id = outcome.getId();
		
		if (id == null || id.version() != 1) {
			id = generateTimeBasedUUID();
			outcome.setId(id);
		}
		
		removeUnusedResources(outcome.getName(), outcome);
		
		beanEncoder.reset();
		beanEncoder.addBean("build-outcome", outcome);
		
		final File outcomeDir = getOutcomeDir(outcome.getName());
		
		if (!outcomeDir.isDirectory() && !outcomeDir.mkdirs()) {
			throw new CannotCreateDirectoryException(outcomeDir);
		}
		
		final File file = new File(outcomeDir, id.toString());
		writeBeanConfig(file);
		
		return id;
	}
	public synchronized ProjectStatusDto loadBuildOutcome(String projectName, UUID id) throws StoreException {
		final File file = new File(getOutcomeDir(projectName), id.toString());
		
		if (!file.exists()) {
			throw new StoreException("No such build outcome for project " + projectName, null);
		}
		
		final BeanFactory beanFactory = new XmlBeanFactory(new FileSystemResource(file), this.beanFactory);
		
		final ProjectStatusDto outcome = (ProjectStatusDto) beanFactory.getBean("build-outcome");
		
		removeUnusedResources(projectName, outcome);

		return outcome;
	}
	public ProjectStatusDto createBuildOutcome(String projectName) {
		final ProjectStatusDto status = new ProjectStatusDto();
		
		final UUID id = generateTimeBasedUUID();
		
		status.setName(projectName);
		status.setId(id);
		status.setDiffId(id);
		status.setBuildLogId(id);
		
		return status;
	}
	public OutputStream getChangeLogOutputStream(String projectName, UUID diffId) throws StoreException {
		final File dir = getChangeLogDir(projectName);
		
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		
		try {
			return new FileOutputStream(new File(dir, diffId.toString()));
		} catch (FileNotFoundException e) {
			throw new StoreException(e);
		}
	}
	public InputStream getChangeLogInputStream(String projectName, UUID diffId) throws StoreException {
		final File file = new File(getChangeLogDir(projectName), diffId.toString());
		
		if (checkFile(file)) {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException ignore) {
			}
		}

		throw new ResourceNotFoundException("ProjectName: " + projectName + ", diffId: " + diffId);
	}
	public OutputStream getBuildLogOutputStream(String projectName, UUID buildLogId) throws StoreException {
		final File dir = getBuildLogDir(projectName);
		
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		
		try {
			return new FileOutputStream(new File(dir, buildLogId.toString()));
		} catch (FileNotFoundException e) {
			throw new StoreException(e);
		}
	}
	public InputStream getBuildLogInputStream(String projectName, UUID buildLogId) throws StoreException {
		final File file = new File(getBuildLogDir(projectName), buildLogId.toString());
				
		if (checkFile(file)) {
			try {
				return new FileInputStream(file);
			} catch (FileNotFoundException ignore) {
			}
		}

		throw new ResourceNotFoundException("ProjectName: " + projectName + ", buildLogId: " + buildLogId);
	}

	public synchronized Map<String, List<UUID>> getBuildOutcomeIDs() {
		final Map<String, List<UUID>> map = new HashMap<String, List<UUID>>();
		final File[] projects = getProjectsRoot().listFiles();
		
		if (projects == null) {
			throw new IllegalStateException("cannot list contents of " + getProjectsRoot());
		}
		
		for (File f : projects) {
			final List<UUID> list = new ArrayList<UUID>();
			
			final String[] ids = new File(f, "outcomes").list();
			if (ids == null) {
				continue;
			}
			
			for (String id : ids) {
				list.add(UUID.fromString(id));
			}
			
			// Sort in chronological order
			Collections.sort(list, new Comparator<UUID>() {
				public int compare(UUID o1, UUID o2) {
					final long t1 = o1.timestamp();
					final long t2 = o2.timestamp();
					
					if (t1 > t2) {
						return 1;
					} else if (t1 < t2) {
						return -1;
					}
					return 0;
				}
			});
			map.put(f.getName(), list);
		}
		return map;
	}
	public BeanFactory getBeanFactory() {
		return beanFactory;
	}
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
	public BeanEncoder getBeanEncoder() {
		return beanEncoder;
	}
	public void setBeanEncoder(BeanEncoder beanEncoder) {
		this.beanEncoder = beanEncoder;
	}
	UUID generateTimeBasedUUID() {
		return UUID.fromString(UUIDGenerator.getInstance().generateTimeBasedUUID().toString());
	}
	Resource getConfigurationResource() throws StoreException {
		final File config = getConfigFile();
		final Resource configResource;
		
		if (config.exists()) {
			configResource = new FileSystemResource(config);
		} else {
			eventHandler.reportEvent(new WarningEvent(this, "FileStore.default.config"));
			configResource = new UrlResource(getClass().getResource("default-config.xml"));			
		}
		
		if (!configResource.exists()) {
			throw new StoreException("Resource " + configResource + " does not exist", null);
		}
		return configResource;
	}

	private void writeBeanConfig(File file) throws StoreException {
		Writer writer = null;
		
		try {
			writer = new FileWriter(file);
			beanEncoder.write(writer);
		} catch (IOException e) {
			throw new StoreException(e);
		} finally {
			try {
				if (writer != null) {
					writer.close();
				}
			} catch (IOException ignore) {
			}
		}
	}
	private File getOutcomeDir(String projectName) {
		return new File(
				getProjectsRoot() + File.separator + projectName,
				"outcomes");
	}
	private File getChangeLogDir(String projectName) {
		return new File(
				getProjectsRoot() + File.separator + projectName,
				"changelogs");
	}
	private File getBuildLogDir(String projectName) {
		return new File(
				getProjectsRoot() + File.separator + projectName,
				"buildlogs");
	}
	private boolean checkFile(File file) {
		if (file.exists()) {
			if (file.length() > 0) {
				return true;
			}
			
			// sometimes diffs are created but have no contents.  delete them.
			file.delete();
		}
		return false;
	}
	private void removeUnusedResources(String projectName, final ProjectStatusDto outcome) {
		if (outcome.getBuildLogId() != null) {
			final File buildLog = new File(getBuildLogDir(projectName), outcome.getBuildLogId().toString());
			
			if (checkFile(buildLog) == false) {
				outcome.setBuildLogId(null);
			}
		}
		
		if (outcome.getDiffId() != null) {
			final File diff = new File(getChangeLogDir(projectName), outcome.getDiffId().toString());
			
			if (checkFile(diff) == false) {
				outcome.setDiffId(null);
			}
		}
	}
}

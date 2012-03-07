/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
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
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.core.support.AbstractFileStore;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.event.WarningEvent;
import net.sourceforge.vulcan.exception.StoreException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;


public class SpringFileStore extends AbstractFileStore implements BeanFactoryAware, ProjectNameChangeListener {
	private BeanFactory beanFactory;
	private BeanEncoder beanEncoder;
	
	public synchronized StateManagerConfigDto loadConfiguration() throws StoreException {
		final Resource configResource = getConfigurationResource();
		
		final XmlBeanFactory beanFactory = new XmlBeanFactory(configResource, this.beanFactory);
		beanFactory.registerCustomEditor(java.util.Date.class, new DateEditor());
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
	
	public void projectNameChanged(String oldName, String newName) {
		final File oldDir = new File(getProjectDir(oldName));
		final File newDir = new File(getProjectDir(newName));
		
		if (oldDir.exists()) {
			if (!oldDir.renameTo(newDir)) {
				eventHandler.reportEvent(
						new WarningEvent(
								this,
								"errors.store.project.rename",
								new String[] { oldDir.getAbsolutePath(), newDir.getAbsolutePath() }));
			}
		}
	}
	
	public boolean buildLogExists(String projectName, UUID id) {
		final File buildLog = new File(getBuildLogDir(projectName), id.toString());
		
		return deleteFileIfEmpty(buildLog);
	}
	
	public boolean diffExists(String projectName, UUID id) {
		final File buildLog = new File(getChangeLogDir(projectName), id.toString());
		
		return deleteFileIfEmpty(buildLog);
	}
	
	public File getBuildLog(String projectName, UUID id) throws StoreException {
		final File dir = getBuildLogDir(projectName);
		
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		
		return new File(dir, id.toString());
	}
	
	public File getChangeLog(String projectName, UUID id) throws StoreException {
		final File dir = getChangeLogDir(projectName);
		
		if (!dir.isDirectory()) {
			dir.mkdirs();
		}
		
		return new File(dir, id.toString());
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
	
	private String getProjectDir(String projectName) {
		return getProjectsRoot() + File.separator + projectName;
	}
	
	private File getChangeLogDir(String projectName) {
		return new File(
				getProjectDir(projectName),
				"changelogs");
	}
	
	private File getBuildLogDir(String projectName) {
		return new File(
				getProjectDir(projectName),
				"buildlogs");
	}
	
	/**
	 * @return true if the file exists and is not empty, false otherwise.
	 */
	private boolean deleteFileIfEmpty(File file) {
		if (file.exists()) {
			if (file.length() > 0) {
				return true;
			}
			
			// sometimes diffs are created but have no contents.  delete them.
			file.delete();
		}
		return false;
	}
}

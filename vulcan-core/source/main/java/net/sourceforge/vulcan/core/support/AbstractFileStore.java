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
package net.sourceforge.vulcan.core.support;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sourceforge.vulcan.core.ConfigurationStore;
import net.sourceforge.vulcan.dto.PluginMetaDataDto;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.event.WarningEvent;
import net.sourceforge.vulcan.exception.CannotCreateDirectoryException;
import net.sourceforge.vulcan.exception.DuplicatePluginIdException;
import net.sourceforge.vulcan.exception.InvalidPluginLayoutException;
import net.sourceforge.vulcan.exception.StoreException;
import net.sourceforge.vulcan.integration.PluginVersionSpec;
import net.sourceforge.vulcan.integration.support.PluginVersionDigester;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;


public abstract class AbstractFileStore implements ConfigurationStore {
	protected String workingCopyLocationPattern;
	protected File configRoot;
	protected EventHandler eventHandler;
	
	public final void init() {
		if (!configRoot.exists()) {
			eventHandler.reportEvent(new WarningEvent(this, "FileStore.create.dir",
					 new Object[] {configRoot}));
			configRoot.mkdirs();
			getPluginsRoot().mkdirs();
		}
		getProjectsRoot().mkdirs();
	}
	public final PluginMetaDataDto extractPlugin(InputStream is) throws StoreException {
		final File pluginsDir = getPluginsRoot();
		String toplevel = null;
		File tmpDir = null;
		
		if (!pluginsDir.exists()) {
			createDir(pluginsDir);
		}
		
		final ZipInputStream zis = new ZipInputStream(is);
		ZipEntry entry;
		
		try {
			entry = zis.getNextEntry();
			if (entry == null || !entry.isDirectory()) {
				throw new InvalidPluginLayoutException();
			}
			toplevel = entry.getName();
			tmpDir = new File(pluginsDir, "tmp-" + toplevel);
			if (!tmpDir.exists()) {
				createDir(tmpDir);
			}

			final String id = toplevel.replaceAll("/", "");

			while ((entry = zis.getNextEntry()) != null) {
				if (!entry.getName().startsWith(toplevel)) {
					throw new InvalidPluginLayoutException();
				}

				final File out = new File(pluginsDir, "tmp-" + entry.getName());
				
				if (entry.isDirectory()) {
					createDir(out);
					zis.closeEntry();
				} else {
					final FileOutputStream os = new FileOutputStream(out);
					
					try {
						IOUtils.copy(zis, os);
					} finally {
						os.close();
						zis.closeEntry();
					}
				}
			}
			
			checkPluginVersion(pluginsDir, id);
			
			return createPluginConfig(new File(pluginsDir, toplevel));
		} catch (DuplicatePluginIdException e) {
			throw e;
		} catch (Exception e) {
			if (e instanceof StoreException) {
				throw (StoreException) e;
			}
			throw new StoreException(e.getMessage(), e);
		} finally {
			try {
				zis.close();
			} catch (IOException ignore) {
			}
			if (tmpDir != null && tmpDir.exists()) {
				try {
					FileUtils.deleteDirectory(tmpDir);
				} catch (Exception ignore) {
				}
			}
		}
	}
	public final void deletePlugin(String id) throws StoreException {
		final File dir = new File(getPluginsRoot(), id);
		try {
			if (!dir.exists()) {
				throw new FileNotFoundException(id);
			}
			FileUtils.deleteDirectory(dir);	
		} catch (Exception e) {
			try {
				new File(dir, ".delete").createNewFile();
			} catch (IOException ioe) {
				eventHandler.reportEvent(new ErrorEvent(this, 
						"FileStore.mark.plugin.for.delete",
						new Object[] {dir}, ioe));
			}
			throw new StoreException(e.getMessage(), e);
		}
	}
	public final PluginMetaDataDto[] getPluginConfigs() {
		final List<PluginMetaDataDto> plugins = new ArrayList<PluginMetaDataDto>();
		final File[] dirs = getPluginsRoot().listFiles((FileFilter)FileFilterUtils.directoryFileFilter());

		if (dirs != null) {
			for (int i=0; i<dirs.length; i++) {
				if (new File(dirs[i], ".delete").exists()) {
					try {
						FileUtils.deleteDirectory(dirs[i]);
					} catch (IOException e) {
						eventHandler.reportEvent(new ErrorEvent(this,
								"FileStore.delete.marked.plugin",
								new Object[] {dirs[i]}, e));
					}
					continue;
				}
				try {
					plugins.add(createPluginConfig(dirs[i]));
				} catch (IOException e) {
					eventHandler.reportEvent(new ErrorEvent(this,
							"errors.plugin.load.version.failure",
							new Object[] {e.getMessage()}, e));
				}
			}
		}
		return plugins.toArray(new PluginMetaDataDto[plugins.size()]);
	}
	public final void setConfigRoot(String root) {
		configRoot = new File(root);
	}
	public final String getConfigRoot() {
		return configRoot.getPath();
	}
	public String getWorkingCopyLocationPattern() {
		if (new File(workingCopyLocationPattern).isAbsolute()) {
			return workingCopyLocationPattern;
		}
		
		return new File(configRoot, workingCopyLocationPattern).getAbsolutePath();
	}
	public void setWorkingCopyLocationPattern(String workingCopyLocationPattern) {
		this.workingCopyLocationPattern = workingCopyLocationPattern;
	}
	public boolean isWorkingCopyLocationInvalid(String location) {
		final File file = new File(location);
		
		if (file.equals(configRoot) || file.equals(getProjectsRoot())
				|| file.getParentFile().equals(getProjectsRoot())) {
			return true;
		}
		return false;
	}
	public final File getConfigFile() {
		return new File(configRoot, "config.xml");
	}
	public final File getPluginsRoot() {
		return new File(getConfigRoot(), "plugins");
	}
	public final File getProjectsRoot() {
		return new File(getConfigRoot(), "projects");
	}
	public EventHandler getEventHandler() {
		return eventHandler;
	}
	public void setEventHandler(EventHandler errorHandler) {
		this.eventHandler = errorHandler;
	}
	@SuppressWarnings("unchecked")
	final URL[] getJars(File dir) {
		final List<URL> list = new ArrayList<URL>();
		
		final Collection<File> jars = FileUtils.listFiles(dir, new SuffixFileFilter(".jar"), null);
		
		for (File file : jars) {
			try {
				list.add(file.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return list.toArray(new URL[list.size()]);
	}
	protected final PluginMetaDataDto createPluginConfig(File pluginDir) throws IOException {
		final PluginMetaDataDto plugin = new PluginMetaDataDto();
		plugin.setId(pluginDir.getName());
		plugin.setClassPath(getJars(pluginDir));
		plugin.setDirectory(pluginDir);
		
		final File versionFile = new File(pluginDir, "plugin-version.xml");
		
		if (!versionFile.exists()) {
			return plugin;
		}
		
		final InputStream is = new FileInputStream(versionFile);
		
		final String versionContents;
		try {
			versionContents = IOUtils.toString(is);
		} finally {
			is.close();
		}
		
		final PluginVersionSpec spec = PluginVersionDigester.digest(new StringReader(versionContents));
		
		plugin.setRevision(spec.getPluginRevision());
		plugin.setVersion(spec.getVersion());

		return plugin;
	}
	protected void checkPluginVersion(File pluginsDir, String id) throws IOException, InvalidPluginLayoutException, DuplicatePluginIdException {
		final File tmpDir = new File(pluginsDir, "tmp-" + id);
		final File newVersionFile = new File(tmpDir, "plugin-version.xml");

		final File targetDir = new File(pluginsDir, id);
		final File existingVersionFile = new File(targetDir, "plugin-version.xml");
		
		if (!targetDir.exists()) {
			if (!tmpDir.renameTo(targetDir)) {
				throw new IOException("Cannot rename " + tmpDir + " to " + targetDir);
			}

			return;
		}
		
		if (!newVersionFile.exists()) {
			throw new InvalidPluginLayoutException();
		}
		
		final PluginVersionSpec importVersion = parsePluginVersion(newVersionFile);
		final PluginVersionSpec installedVersion = parsePluginVersion(existingVersionFile);
			
		if (importVersion.getPluginRevision() <= installedVersion.getPluginRevision()) {
			throw new DuplicatePluginIdException(id);				
		}
			
		FileUtils.deleteDirectory(targetDir);
		if (!tmpDir.renameTo(targetDir)) {
			throw new IOException("Cannot rename " + tmpDir + " to " + targetDir);
		}
		
		eventHandler.reportEvent(new WarningEvent(this, "FileStore.plugin.updated", new String[] {id}));
	}
	private PluginVersionSpec parsePluginVersion(final File existingVersionFile) throws FileNotFoundException, IOException {
		final InputStream is = new FileInputStream(existingVersionFile);

		try {
			return PluginVersionDigester.digest(new InputStreamReader(is));
		} finally {
			is.close();
		}
	}
	private void createDir(final File dir) throws CannotCreateDirectoryException {
		if (!dir.mkdirs()) {
			throw new CannotCreateDirectoryException(dir);
		}
	}
}

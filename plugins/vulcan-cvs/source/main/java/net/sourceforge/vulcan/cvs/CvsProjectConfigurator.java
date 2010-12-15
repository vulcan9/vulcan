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
package net.sourceforge.vulcan.cvs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import net.sourceforge.vulcan.ProjectRepositoryConfigurator;
import net.sourceforge.vulcan.cvs.dto.CvsConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsProjectConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsRepositoryProfileDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.exception.AuthenticationRequiredRepositoryException;
import net.sourceforge.vulcan.exception.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.netbeans.lib.cvsclient.CVSRoot;
import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.StandardAdminHandler;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.export.ExportCommand;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.springframework.context.ApplicationContext;

public class CvsProjectConfigurator extends CvsSupport implements ProjectRepositoryConfigurator {
	private final String file;
	private final boolean newProfile;

	CvsProjectConfigurator(CvsConfigDto globalConfig, CvsRepositoryProfileDto profile, CvsProjectConfigDto projectConfig, CVSRoot cvsRoot, String file, boolean newProfile) throws RepositoryException {
		super(globalConfig, profile, projectConfig, cvsRoot);
		this.file = file;
		this.newProfile = newProfile;
	}

	static CvsProjectConfigurator createInstance(ApplicationContext applicationContext, CvsConfigDto globalConfig, String url, String username, String password) throws RepositoryException {
		String method = url;
		
		if (method.charAt(0) == ':') {
			method = url.substring(1);
		}

		int index = url.lastIndexOf(':');
		
		if (index < 0) {
			return null;
		}
		
		final String file = url.substring(index + 1);
		url = url.substring(0, index);
		
		if (!method.startsWith(CVSRoot.METHOD_EXT)
				&&!method.startsWith(CVSRoot.METHOD_FORK)
				&&!method.startsWith(CVSRoot.METHOD_LOCAL)
				&&!method.startsWith(CVSRoot.METHOD_PSERVER)
				&&!method.startsWith(CVSRoot.METHOD_SERVER)) {
			return null;
		}

		final CvsProjectConfigDto projectConfig = new CvsProjectConfigDto();
		final CVSRoot cvsRoot = CVSRoot.parse(url);
		
		CvsRepositoryProfileDto profile = getSelectedEnvironment(globalConfig.getProfiles(), new Visitor<CvsRepositoryProfileDto>() {
			public boolean isMatch(CvsRepositoryProfileDto node) {
				return new EqualsBuilder().append(
						node.getHost(), cvsRoot.getHostName()).append(
						node.getRepositoryPath(), cvsRoot.getRepository()).append(
						node.getProtocol(), cvsRoot.getMethod()).isEquals();
			}
		});

		boolean newProfile = profile == null;
		
		if (newProfile) {
			profile = new CvsRepositoryProfileDto();
			
			profile.setApplicationContext(applicationContext);
			profile.setDescription(cvsRoot.getHostName());
			profile.setHost(cvsRoot.getHostName());
			profile.setUsername(cvsRoot.getUserName());
			profile.setPassword(cvsRoot.getPassword());
			profile.setRepositoryPath(cvsRoot.getRepository());
			profile.setProtocol(cvsRoot.getMethod());
		}
		
		projectConfig.setApplicationContext(applicationContext);
		projectConfig.setRepositoryProfile(profile.getDescription());
		
		return new CvsProjectConfigurator(globalConfig, profile, projectConfig, cvsRoot, file, newProfile);
	}
	
	@SuppressWarnings("unchecked")
	public void download(File target) throws RepositoryException, IOException {
		openConnection();
		
		final File exportDir = new File(target.getParentFile(), "cvstmp");
		
		try {
			final Client client = new Client(this.connection, new StandardAdminHandler());
			final ExportCommand cmd = new ExportCommand();
			
			client.setLocalPath(exportDir.getParent());
			
			cmd.setModules(new String[] {file});
			
			//TODO: how to allow user to specify branch/tag?
			cmd.setExportByRevision(tag);
			
			cmd.setExportDirectory(exportDir.getName());
			cmd.setRecursive(false);
			
			client.executeCommand(cmd, options);
			
			final Collection<File> files = FileUtils.listFiles(exportDir, null, false);
			if (files.size() != 1) {
				throw new RepositoryException("cvs.erorrs.export.failed", null, files.size());
			}
			
			final File source = files.iterator().next();
			if (!source.renameTo(target)) {
				FileUtils.copyFile(source, target);
			}
		} catch (CommandAbortedException e) {
			throw new RepositoryException(e);
		} catch (CommandException e) {
			throw new RepositoryException(e);
		} catch (AuthenticationException e) {
			throw new AuthenticationRequiredRepositoryException(cvsRoot.getUserName());
		} finally {
			connection.close();
			FileUtils.deleteDirectory(exportDir);
		}
	}
	
	public void applyConfiguration(ProjectConfigDto projectConfig, String projectBasedirUrl) {
		projectConfig.setRepositoryAdaptorConfig(this.config);
		
		final String[] strings = projectBasedirUrl.split(":");
		
		this.config.setModule(strings[strings.length-1]);
		this.config.setBranch("");
	}
	
	public void setNonRecursive() {
		this.config.setRecursive(false);
	}
	
	public boolean updateGlobalConfig(PluginConfigDto globalRaConfig) {
		if (!newProfile) {
			return false;
		}
		
		final CvsConfigDto globalConfig = (CvsConfigDto) globalRaConfig;
		
		final List<CvsRepositoryProfileDto> profiles =
			new ArrayList<CvsRepositoryProfileDto>(Arrays.asList(globalConfig.getProfiles()));
		
		profiles.add(this.profile);
		
		globalConfig.setProfiles(profiles.toArray(new CvsRepositoryProfileDto[profiles.size()]));
		
		return true;
	}
	
	public String getFile() {
		return file;
	}
}

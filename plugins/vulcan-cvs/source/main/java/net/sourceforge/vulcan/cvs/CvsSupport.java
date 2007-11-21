/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import net.sourceforge.vulcan.cvs.dto.CvsConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsProjectConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsRepositoryProfileDto;
import net.sourceforge.vulcan.exception.AuthenticationRequiredRepositoryException;
import net.sourceforge.vulcan.exception.RepositoryException;
import net.sourceforge.vulcan.integration.support.PluginSupport;

import org.apache.commons.io.FileUtils;
import org.netbeans.lib.cvsclient.CVSRoot;
import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.StandardAdminHandler;
import org.netbeans.lib.cvsclient.command.Command;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.GlobalOptions;
import org.netbeans.lib.cvsclient.command.checkout.CheckoutCommand;
import org.netbeans.lib.cvsclient.commandLine.BasicListener;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.Connection;
import org.netbeans.lib.cvsclient.connection.ConnectionFactory;
import org.netbeans.lib.cvsclient.event.FileAddedEvent;

public abstract class CvsSupport extends PluginSupport {
	protected final GlobalOptions options = new GlobalOptions();
	protected final CvsConfigDto globalConfig;
	protected final CvsRepositoryProfileDto profile;
	protected final CvsProjectConfigDto config;
	protected final CVSRoot cvsRoot;
	protected final Connection connection;
	
	protected String tag = "HEAD";
	
	protected CvsSupport(CvsConfigDto globalConfig, CvsRepositoryProfileDto profile, CvsProjectConfigDto config) {
		this(globalConfig, profile, config, createCvsRoot(profile));
	}

	protected CvsSupport(CvsConfigDto globalConfig, CvsRepositoryProfileDto profile, CvsProjectConfigDto config, CVSRoot cvsRoot) {
		this.globalConfig = globalConfig;
		this.profile = profile;
		this.config = config;
		this.cvsRoot = cvsRoot;

		this.tag = config.getBranch();
		
		if (isBlank(this.tag)) {
			this.tag = "HEAD";
		}
		
		this.options.setCVSRoot(cvsRoot.toString());
		this.options.setModeratelyQuiet(true);
		this.options.setVeryQuiet(true);

		this.connection = ConnectionFactory.getConnection(cvsRoot);
	}
	
	protected void openConnection() throws RepositoryException {
		if (connection.isOpen()) {
			return;
		}
		
		try {
			connection.open();
		} catch (CommandAbortedException e) {
			throw new RepositoryException(e);
		} catch (AuthenticationException e) {
			throw new AuthenticationRequiredRepositoryException(cvsRoot.getUserName());			
		}
	}
	/**
	 * This method begins a non-recursive checkout of a given module
	 * to a temp directory in order to discover the name of a versioned
	 * file in the module.
	 * <br>
	 * CVS does not seem to have a concept of listing files remotely
	 * without having or creating a working copy locally.  This method
	 * starts to create a working copy, but aborts after the first file
	 * is discovered.
	 * <br>
	 * This method creates its own connection because the abort will
	 * leave the connection in an unusable state, which would cause
	 * subsequent operations to fail. 
	 */
	protected String findFile(String module) throws RepositoryException {
		final Connection tmpConn = ConnectionFactory.getConnection(cvsRoot);
		
		final Client client = new Client(tmpConn, new StandardAdminHandler());
		final CheckoutCommand cmd = new CheckoutCommand();
		
		final File tmpDir = createTmpDir();
		final String[] paths = new String[1];
		
		try {
			client.setLocalPath(tmpDir.getParent());
			
			client.getEventManager().addCVSListener(new BasicListener() {
				@Override
				public void fileAdded(FileAddedEvent e) {
					paths[0] = e.getFilePath();
					
					// we have a file, abort the superfluous operation.
					client.abort();
				}
			});
			
			cmd.setModule(config.getModule());
			cmd.setCheckoutDirectory(tmpDir.getName());
			cmd.setRecursive(false);
			
			client.executeCommand(cmd, options);
		} catch (CommandAbortedException e) {
			// ignore.
		} catch (CommandException e) {
			throw new RepositoryException(e);
		} catch (AuthenticationException e) {
			throw new RepositoryException(e);
		} finally {
			try {
				FileUtils.deleteDirectory(tmpDir);
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
		
		if (paths[0] == null) {
			/* No file was found.  return the module itself.
			 * This will result in a much slower rlog operation
			 * because it will fetch information on everything
			 * instead of the single file.
			 */
			return module;
		}
		
		return module + "/" + new File(paths[0]).getName();
	}

	protected void executeCvsCommand(Client client, Command cmd) throws RepositoryException {
		try {
			client.executeCommand(cmd, options);
		} catch (CommandAbortedException e) {
			throw new RepositoryException(e);
		} catch (CommandException e) {
			throw new RepositoryException(e);
		} catch (AuthenticationException e) {
			throw new RepositoryException(e);
		}
	}
	
	protected static CVSRoot createCvsRoot(CvsRepositoryProfileDto profile) {
		final Properties props = new Properties();
		
		props.setProperty("method", profile.getProtocol());
		props.setProperty("hostname", profile.getHost());
		props.setProperty("port", "2401");	// use default
		props.setProperty("username", profile.getUsername());
		props.setProperty("password", profile.getPassword());
		props.setProperty("repository", profile.getRepositoryPath());
		
		final CVSRoot cvsRoot = CVSRoot.parse(props);
		return cvsRoot;
	}

	protected File createTmpDir() throws RepositoryException {
		final File tmpDir;
		try {
			tmpDir = File.createTempFile("vulcan-cvs-", null);
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
		
		tmpDir.delete();
		
		if (!tmpDir.mkdir()) {
			throw new RepositoryException("cvs.errors.cannot.create.tmp.dir",
					new Object[] {tmpDir.getPath()}, null);
		}
		
		return tmpDir;
	}
}

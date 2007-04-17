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

import static net.sourceforge.vulcan.cvs.support.CvsDateFormat.format;
import static net.sourceforge.vulcan.cvs.support.CvsDateFormat.parseDate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.cvs.dto.CvsAggregateRevisionTokenDto;
import net.sourceforge.vulcan.cvs.dto.CvsConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsProjectConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsRepositoryProfileDto;
import net.sourceforge.vulcan.cvs.support.ChangeLogListener;
import net.sourceforge.vulcan.cvs.support.CheckoutListener;
import net.sourceforge.vulcan.cvs.support.Digester;
import net.sourceforge.vulcan.cvs.support.JavaSecurityDigester;
import net.sourceforge.vulcan.cvs.support.NewestRevisionsLogListener;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.netbeans.lib.cvsclient.CVSRoot;
import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.StandardAdminHandler;
import org.netbeans.lib.cvsclient.command.Command;
import org.netbeans.lib.cvsclient.command.CommandAbortedException;
import org.netbeans.lib.cvsclient.command.CommandException;
import org.netbeans.lib.cvsclient.command.GlobalOptions;
import org.netbeans.lib.cvsclient.command.checkout.CheckoutCommand;
import org.netbeans.lib.cvsclient.command.log.RlogCommand;
import org.netbeans.lib.cvsclient.command.update.UpdateCommand;
import org.netbeans.lib.cvsclient.commandLine.BasicListener;
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.Connection;
import org.netbeans.lib.cvsclient.connection.ConnectionFactory;
import org.netbeans.lib.cvsclient.event.FileAddedEvent;

public class CvsRepositoryAdaptor implements RepositoryAdaptor {
	private final Digester digester = new JavaSecurityDigester("MD5");
	private final GlobalOptions options = new GlobalOptions();
	private final CvsConfigDto globalConfig;
	private final CvsRepositoryProfileDto profile;
	private final CvsProjectConfigDto config;
	private final String projectName;
	private final Connection conn;
	
	private String tag = "HEAD";
	private Set<String> symbolicNames;
	
	public CvsRepositoryAdaptor(CvsConfigDto globalConfig, CvsRepositoryProfileDto profile, CvsProjectConfigDto config, String projectName) throws RepositoryException {
		this.globalConfig = globalConfig;
		this.profile = profile;
		this.config = config;
		this.projectName = projectName;
		
		conn = createConnection(profile);
	
		try {
			conn.open();
		} catch (CommandAbortedException e) {
			throw new RepositoryException(e);
		} catch (AuthenticationException e) {
			throw new RepositoryException(e);			
		}
	}

	public CvsAggregateRevisionTokenDto getLatestRevision(RevisionTokenDto previousRevision) throws RepositoryException {
		final CvsAggregateRevisionTokenDto prevRev = (CvsAggregateRevisionTokenDto) previousRevision;
		// first, check to see if there are any changes since the previous revision
		if (previousRevision != null) {
			if (getLatestRevision(config.getModule(), prevRev) == prevRev) {
				// no changes, short circuit.
				return prevRev;
			}
		}
		
		// get the aggregate revision of all files.
		return getLatestRevision(config.getModule(), null);
	}

	public ChangeLogDto getChangeLog(RevisionTokenDto first, RevisionTokenDto last, OutputStream diffOutputStream) throws RepositoryException {
		final CvsAggregateRevisionTokenDto cvsFirstRev = (CvsAggregateRevisionTokenDto) first;
		final CvsAggregateRevisionTokenDto cvsLastRev = (CvsAggregateRevisionTokenDto) last;

		try {
			// "cvs rdiff -u" does not seem to be supported by netbeans-cvslib at this time.
			diffOutputStream.close();
		} catch (IOException e) {
			throw new RepositoryException(e);
		}
		
		final ChangeLogDto changeLog = doChangeLogs(cvsFirstRev, cvsLastRev);
		
		return changeLog;
	}

	/**
	 * The fastest known way to find a list of available tags for a given CVS module
	 * is as follows:
	 *   <ol>
	 *   	<li>Perform a non-recursive checkout of the module</li>
	 *   	<li>Find the first file checked out in the top-level directory</li>
	 *   	<li>Do an "rlog" of the HEAD revision of that file</li>
	 *   	<li>Capture the symbolic names for the rlog</li>
	 *   </ol>
	 */
	public List<RepositoryTagDto> getAvailableTags() throws RepositoryException {
		final List<RepositoryTagDto> names = new ArrayList<RepositoryTagDto>();
		
		if (symbolicNames == null) {
			final String path = findFile(config.getModule());
			getLatestRevision(path, null);
		}
		
		for (String name : symbolicNames) {
			final RepositoryTagDto tag = new RepositoryTagDto();
			tag.setName(name);
			tag.setDescription(name);
			names.add(tag);
		}
		
		Collections.sort(names, new Comparator<RepositoryTagDto>() {
			public int compare(RepositoryTagDto o1, RepositoryTagDto o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});

		final RepositoryTagDto tag = new RepositoryTagDto();
		tag.setName("HEAD");
		tag.setDescription("HEAD");
		names.add(0, tag);

		return names;
	}
	
	public void createWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		final Map<String, Long> counters = globalConfig.getWorkingCopyByteCounts();
		long previousBytesCounted = -1;
		
		synchronized (counters) {
			if (counters.containsKey(projectName)) {
				previousBytesCounted = counters.get(projectName).longValue();
			}
		}
		
		final Client client = new Client(conn, new StandardAdminHandler());
		final CheckoutCommand cmd = new CheckoutCommand();
		final CheckoutListener listener = new CheckoutListener(buildDetailCallback, previousBytesCounted);

		client.setLocalPath(absolutePath.getParent());
		client.getEventManager().addCVSListener(listener);
		
		cmd.setModule(config.getModule());
		cmd.setCheckoutByRevision(tag);
		cmd.setCheckoutDirectory(absolutePath.getName());

		executeCvsCommand(client, cmd);
		
		synchronized (counters) {
			counters.put(projectName, listener.getBytesCounted());
		}
	}

	public void updateWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		final Client client = new Client(conn, new StandardAdminHandler());
		final UpdateCommand cmd = new UpdateCommand();

		client.setLocalPath(absolutePath.getPath());
		
		executeCvsCommand(client, cmd);
	}
	
	public String getRepositoryUrl() {
		return null;
	}
	
	public String getTagName() {
		return tag;
	}

	public void setTagName(String tagName) {
		this.tag = tagName;
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
	private String findFile(String module) throws RepositoryException {
		final Connection tmpConn = createConnection(profile);
		
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
			cmd.setCheckoutByRevision(tag);
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

	private File createTmpDir() throws RepositoryException {
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

	private CvsAggregateRevisionTokenDto getLatestRevision(String path, CvsAggregateRevisionTokenDto previousRevision) throws RepositoryException {
		final List<String> revisions = new ArrayList<String>();
		final NewestRevisionsLogListener logListener = new NewestRevisionsLogListener(revisions);
		final Client client = new Client(conn, new StandardAdminHandler());
		final RlogCommand cmd = new RlogCommand();

		cmd.setModule(path);
		
		if ("HEAD" != tag) {
			cmd.setRevisionFilter(tag);
		}

		if (previousRevision != null) {
			cmd.setDateFilter(">" + previousRevision.getLabel());
		}
		
		client.getEventManager().addCVSListener(logListener);
		
		executeCvsCommand(client, cmd);
		
		symbolicNames = logListener.getSymbolicNames();
		
		Collections.sort(revisions);
		
		final String combined = StringUtils.join(revisions.iterator(), ";");
		
		final String newestModificationString = logListener.getNewestModificationString();
		if (newestModificationString == null) {
			if (previousRevision != null) {
				return previousRevision;
			}
			throw new RepositoryException("cvs.errors.rlog.failed", null, null);
		}
		
		return new CvsAggregateRevisionTokenDto(
				digester.digest(combined.getBytes()),
				newestModificationString);
	}

	private void executeCvsCommand(Client client, Command cmd) throws RepositoryException {
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
	
	private ChangeLogDto doChangeLogs(final CvsAggregateRevisionTokenDto cvsFirstRev, final CvsAggregateRevisionTokenDto cvsLastRev) throws RepositoryException {
		final ChangeLogListener logListener = new ChangeLogListener();
		final Client client = new Client(conn, new StandardAdminHandler());
		final RlogCommand cmd = new RlogCommand();

		cmd.setModule(config.getModule());
		cmd.setDateFilter(cvsFirstRev.toString() + "<" + adjustLastRevision(cvsLastRev));
		cmd.setRevisionFilter(tag);
		
		client.getEventManager().addCVSListener(logListener);
		
		executeCvsCommand(client, cmd);
		
		final List<ChangeSetDto> entries = groupChangeLogs(logListener.getEntries());
		
		final ChangeLogDto changeLog = new ChangeLogDto();
		
		changeLog.setChangeSets(entries);
		
		return changeLog;
	}

	private List<ChangeSetDto> groupChangeLogs(List<ChangeSetDto> entries) {
		final RevisionTokenDto merged = new RevisionTokenDto(0l, "<many>");
		final Map<String, ChangeSetDto> map = new HashMap<String, ChangeSetDto>();
		
		// add entries by key, merging duplicate keys
		for (ChangeSetDto e : entries) {
			final String key = key(e);
			
			if (map.containsKey(key)) {
				final ChangeSetDto m = map.get(key);
				m.setRevision(merged);
				
				final Set<String> paths = new HashSet<String>(Arrays.asList(m.getModifiedPaths()));
				paths.addAll(Arrays.asList(e.getModifiedPaths()));
				
				m.setModifiedPaths(paths.toArray(new String[paths.size()]));
				
				if (e.getTimestamp().after(m.getTimestamp())) {
					m.setTimestamp(e.getTimestamp());
				}
			} else {
				map.put(key, e);
			}
		}
		
		// put back into a list
		entries.clear();
		entries.addAll(map.values());
		
		return entries;
	}

	private String key(ChangeSetDto e) {
		return e.getAuthor() + ":" + e.getMessage();
	}
	
	/* When fetching changelog, CVS sometimes returns logs on the same end date,
		and other times not.  Add one second to the date as a hack. */
	private String adjustLastRevision(final CvsAggregateRevisionTokenDto cvsLastRev) {
		Date d = parseDate(cvsLastRev.getLabel());
		
		return format(new Date(d.getTime() + 1000));
	}

	private Connection createConnection(CvsRepositoryProfileDto profile) {
		final Properties props = new Properties();
		
		props.setProperty("method", profile.getProtocol());
		props.setProperty("hostname", profile.getHost());
		props.setProperty("port", "2401");	// use default
		props.setProperty("username", profile.getUsername());
		props.setProperty("password", profile.getPassword());
		props.setProperty("repository", profile.getRepositoryPath());
		
		final CVSRoot cvsRoot = CVSRoot.parse(props);
		
		options.setCVSRoot(cvsRoot.toString());
		options.setVeryQuiet(true);

		return ConnectionFactory.getConnection(cvsRoot);
	}
}

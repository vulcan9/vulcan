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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
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
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;

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
import org.netbeans.lib.cvsclient.connection.AuthenticationException;
import org.netbeans.lib.cvsclient.connection.Connection;
import org.netbeans.lib.cvsclient.connection.ConnectionFactory;

public class CvsRepositoryAdaptor implements RepositoryAdaptor {
	private final Digester digester = new JavaSecurityDigester("MD5");
	private final GlobalOptions options = new GlobalOptions();
	private final CvsConfigDto globalConfig;
	private final CvsProjectConfigDto config;
	private final String projectName;
	private final CVSRoot cvsRoot;
	private final Connection conn;
	
	private String tag = "HEAD";
	private Set<String> symbolicNames;
	
	public CvsRepositoryAdaptor(CvsConfigDto globalConfig, CvsRepositoryProfileDto profile, CvsProjectConfigDto config, String projectName) throws RepositoryException {
		this.globalConfig = globalConfig;
		this.config = config;
		this.projectName = projectName;
		
		final Properties props = new Properties();
		
		props.setProperty("method", profile.getProtocol());
		props.setProperty("hostname", profile.getHost());
		props.setProperty("port", "2401");	// use default
		props.setProperty("username", profile.getUsername());
		props.setProperty("password", profile.getPassword());
		props.setProperty("repository", profile.getRepositoryPath());
		
		cvsRoot = CVSRoot.parse(props);
		options.setCVSRoot(cvsRoot.toString());
		
		conn = ConnectionFactory.getConnection(cvsRoot);
	
		try {
			conn.open();
		} catch (CommandAbortedException e) {
			throw new RepositoryException(e);
		} catch (AuthenticationException e) {
			throw new RepositoryException(e);			
		}
	}

	public CvsAggregateRevisionTokenDto getLatestRevision() throws RepositoryException {
		final List<String> revisions = new ArrayList<String>();
		final NewestRevisionsLogListener logListener = new NewestRevisionsLogListener(revisions);
		final Client client = new Client(conn, new StandardAdminHandler());
		final RlogCommand cmd = new RlogCommand();

		cmd.setModule(config.getModule());
		
		if ("HEAD" != tag) {
			cmd.setRevisionFilter(tag);
		}

		client.getEventManager().addCVSListener(logListener);
		
		executeCvsCommand(client, cmd);
		
		symbolicNames = logListener.getSymbolicNames();
		
		Collections.sort(revisions);
		
		final String combined = StringUtils.join(revisions.iterator(), ";");
		
		final String newestModificationString = logListener.getNewestModificationString();
		if (newestModificationString == null) {
			throw new RepositoryException("cvs.errors.rlog.failed", null, null);
		}
		
		return new CvsAggregateRevisionTokenDto(
				digester.digest(combined.getBytes()),
				newestModificationString);
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

	public List<RepositoryTagDto> getAvailableTags() throws RepositoryException {
		final List<RepositoryTagDto> names = new ArrayList<RepositoryTagDto>();
		
		if (symbolicNames == null) {
			getLatestRevision();
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
	
	public void download(File target) throws RepositoryException, IOException {
		throw new UnsupportedOperationException("not implemented");
	}

	public ProjectConfigDto getProjectConfig() {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public void setNonRecursive() {
		throw new UnsupportedOperationException("not implemented");
	}
	
	public void updateGlobalConfig(PluginConfigDto globalRaConfig) {
		throw new UnsupportedOperationException("not implemented");
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

		cmd.setModule(config.getModule());
		cmd.setCheckoutByRevision(tag);
		
		client.setLocalPath(absolutePath.getPath());
		client.getEventManager().addCVSListener(listener);
		
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
		
		// add entries by key, mergine duplicate keys
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
	
}

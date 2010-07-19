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
import java.util.Set;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.core.BuildDetailCallback;
import net.sourceforge.vulcan.cvs.dto.CvsConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsProjectConfigDto;
import net.sourceforge.vulcan.cvs.dto.CvsRepositoryProfileDto;
import net.sourceforge.vulcan.cvs.support.ChangeLogListener;
import net.sourceforge.vulcan.cvs.support.CheckoutListener;
import net.sourceforge.vulcan.cvs.support.NewestRevisionsLogListener;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.RepositoryTagDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.exception.RepositoryException;

import org.netbeans.lib.cvsclient.Client;
import org.netbeans.lib.cvsclient.admin.StandardAdminHandler;
import org.netbeans.lib.cvsclient.command.checkout.CheckoutCommand;
import org.netbeans.lib.cvsclient.command.log.RlogCommand;
import org.netbeans.lib.cvsclient.command.update.UpdateCommand;

public class CvsRepositoryAdaptor extends CvsSupport implements RepositoryAdaptor {
	private final String projectName;
	
	private Set<String> symbolicNames;
	
	public CvsRepositoryAdaptor(CvsConfigDto globalConfig, CvsRepositoryProfileDto profile, CvsProjectConfigDto config, String projectName) throws RepositoryException {
		this(globalConfig, profile, config, projectName, true);
	}

	protected CvsRepositoryAdaptor(CvsConfigDto globalConfig, CvsRepositoryProfileDto profile, CvsProjectConfigDto config, String projectName, boolean connect) throws RepositoryException {
		super(globalConfig, profile, config);
		this.projectName = projectName;

		if (connect) {
			openConnection();
		}
	}
	
	public RevisionTokenDto getLatestRevision(RevisionTokenDto previousRevision) throws RepositoryException {
		// first, check to see if there are any changes since the previous revision
		if (previousRevision != null) {
			if (getLatestRevision(config.getModule(), previousRevision, tag) == previousRevision) {
				// no changes, short circuit.
				return previousRevision;
			}
		}
		
		// get the aggregate revision of all files.
		return getLatestRevision(config.getModule(), null, tag);
	}

	public ChangeLogDto getChangeLog(RevisionTokenDto first, RevisionTokenDto last, OutputStream diffOutputStream) throws RepositoryException {
		if (diffOutputStream != null) {
			try {
				// "cvs rdiff -u" does not seem to be supported by netbeans-cvslib at this time.
				diffOutputStream.close();
			} catch (IOException e) {
				throw new RepositoryException(e);
			}
		}
		
		final ChangeLogDto changeLog = doChangeLogs(first, last);
		
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
			getLatestRevision(path, null, null);
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
		
		final Client client = new Client(connection, new StandardAdminHandler());
		
		final CheckoutCommand cmd = new CheckoutCommand();
		final CheckoutListener listener = new CheckoutListener(buildDetailCallback, previousBytesCounted);

		client.setLocalPath(absolutePath.getParent());
		client.getEventManager().addCVSListener(listener);
		
		cmd.setModule(config.getModule());
		cmd.setCheckoutByRevision(tag);
		cmd.setCheckoutDirectory(absolutePath.getName());
		cmd.setRecursive(config.isRecursive());
		
		executeCvsCommand(client, cmd);
		
		synchronized (counters) {
			counters.put(projectName, listener.getBytesCounted());
		}
	}

	public void updateWorkingCopy(File absolutePath, BuildDetailCallback buildDetailCallback) throws RepositoryException {
		final Client client = new Client(connection, new StandardAdminHandler());
		final UpdateCommand cmd = new UpdateCommand();

		client.setLocalPath(absolutePath.getPath());
		cmd.setRecursive(config.isRecursive());
		
		executeCvsCommand(client, cmd);
	}
	
	public boolean isWorkingCopy(File absolutePath) {
		final Client client = new Client(connection, new StandardAdminHandler());
		try {
			if (client.getRepositoryForDirectory(absolutePath) != null) {
				return true;
			}
		} catch (IOException e) {
		}
		return false;
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

	private RevisionTokenDto getLatestRevision(String path, RevisionTokenDto previousRevision, String tag) throws RepositoryException {
		final List<String> revisions = new ArrayList<String>();
		final NewestRevisionsLogListener logListener = new NewestRevisionsLogListener(revisions);
		final Client client = new Client(connection, new StandardAdminHandler());
		final RlogCommand cmd = new RlogCommand();

		cmd.setModule(path);
		cmd.setRecursive(config.isRecursive());
		
		if (tag != null && !"HEAD".equals(tag)) {
			cmd.setRevisionFilter(tag);
		}

		if (previousRevision != null) {
			cmd.setDateFilter(">" + previousRevision.getLabel());
		}
		
		client.getEventManager().addCVSListener(logListener);
		
		executeCvsCommand(client, cmd);
		
		symbolicNames = logListener.getSymbolicNames();
		
		Collections.sort(revisions);
		
		final String newestModificationString = logListener.getNewestModificationString();
		if (newestModificationString == null) {
			if (previousRevision != null) {
				return previousRevision;
			}
			throw new RepositoryException("cvs.errors.rlog.failed", null, null);
		}
		
		return new RevisionTokenDto(
				toLong(newestModificationString),
				newestModificationString);
	}

	private static Long toLong(String newestModificationDateString) {
		return Long.valueOf(newestModificationDateString.replaceAll("\\D", ""));
	}
	
	private ChangeLogDto doChangeLogs(final RevisionTokenDto first, final RevisionTokenDto last) throws RepositoryException {
		final ChangeLogListener logListener = new ChangeLogListener();
		final Client client = new Client(connection, new StandardAdminHandler());
		final RlogCommand cmd = new RlogCommand();

		cmd.setModule(config.getModule());
		cmd.setDateFilter(first.toString() + "<" + adjustLastRevision(last));
		cmd.setRevisionFilter(tag);
		cmd.setRecursive(config.isRecursive());
		
		client.getEventManager().addCVSListener(logListener);
		
		executeCvsCommand(client, cmd);
		
		final List<ChangeSetDto> entries = groupChangeLogs(logListener.getEntries());
		
		final ChangeLogDto changeLog = new ChangeLogDto();
		
		changeLog.setChangeSets(entries);
		
		return changeLog;
	}

	private List<ChangeSetDto> groupChangeLogs(List<ChangeSetDto> entries) {
		final String revisionLabel = "<many>";
		final Map<String, ChangeSetDto> map = new HashMap<String, ChangeSetDto>();
		
		// add entries by key, merging duplicate keys
		for (ChangeSetDto e : entries) {
			final String key = key(e);
			
			if (map.containsKey(key)) {
				final ChangeSetDto m = map.get(key);
				m.setRevisionLabel(revisionLabel);
				
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
	
	/* When fetching change log, CVS sometimes returns logs on the same end date,
		and other times not.  Add one second to the date as a hack. */
	private String adjustLastRevision(final RevisionTokenDto lastRev) {
		Date d = parseDate(lastRev.getLabel());
		
		return format(new Date(d.getTime() + 1000));
	}
}

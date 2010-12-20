/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2010 Chris Eldredge
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
package net.sourceforge.vulcan.mercurial;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.RepositoryAdaptorConfigDto;

public class MercurialProjectConfig extends RepositoryAdaptorConfigDto {
	private String branch = "";
	private String remoteRepositoryUrl = "";
	private String sshCommand = "";
	private String remoteCommand = "";
	private boolean cloneWithPullProtocol;
	private boolean uncompressed;

	//TODO: look into subrepos
	
	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final ArrayList<PropertyDescriptor> list = new ArrayList<PropertyDescriptor>();
		
		addProperty(list, "remoteRepositoryUrl", "hg.remote.url.label", "hg.remote.url.text", locale);
		addProperty(list, "branch", "hg.branch.label", "hg.branch.text", locale);
		addProperty(list, "sshCommand", "hg.sshCommand.label", "hg.sshCommand.text", locale);
		addProperty(list, "remoteCommand", "hg.remoteCommand.label", "hg.remoteCommand.text", locale);
		addProperty(list, "cloneWithPullProtocol", "hg.cloneWithPullProtocol.label", "hg.cloneWithPullProtocol.text", locale);
		addProperty(list, "uncompressed", "hg.uncompressed.label", "hg.uncompressed.text", locale);
		
		return list;
	}
	
	@Override
	public String getPluginId() {
		return MercurialPlugin.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return MercurialPlugin.PLUGIN_NAME;
	}

	@Override
	public String getHelpTopic() {
		return "MercurialProjectConfig";
	}
	
	public String getBranch() {
		return branch;
	}
	
	public void setBranch(String branch) {
		this.branch = branch;
	}
	
	public String getRemoteRepositoryUrl() {
		return remoteRepositoryUrl;
	}
	
	public void setRemoteRepositoryUrl(String remoteRepositoryUrl) {
		this.remoteRepositoryUrl = remoteRepositoryUrl;
	}

	public String getSshCommand() {
		return sshCommand;
	}

	public void setSshCommand(String sshCommand) {
		this.sshCommand = sshCommand;
	}

	public String getRemoteCommand() {
		return remoteCommand;
	}

	public void setRemoteCommand(String remoteCommand) {
		this.remoteCommand = remoteCommand;
	}

	public boolean isCloneWithPullProtocol() {
		return cloneWithPullProtocol;
	}

	public void setCloneWithPullProtocol(boolean cloneWithPullProtocol) {
		this.cloneWithPullProtocol = cloneWithPullProtocol;
	}

	public boolean isUncompressed() {
		return uncompressed;
	}

	public void setUncompressed(boolean uncompressed) {
		this.uncompressed = uncompressed;
	}
}

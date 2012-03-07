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
package net.sourceforge.vulcan.web.struts.actions;

import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.core.ConfigurationStore;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class ViewConfigAction extends Action {
	private ConfigurationStore configurationStore;
	private String filename;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (request.getParameter("download") != null) {
			response.setHeader("Content-Disposition", "attachment; filename=" + filename);
		}
		
		response.setContentType(configurationStore.getExportMimeType());
		
		final OutputStream os = response.getOutputStream();
		
		try {
			configurationStore.exportConfiguration(os);
		} finally {
			os.close();
		}
		
		return null;
	}
	
	public void setConfigurationStore(ConfigurationStore store) {
		this.configurationStore = store;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
}

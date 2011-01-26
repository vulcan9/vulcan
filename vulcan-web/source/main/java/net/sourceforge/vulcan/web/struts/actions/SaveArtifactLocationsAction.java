/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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

import static net.sourceforge.vulcan.web.struts.actions.BaseDispatchAction.saveSuccessMessage;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.dto.BuildArtifactLocationDto;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public final class SaveArtifactLocationsAction extends Action {
	private StateManager stateManager;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response) throws Exception {

		final List<BuildArtifactLocationDto> list = new ArrayList<BuildArtifactLocationDto>();
		
		final String[] names = request.getParameterValues("name");
		final String[] descs = request.getParameterValues("desc");
		final String[] paths = request.getParameterValues("path");
		
		int numParams = 0;
		
		if (names != null) {
			numParams++;
		}
		
		if (descs != null) {
			numParams++;
		}
		
		if (paths != null) {
			numParams++;
		}
		
		final int length;
		if (numParams > 0 && numParams < 3) {
			throw new IllegalStateException("Missing parameters");
		} else if (numParams > 0 && (names.length != descs.length || names.length != paths.length)) {
			throw new IllegalStateException("Unbalanced parameters");
		} else {
			length = numParams > 0 ? names.length : 0;
		}
		
		for (int i=0; i<length; i++) {
			list.add(new BuildArtifactLocationDto(names[i], descs[i], paths[i], true));
		}
		
		stateManager.updateArtifactLocations(list);
		
		saveSuccessMessage(request);
		
		return mapping.findForward("setup");
	}

	public void setStateManager(StateManager stateManager) {
		this.stateManager = stateManager;
	}
}

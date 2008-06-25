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
package net.sourceforge.vulcan.web.struts.actions;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.Keys;
import net.sourceforge.vulcan.web.PreferencesStore;
import net.sourceforge.vulcan.web.struts.forms.PreferencesForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

@SvnRevision(id="$Id$", url="$HeadURL$")
public final class ManagePreferencesAction extends BaseDispatchAction {
	private PreferencesStore preferencesStore;
	
	public ActionForward save(ActionMapping mapping, ActionForm actionForm,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		
		final PreferencesForm form = (PreferencesForm) actionForm;
		
		final String toggleLabel = form.getToggleLabel();
		if (isNotBlank(toggleLabel)) {
			final ArrayList<String> labels = new ArrayList<String>();
			
			final String[] existingLabels = form.getConfig().getLabels();
			
			if (existingLabels != null) {
				labels.addAll(Arrays.asList(existingLabels));
			}
			
			if (!labels.remove(toggleLabel)) {
				labels.add(toggleLabel);
			}
			
			Collections.sort(labels);
			form.getConfig().setLabels(labels.toArray(new String[labels.size()]));
		}
		
		request.removeAttribute(Keys.PREFERENCES);
		request.getSession().setAttribute(Keys.PREFERENCES, form.getConfig());
		
		final Cookie cookie = new Cookie(Keys.PREFERENCES, preferencesStore.convertToString(form.getConfig()));
		cookie.setPath(request.getContextPath());
		cookie.setMaxAge(60 * 60 * 24 * 365);
		
		response.addCookie(cookie);
		return mapping.findForward("dashboard");
	}
	
	public void setPreferencesStore(PreferencesStore preferencesStore) {
		this.preferencesStore = preferencesStore;
	}
}

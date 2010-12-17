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
package net.sourceforge.vulcan.web.struts.forms;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.vulcan.StateManager;
import net.sourceforge.vulcan.dto.NameDto;
import net.sourceforge.vulcan.dto.NamedObject;
import net.sourceforge.vulcan.web.Keys;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.validator.ValidatorForm;


public abstract class ConfigForm extends ValidatorForm implements DispatchForm {
	private final NameDto blank;
	private NamedObject original;
	private NamedObject config;
	
	private boolean createNew;
	private String action;
	
	private boolean allowCronExpression;
	
	public ConfigForm(NameDto blank) {
		this.blank = blank;
	}
	public String getName() {
		return config.getName();
	}
	@Override
	public final void reset(ActionMapping mapping, HttpServletRequest request) {
		super.reset(mapping, request);
		reset0(mapping, request);
	}
	@Override
	public final ActionErrors validate(ActionMapping mapping,
			HttpServletRequest request) {
		final ActionErrors errors;
		
		if (isCommitAction() && shouldValidate()) {
			errors = super.validate(mapping, request);
			customValidate(mapping, request, errors);
			if (errors.isEmpty()) {
				doAfterValidate();
			}
		} else {
			errors = null;
		}

		return errors;
	}
	protected void doAfterValidate() {
	}
	protected void customValidate(ActionMapping mapping, HttpServletRequest request, ActionErrors errors) {
	}
	public final String getAction() {
		return action;
	}
	public final void setAction(String action) {
		final String mungedAction = translateAction(action);
		
		this.action = mungedAction;
		if (isCreateAction()) {
			createNew = true;
			original = null;
		}
	}
	public static final String translateAction(String action) {
		if (StringUtils.isBlank(action)) {
			return "";
		}
		return (action.substring(0,1).toLowerCase()
				+ action.substring(1)).split(" ")[0];
	}
	public final boolean isCreateNew() {
		return createNew;
	}
	public final boolean isCreateAction() {
		return "create".equals(action);
	}
	public final boolean isUpdateAction() {
		return "update".equals(action);
	}
	public final boolean isUploadAction() {
		return "upload".equals(action);
	}
	public final boolean isDeleteAction() {
		return "delete".equals(action);
	}
	public final boolean isCommitAction() {
		return isCreateAction() || isUpdateAction() || isUploadAction() || isDeleteAction();
	}
	public final void setCreateNew(boolean createNew) {
		this.createNew = createNew;
	}
	public boolean isAllowCronExpression() {
		return allowCronExpression;
	}
	public final NamedObject getConfig() {
		return config;
	}
	public final String getOriginalName() {
		if (original == null) {
			return "";
		}
		return original.getName();
	}
	public boolean isDirty() {
		if (original != null) {
			return (!original.equals(config));
		}
		return true;
	}
	public final void populate(NameDto config, boolean allowCronExpression) {
		this.config = (NamedObject) config.copy();
		this.original = config;
		this.allowCronExpression = allowCronExpression;
		doAfterPopulate();
	}
	public static StateManager getStateManager(ActionServlet servlet) {
		final StateManager mgr = (StateManager)servlet.getServletContext().getAttribute(Keys.STATE_MANAGER);
		if (mgr == null) {
			throw new IllegalStateException();
		}
		return mgr;
	}
	protected final void reset0(ActionMapping mapping, HttpServletRequest request) {
		action = null;
		createNew = false;
		
		resetInternal(mapping, request);
		
		final NamedObject oldConfig = config;
		config = (NamedObject) blank.copy();
		
		initializeConfig(config, oldConfig);
	}
	protected void initializeConfig(NamedObject config, NamedObject previousConfig) {
	}
	protected void resetInternal(ActionMapping mapping, HttpServletRequest request) {
	}
	protected boolean shouldValidate() {
		return true;
	}
	protected void doAfterPopulate() {
	}
	protected final NamedObject getOriginal() {
		return original;
	}
}

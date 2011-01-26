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
package net.sourceforge.vulcan.jabber;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.exception.ValidationException;

public class JabberTemplatesConfig extends PluginConfigDto {
	private String notifyCommitterTemplate =
		"You may have broken the build!  See {Link} for more info.";
	private String notifyBuildMasterTemplate =
		"One of these users broke the build: {Users}.";
	private String pithyRetortTemplate = "";
	private String brokenBuildAcknowledgementTemplate = "";
	private String brokenBuildClaimedByTemplate = "";
	private String claimKeywords = "mine";
	
	@Override
	public String getPluginId() {
		return JabberPlugin.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return JabberPlugin.PLUGIN_NAME;
	}

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		final List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "notifyCommitterTemplate", "JabberTemplatesConfig.notifyCommitterTemplate.name", "JabberTemplatesConfig.notifyCommitterTemplate.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.TEXTAREA));
		addProperty(pds, "notifyBuildMasterTemplate", "JabberTemplatesConfig.notifyBuildMasterTemplate.name", "JabberTemplatesConfig.notifyBuildMasterTemplate.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.TEXTAREA));
		addProperty(pds, "claimKeywords", "JabberTemplatesConfig.claimKeywords.name", "JabberTemplatesConfig.claimKeywords.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.TEXTAREA));
		addProperty(pds, "brokenBuildAcknowledgementTemplate", "JabberTemplatesConfig.brokenBuildAcknowledgementTemplate.name", "JabberTemplatesConfig.brokenBuildAcknowledgementTemplate.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.TEXTAREA));
		addProperty(pds, "brokenBuildClaimedByTemplate", "JabberTemplatesConfig.brokenBuildClaimedByTemplate.name", "JabberTemplatesConfig.brokenBuildClaimedByTemplate.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.TEXTAREA));
		addProperty(pds, "pithyRetortTemplate", "JabberTemplatesConfig.pithyRetortTemplate.name", "JabberTemplatesConfig.pithyRetortTemplate.description", locale,
				Collections.singletonMap(ATTR_WIDGET_TYPE, Widget.TEXTAREA));
		
		return pds;
	}

	@Override
	public JabberTemplatesConfig copy() {
		final JabberTemplatesConfig copy = (JabberTemplatesConfig) super.copy();
		return copy;
	}

	@Override
	public String getHelpTopic() {
		return "JabberTemplatesConfig";
	}
	
	@Override
	public void validate() throws ValidationException {
		super.validate();
		
		final ProjectStatusDto sampleStatus = new ProjectStatusDto();
		sampleStatus.setName("example project");
		sampleStatus.setBuildNumber(1001);
		final BuildMessageDto sampleMessage = new BuildMessageDto();
		sampleMessage.setMessage("expected identifier");
		sampleMessage.setFile("SampleFile");
		sampleMessage.setLineNumber(123);
		sampleMessage.setCode("CODE12");

		validateTemplate("notifyCommitterTemplate", getNotifyCommitterTemplate(), sampleStatus, sampleMessage);
		validateTemplate("notifyBuildMasterTemplate", getNotifyBuildMasterTemplate(), sampleStatus, sampleMessage);
		validateTemplate("brokenBuildAcknowledgementTemplate", getNotifyBuildMasterTemplate(), sampleStatus, sampleMessage);
		validateTemplate("brokenBuildClaimedByTemplate", getNotifyBuildMasterTemplate(), sampleStatus, sampleMessage);
		validateTemplate("pithyRetortTemplate", getPithyRetortTemplate(), sampleStatus, sampleMessage);
	}

	private void validateTemplate(String propertyName, String template, ProjectStatusDto sampleStatus, BuildMessageDto sampleMessage) throws ValidationException {
		try {
			TemplateFormatter.substituteParameters(template, "http://example.com", "user1", sampleMessage, sampleStatus);	
		} catch (IllegalArgumentException e) {
			final String invalidParamName = "can't parse argument number ";
			if (e.getMessage().startsWith(invalidParamName)) {
				throw new ValidationException(propertyName, "jabber.validation.template.param.name", e.getMessage().substring(invalidParamName.length()));	
			}
			throw new ValidationException(propertyName, "jabber.validation.template.format", e.getMessage());
			
		}
	}

	public String getNotifyCommitterTemplate() {
		return notifyCommitterTemplate;
	}

	public void setNotifyCommitterTemplate(String notifyCommitterTemplate) {
		this.notifyCommitterTemplate = notifyCommitterTemplate;
	}

	public String getNotifyBuildMasterTemplate() {
		return notifyBuildMasterTemplate;
	}

	public void setNotifyBuildMasterTemplate(String notifyBuildMasterTemplate) {
		this.notifyBuildMasterTemplate = notifyBuildMasterTemplate;
	}

	public String getPithyRetortTemplate() {
		return pithyRetortTemplate;
	}

	public void setPithyRetortTemplate(String pithyRetortTemplate) {
		this.pithyRetortTemplate = pithyRetortTemplate;
	}

	public String getBrokenBuildAcknowledgementTemplate() {
		return brokenBuildAcknowledgementTemplate;
	}
	
	public void setBrokenBuildAcknowledgementTemplate(String brokenBuildAcknowledgementTemplate) {
		this.brokenBuildAcknowledgementTemplate = brokenBuildAcknowledgementTemplate;
	}

	public String getBrokenBuildClaimedByTemplate() {
		return brokenBuildClaimedByTemplate;
	}
	
	public void setBrokenBuildClaimedByTemplate(String brokenBuildClaimedByTemplate) {
		this.brokenBuildClaimedByTemplate = brokenBuildClaimedByTemplate;
	}

	public String getClaimKeywords() {
		return claimKeywords;
	}
	
	public void setClaimKeywords(String claimKeywords) {
		this.claimKeywords = claimKeywords;
	}
}

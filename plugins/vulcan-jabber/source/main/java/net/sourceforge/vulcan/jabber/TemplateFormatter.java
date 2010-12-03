/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.text.MessageFormat;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.dto.BuildMessageDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;

public class TemplateFormatter {
	static BuildMessageDto blankMessage = new BuildMessageDto();
	static ProjectStatusDto blankStatus = new ProjectStatusDto();

	static String substituteParameters(String template, String url, String users, BuildMessageDto message, ProjectStatusDto status) {
		if (status == null) {
			status = blankStatus;
		}

		return substituteParameters(template, url, users, null, message, status.getName(), status.getBuildNumber());
	}
	
	static String substituteParameters(String template, String url, String users, String claimUser, BuildMessageDto message, String projectName, Integer buildNumber) {
		if (message == null) {
			message = blankMessage;
		}
		
		final Object[] args = {
				isNotBlank(message.getMessage()) ? 1 : 0, message.getMessage(), 
				isNotBlank(message.getFile()) ? 1 : 0, message.getFile(), 
				message.getLineNumber() != null ? 1 : 0, message.getLineNumber() != null ? message.getLineNumber() : -1,
				isNotBlank(message.getCode()) ? 1 : 0, message.getCode(), 
				isNotBlank(users) ? 1 : 0, users,
				url, 
				projectName, 
				buildNumber,
				isNotBlank(claimUser) ? 1 : 0, claimUser};
	
		template = template.replaceAll("'", "''");
		template = template.replaceAll("\\{(\\w+)\\?,(?!choice,)", "{$1NotBlank,choice,0#|1#");
		template = template.replaceAll("\\{(\\w+)\\?,choice,", "{$1NotBlank,choice,");
		
		final String[] paramNames = {"Message", "File", "LineNumber", "Code", "Users",
				"Link", "ProjectName", "BuildNumber"};
		
		for (String s : paramNames) {
			template = Pattern.compile("\\{" + s, Pattern.CASE_INSENSITIVE).matcher(template).replaceAll("{" + s);
		}
		
		template = template.
			replace("{MessageNotBlank", "{0").replace("{Message", "{1").
			replace("{FileNotBlank", "{2").replace("{File", "{3").
			replace("{LineNumberNotBlank", "{4").replace("{LineNumber", "{5").
			replace("{CodeNotBlank", "{6").replace("{Code", "{7").
			replace("{UsersNotBlank", "{8").replace("{Users", "{9").
			replace("{Link", "{10").
			replace("{ProjectName", "{11").
			replace("{BuildNumber", "{12").
			replace("{ClaimUserNotBlank", "{13").
			replace("{ClaimUser", "{14");
		
		MessageFormat fmt = new MessageFormat(template);
		
		return fmt.format(args);
	}
}
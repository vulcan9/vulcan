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
package net.sourceforge.vulcan.dotnet.dto;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.sourceforge.vulcan.dotnet.DotNetBuildPlugin;
import net.sourceforge.vulcan.dto.PluginConfigDto;

public class MSBuildConsoleLoggerParametersDto extends PluginConfigDto {
	public static enum Verbosity {
		Quiet, Minimal, Normal, Detailed, Diagnostic
	}
	
	public static enum ConsoleLoggerParameter {
		PerformanceSummary, Summary, NoSummary, ErrorsOnly,
		WarningsOnly, NoItemAndPropertyList, ShowCommandLine,
		ShowTimestamp, ShowEventId, ForceNoAlign, DisableMPLogging,
		EnableMPLogging
	}
	
	private Verbosity verbosity = Verbosity.Normal;
	private ConsoleLoggerParameter[] parameters = {};

	@Override
	public List<PropertyDescriptor> getPropertyDescriptors(Locale locale) {
		List<PropertyDescriptor> pds = new ArrayList<PropertyDescriptor>();
		
		addProperty(pds, "verbosity", "MSBuildConsoleLoggerParametersDto.verbosity.name",
				"MSBuildConsoleLoggerParametersDto.verbosity.text", locale);

		addProperty(pds, "parameters", "MSBuildConsoleLoggerParametersDto.parameters.name",
				"MSBuildConsoleLoggerParametersDto.parameters.text", locale);
		
		return pds;
	}
	
	@Override
	public String getPluginId() {
		return DotNetBuildPlugin.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return DotNetBuildPlugin.PLUGIN_NAME;
	}

	public Verbosity getVerbosity() {
		return verbosity;
	}
	
	public void setVerbosity(Verbosity verbosity) {
		this.verbosity = verbosity;
	}
	
	public ConsoleLoggerParameter[] getParameters() {
		return parameters;
	}
	
	public void setParameters(ConsoleLoggerParameter[] parameters) {
		this.parameters = parameters;
	}
}

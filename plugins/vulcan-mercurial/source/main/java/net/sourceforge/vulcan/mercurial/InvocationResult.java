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


public class InvocationResult {
	private final String error;
	private final String output;
	private final boolean success;
	
	public InvocationResult(String output, String error, boolean success) {
		this.output = output;
		this.error = error;
		this.success = success;
	}

	public String getError() {
		return error;
	}
	
	public String getOutput() {
		if (output == null) {
			throw new IllegalStateException("Output was redirected.");
		}
		return output;
	}
	
	public boolean isSuccess() {
		return success;
	}
}
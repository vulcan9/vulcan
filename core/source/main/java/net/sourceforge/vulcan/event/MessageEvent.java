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
package net.sourceforge.vulcan.event;

import java.util.Date;

import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class MessageEvent implements Event, Message {
	final Object source;
	final String messageKey;
	final Object[] messageArgs;
	final Date date;
	
	public MessageEvent(final Object source, final String messageKey) {
		this(source, messageKey, new Object[0]);
	}
	public MessageEvent(final Object source, final String messageKey,
			final Object[] messageArgs) {
		super();
		this.source = source;
		this.messageKey = messageKey;
		this.messageArgs = messageArgs;
		this.date = new Date();
	}
	
	public Object[] getArgs() {
		return messageArgs;
	}
	public String getKey() {
		return messageKey;
	}
	public Object getSource() {
		return source;
	}
	public Date getDate() {
		return date;
	}
}

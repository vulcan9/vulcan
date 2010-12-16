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
package net.sourceforge.vulcan.ant.receiver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.metadata.SvnRevision;


@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class AbstractEventSource implements AntEventSource {
	private final List<EventListener> listeners = new ArrayList<EventListener>();
	
	public final void addEventListener(EventListener listener) {
		listeners.add(listener);
	}
	public final boolean removeEventListener(EventListener listener) {
		return listeners.remove(listener);
	}

	protected final void fireEvent(final AntEventSummary event) {
		for (Iterator itr = listeners.iterator(); itr.hasNext();) {
			final EventListener lstr = (EventListener) itr.next();
			lstr.eventReceived(event);
		}
	}
}

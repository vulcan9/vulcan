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

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.sourceforge.vulcan.ant.buildlistener.AntEventSummary;
import net.sourceforge.vulcan.ant.io.ByteSerializer;

public class UdpReceiver {
	
	public static void main(String[] args) throws UnknownHostException {
		final UdpEventSource source = new UdpEventSource(
				InetAddress.getByName("localhost"),
				3001, 100, new ByteSerializer());

		final Thread main = Thread.currentThread();
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				System.out.println("Shutting down...");
				source.shutDown();
				try {
					main.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
		source.addEventListener(new EventListener() {
			public void eventReceived(AntEventSummary event) {
				System.out.println();
				System.out.println("Type: " + event.getType());
				System.out.println("Project: " + event.getProjectName());
				System.out.println("Target: " + event.getTargetName());
				System.out.println("Task: " + event.getTaskName());
				System.out.println("Priority: " + event.getPriority());
				System.out.println("File: " + event.getFile());
				System.out.println("Line Number: " + event.getLineNumber());
				System.out.println("Code: " + event.getCode());
				System.out.println("Message: " + event.getMessage());
			}
		});
		
		source.run();
	}
}

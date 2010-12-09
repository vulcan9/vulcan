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

import java.io.File;

import net.sourceforge.vulcan.EasyMockTestCase;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.Executor;

public class ProcessInvokerTest extends EasyMockTestCase {
	private ProcessInvoker invoker = new ProcessInvoker();
	private Executor executor;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		executor = createMock(Executor.class);
		invoker.setExecutor(executor);
	}
	
	public void testInvoke() throws Exception {
		CommandLine commandLine = new CommandLine("hg");
		commandLine.addArgument("help");
		commandLine.addArgument("--noninteractive");
		commandLine.addArgument("arg 1");
		commandLine.addArgument("arg 2");
		
		executor.setExitValues(reflectionEq(new int[] {0, 1}));
		executor.setWorkingDirectory(new File("."));
		executor.setStreamHandler((ExecuteStreamHandler) notNull());
		expect(executor.execute(stringEq(commandLine))).andReturn(0);
		
		replay();

		invoker.setExecutable("hg");
		final InvocationResult result = invoker.invoke("help", new File("."), "arg 1", "arg 2");
		
		assertEquals("result.isSuccess()", true, result.isSuccess());
		
		verify();
	}
	
	public void testInvokeSetsSuccessFlag() throws Exception {
		CommandLine commandLine = new CommandLine("hg1");
		commandLine.addArgument("help");
		commandLine.addArgument("--noninteractive");
		commandLine.addArgument("arg 1");
		commandLine.addArgument("arg 2");
		
		executor.setExitValues(reflectionEq(new int[] {0, 1}));
		executor.setWorkingDirectory(new File("."));
		executor.setStreamHandler((ExecuteStreamHandler) notNull());
		expect(executor.execute(stringEq(commandLine))).andReturn(1);
		
		replay();

		invoker.setExecutable("hg1");
		final InvocationResult result = invoker.invoke("help", new File("."), "arg 1", "arg 2");
		
		assertEquals("result.isSuccess()", false, result.isSuccess());
		assertEquals("result.getExitCode()", 1, invoker.getExitCode());
		
		verify();
	}
}

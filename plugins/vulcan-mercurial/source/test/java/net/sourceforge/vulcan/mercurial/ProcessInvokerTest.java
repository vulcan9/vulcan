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
package net.sourceforge.vulcan.mercurial;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import net.sourceforge.vulcan.EasyMockTestCase;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.Executor;
import org.easymock.EasyMock;
import org.easymock.IAnswer;

public class ProcessInvokerTest extends EasyMockTestCase {
	private ProcessInvoker invoker = new ProcessInvoker();
	private Executor executor;
	
	private String executable = "hg";
	private int exitCode = 0;
	private OutputStream outputStream;
	private InvocationResult result;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		executor = createMock(Executor.class);
		invoker.setExecutor(executor);
	}
	
	public void testInvoke() throws Exception {
		doInvokeTest();
		
		assertEquals("isSuccess", true, result.isSuccess());
		assertEquals("output", "sample output", result.getOutput());
		assertEquals("error", "sample error", result.getError());
	}
	
	public void testInvokeWithAlternateCommand() throws Exception {
		executable = "hg1";
		
		doInvokeTest();
	}
	
	public void testInvokeSetsSuccessFlag() throws Exception {
		exitCode = 1;
		
		doInvokeTest();
		
		assertEquals("result.isSuccess()", false, result.isSuccess());
		assertEquals("result.getExitCode()", 1, invoker.getExitCode());
	}
	
	public void testRedirectOutput() throws Exception {
		outputStream = new ByteArrayOutputStream();
		
		doInvokeTest();
		
		assertEquals("sample output", outputStream.toString());
	}
	
	public void testRedirectOutputResultOutputThrows() throws Exception {
		outputStream = new ByteArrayOutputStream();
		
		doInvokeTest();
		
		try {
			result.getOutput();
			fail("expected exception");
		} catch (IllegalStateException e) {
		}
	}
	
	private InvocationResult doInvokeTest() throws ExecuteException, IOException {
		CommandLine commandLine = new CommandLine(executable);
		commandLine.addArgument("help");
		commandLine.addArgument("--noninteractive");
		commandLine.addArgument("arg 1", false);
		commandLine.addArgument("arg 2", false);
		
		executor.setExitValues(reflectionEq(new int[] {0, 1}));
		executor.setWorkingDirectory(new File("."));
		executor.setStreamHandler((ExecuteStreamHandler) notNull());
		
		expectLastCall().andAnswer(captureStreamHandler);
		expect(executor.execute(stringEq(commandLine))).andAnswer(executeAnswer);
		
		replay();

		if (outputStream != null) {
			invoker.setOutputStream(outputStream);
		}
		
		invoker.setExecutable(executable);
		result = invoker.invoke("help", new File("."), "arg 1", "arg 2");
		
		verify();
		
		return result;
	}
	
	private MyPumpStreamHandler handler;
	
	private IAnswer<Object> captureStreamHandler = new IAnswer<Object>() {
		public Object answer() throws Throwable {
			handler = (MyPumpStreamHandler) EasyMock.getCurrentArguments()[0];
			return null;
		}
	};
	
	private IAnswer<Integer> executeAnswer = new IAnswer<Integer>() {
		public Integer answer() throws Throwable {
			handler.getOut().write("sample output".getBytes());
			handler.getErr().write("sample error".getBytes());
			return exitCode;
		}
	};
}

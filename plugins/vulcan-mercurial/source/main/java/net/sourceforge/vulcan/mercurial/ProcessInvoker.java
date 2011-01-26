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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProcessInvoker implements Invoker {
	private static final Log LOG = LogFactory.getLog(ProcessInvoker.class);
	
	private String executable;
	private Executor executor = new DefaultExecutor();
	
	private OutputStream defaultOut = new ByteArrayOutputStream();
	private OutputStream out = defaultOut;
	private OutputStream err;
	private int exitCode;
	
	public InvocationResult invoke(String command, File workDir, String... args) throws IOException {
		CommandLine cmdLine = new CommandLine(executable);
		
		cmdLine.addArgument(command);
		cmdLine.addArgument("--noninteractive");
		cmdLine.addArguments(args, false);
		
		err = new ByteArrayOutputStream();
		
		executor.setExitValues(new int[] {0, 1});
		executor.setWorkingDirectory(workDir);
		executor.setStreamHandler(new MyPumpStreamHandler(out, err));
	
		LOG.debug("Executing " + cmdLine);
		
		try {
			exitCode = executor.execute(cmdLine);
			return new InvocationResult(isOutputRedirected() ? null : out.toString(), err.toString(), exitCode == 0);
		} catch (ExecuteException e) {
			exitCode = e.getExitValue();
			throw e;
		}
		
	}

	private boolean isOutputRedirected() {
		return this.out != this.defaultOut;
	}
	
	public void setOutputStream(OutputStream stream) {
		out = stream;
	}

	public String getExecutable() {
		return executable;
	}
	
	public void setExecutable(String executable) {
		this.executable = executable;
	}
	
	public void setExecutor(Executor executor) {
		this.executor = executor;
	}

	public String getErrorText() {
		return err.toString();
	}
	
	public String getOutputText() {
		if (isOutputRedirected()) {
			throw new IllegalStateException("output was redirected.");
		}
		
		return out.toString();
	}
	public int getExitCode() {
		return exitCode;
	}
}

class MyPumpStreamHandler extends PumpStreamHandler {
	public MyPumpStreamHandler(OutputStream out, OutputStream err) {
		super(out, err);
	}

	@Override
	public OutputStream getOut() {
		return super.getOut();
	}
	
	@Override
	public OutputStream getErr() {
		return super.getErr();
	}
}

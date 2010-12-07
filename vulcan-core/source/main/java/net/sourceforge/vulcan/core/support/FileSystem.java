/**
 * 
 */
package net.sourceforge.vulcan.core.support;

import java.io.File;
import java.io.IOException;

import net.sourceforge.vulcan.RepositoryAdaptor;
import net.sourceforge.vulcan.exception.ConfigException;

import org.apache.commons.io.FileUtils;

public class FileSystem {
	private int deleteDirectoryAttempts = 1;
	private long deleteFailureSleepTime;
	
	public File cleanWorkingDirectory(String workDir, RepositoryAdaptor ra) throws ConfigException, IOException {
		final File path = new File(workDir).getCanonicalFile();
		
		if (path.exists() && !ra.isWorkingCopy()) {
			throw new ConfigException(
					"errors.wont.delete.non.working.copy",
					new Object[] {path.toString()});
		}
		
		if (!createWorkingDirectories(path)) {
			throw new ConfigException(
					"errors.cannot.create.dir",
					new Object[] {path.toString()});
		}
		
		return path;
	}
	
	/**
	 * Create working directory and parent directories if they don't exist.
	 * @return success flag (false if directories were not created).
	 */
	public boolean createWorkingDirectories(File path) throws ConfigException {
		if (!deleteWorkingDirectory(path)) {
			return false;
		}
		
		return path.mkdirs();
	}

	public boolean deleteWorkingDirectory(File path) throws ConfigException {
		int tries = 0;
		
		try {
			while (path.exists()) {
				tries++;
				try {
					FileUtils.deleteDirectory(path);
					break;
				} catch (IOException e) {
					if (tries >= deleteDirectoryAttempts) {
						throw e;
					}
					try {
						Thread.sleep(deleteFailureSleepTime);
					} catch (InterruptedException e1) {
						return false;
					}
				}
			}
		} catch (IOException e) {
				throw new ConfigException(
						"messages.build.cannot.delete.work.dir",
						new Object[] {
							path.getPath(),
							e.getMessage()});
		}
		
		return true;
	}
	
	public void setDeleteDirectoryAttempts(int deleteDirectoryAttempts) {
		this.deleteDirectoryAttempts = deleteDirectoryAttempts;
	}
	
	public void setDeleteFailureSleepTime(long deleteFailureSleepTime) {
		this.deleteFailureSleepTime = deleteFailureSleepTime;
	}
}
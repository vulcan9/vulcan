package net.sourceforge.vulcan.subversion;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

class LineOfDevelopment {
	Set<String> tagFolderNames = new HashSet<String>();

	private String repositoryRoot;
	private String path;
	private String alternateTagName;

	private String computedTagName;
	private String computedRelativePath;
	private String computedTagRoot;
	private String computedModule;
	
	void setTagFolderNames(Set<String> tagFolderNames) {
		this.tagFolderNames = tagFolderNames;
		compute();
	}

	void setPath(String path) {
		this.path = path;
		compute();
	}

	void setAlternateTagName(String alternateTagName) {
		this.alternateTagName = alternateTagName;
		compute();
	}
	
	void setRepositoryRoot(String repositoryRoot) {
		this.repositoryRoot = repositoryRoot;
	}

	boolean isTag(String name) {
		return tagFolderNames.contains(name);
	}

	String getComputedTagName() {
		return computedTagName;
	}
	
	String getComputedRelativePath() {
		return computedRelativePath;
	}

	String getComputedTagRoot() {
		return computedTagRoot;
	}

	String getAbsoluteUrl() {
		final StringBuffer url = new StringBuffer(repositoryRoot);
	
		if (!repositoryRoot.endsWith("/") && !computedRelativePath.startsWith("/")) {
			url.append("/");
		}
		
		url.append(computedRelativePath);
		
		return url.toString();
	}

	private void compute() {
		computeTagName();
		computeTagRootAndModule();
		computeRelativePath();
	}
	
	private void computeTagName() {
		if (alternateTagName != null) {
			this.computedTagName = alternateTagName;
		} else {
			this.computedTagName = "trunk";

			final String[] paths = path.split("/");
			
			if (paths.length > 1) {
				for (int i=0; i<paths.length-1; i++) {
					if (isTag(paths[i])) {
						this.computedTagName = paths[i] + "/" + paths[i+1];
					}
				}
			}
		}
	}
	
	private void computeTagRootAndModule() {
		final String[] paths = path.split("/");
		int lastPath = -1;
		int moduleStart = -1;
		
		for (int i=0; i<paths.length; i++) {
			if ("trunk".equals(paths[i])) {
				lastPath = i;
				moduleStart = i+1;
				break;
			} else if (isTag(paths[i])) {
				lastPath = i;
				moduleStart = i+2;
				break;
			}
		}
		
		if (lastPath < 0) {
			this.computedTagRoot = path;
			this.computedModule = "";
			return;
		}
		
		final StringBuffer buf = new StringBuffer();
		
		for (int i=0; i<lastPath; i++) {
			if (StringUtils.isBlank(paths[i])) {
				continue;
			}
			
			buf.append("/");
			buf.append(paths[i]);
		}
		
		this.computedTagRoot = buf.toString();
		
		buf.delete(0, buf.length());

		if (moduleStart > 0) {
			for (int i=moduleStart; i<paths.length; i++) {
				buf.append("/");
				buf.append(paths[i]);
			}
		}
		
		this.computedModule = buf.toString();
	}

	private void computeRelativePath() {
		if (alternateTagName != null) {
			final StringBuffer buf = new StringBuffer(computedTagRoot);
			
			if (buf.length() == 0 || (buf.charAt(buf.length()-1) != '/' && alternateTagName.charAt(0) != '/')) {
				buf.append('/');
			}
			
			buf.append(alternateTagName);
			buf.append(computedModule);
			
			this.computedRelativePath = buf.toString();
		} else {
			this.computedRelativePath = path;
		}
	}
}

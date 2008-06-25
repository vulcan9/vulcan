package net.sourceforge.vulcan.dto;

public class BuildArtifactLocationDto extends NameDto {
	private String description;
	private String path;
	private boolean report;
	
	public BuildArtifactLocationDto() {
	}
	
	public BuildArtifactLocationDto(String name, String description, String path, boolean report) {
		setName(name);
		this.description = description;
		this.path = path;
		this.report = report;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public boolean isReport() {
		return report;
	}
	public void setReport(boolean report) {
		this.report = report;
	}
}

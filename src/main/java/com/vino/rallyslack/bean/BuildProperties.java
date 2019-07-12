package com.vino.rallyslack.bean;

public class BuildProperties {

	private String group;
	
	private String artifact;
	
	private String name;

	private String version;

	private String buildTime;

	

	public BuildProperties(String name, String version, String buildTime, String artifact, String group) {
		super();
		this.name = name;
		this.version = version;
		this.buildTime = buildTime;
		this.artifact = artifact;
		this.group = group;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getBuildTime() {
		return buildTime;
	}

	public void setBuildTime(String buildTime) {
		this.buildTime = buildTime;
	}

	public String getArtifact() {
		return artifact;
	}

	public void setArtifact(String artifact) {
		this.artifact = artifact;
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

}

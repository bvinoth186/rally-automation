package com.vino.rallyslack.bean;

import java.util.List;

public class Story {

	private String storyId;

	private String storyName;

	private List<Task> tasks;

	public List<Task> getTasks() {
		return tasks;
	}

	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

	public Story(String storyId, String storyName) {
		super();
		this.storyId = storyId;
		this.storyName = storyName;
	}

	

	public String getStoryId() {
		return storyId;
	}

	public void setStoryId(String storyId) {
		this.storyId = storyId;
	}

	public String getStoryName() {
		return storyName;
	}

	public void setStoryName(String storyName) {
		this.storyName = storyName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((storyId == null) ? 0 : storyId.hashCode());
		result = prime * result + ((storyName == null) ? 0 : storyName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Story other = (Story) obj;
		if (storyId == null) {
			if (other.storyId != null)
				return false;
		} else if (!storyId.equals(other.storyId))
			return false;
		if (storyName == null) {
			if (other.storyName != null)
				return false;
		} else if (!storyName.equals(other.storyName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Story [storyId=" + storyId + ", storyName=" + storyName + "]";
	}

}

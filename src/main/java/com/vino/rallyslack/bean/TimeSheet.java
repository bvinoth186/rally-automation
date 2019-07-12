package com.vino.rallyslack.bean;

import java.util.List;

public class TimeSheet {

	private String userName;

	private List<Story> stories;

	public TimeSheet(String userName) {
		super();
		this.userName = userName;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public List<Story> getStories() {
		return stories;
	}

	public void setStories(List<Story> stories) {
		this.stories = stories;
	}

}

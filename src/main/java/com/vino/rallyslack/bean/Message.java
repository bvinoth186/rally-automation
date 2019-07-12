package com.vino.rallyslack.bean;

import java.util.List;

public class Message {
	
	private String response_type;
	
	private String text;
	
	private List<TimeSheet> timeEntryItems;
	
	public Message(String response_type, List<TimeSheet> timeEntryItems) {
		super();
		this.response_type = response_type;
		this.timeEntryItems = timeEntryItems;
	}

	public Message(String response_type, String text) {
		super();
		this.response_type = response_type;
		this.text = text;
	}

	public String getResponse_type() {
		return response_type;
	}

	public void setResponse_type(String response_type) {
		this.response_type = response_type;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public List<TimeSheet> getTimeEntryItems() {
		return timeEntryItems;
	}

	public void setTimeEntryItems(List<TimeSheet> timeEntryItems) {
		this.timeEntryItems = timeEntryItems;
	}
	
	
	
	
	

}

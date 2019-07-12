package com.vino.rallyslack.controller;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.rallydev.rest.RallyRestApi;
import com.rallydev.rest.request.QueryRequest;
import com.rallydev.rest.response.QueryResponse;
import com.rallydev.rest.util.Fetch;
import com.rallydev.rest.util.QueryFilter;
import com.vino.rallyslack.bean.Message;
import com.vino.rallyslack.bean.Story;
import com.vino.rallyslack.bean.Task;
import com.vino.rallyslack.bean.TimeEntryItem;
import com.vino.rallyslack.bean.TimeSheet;

@RestController
public class RallyController {

	private static final Logger LOGGER = LoggerFactory.getLogger(RallyController.class);

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

	private static final String NO_DATA_FOUND = "No Data Found";

	private static final String SUCCESS = "Success";

	private static final String DEFAULT_PROJECT = "";

	private static final String SLACK_RESPONSE_TYPE = "in_channel";

	private static final String SPACE = "    ";

	@Value("${apikey}")
	private String apikey;

	@Value("${apikey2}")
	private String apikey2;

	@Value("${slack-channel-transaction}")
	private String slackChTransaction;

	@Value("${projects}")
	private String projects;

	@Value("${projects2}")
	private String projects2;

	@Autowired
	private Environment env;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	BuildProperties buildProperties;

	@RequestMapping(value = "/ping", method = RequestMethod.GET)
	public ResponseEntity<String> ping() {

		String result = SUCCESS + " " + getCentralTime();
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequestMapping(value = "/build-info", method = RequestMethod.GET)
	public ResponseEntity<com.vino.rallyslack.bean.BuildProperties> buildInfo() {

		String ctTime = getCentralTime(buildProperties.getTime());

		com.vino.rallyslack.bean.BuildProperties bp = new com.vino.rallyslack.bean.BuildProperties(
				buildProperties.getName(), buildProperties.getVersion(), ctTime, buildProperties.getArtifact(),
				buildProperties.getGroup());

		return new ResponseEntity<>(bp, HttpStatus.OK);
	}

	// Date and Time of the build

	public static String getUsage() {

		String usage = "Slack application intracts with Rally and returns the timesheet data for the given project and date\n\n";
		usage = usage + "`USAGE: `\n\n";
		usage = usage + "   \t/fly-rally-timesheet project-name[,date] \n\n";
		usage = usage + "   --> project-name - Name of the Rally Project (required) \n";
		usage = usage + "   --> date         - Date in which timesheet details required (optional) \n\n";
		usage = usage + "   --> project-name shoufd match with Rally \n";
		usage = usage + "   --> if the date is not provided, timesheet details of current day would be returned \n";
		usage = usage + "   --> date should be in  YYYY-MM-dd format (optional) \n";
		usage = usage + "   --> project-name and date is seperated by comma ',' \n\n";
		usage = usage + "`EXAMPLE: `\n\n";
		usage = usage + "   /fly-rally-timesheet Brainiacs \n";
		usage = usage + "   /fly-rally-timesheet Brainiacs,2019-05-31\n";

		return usage;
	}

//	@Scheduled(cron = "0 00 13 * * 1-5") // 8.00 AM CST - 6.30 PM IST
//	@Scheduled(cron = "0 45 13 * * *") //7.45 AM CST - 6.15 PM IST
	public void schedule() throws Exception {
		System.out.println("Projects : " + projects);
		if (projects != null && projects.length() > 0) {
			projects = projects.trim();
			String[] values = projects.split(",");
			for (int i = 0; i < values.length; i++) {
				String projectName = values[i];
				publishToSlack(projectName, "1");
			}
		}
	}

	@Scheduled(cron = "0 00 03 * * 2-6") // 10.00 PM CST - 8.30 AM IST
	public void schedule2() throws Exception {
		System.out.println("Projects : " + projects2);
		if (projects2 != null && projects2.length() > 0) {
			projects2 = projects2.trim();
			String[] values = projects2.split(",");
			for (int i = 0; i < values.length; i++) {
				String projectName = values[i];
				publishToSlack(projectName, "2");
			}
		}
	}

	private void publishToSlack(String projectName, String apiIndex) throws Exception {

		LOGGER.info("********************************************************************************");
		LOGGER.info("Rally timesheets for " + projectName + " Scheduled at " + new Date());

		MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<String, String>();
		List<String> inputList = new ArrayList<String>();
		inputList.add(projectName);
		bodyMap.put("text", inputList);

		List<String> nameList = new ArrayList<String>();
		nameList.add("rally-scheduler");
		bodyMap.put("user_name", nameList);

		List<String> apiList = new ArrayList<String>();
		apiList.add(apiIndex);
		bodyMap.put("apiIndex", apiList);

		List<String> commandList = new ArrayList<String>();
		String command = "/fly-rally-timesheet";
		if (apiIndex == "2") {
			command = "/chub-rally-timesheet";
		}
		commandList.add(command);
		bodyMap.put("command", commandList);

		ResponseEntity<Message> responseEntity = timeentry(bodyMap);

		projectName = projectName.replaceAll("\\s+", "");
		String endpoint = getSlackWebHookUrl(projectName);
		if (endpoint != null) {
			String results = "[Auto triggered on " + getCentralTime() + "] \n\n" + responseEntity.getBody().getText();
			ResponseEntity<String> response = post(endpoint, results);

			LOGGER.info("published to Slack : " + response.getBody() + " for the project " + projectName);
		} else {
			LOGGER.info("Webhook URL is not defined for " + projectName);
		}
		LOGGER.info("Scheduler response for project : " + projectName + " is OK");
		LOGGER.info("********************************************************************************");
	}

	private String getSlackWebHookUrl(String project) {
		return env.getProperty(project);
	}

	@RequestMapping(value = "/timeentry", method = RequestMethod.POST)
	public ResponseEntity<Message> timeentry(@RequestBody MultiValueMap<String, String> bodyMap) throws Exception {

		LOGGER.info("Post parameters " + bodyMap);
		logTransactionIntoSlack(bodyMap);

		List<String> inputList = parseInputArgument(bodyMap);
		if (inputList == null) {
			return new ResponseEntity<Message>(new Message(SLACK_RESPONSE_TYPE, getUsage()), HttpStatus.OK);
		}

		String project = inputList.get(0);
		String date = inputList.get(1);

		String apiIndex = "1";
		if (bodyMap.get("apiIndex") != null && !bodyMap.get("apiIndex").isEmpty()) {
			apiIndex = bodyMap.get("apiIndex").get(0);
		}
		List<TimeEntryItem> timeEntryItems = process(project, date, apiIndex);

		List<TimeSheet> timeSheets = constructTimeSheetData(timeEntryItems);
		String result = constructResultString(project, date, timeSheets, apiIndex);

		LOGGER.info("Timesheet data fetched for " + timeSheets.size() + " users");
		return new ResponseEntity<Message>(new Message(SLACK_RESPONSE_TYPE, result), HttpStatus.OK);
	}

	@RequestMapping(value = "/timeentry2", method = RequestMethod.POST)
	public ResponseEntity<Message> timeentry2(@RequestBody MultiValueMap<String, String> bodyMap) throws Exception {

		LOGGER.info("Post parameters " + bodyMap);
		logTransactionIntoSlack(bodyMap);

		List<String> inputList = parseInputArgument(bodyMap);
		if (inputList == null) {
			return new ResponseEntity<Message>(new Message(SLACK_RESPONSE_TYPE, getUsage()), HttpStatus.OK);
		}

		String project = inputList.get(0);
		String date = inputList.get(1);

		String apiIndex = "2";
		if (bodyMap.get("apiIndex") != null && !bodyMap.get("apiIndex").isEmpty()) {
			apiIndex = bodyMap.get("apiIndex").get(0);
		}

		List<TimeEntryItem> timeEntryItems = process(project, date, apiIndex);

		List<TimeSheet> timeSheets = constructTimeSheetData(timeEntryItems);
		String result = constructResultString(project, date, timeSheets, apiIndex);

		LOGGER.info("Timesheet data fetched for " + timeSheets.size() + " users");
		return new ResponseEntity<Message>(new Message(SLACK_RESPONSE_TYPE, result), HttpStatus.OK);
	}

	private List<TimeSheet> constructTimeSheetData(List<TimeEntryItem> timeEntryItems) {

		List<TimeSheet> timeSheets = new ArrayList<TimeSheet>();

		Map<String, List<Story>> timeMap = new HashMap<String, List<Story>>();
		for (Iterator iterator = timeEntryItems.iterator(); iterator.hasNext();) {
			TimeEntryItem item = (TimeEntryItem) iterator.next();

			String userName = item.getUserName();

			List<Story> stories = timeMap.get(userName);
			if (stories == null) {

				Story story = getStory(item);
				stories = new ArrayList<Story>();
				stories.add(story);

				TimeSheet timeSheet = new TimeSheet(item.getUserName());
				timeSheet.setStories(stories);
				timeSheets.add(timeSheet);
				timeMap.put(userName, stories);
			} else {

				boolean storyFound = false;
				for (Iterator iterator2 = stories.iterator(); iterator2.hasNext();) {
					Story story = (Story) iterator2.next();

					if (story.getStoryId().equals(item.getStoryId())) {
						Task task = new Task(item.getTaskId(), item.getTaskName(), "", item.getStatus());
						task.setHours(item.getHours());
						story.getTasks().add(task);
						storyFound = true;
					}
				}

				if (!storyFound) {
					Story story = getStory(item);
					stories.add(story);
				}
			}
		}

		return timeSheets;

	}

	private Story getStory(TimeEntryItem item) {

		Story story = new Story(item.getStoryId(), item.getStoryName());
		Task task = new Task(item.getTaskId(), item.getTaskName(), "", item.getStatus());
		task.setHours(item.getHours());

		List<Task> tasks = new ArrayList<Task>();

		tasks.add(task);
		story.setTasks(tasks);
		return story;
	}

	private TimeSheet getTimeSheet(TimeEntryItem item) {
		List<Story> stories = new ArrayList<Story>();

		Story story = new Story(item.getStoryId(), item.getStoryName());
		Task task = new Task(item.getTaskId(), item.getTaskName(), "", item.getStatus());

		List<Task> tasks = new ArrayList<Task>();

		tasks.add(task);
		story.setTasks(tasks);
		stories.add(story);

		TimeSheet timeSheet = new TimeSheet(item.getUserName());
		timeSheet.setStories(stories);
		return timeSheet;
	}

	private List<String> parseInputArgument(MultiValueMap<String, String> bodyMap) {

		List<String> textList = bodyMap.get("text");
		String project = DEFAULT_PROJECT;
		String date = getTodaysDate();

		if (textList != null && !textList.isEmpty()) {
			String text = textList.get(0);
			if (text != null && text.length() > 0) {
				StringTokenizer token = new StringTokenizer(text, ",");
				if (token.countTokens() == 2) {
					project = token.nextElement().toString();
					date = token.nextElement().toString();
				} else if (token.countTokens() == 1) {
					project = text;
				} else {
					return null;

				}
			} else {
				return null;
			}
		} else {
			return null;
		}

		List<String> inputList = new ArrayList<String>();
		inputList.add(project);
		inputList.add(date);

		return inputList;
	}

	private void logTransactionIntoSlack(MultiValueMap<String, String> bodyMap) {
		String transactionLog = "User Name = " + bodyMap.get("user_name") + ", Channel Name = "
				+ bodyMap.get("channel_name") + ", Command = " + bodyMap.get("command") + ", Arguments = "
				+ bodyMap.get("text") + ", Time = " + getCentralTime();

		ResponseEntity<String> response = post(slackChTransaction, transactionLog);
		LOGGER.info("logTransactionToSlack : " + response.getBody());
	}

	private ResponseEntity<String> post(String endpoint, String text) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Map<String, String> map = new HashMap<String, String>();
		map.put("text", text);
		HttpEntity<Map<String, String>> request = new HttpEntity<Map<String, String>>(map, headers);
		ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);

		return response;
	}

	private String constructResultString(String project, String date, List<TimeSheet> timeSheets, String apiIndex) {

		String result = "`" + project + " Staus Update - " + date + "`" + "\n"
				+ "=================================================================\n\n";

		if (timeSheets == null || timeSheets.isEmpty()) {
			result = result + "    " + "- " + NO_DATA_FOUND;
			return result;
		}

		for (Iterator iterator = timeSheets.iterator(); iterator.hasNext();) {
			TimeSheet timeSheet = (TimeSheet) iterator.next();

			result = result + "`" + timeSheet.getUserName() + "`" + "\n\n";

			List<Story> stories = timeSheet.getStories();

			for (Iterator iterator2 = stories.iterator(); iterator2.hasNext();) {
				Story story = (Story) iterator2.next();

				if (apiIndex == "2") {
					result = result + SPACE + "-" + SPACE + "*" + story.getStoryId() + "*" + SPACE + "- "
							+ story.getStoryName() + "\n\n";
				}

				List<Task> tasks = story.getTasks();

				for (Iterator iterator3 = tasks.iterator(); iterator3.hasNext();) {
					Task task = (Task) iterator3.next();

					if (apiIndex == "2") {
						result = result + SPACE + SPACE;
					}
					result = result + SPACE + SPACE + "-" + SPACE + "*" + task.getTaskId() + "* (" + task.getHours()
							+ " hours)" + SPACE + "-" + SPACE + task.getState() + SPACE + "-" + SPACE
							+ task.getTaskName() + "\n";

				}
				result = result + "\n";

			}
			result = result + "\n";
		}
		return result;
	}

	private List<TimeEntryItem> process(String project, String date, String apiIndex) throws Exception {

		LOGGER.info("Project : " + project + ", Input Date " + date);
		List<TimeEntryItem> timeEntryItems = new ArrayList<TimeEntryItem>();

		RallyRestApi restApi = null;
		try {
			restApi = getRallyRestApi(apiIndex);
			LOGGER.info("Rally API " + restApi);

			timeEntryItems = getTimeEntries(restApi, project, date);

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			if (restApi != null) {
				restApi.close();
			}
		}
		return timeEntryItems;
	}

	private List<TimeEntryItem> getTimeEntries(RallyRestApi restApi, String projectName, String date) throws Exception {

		List<TimeEntryItem> timeEntryItems = new ArrayList<TimeEntryItem>();

		String projectRef = getProjectRefByName(restApi, projectName);
		LOGGER.info("projectRef : " + projectRef);
		if (projectRef == null) {
			return timeEntryItems;
		}

		QueryFilter queryFilter = getQueryFilterStringByDate(date);
		LOGGER.info("queryFilter : " + queryFilter);
		if (queryFilter == null) {
			return timeEntryItems;
		}

		QueryRequest timeRequest = new QueryRequest("TimeEntryValue");
		timeRequest.setQueryFilter(queryFilter);
		timeRequest.setFetch(
				new Fetch(new String[] { "Task", "Hours", "TimeEntryItem", "User", "DateVal", "ObjectID", "Name" }));
		timeRequest.setProject(projectRef);
		timeRequest.setLimit(25000);
		timeRequest.setScopedDown(false);
		timeRequest.setScopedUp(false);

		QueryResponse timeQueryResponse = restApi.query(timeRequest);
		JsonArray timeJsonArray = timeQueryResponse.getResults();
		if (timeJsonArray.size() == 0) {
			return timeEntryItems;
		}

		timeEntryItems = getTimeEntryMap(restApi, timeJsonArray, projectRef);

		return timeEntryItems;
	}

	private List<TimeEntryItem> getTimeEntryMap(RallyRestApi restApi, JsonArray timeJsonArray, String projectRef)
			throws Exception {

		List<TimeEntryItem> timeEntryItems = new ArrayList<TimeEntryItem>();
		Set<String> objSet = new HashSet<String>();
		for (int i = 0; i < timeJsonArray.size(); i++) {
			JsonObject timeJsonObject = timeJsonArray.get(i).getAsJsonObject();
			JsonObject itemJsonObject = timeJsonObject.get("TimeEntryItem").getAsJsonObject();

			if (itemJsonObject.get("Task") != JsonNull.INSTANCE) {

				JsonObject taskObj = itemJsonObject.get("Task").getAsJsonObject();
				String taskName = taskObj.get("Name").toString();
				taskName = taskName.replace("\"", "");

				String hours = "0";
				if (timeJsonObject.get("Hours") != null) {
					hours = timeJsonObject.get("Hours").toString();
				}

				if (!taskName.toUpperCase().contains("Project MeetingsS".toUpperCase())) {
					String user = itemJsonObject.get("User").getAsJsonObject().get("_refObjectName").toString();
					user = user.replace("\"", "");

					String taskObjId = taskObj.get("ObjectID").toString();
					if (!objSet.contains(taskObjId)) {
						objSet.add(taskObjId);
						TimeEntryItem timeEntryItem = getTaskBean(restApi, taskObjId, projectRef);

						if (timeEntryItem != null) {
							timeEntryItem.setHours(hours);
							timeEntryItem.setUserName(user);
							timeEntryItems.add(timeEntryItem);
						}
					}
				}

			}
		}

		return timeEntryItems;
	}

	private TimeEntryItem getTaskBean(RallyRestApi restApi, String taskObjId, String projectRef) throws Exception {

		QueryRequest taskRequest = new QueryRequest("Task");
		taskRequest.setProject(projectRef);
		taskRequest.setFetch(
				new Fetch(new String[] { "Name", "Notes", "FormattedID", "ObjectID", "State", "WorkProduct" }));
		taskRequest.setQueryFilter(new QueryFilter("ObjectID", "=", taskObjId));

		QueryResponse taskQueryResponse = restApi.query(taskRequest);

		JsonArray taskJsonArray = taskQueryResponse.getResults();
		TimeEntryItem timeEntryItem = null;
		if (taskJsonArray.size() != 0) {
			JsonObject taskJsonObject = taskJsonArray.get(0).getAsJsonObject();

			String taskName = taskJsonObject.get("Name").toString();
			String taskId = taskJsonObject.get("FormattedID").toString();
			String notes = taskJsonObject.get("Notes").toString();
			String status = taskJsonObject.get("State").toString();

			JsonObject wpJsonObject = taskJsonObject.get("WorkProduct").getAsJsonObject();
			String storyId = "";
			String storyName = "";
			if (wpJsonObject != null) {
				storyId = wpJsonObject.get("FormattedID").toString();
				storyName = wpJsonObject.get("Name").toString();
			}

			taskName = taskName.replace("\"", "");
			taskId = taskId.replace("\"", "");
			notes = notes.replace("\"", "");
			status = status.replace("\"", "");
			storyId = storyId.replace("\"", "");
			storyName = storyName.replace("\"", "");

			notes = notes.replaceAll("\\<.*?\\>", "");
			notes = notes.replaceAll("&nbsp;", "");
			notes = notes.replaceAll("&amp;", "");

			status = getTaskState(status);

			timeEntryItem = new TimeEntryItem();
			timeEntryItem.setStatus(status);
			timeEntryItem.setStoryId(storyId);
			timeEntryItem.setStoryName(storyName);
			timeEntryItem.setTaskId(taskId);
			timeEntryItem.setTaskName(taskName);
		}

		return timeEntryItem;
	}

	private String getTaskState(String state) {

		String status = state;
		if ("Completed".equals(state)) {
			status = "Completed    ";
		} else if ("Defined".equals(state)) {
			status = "Defined      ";
		} else if ("In-Progress".equals(state)) {
			status = "In-Progress   ";
		}
		return status;
	}

	private QueryFilter getQueryFilterStringByDate(String inputDateStr) {

		QueryFilter filter = null;
		try {
			LocalDate current = null;

			if (inputDateStr == null) {
				current = LocalDate.now();
			} else {
				current = LocalDate.parse(inputDateStr, DATE_FORMATTER);
			}

			LocalDate next = current.plusDays(1);
			LocalDate prev = current.minusDays(1);

			String nextStr = next.format(DATE_FORMATTER);
			String prevStr = prev.format(DATE_FORMATTER);

			prevStr = "\"" + prevStr + "\"";
			nextStr = "\"" + nextStr + "\"";

			filter = new QueryFilter("DateVal", ">", prevStr).and(new QueryFilter("DateVal", "<", nextStr));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return filter;
	}

	private String getTodaysDate() {

		Instant nowUtc = Instant.now();
		ZoneId usCentral = ZoneId.of("US/Central");
		ZonedDateTime centralTime = ZonedDateTime.ofInstant(nowUtc, usCentral);
		return centralTime.format(DATE_FORMATTER);
	}

	private String getProjectRefByName(RallyRestApi restApi, String projectName) throws IOException {

		if (projectName == null) {
			projectName = DEFAULT_PROJECT;
		}
		QueryRequest storyRequest = new QueryRequest("project");
		storyRequest.setFetch(new Fetch("Name", "ObjectID"));
		storyRequest.setQueryFilter(new QueryFilter("Name", "=", projectName));

		QueryResponse storyQueryResponse = restApi.query(storyRequest);

		String projectRef = null;
		if (storyQueryResponse.getTotalResultCount() > 0) {
			projectRef = "project/" + storyQueryResponse.getResults().get(0).getAsJsonObject().get("ObjectID");
		}

		return projectRef;
	}

	private void printAllProjects(RallyRestApi restApi) throws IOException {

		QueryRequest projectRequest = new QueryRequest("project");
		projectRequest.setFetch(new Fetch("Name", "ObjectID"));

		QueryResponse projectQueryResponse = restApi.query(projectRequest);
		JsonArray projectJsonArray = projectQueryResponse.getResults();

		for (int i = 0; i < projectJsonArray.size(); i++) {
			JsonObject obj = projectJsonArray.get(i).getAsJsonObject();
			String name = obj.get("Name").toString();
			name = name.replace("\"", "");
			LOGGER.info(name);
		}
	}

	private RallyRestApi getRallyRestApi(String apiIndex) throws Exception {

		URI server = new URI("https://rally1.rallydev.com");

		String apiKeyTmp = apikey;
		if (apiIndex == "2") {
			apiKeyTmp = apikey2;
		}

		return new RallyRestApi(server, apiKeyTmp);
	}

	private String getCentralTime() {

		Instant nowUtc = Instant.now();
		ZoneId usCentral = ZoneId.of("US/Central");
		ZonedDateTime centralTime = ZonedDateTime.ofInstant(nowUtc, usCentral);
		return centralTime.format(DATE_TIME_FORMATTER);
	}

	private String getCentralTime(Instant instant) {

		ZoneId usCentral = ZoneId.of("US/Central");
		ZonedDateTime centralTime = ZonedDateTime.ofInstant(instant, usCentral);
		return centralTime.format(DATE_TIME_FORMATTER);
	}

	public static void main(String args[]) throws Exception {
		RallyController r = new RallyController();

		String time = r.getCentralTime(Instant.now());
		System.out.println(time);

	}

}

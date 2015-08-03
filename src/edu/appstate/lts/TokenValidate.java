package edu.appstate.lts;

import com.wowza.wms.module.*;
import com.wowza.wms.client.*;

public class TokenValidate extends ModuleBase {
	private String token;
	private String application;
	private String url;
	private String dateStarted;
	private String ipAddress;
	private String mediaHash;
	private String wsApplication;
	private String wsUrl;
	private String wsDateIssued;
	private String wsValidMinutes;
	private String wsIpAddress;
	
	public TokenValidate(IClient client) {
		// token = client.getQueryStr();
		url = client.getPageUrl();
		dateStarted = client.getDateStarted();
		ipAddress = client.getIp();
		callWebService();
	}
	
	private void callWebService() {
		mediaHash = "aabbccddeeffgghhiijjkkllmmnnooppqqrrsstt";
		wsApplication = "appstate";
		wsUrl = "http://www.appstate.edu/~meltonml/video/flowtest.html";
		wsDateIssued = "2015-08-04T15:00:00-04:00";
		wsValidMinutes = "5";
		wsIpAddress = "10.0.2.15";
	}
	
	// App validation tested to work when tested with 
	// text in URL after "http://www." and before ".edu"
	private boolean validateApp() {
		String newUrl = url.substring(7);
		if (newUrl.substring(0,4).equals("www.")) {
			newUrl = newUrl.substring(4);
		}
		int endIndex = newUrl.indexOf(".edu");
		application = newUrl.substring(0, endIndex);
		return application.equals(wsApplication);
	}
	
	// URL validation tested to work.
	private boolean validateUrl() {
		return url.equals(wsUrl);
	}
	
	private boolean validateDateTime() {
		// Fri, Jul 31 2015 10:37:20 -0400
		// Mon, Aug 3 2015 14:14:44 -0400
		String[] dateTime = dateStarted.split(" ");
		int month = 13;
		switch (dateTime[1]) {
			case "Jan":
				month = 1;
				break;
			case "Feb":
				month = 2;
				break;
			case "Mar":
				month = 3;
				break;
			case "Apr":
				month = 4;
				break;
			case "May":
				month = 5;
				break;
			case "Jun":
				month = 6;
				break;
			case "Jul":
				month = 7;
				break;
			case "Aug":
				month = 8;
				break;
			case "Sep":
				month = 9;
				break;
			case "Oct":
				month = 10;
				break;
			case "Nov":
				month = 11;
				break;
			case "Dec": 
				month = 12;
				break;
		}
		int day = Integer.parseInt(dateTime[2]);
		int year = Integer.parseInt(dateTime[3]);
		String[] time = dateTime[4].split(":");
		int hour = Integer.parseInt(time[0]);
		int minutes = Integer.parseInt(time[1]);
		return validateDateTimeHelper(month, day, year, hour, minutes);
	}
	
	// Need to evaluate days by month, and consider leap years.
	private boolean validateDateTimeHelper(int month, int day, int year, int hour, int minutes) {
		// wsDateIssued = "2015-07-31T15:00:00-04:00"; 2015-08-04T15:00:00-04:00
		// wsValidMinutes = "5";
		boolean validDateTime = true;
		int vYear = Integer.parseInt(wsDateIssued.substring(0, 4));
		int vMonth = Integer.parseInt(wsDateIssued.substring(5, 7));
		int vDay = Integer.parseInt(wsDateIssued.substring(8, 10));
		int vHour = Integer.parseInt(wsDateIssued.substring(11, 13));
		int vMinutes = Integer.parseInt(wsDateIssued.substring(14, 16));
		vMinutes += (Integer.parseInt(wsValidMinutes));
		if (vMinutes >= 60) {
			vHour++;
			vMinutes -= 60;
		}
		if (vHour >= 24) {
			vDay++;
			vHour -= 24;
		}
		if (vDay > 31) {
			vMonth++;
			vDay -= 31;
		}
		if (vMonth > 12) {
			vYear++;
			vMonth -= 12;
		}
		
		if (vYear < year) {
			validDateTime = false;
		}
		else if (vMonth < month) {
			validDateTime = false;
		}
		else if (vDay < day) {
			validDateTime = false;
		}
		else if (vHour < hour) {
			validDateTime = false;
		}
		else if (vMinutes < minutes) {
			validDateTime = false;
		}
		return validDateTime;
	}
	
	// IP address validation tested to work.
	private boolean validateIpAddress() {
		return ipAddress.equals(wsIpAddress);
	}
	
	public boolean validate() {
		// validateDateTime() not working yet
		return validateApp() && validateUrl() && validateDateTime() && validateIpAddress();
	}
}

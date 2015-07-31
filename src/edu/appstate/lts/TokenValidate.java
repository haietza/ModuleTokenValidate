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
		wsDateIssued = "2015-07-31T15:00:00-04:00";
		wsValidMinutes = "5";
		wsIpAddress = "10.0.2.15";
	}
	
	private boolean validateApp() {
		String newUrl = url.substring(7);
		if (newUrl.substring(0,4).equals("www.")) {
			newUrl = newUrl.substring(4);
		}
		StringBuilder sbUrl = new StringBuilder(newUrl);
		int endIndex = sbUrl.indexOf(".edu");
		application = newUrl.substring(0, endIndex);
		return application.equals(wsApplication);
	}
	
	private boolean validateUrl() {
		return url.equals(wsUrl);
	}
	
	private boolean validateDateTime() {
		// Date started: Fri, Jul 31 2015 10:37:20 -0400
		String monthWord = dateStarted.substring(5, 8);
		int month;
		if (monthWord.equals("Jan")) {
			month = 1;
		}
		else if (monthWord.equals("Feb")) {
			month = 2;
		}
		else if (monthWord.equals("Mar")) {
			month = 3;
		}
		else if (monthWord.equals("Apr")) {
			month = 4;
		}
		else if (monthWord.equals("May")) {
			month = 5;
		}
		else if (monthWord.equals("Jun")) {
			month = 6;
		}
		else if (monthWord.equals("Jul")) {
			month = 7;
		}
		else if (monthWord.equals("Aug")) {
			month = 8;
		}
		else if (monthWord.equals("Sep")) {
			month = 9;
		}
		else if (monthWord.equals("Oct")) {
			month = 10;
		}
		else if (monthWord.equals("Nov")) {
			month = 11;
		}
		else if (monthWord.equals("Dec")) {
			month = 12;
		}
		else {
			month = 13;
		}
		int day = Integer.parseInt(dateStarted.substring(9, 11));
		int year = Integer.parseInt(dateStarted.substring(12, 16));
		int hour = Integer.parseInt(dateStarted.substring(17, 19));
		int minutes = Integer.parseInt(dateStarted.substring(20, 22));
		return validateDateTimeHelper(month, day, year, hour, minutes);
	}
	
	private boolean validateDateTimeHelper(int month, int day, int year, int hour, int minutes) {
		// wsDateIssued = "2015-07-31T15:00:00-04:00";
		// wsValidMinutes = "5";
		boolean validDateTime = false;
		int vYear = Integer.parseInt(wsDateIssued.substring(0, 4));
		int vMonth = Integer.parseInt(wsDateIssued.substring(5, 7));
		int vDay = Integer.parseInt(wsDateIssued.substring(8, 10));
		int vHour = Integer.parseInt(wsDateIssued.substring(11, 13));
		int vMinutes = Integer.parseInt(wsDateIssued.substring(15, 17));
		vMinutes += (Integer.parseInt(wsValidMinutes));
		if (vMinutes >= 60) {
			vHour++;
			vMinutes -= 60;
		}
		if (vHour >= 24) {
			vHour -= 24;
		}
		if (vYear >= year) {
			if (vMonth >= month) {
				if (vDay >= day) {
					if (vHour >= hour) {
						if (vMinutes >= minutes) {
							validDateTime = true;
						}
					}
				}
			}
		}
		return validDateTime;
	}
	
	private boolean validateIpAddress() {
		return ipAddress.equals(wsIpAddress);
	}
	
	public boolean validate() {
		// validateDateTime() not working yet
		return validateApp() && validateUrl() && validateDateTime() && validateIpAddress();
	}
}

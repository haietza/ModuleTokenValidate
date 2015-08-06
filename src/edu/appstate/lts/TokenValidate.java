package edu.appstate.lts;

import com.wowza.wms.module.*;
import com.wowza.wms.stream.IMediaStream;
import com.wowza.wms.client.*;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;

import java.util.Calendar;
import java.util.Date;

public class TokenValidate extends ModuleBase {
	private String token;
	private String application;
	private String url;
	private Date startDate;
	private String ipAddress;
	private String mediaHash;
	private String wsApplication;
	private String wsUrl;
	private String wsDateIssued;
	private String wsValidMinutes;
	private String wsIpAddress;
	
	public TokenValidate(IClient client) {
		startDate = new Date();
		token = client.getQueryStr();
		url = client.getPageUrl();
		ipAddress = client.getIp();
		callWebService(token);
	}
	
	public TokenValidate(IHTTPStreamerSession httpSession) {
		startDate = new Date();
		token = httpSession.getQueryStr();
		url = httpSession.getReferrer();
		ipAddress = httpSession.getIpAddress();
		callWebService(token);
	}
	
	// Need to figure out how to call web service from
	// http://ltsdev04.lts.appstate.edu/resolvetoken.php
	private void callWebService(String token) {
		mediaHash = "aabbccddeeffgghhiijjkkllmmnnooppqqrrsstt";
		wsApplication = "appstate";
		wsUrl = "http://www.appstate.edu/~meltonml/video/flowtest.html";
		wsDateIssued = "2015-08-29T15:00:00-04:00";
		wsValidMinutes = "5";
		wsIpAddress = "127.0.0.1";
	}
	
	// App validation tested with text in URL after "http://www." and before ".edu"
	private boolean validateApp() {
		String newUrl = url.substring(7);
		if (newUrl.substring(0,4).equals("www.")) {
			newUrl = newUrl.substring(4);
		}
		int endIndex = newUrl.indexOf(".edu");
		application = newUrl.substring(0, endIndex);
		getLogger().info(application + " = " + wsApplication + " : " + application.equals(wsApplication));
		return application.equals(wsApplication);
	}
	
	private boolean validateUrl() {
		getLogger().info(url + " = " + wsUrl + " : " + url.equals(wsUrl));
		return url.equals(wsUrl);
	}
	
	private boolean validateDate() {
		Calendar validDate = Calendar.getInstance();
		String[] split = wsDateIssued.split("-|:|T");
		int wsYear = Integer.parseInt(split[0]);
		int wsMonth = Integer.parseInt(split[1]);
		int wsDay = Integer.parseInt(split[2]);
		int wsHour = Integer.parseInt(split[3]);
		int wsMinutes = Integer.parseInt(split[4]);
		int wsSeconds = Integer.parseInt(split[5]);
		
		validDate.set(wsYear, wsMonth - 1, wsDay, wsHour, wsMinutes, wsSeconds);
		validDate.add(validDate.get(Calendar.MINUTE), Integer.parseInt(wsValidMinutes));
		
		Date wsDate = validDate.getTime();
		getLogger().info(startDate + " before " + wsDate + " : " + wsDate.after(startDate));
		return wsDate.after(startDate);
	}
	
	private boolean validateIpAddress() {
		getLogger().info(ipAddress + " = " + wsIpAddress + " : " + ipAddress.equals(wsIpAddress));
		return ipAddress.equals(wsIpAddress);
	}
	
	public boolean validate() {
		return validateApp() && validateUrl() && validateDate() && validateIpAddress();
	}
}

package edu.appstate.lts;

import com.wowza.wms.module.*;
import com.wowza.wms.client.*;

public class TokenValidate extends ModuleBase {
	
	private String token;
	private String url;
	private String dateStarted;
	private String ipAddress;
	private String fileName;
	private String wsUrl;
	private String wsDateIssued;
	private String wsValidTime;
	private String wsIpAddress;
	
	public TokenValidate(IClient client) {
		token = client.getQueryStr();
		url = client.getPageUrl();
		dateStarted = client.getDateStarted();
		ipAddress = client.getIp();
		callWebService();
	}
	
	public void callWebService() {
		// Get filename
		// Get wsUrl
		// Get wsDateIssued
		// Get wsValidTime
		// Get wsIpAddress
	}
	
	public boolean validateUrl() {
		return url.equals(wsUrl);
	}
	
	public boolean validateDateTime() {
		return true;
	}
	
	public boolean validateIpAddress() {
		return ipAddress.equals(wsIpAddress);
	}
	
	public boolean validate() {
		return true;
	}
}

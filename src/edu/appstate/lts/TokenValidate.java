package edu.appstate.lts;

import com.wowza.wms.module.*;
//import com.sun.org.apache.xml.internal.security.utils.XPathFactory;
import com.wowza.wms.client.*;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathFactory;

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
	
	private void callWebService(String token) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new URL("http://ltsdev04.lts.appstate.edu/resolvetoken.php?token=" + token).openConnection().getInputStream());
			doc.getDocumentElement().normalize();
			XPath xpath = XPathFactory.newInstance().newXPath();
			// May need to revise compile expression if all nodes won't always be present or in same order.
			XPathExpression expr = xpath.compile("//mensch-access-ticket/*/text()");
			Object result = expr.evaluate(doc,  XPathConstants.NODESET);
			NodeList nodes = (NodeList) result;
			
			wsDateIssued = nodes.item(1).getNodeValue();
			wsValidMinutes = nodes.item(2).getNodeValue();
			wsApplication = nodes.item(3).getNodeValue();
			wsUrl = nodes.item(4).getNodeValue();
			wsIpAddress = nodes.item(5).getNodeValue();
			mediaHash = nodes.item(6).getNodeValue();
		} catch (Exception e) {
			e.getMessage();
		}
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
	
	// www. removed from URLs before validation
	private boolean validateUrl() {
		if (url.substring(7, 11).equals("www.")) {
			url = url.substring(0, 7) + url.substring(11);
		}
		if (wsUrl.substring(7, 11).equals("www.")) {
			wsUrl = wsUrl.substring(0, 7) + wsUrl.substring(11);
		}
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

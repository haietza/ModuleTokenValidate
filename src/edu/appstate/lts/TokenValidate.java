package edu.appstate.lts;

import com.wowza.wms.module.*;
import com.wowza.wms.client.*;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class TokenValidate extends ModuleBase {
	private String token;
	private String application;
	private String url;
	private String ipAddress;
	private String hashValue;
	private String wsApplication;
	private String wsUrl;
	private String wsIpAddress;
	
	public TokenValidate(IClient client) {
		//startDate = new Date();
		token = client.getQueryStr();
		url = client.getPageUrl();
		ipAddress = client.getIp();
		callWebService(token);
	}
	
	public TokenValidate(IHTTPStreamerSession httpSession) {
		//startDate = new Date();
		token = httpSession.getQueryStr();
		url = httpSession.getReferrer();
		ipAddress = httpSession.getIpAddress();
		callWebService(token);
	}
	
	private void callWebService(String token) {
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new URL("http://lts6.lts.appstate.edu/mensch-tickets/ticket/" + token).openConnection().getInputStream());
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("Ticket");
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					hashValue = eElement.getElementsByTagName("HashValue").item(0).getTextContent();
					wsIpAddress = eElement.getElementsByTagName("IpAddr").item(0).getTextContent();
					wsApplication = eElement.getElementsByTagName("Issuer").item(0).getTextContent();
					wsUrl = eElement.getElementsByTagName("Url").item(0).getTextContent();
				}
			}
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
	
	private boolean validateIpAddress() {
		getLogger().info(ipAddress + " = " + wsIpAddress + " : " + ipAddress.equals(wsIpAddress));
		return ipAddress.equals(wsIpAddress);
	}
	
	public boolean validate() {
		return validateApp() && validateUrl() && validateIpAddress();
	}
	
	public String getFileName() {
		String fileName = null;
		File file = new File("C:/Program Files (x86)/Wowza Media Systems/Wowza Streaming Engine 4.2.0/content/" + hashValue + ".mp4");
		BufferedInputStream in = null;
		FileOutputStream fos = null;
		if (file.exists()) {
			fileName = file.getName();
		}
		else if (!file.exists()) {
			try {
				in = new BufferedInputStream(new URL("http://lts6.lts.appstate.edu/mensch-store-web/artifact/" + hashValue).openStream());
				fos = new FileOutputStream(file, true);
				int read = 0;
				byte[] bytes = new byte[1024];
				while ((read = in.read(bytes, 0, 1024)) != -1) {
					fos.write(bytes, 0, read);
				}
				fileName = hashValue + ".mp4";
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return fileName;
	}
}

package edu.appstate.lts;

import com.wowza.wms.application.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.wowza.wms.amf.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;
import com.wowza.wms.rtp.model.*;
import com.wowza.wms.httpstreamer.model.*;
import com.wowza.wms.mediacaster.IMediaCaster;

public class ModuleTokenValidate extends ModuleBase implements IMediaStreamNameAliasProvider2 {
	
	public ModuleTokenValidate() {
		super();
	}
	
	private String validateToken(String token, String Url, String IpAddress) {
		String hashValue = null;
		String wsApplication;
		String wsUrl = null;
		String wsIpAddress = null;
		
		getLogger().info("VT Token: " + token);
		getLogger().info("VT URL: " + Url);
		getLogger().info("VT IP: " + IpAddress);
		
		// Get token info from web service
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
			getLogger().info("WS hash value: " + hashValue);
			getLogger().info("WS IP: " + wsIpAddress);
			getLogger().info("WS URL: " + wsUrl);
		} catch (MalformedURLException e) {
			getLogger().info("Malformed URL: " + e.getMessage());
		} catch (ParserConfigurationException e) {
			getLogger().info("Parser configuration: " + e.getMessage());
		} catch (IOException e) {
			getLogger().info("IOException: " + e.getMessage());
		} catch (SAXException e) {
			getLogger().info("SAXException: " + e.getMessage());
		}
		
		// Validate token
		// Get file and/or filename from Wowza cache or Mensch store 
		String fileName = null;
		getLogger().info(Url + " equals " + wsUrl + ": " + Url.equals(wsUrl));
		getLogger().info(IpAddress + " equals " + wsIpAddress + ": " + IpAddress.equals(wsIpAddress));
		if (Url.equals(wsUrl) && IpAddress.equals(wsIpAddress)) {
			File file = new File("C:/Program Files (x86)/Wowza Media Systems/Wowza Streaming Engine 4.2.0/content/" + hashValue + ".mp4");
			BufferedInputStream in = null;
			FileOutputStream fos = null;
			if (file.exists()) {
				fileName = "mp4:" + file.getName();
				getLogger().info("Existing file name: " + fileName);
			}
			else {
				try {
					in = new BufferedInputStream(new URL("http://lts6.lts.appstate.edu/mensch-store-web/artifact/" + hashValue).openStream());
					fos = new FileOutputStream(file, true);
					int read = 0;
					byte[] bytes = new byte[1024];
					while ((read = in.read(bytes, 0, 1024)) != -1) {
						fos.write(bytes, 0, read);
					}
					fileName = "mp4:" + hashValue + ".mp4";
					getLogger().info("Downloaded file name: " + fileName);
				} catch (MalformedURLException e) {
					getLogger().info("Malformed URL: " + e.getMessage());
				} catch (FileNotFoundException e) {
					getLogger().info("File not found: " + e.getMessage());
				} catch (IOException e) {
					getLogger().info("IO exception: " + e.getMessage());
				} finally {
					if (in != null) {
						try {
							in.close();
						} catch (IOException e) {
							getLogger().info("IN close IO exception: " + e.getMessage());
						}
					}
					if (fos != null) {
						try {
							fos.close();
						} catch (IOException e) {
							getLogger().info("FOS close IO exception: " + e.getMessage());
						}
					}
				}
			}
			
		}
		getLogger().info("Returned file name: " + fileName);
		return fileName;
	}

	public void onAppStart(IApplicationInstance appInstance) {
		String fullname = appInstance.getApplication().getName() + "/" + appInstance.getName();
		getLogger().info("onAppStart: " + fullname);
		appInstance.setStreamNameAliasProvider(this);
	}

	public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
		getLogger().info("onConnect: " + client.getClientId());
		
		/*
		tokenValidate = new TokenValidate(client);		
		
		if (!tokenValidate.validate()) {
			getLogger().info("Client ID " + client.getClientId() + " not valid.");
			client.rejectConnection("Invalid token.");
			client.shutdownClient();
		}
		else {
			// Get video from Mensch store via token/hash and send to RTMP player
			client.acceptConnection("Valid token.");
			IApplicationInstance appInstance = client.getAppInstance();
			appInstance.setStreamNameAliasProvider(this);
		}
		*/
	}
	
	public void onHTTPSessionCreate(IHTTPStreamerSession httpSession) {
		getLogger().info("onHTTPSessionCreate: " + httpSession.getSessionId());
		/*
		tokenValidate = new TokenValidate(httpSession);
		if (!tokenValidate.validate()) {
			httpSession.rejectSession();
			httpSession.shutdown();
		}
		*/
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IClient client) {
		getLogger().info("Token: " + client.getQueryStr());
		getLogger().info("Url: " + client.getPageUrl());
		getLogger().info("Ip: " + client.getIp());
		getLogger().info("Name: " + name);
		getLogger().info("Validate token: " + validateToken(client.getQueryStr(), client.getPageUrl(), client.getIp()));
		
		try {
			return validateToken(client.getQueryStr(), client.getPageUrl(), client.getIp());
		} catch (Exception e) {
			getLogger().info("Validate token exception: " + e.getMessage());
			client.rejectConnection(e.getMessage());
			return null;
		}
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IHTTPStreamerSession httpSession){
		// Resolve play alias for HTTP streaming
		try {
			return validateToken(httpSession.getQueryStr(), httpSession.getReferrer(), httpSession.getIpAddress());
		} catch (Exception e) {
			httpSession.rejectSession();
			httpSession.shutdown();
			return null;
		}
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, RTPSession rtpSession) {
		// Resolve play alias for RTSP/RTP streaming
		return null;
	}

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name,
			ILiveStreamPacketizer liveStreamPacketizer) {
		// Resolve play alias for live stream packetizer
		return null;
	}

	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name, IMediaCaster mediaCaster) {
		// Resolve stream alias for MediaCaster
		return null;
	}
}

package edu.appstate.lts;

import com.wowza.wms.application.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
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
	
	private String validateToken(IApplicationInstance appInstance, String token, String url, String ipAddress) {
		String hashValue = null;
		String wsApplication;
		String wsUrl = null;
		String wsIpAddress = null;
		
		getLogger().info("Arguments passed to validateToken from resolvePlayAlias: Token = " + token + ", URL = " + url + ", IP = " + ipAddress);
		
		// Get token info from web service
		try {
			URL ticket = new URL("http://lts6.lts.appstate.edu/mensch-tickets/ticket/" + token);
			HttpURLConnection connection = (HttpURLConnection) ticket.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/xml");
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(connection.getInputStream());
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
			getLogger().info("Values received from web service: HashValue = " + hashValue + ", IP = " + wsIpAddress + ", URL = " + wsUrl);
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
		getLogger().info("Validate URL: Wowza URL = " + url + " equals web service URL =  " + wsUrl + ": " + url.equalsIgnoreCase(wsUrl));
		getLogger().info("Validate IP: Wowza IP = " + ipAddress + " equals web service IP = " + wsIpAddress + ": " + ipAddress.equalsIgnoreCase(wsIpAddress));
		String fileName = "mp4:mensch/access-denied.mp4";
		if (url.equalsIgnoreCase(wsUrl) && ipAddress.equalsIgnoreCase(wsIpAddress)) {
			getLogger().info("Stream storage directory: " + appInstance.getStreamStorageDir());
			//File file = new File("C:/Program Files (x86)/Wowza Media Systems/Wowza Streaming Engine 4.2.0/content/" + hashValue + ".mp4");
			File file = new File(appInstance.getStreamStorageDir() + "/" + hashValue + ".mp4");
			BufferedInputStream in = null;
			FileOutputStream fos = null;
			if (file.exists()) {
				fileName = "mp4:" + appInstance.getApplication().getName() + "/" + file.getName();
				getLogger().info("Cache file name: " + fileName);
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
					fileName = "mp4:" + appInstance.getApplication().getName() + "/" + hashValue + ".mp4";
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
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IClient client) {
		getLogger().info("RTMP resolvePlayAlias Wowza values: Token = " + client.getQueryStr() + ", URL = " + client.getPageUrl() + ", IP = " + client.getIp() + ", Name = " + name);
		String fileName = "mp4:mensch/access-denied.mp4";
		try {
			fileName = validateToken(appInstance, client.getQueryStr(), client.getPageUrl(), client.getIp());
		} catch (Exception e) {
			getLogger().info("Validate token RTMP exception: " + e.getMessage());
			client.rejectConnection(e.getMessage());
			client.shutdownClient();
			return fileName;
		}
		return fileName;
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IHTTPStreamerSession httpSession){
		// Resolve play alias for HTTP streaming
		getLogger().info("HTTP resolvePlayAlias Wowza values: Token = " + httpSession.getQueryStr() + ", URL = " + httpSession.getReferrer() + ", IP = " + httpSession.getIpAddress());
		String fileName = "mp4:mensch/access-denied.mp4";
		try {
			fileName = validateToken(appInstance, httpSession.getQueryStr(), httpSession.getReferrer(), httpSession.getIpAddress());
		} catch (Exception e) {
			getLogger().info("Validate token HTTP exception: " + e.getMessage());
			httpSession.rejectSession();
			httpSession.shutdown();
			return fileName;
		}
		return fileName;
	}
	
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name) {
		return null;
	}

	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name) {
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

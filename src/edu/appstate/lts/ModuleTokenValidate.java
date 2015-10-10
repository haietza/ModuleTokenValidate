package edu.appstate.lts;

import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;
import com.wowza.wms.rtp.model.*;
import com.wowza.wms.httpstreamer.model.*;
import com.wowza.wms.mediacaster.IMediaCaster;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class to validate tokens for file access/playback for Wowza streaming server.
 * 
 * @author Michelle Melton
 * @version Sep 2015
 */

public class ModuleTokenValidate extends ModuleBase implements IMediaStreamNameAliasProvider2 
{
	private int timeToLive = 30;
	/**
	 * Constructor calls ModuleBase constructor.
	 */
	public ModuleTokenValidate() 
	{
		super();
	}
	
	/**
	 * Given the appInstance and the token, URL and ipAddress from the request,
	 * validate the request and return the filename of the artifact.
	 * 
	 * @param appInstance 
	 * @param token 
	 * @param url 
	 * @param ipAddress 
	 * @return fileName 
	 */
	private String validateToken(IApplicationInstance appInstance, String token, String url, String ipAddress) 
	{
		String mediaHashValue = null;
		String scriptHashValue = null;
		//String wsApplication;
		String wsUrl = null;
		String wsIpAddress = null;
		
		getLogger().info("Arguments passed to validateToken from resolvePlayAlias: Token = " 
				+ token + ", URL = " + url + ", IP = " + ipAddress);
		
		// Get token info from LTS6 ticket web service
		try 
		{
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
					mediaHashValue = eElement.getElementsByTagName("MediaHashValue").item(0).getTextContent();
					scriptHashValue = eElement.getElementsByTagName("ScriptHashValue").item(0).getTextContent();
					wsIpAddress = eElement.getElementsByTagName("IpAddr").item(0).getTextContent();
					//wsApplication = eElement.getElementsByTagName("Issuer").item(0).getTextContent();
					wsUrl = eElement.getElementsByTagName("Url").item(0).getTextContent();
				}
			}
			
			getLogger().info("Values received from web service: MediaHashValue = " + mediaHashValue + ", ScriptHashValue = " + scriptHashValue + ", IP = " + wsIpAddress + ", URL = " + wsUrl);
		
		} 
		catch (MalformedURLException e) 
		{
			getLogger().info("Malformed URL: " + e.getMessage());
		} 
		catch (ParserConfigurationException e) 
		{
			getLogger().info("Parser configuration: " + e.getMessage());
		} 
		catch (IOException e) 
		{
			getLogger().info("IOException: " + e.getMessage());
		} 
		catch (SAXException e) 
		{
			getLogger().info("SAXException: " + e.getMessage());
		}
		
		// Validate token
		// Get file and/or filename from Wowza cache or Mensch store 
		getLogger().info("Validate URL: Wowza URL = " + url + " equals web service URL =  " + wsUrl + ": " + url.equalsIgnoreCase(wsUrl));
		getLogger().info("Validate IP: Wowza IP = " + ipAddress + " equals web service IP = " + wsIpAddress + ": " + ipAddress.equalsIgnoreCase(wsIpAddress));
		
		String fileName = "mp4:mensch/access-denied.mp4";
		if (url.equalsIgnoreCase(wsUrl) && ipAddress.equalsIgnoreCase(wsIpAddress)) 
		{
			getLogger().info("Stream storage directory: " + appInstance.getStreamStorageDir());
			
			File mediaFile = new File(appInstance.getStreamStorageDir() + "/" + mediaHashValue + ".mp4");
			
			BufferedInputStream mediaIn = null;
			FileOutputStream mediaFos = null;
			if (mediaFile.exists()) 
			{
				fileName = "mp4:" + appInstance.getApplication().getName() + "/" + mediaFile.getName();
				getLogger().info("Cache file name: " + fileName);
			}
			else 
			{
				try 
				{
					mediaIn = new BufferedInputStream(new URL("http://lts6.lts.appstate.edu/mensch-store-web/artifact/" + mediaHashValue).openStream());
					mediaFos = new FileOutputStream(mediaFile, true);
					int read = 0;
					byte[] bytes = new byte[1024];
					while ((read = mediaIn.read(bytes, 0, 1024)) != -1) 
					{
						mediaFos.write(bytes, 0, read);
					}
					fileName = "mp4:" + appInstance.getApplication().getName() + "/" + mediaHashValue + ".mp4";
					
					getLogger().info("Downloaded file name: " + fileName);
				} 
				catch (MalformedURLException e) 
				{
					getLogger().info("Malformed URL: " + e.getMessage());
				} 
				catch (FileNotFoundException e) 
				{
					getLogger().info("File not found: " + e.getMessage());
				} 
				catch (IOException e) 
				{
					getLogger().info("IO exception: " + e.getMessage());
				} 
				finally 
				{
					if (mediaIn != null) 
					{
						try 
						{
							mediaIn.close();
						} 
						catch (IOException e) 
						{
							getLogger().info("IN close IO exception: " + e.getMessage());
						}
					}
					if (mediaFos != null) 
					{
						try 
						{
							mediaFos.close();
						} 
						catch (IOException e) 
						{
							getLogger().info("FOS close IO exception: " + e.getMessage());
						}
					}
				}
			}
			
			if (scriptHashValue != null)
			{
				File scriptFile = new File(appInstance.getStreamStorageDir() + "/" + mediaHashValue + ".srt");
				BufferedInputStream scriptIn = null;
				FileOutputStream scriptFos = null;
				if (!scriptFile.exists()) 
				{
					try 
					{
						scriptIn = new BufferedInputStream(new URL("http://lts6.lts.appstate.edu/mensch-store-web/artifact/" + scriptHashValue).openStream());
						scriptFos = new FileOutputStream(scriptFile, true);
						int read = 0;
						byte[] bytes = new byte[1024];
						while ((read = scriptIn.read(bytes, 0, 1024)) != -1) 
						{
							scriptFos.write(bytes, 0, read);
						}
					} 
					catch (MalformedURLException e) 
					{
						getLogger().info("SRT malformed URL: " + e.getMessage());
					} 
					catch (FileNotFoundException e) 
					{
						getLogger().info("SRT file not found: " + e.getMessage());
					} 
					catch (IOException e) 
					{
						getLogger().info("SRT IO exception: " + e.getMessage());
					} 
					finally 
					{
						if (scriptIn != null) 
						{
							try 
							{
								scriptIn.close();
							} 
							catch (IOException e) 
							{
								getLogger().info("SRT IN close IO exception: " + e.getMessage());
							}
						}
						if (mediaFos != null) 
						{
							try 
							{
								scriptFos.close();
							} 
							catch (IOException e) 
							{
								getLogger().info("SRT FOS close IO exception: " + e.getMessage());
							}
						}
					}
				}
			}
		}
		
		getLogger().info("Returned file name: " + fileName);
		
		return fileName;
	}
	
	/**
	 * API method for start of application; sets the stream alias based on token validation.
	 * 
	 * @param appInstance 
	 */
	public void onAppStart(IApplicationInstance appInstance) 
	{
		String fullname = appInstance.getApplication().getName() + "/" + appInstance.getName();
		
		getLogger().info("onAppStart: " + fullname);
		
		// Default days for files to stay on server is 30
		this.timeToLive = appInstance.getProperties().getPropertyInt("timeToLive", this.timeToLive);
		// Set days for files to stay on server based on GUI config
		this.timeToLive = appInstance.getProperties().getPropertyInt("validateTTL", this.timeToLive);
		
		File directory = new File(appInstance.getStreamStorageDir());
		File[] files = directory.listFiles();
		Date modified;
		Date today = new Date();
		long age;
		long days = (long) this.timeToLive;
		long daysToLong = 86400000;
		for (int i = 0; i < files.length; i++)
		{
			modified = new Date(files[i].lastModified());
			age = today.getTime() - modified.getTime();
			//getLogger().info("File " + files[i].getName() + " last modified " + modified.toString());
			if (age > (days * daysToLong) 
					&& !files[i].getName().equals("wowzalogo.png")
					&& !files[i].getName().equals("sample.mp4")
					&& !files[i].getName().equals("access-denied.srt")
					&& !files[i].getName().equals("access-denied.mp4"))
			{
				getLogger().info("File " + files[i].getName() + " last modified " + modified.toString() + ". File will be deleted.");
				files[i].delete();
			}
			else
			{
				getLogger().info("File " + files[i].getName() + " last modified " + modified.toString() + ", or it is required. File will not be deleted.");
			}
		}
		
		appInstance.setStreamNameAliasProvider(this);
	}
	
	/**
	 * Sets the stream alias based on the validation of the token, RTMP streaming.
	 * 
	 * @param appInstance
	 * @param name
	 * @param client
	 * @return fileName
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IClient client) 
	{
		getLogger().info("RTMP resolvePlayAlias Wowza values: Token = " + client.getQueryStr() + ", URL = " + client.getPageUrl() + ", IP = " + client.getIp() + ", Name = " + name);
		
		String fileName = "mp4:mensch/access-denied.mp4";
		try 
		{
			fileName = validateToken(appInstance, client.getQueryStr(), client.getPageUrl(), client.getIp());
		} 
		catch (Exception e) 
		{
			getLogger().info("Validate token RTMP exception: " + e.getMessage());
			
			client.rejectConnection(e.getMessage());
			client.shutdownClient();
			return fileName;
		}
		return fileName;
	}
	
	/**
	 * Sets the stream alias based on the validation of the token, HTTP streaming.
	 * 
	 * @param appInstance
	 * @param name
	 * @param httpSession
	 * @return fileName
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IHTTPStreamerSession httpSession)
	{
		getLogger().info("HTTP resolvePlayAlias Wowza values: Token = " + httpSession.getQueryStr() + ", URL = " + httpSession.getReferrer() + ", IP = " + httpSession.getIpAddress());
		
		String fileName = "mp4:mensch/access-denied.mp4";
		try 
		{
			fileName = validateToken(appInstance, httpSession.getQueryStr(), httpSession.getReferrer(), httpSession.getIpAddress());
		} 
		catch (Exception e) 
		{
			getLogger().info("Validate token HTTP exception: " + e.getMessage());
			
			httpSession.rejectSession();
			httpSession.shutdown();
			return fileName;
		}
		return fileName;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface.
	 * 
	 * @param appInstance
	 * @param name
	 * @return name
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name) 
	{
		return null;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface.
	 * 
	 * @param appInstance
	 * @param name
	 * @return name
	 */
	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name) 
	{
		return null;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface.
	 * Resolves the play alias for RTSP/RTP streaming.
	 * 
	 * @param appInstance
	 * @param name
	 * @param rtpSession
	 * @return name
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, RTPSession rtpSession) 
	{
		return null;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface.
	 * Resolves the play alias for live stream packetizer.
	 * 
	 * @param appInstance
	 * @param name
	 * @param liveStreamPacketizer
	 * @return name
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, ILiveStreamPacketizer liveStreamPacketizer) 
	{
		return null;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface.
	 * Resolves the stream alias for MediaCaster.
	 * 
	 * @param appInstance
	 * @param name
	 * @param mediaCaster
	 * @return name
	 */
	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name, IMediaCaster mediaCaster) 
	{
		return null;
	}
}

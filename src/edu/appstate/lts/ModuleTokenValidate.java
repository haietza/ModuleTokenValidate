package edu.appstate.lts;

import com.wowza.util.ElapsedTimer;
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
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

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
	// Set default ticket web service URL.
	private String ticketService = "";
	
	// Set default store web service URL.
	private String storeService = "";
	
	TimerTask filePurge;
	Timer timer;
	
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
		String hashValue = null;
		// String ticketOutlet;
		String ticketUrl = null;
		String ticketIpAddr = null;
		String transcript = null;
		
		getLogger().info("Arguments passed to validateToken from resolvePlayAlias: Token = " 
				+ token + ", URL = " + url + ", IP = " + ipAddress);
		getLogger().info("Ticket URL: " + ticketService);
		getLogger().info("Store URL: " + storeService);
		
		// Get token info from ticket web service
		try 
		{
			URL ticket = new URL(ticketService + token);
			getLogger().info(ticketService + token);
			HttpURLConnection connection = (HttpURLConnection) ticket.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/xml");
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(connection.getInputStream());
			doc.getDocumentElement().normalize();
			NodeList nList = doc.getElementsByTagName("StreamTicket");
			for (int i = 0; i < nList.getLength(); i++) {
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) nNode;
					hashValue = eElement.getElementsByTagName("HashValue").item(0).getTextContent();
					ticketIpAddr = eElement.getElementsByTagName("IpAddr").item(0).getTextContent();
					//ticketOutlet = eElement.getElementsByTagName("Outlet").item(0).getTextContent();
					ticketUrl = eElement.getElementsByTagName("Url").item(0).getTextContent();
					transcript = eElement.getElementsByTagName("Transcript").item(0).getTextContent();
				}
			}
			
			getLogger().info("Values received from web service: hash value = " + hashValue 
					+ ", IP = " + ticketIpAddr + ", URL = " + ticketUrl);
		
		} 
		catch (MalformedURLException e) 
		{
			getLogger().warn("Malformed URL: " + e.getMessage());
		} 
		catch (ParserConfigurationException e) 
		{
			getLogger().warn("Parser configuration: " + e.getMessage());
		} 
		catch (IOException e) 
		{
			getLogger().warn("IOException: " + e.getMessage());
		} 
		catch (SAXException e) 
		{
			getLogger().warn("SAXException: " + e.getMessage());
		}
		
		getLogger().info("Validate URL: Wowza URL = " + url + " equals web service URL =  " 
		    + ticketUrl + ": " + url.equalsIgnoreCase(ticketUrl));
		getLogger().info("Validate IP: Wowza IP = " + ipAddress + " equals web service IP = " 
		    + ticketIpAddr + ": " + ipAddress.equalsIgnoreCase(ticketIpAddr));
		
		String fileName = "mp4:access-denied.mp4";
		// Validate token
		if (url.equalsIgnoreCase(ticketUrl) && ipAddress.equalsIgnoreCase(ticketIpAddr)) 
		{
			getLogger().info("Stream storage directory: " + appInstance.getStreamStorageDir());
			
			File mediaFile = new File(appInstance.getStreamStorageDir() + "/" + hashValue + ".mp4");
			
			BufferedInputStream mediaIn = null;
			FileOutputStream mediaFos = null;
			
			// Get file/filename from Wowza content directory
			if (mediaFile.exists()) 
			{
				fileName = "mp4:" + mediaFile.getName();
				getLogger().info("Existing file name: " + fileName);
			}
			// Get file/filename from Mensch store
			else 
			{
				try 
				{
					mediaIn = new BufferedInputStream(new URL(storeService + hashValue).openStream());
					mediaFos = new FileOutputStream(mediaFile, true);
					int read = 0;
					byte[] bytes = new byte[1024];
					while ((read = mediaIn.read(bytes, 0, 1024)) != -1) 
					{
						mediaFos.write(bytes, 0, read);
					}
					fileName = "mp4:" + hashValue + ".mp4";
					
					getLogger().info("Downloaded file name: " + fileName);
				} 
				catch (MalformedURLException e) 
				{
					getLogger().warn("Malformed URL: " + e.getMessage());
				} 
				catch (FileNotFoundException e) 
				{
					getLogger().warn("File not found: " + e.getMessage());
				} 
				catch (IOException e) 
				{
					getLogger().warn("IO exception: " + e.getMessage());
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
							getLogger().warn("IN close IO exception: " + e.getMessage());
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
							getLogger().warn("FOS close IO exception: " + e.getMessage());
						}
					}
				}
			}
			
			if (!transcript.equals("")) 
			{
				// Decode -->
				transcript = transcript.replaceAll("--&gt;", "-->");
				// Save to file
				try
				{
					File scriptFile = new File(appInstance.getStreamStorageDir() + "/" + hashValue + ".srt");
					FileWriter fileWriter = new FileWriter(scriptFile);
					fileWriter.write(transcript);
					fileWriter.flush();
					fileWriter.close();
				}
				catch (IOException e)
				{
					getLogger().warn("FileWriter close IO exception: " + e.getMessage());
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
		
		// Default URL for ticket web service
		this.ticketService = appInstance.getProperties().getPropertyStr("ticketService", this.ticketService);
		// Set URL for ticket web service
		this.ticketService = appInstance.getProperties().getPropertyStr("validateTicketURL", this.ticketService);
		
		// Default URL for ticket web service
		this.storeService = appInstance.getProperties().getPropertyStr("storeService", this.storeService);
		// Set URL for ticket web service
		this.storeService = appInstance.getProperties().getPropertyStr("validateStoreURL", this.storeService);
		
		appInstance.setStreamNameAliasProvider(this);
				
		Timer timer = new Timer(true);
		TimerTask filePurge = new FilePurge(appInstance);
		try 
		{
			// Start after 10 seconds, repeat every 3 minutes
			getLogger().info("File purge starting in 10 seconds.");
			timer.schedule(filePurge, 10000, 180000);
		}
		catch (IllegalArgumentException e)
		{
			getLogger().error("Delay time was negative.");
		}
		catch (IllegalStateException e)
		{
			getLogger().error("Task was already scheduled or cancelled.");
		}
		catch (NullPointerException e)
		{
			getLogger().error("Task was null.");
		}
	}
	
	/**
	 * On application stop, cancels the timer task to purge files and purges cancelled tasks.
	 * @param appInstance
	 */
	public void onAppStop(IApplicationInstance appInstance) {
		String fullname = appInstance.getApplication().getName() + "/" + appInstance.getName();
		getLogger().info("onAppStop: " + fullname);
		
		//timer.cancel();
		//timer.purge();
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
		
		String fileName = "mp4:access-denied.mp4";
		try 
		{
			fileName = validateToken(appInstance, client.getQueryStr(), client.getPageUrl(), client.getIp());
		} 
		catch (Exception e) 
		{
			getLogger().warn("Validate token RTMP exception: " + e.getMessage());
			
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
		
		String fileName = "mp4:access-denied.mp4";
		try 
		{
			fileName = validateToken(appInstance, httpSession.getQueryStr(), httpSession.getReferrer(), httpSession.getIpAddress());
		} 
		catch (Exception e) 
		{
			getLogger().warn("Validate token HTTP exception: " + e.getMessage());
			
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

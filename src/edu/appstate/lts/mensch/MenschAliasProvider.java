package edu.appstate.lts.mensch;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
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

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.client.IClient;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.mediacaster.IMediaCaster;
import com.wowza.wms.rtp.model.RTPSession;
import com.wowza.wms.stream.IMediaStreamNameAliasProvider2;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;

/**
 * Class to validate tokens for file access/playback for Wowza streaming server.
 * 
 * @author Michelle Melton
 * @version Sep 2015
 */
public class MenschAliasProvider implements IMediaStreamNameAliasProvider2
{
	private static final String TICKET_SERVICE_URL = "ticket-service-url";
	private static final String STORE_SERVICE_URL = "store-service-url";
	
	private IApplicationInstance appInstance;
	private WMSLogger logger;
	
	private String hashValue;
	// private String ticketOutlet;
	private String ticketUrl;
	private String ticketIpAddr;
	private String transcript;
	private String ticketService;
	private String storeService;
	
	/**
	 * Constructor to get properties from application.
	 * 
	 * @param appInstance
	 */
	public MenschAliasProvider(IApplicationInstance appInstance)
	{
		// Set URL for ticket web service
		ticketService = appInstance.getProperties().getPropertyStr(TICKET_SERVICE_URL, this.ticketService);
		
		// Set URL for ticket web service
		storeService = appInstance.getProperties().getPropertyStr(STORE_SERVICE_URL, this.storeService);
		
		this.appInstance = appInstance;
		logger = WMSLoggerFactory.getLoggerObj(appInstance);
	}
	
	/**
	 * Given the appInstance and the token, URL and ipAddress from the request,
	 * validate the request and return the filename of the artifact.
	 * 
	 * @param token 
	 * @param url 
	 * @param ipAddress 
	 * @return fileName 
	 */
	private String validateToken(String token, String url, String ipAddress) 
	{
		
		logger.info(String.format("Arguments passed to validateToken from resolvePlayAlias: Token = %s, URL = %s, IP = %s", token, url, ipAddress));
		logger.info(String.format("Ticket URL: %s", ticketService));
		logger.info(String.format("Store URL: %s", storeService));
		
		// Do not attempt to validate token if ticket or store service URLs have not been configured
		if (ticketService.isEmpty() || storeService.isEmpty())
		{
			logger.error("Ticket or store URL not configured.");
			return null;
		}
		
		// Get token info from ticket web service
		// URL class checks format of ticket service URL
		try 
		{
			URL ticket = new URL(ticketService + token);
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
			
			logger.info(String.format("Values received from web service: hash value = %s, IP = %s, URL = %s", hashValue, ticketIpAddr, ticketUrl));
		} 
		catch (MalformedURLException e) 
		{
			logger.error(String.format("Malformed URL: %s", e.getMessage()));
			return null;
		} 
		catch (ParserConfigurationException e) 
		{
			logger.error(String.format("Parser configuration: %s", e.getMessage()));
			return null;
		} 
		catch (IOException e) 
		{
			logger.error(String.format("IOException: %s", e.getMessage()));
			return null;
		} 
		catch (SAXException e) 
		{
			logger.error(String.format("SAXException: %s", e.getMessage()));
			return null;
		}
		
		logger.info(String.format("Validate Wowza URL = %s, equals web service URL = %s: %s", url, ticketUrl, url.equalsIgnoreCase(ticketUrl)));
		logger.info(String.format("Validate Wowza IP = %s, equals web service IP = %s: %s", ipAddress, ticketIpAddr, ipAddress.equalsIgnoreCase(ticketIpAddr)));
		
		// Validate token
		if (url.equalsIgnoreCase(ticketUrl) && ipAddress.equalsIgnoreCase(ticketIpAddr)) 
		{
			logger.info(String.format("Stream storage directory: %s", appInstance.getStreamStorageDir()));
			
			// Save non-empty transcript text as SRT file
			if (!transcript.isEmpty()) 
			{
				// Decode -->
				transcript = transcript.replaceAll("--&gt;", "-->");
				// Save to file
				try
				{
					
					File scriptFile = new File(String.format("%s/%s.srt", appInstance.getStreamStorageDir(), hashValue));
					FileWriter fileWriter = new FileWriter(scriptFile);
					fileWriter.write(transcript);
					fileWriter.flush();
					fileWriter.close();
				}
				catch (IOException e)
				{
					logger.error(String.format("FileWriter close IO exception: %s", e.getMessage()));
					return null;
				}
			}
			
			File mediaFile = new File(String.format("%s/%s.mp4", appInstance.getStreamStorageDir(), hashValue));
			BufferedInputStream mediaIn = null;
			FileOutputStream mediaFos = null;
			
			// Get existing filename from Wowza content directory
			if (mediaFile.exists()) 
			{
				logger.info(String.format("File already on Wowza server: %s", mediaFile.getName()));
				return String.format("mp4:%s", mediaFile.getName());
				
			}
			// Get file from Mensch store
			// URL class checks format of store service URL
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
					logger.info(String.format("File streamed from store service: %s.mp4", hashValue));
					return String.format("mp4:%s.mp4", hashValue);
				} 
				catch (MalformedURLException e) 
				{
					logger.error(String.format("Malformed URL: %s", e.getMessage()));
					return null;
				} 
				catch (FileNotFoundException e) 
				{
					logger.error(String.format("File not found: %s", e.getMessage()));
					return null;
				} 
				catch (IOException e) 
				{
					logger.error(String.format("IO exception: %s", e.getMessage()));
					return null;
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
							logger.error(String.format("Input stream close IO exception: %s", e.getMessage()));
							return null;
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
							logger.error(String.format("Output stream close IO exception: %s", e.getMessage()));
							return null;
						}
					}
				}
			}
		}
		// Ticket not validated, return null
		return null;
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
		logger.info(String.format("RTMP resolvePlayAlias Wowza values: Token = %s, URL = %s, IP = %s", name, client.getPageUrl(), client.getIp()));
		
		String validateName = null;
		try 
		{
			validateName = validateToken(name, client.getPageUrl(), client.getIp());
			if (validateName == null)
			{
				//client.rejectConnection();
				client.setShutdownClient(true);
				client.shutdownClient();
			}
		} 
		catch (Exception e) 
		{
			logger.error(String.format("Validate token RTMP exception: %s", e.getMessage()));
			return null;
		}
		return validateName;
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
		logger.info(String.format("HTTP resolvePlayAlias Wowza values: Token = %s, URL = %s, IP = %s", name, httpSession.getReferrer(), httpSession.getIpAddress()));
		
		String validateName = null;
		try 
		{
			validateName = validateToken(name, httpSession.getReferrer(), httpSession.getIpAddress());
			if (validateName == null)
			{
				httpSession.shutdown();
			}
		} 
		catch (Exception e) 
		{
			logger.error(String.format("Validate token HTTP exception: %s", e.getMessage()));
			return null;
		}
		return validateName;
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

	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name) {
		// TODO Auto-generated method stub
		return null;
	}
}

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
 * Class to validate token and provide alias.
 * 
 * @author Michelle Melton
 * @version 0.0.1
 */
public class MenschAliasProvider implements IMediaStreamNameAliasProvider2
{
	private static final String CFGNAME_TICKET_SERVICE_URL = "ticketServiceUrl";
	private static final String CFGNAME_STORE_SERVICE_URL = "storeServiceUrl";
	private static final String ERRSTR_INVALID_CFG = "Invalid configuration value: %s";
	private static final String LOG_RESOLVEPLAYALIAS_RTMP = "RTMP resolvePlayAlias: token=%s, url=%s, ipaddr=%s";
	private static final String LOG_RESOLVEPLAYALIAS_HTTP = "HTTP resolvePlayAlias: token=%s, url=%s, ipaddr=%s";
	private static final String LOG_VALIDATETOKEN = "validateToken web service: hash=%s, url=%s, ipaddr=%s";
	
	private IApplicationInstance appInstance;
	private WMSLogger logger;
	
	private String ticketServiceUrl;
	private String storeServiceUrl;
	
	private String alias = null;
	
	private String hashValue;
	// private String ticketOutlet;
	private String ticketUrl;
	private String ticketIpAddr;
	private String transcript;
	
	/**
	 * Constructor to get properties from application.
	 * 
	 * @param appInstance
	 */
	public MenschAliasProvider(IApplicationInstance appInstance)
	{
		// Set application instance to the one called by ModuleMenschTickets
		this.appInstance = appInstance;
		
		// Set the logger for the application instance
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
		// Fetch and validate needed application configs
		ticketServiceUrl = appInstance.getProperties().getPropertyStr(CFGNAME_TICKET_SERVICE_URL);
		if (ticketServiceUrl.isEmpty())
		{
			logger.error(String.format(ERRSTR_INVALID_CFG, CFGNAME_TICKET_SERVICE_URL));
			return null;
		}
		
		storeServiceUrl = appInstance.getProperties().getPropertyStr(CFGNAME_STORE_SERVICE_URL);
		if (storeServiceUrl.isEmpty())
		{
			logger.error(String.format(ERRSTR_INVALID_CFG, CFGNAME_STORE_SERVICE_URL));
			return null;
		}
		
		// Get token info from ticket web service
		// URL class checks format of ticket service URL
		try 
		{
			URL ticket = new URL(ticketServiceUrl + token);
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
			
			logger.info(String.format(LOG_VALIDATETOKEN, hashValue, ticketUrl, ticketIpAddr));
		} 
		catch (MalformedURLException e) 
		{
			logger.error("validateToken ticket service", e);
		} 
		catch (ParserConfigurationException e) 
		{
			logger.error("validateToken ticket service", e);
		} 
		catch (IOException e) 
		{
			logger.error("validateToken ticket service", e);
		} 
		catch (SAXException e) 
		{
			logger.error("validateToken ticket service", e);
		}
		
		logger.info(String.format("validateToken: url=%s, ip=%s", url.equalsIgnoreCase(ticketUrl), 
				ipAddress.equalsIgnoreCase(ticketIpAddr)));
		
		// Validate token
		if (url.equalsIgnoreCase(ticketUrl) && ipAddress.equalsIgnoreCase(ticketIpAddr)) 
		{			
			// Save non-empty transcript text as SRT file
			if (!transcript.isEmpty()) 
			{
				// Decode -->
				transcript = transcript.replaceAll("--&gt;", "-->");
				
				// Save to file
				try
				{
					File scriptFile = new File(String.format("%s/%s.srt", 
							appInstance.getStreamStorageDir(), hashValue));
					FileWriter fileWriter = new FileWriter(scriptFile);
					fileWriter.write(transcript);
					fileWriter.flush();
					fileWriter.close();
				}
				catch (IOException e)
				{
					logger.error("Write transcript", e);
				}
			}
			
			File mediaFile = new File(String.format("%s/%s.mp4", appInstance.getStreamStorageDir(), hashValue));
			BufferedInputStream mediaIn = null;
			FileOutputStream mediaFos = null;
			
			// Get existing filename from Wowza content directory
			if (mediaFile.exists()) 
			{
				logger.info(String.format("File %s on server", mediaFile.getName()));
				return String.format("mp4:%s", mediaFile.getName());
				
			}
			// Get file from Mensch store
			// URL class checks format of store service URL
			else 
			{
				try 
				{
					mediaIn = new BufferedInputStream(new URL(storeServiceUrl + hashValue).openStream());
					mediaFos = new FileOutputStream(mediaFile, true);
					int read = 0;
					byte[] bytes = new byte[1024];
					while ((read = mediaIn.read(bytes, 0, 1024)) != -1) 
					{
						mediaFos.write(bytes, 0, read);
					}
					logger.info(String.format("File %s streamed from store service", mediaFile.getName()));
					return String.format("mp4:%s.mp4", hashValue);
				} 
				catch (MalformedURLException e) 
				{
					logger.error("Stream file from store service", e);
				} 
				catch (FileNotFoundException e) 
				{
					logger.error("Stream file from store service", e);
				} 
				catch (IOException e) 
				{
					logger.error("Stream file from store service", e);
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
							logger.error("Stream file from store service", e);
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
							logger.error("Stream file from store service", e);
						}
					}
				}
			}
		}
		
		// Ticket not validated, return null
		return null;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface
	 * Resolves the play alias
	 * 
	 * @param appInstance
	 * @param name
	 * @return alias
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name) 
	{
		return null;
	}
	
	/**
	 * Sets the play alias based on the validation of the token, RTMP streaming
	 * 
	 * @param appInstance
	 * @param name
	 * @param client
	 * @return alias
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IClient client) 
	{		
		logger.info(String.format(LOG_RESOLVEPLAYALIAS_RTMP, name, client.getPageUrl(), client.getIp()));
			
		try 
		{
			alias = validateToken(name, client.getPageUrl(), client.getIp());
			if (alias == null)
			{
				client.setShutdownClient(true);
				client.shutdownClient();
			}
		} 
		catch (Exception e) 
		{
			logger.error("validateToken", e);
			client.setShutdownClient(true);
			client.shutdownClient();
		}
		return alias;
	}
	
	/**
	 * Sets the play alias based on the validation of the token, HTTP streaming
	 * 
	 * @param appInstance
	 * @param name
	 * @param httpSession
	 * @return alias
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, IHTTPStreamerSession httpSession)
	{		
		logger.info(String.format(LOG_RESOLVEPLAYALIAS_HTTP, name, httpSession.getReferrer(), 
				httpSession.getIpAddress()));
		
		try 
		{
			alias = validateToken(name, httpSession.getReferrer(), httpSession.getIpAddress());
			if (alias == null)
			{
				httpSession.shutdown();
			}
		} 
		catch (Exception e) 
		{
			logger.error("validateToken", e);
			httpSession.shutdown();
		}
		return alias;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface
	 * Resolves the play alias for RTSP/RTP streaming
	 * 
	 * @param appInstance
	 * @param name
	 * @param rtpSession
	 * @return alias
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, RTPSession rtpSession) 
	{
		return null;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface
	 * Resolves the play alias for live stream packetizer
	 * 
	 * @param appInstance
	 * @param name
	 * @param liveStreamPacketizer
	 * @return alias
	 */
	@Override
	public String resolvePlayAlias(IApplicationInstance appInstance, String name, 
			ILiveStreamPacketizer liveStreamPacketizer) 
	{
		return null;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface
	 * Resolves the stream alias
	 * 
	 * @param appInstance
	 * @param name
	 * @return alias
	 */
	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name) 
	{
		return null;
	}
	
	/**
	 * Required for IMediaStreamNameAliasProvider2 interface
	 * Resolves the stream alias for MediaCaster
	 * 
	 * @param appInstance
	 * @param name
	 * @param mediaCaster
	 * @return alias
	 */
	@Override
	public String resolveStreamAlias(IApplicationInstance appInstance, String name, IMediaCaster mediaCaster) 
	{
		return null;
	}
}
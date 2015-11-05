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
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;

public class ValidateToken 
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
	
	public ValidateToken(IApplicationInstance appInstance)
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
	protected String validateToken(String token, String url, String ipAddress) 
	{
		logger.info("Arguments passed to validateToken from resolvePlayAlias: Token = " 
				+ token + ", URL = " + url + ", IP = " + ipAddress);
		logger.info("Ticket URL: " + ticketService);
		logger.info("Store URL: " + storeService);
		
		// Do not attempt to validate token if ticket or store service URLs have not been configured
		if (ticketService.isEmpty() || storeService.isEmpty())
		{
			logger.error("Ticket or store URL not configured.");
			return "";
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
			
			logger.info("Values received from web service: hash value = " + hashValue 
					+ ", IP = " + ticketIpAddr + ", URL = " + ticketUrl);
		} 
		catch (MalformedURLException e) 
		{
			logger.error("Malformed URL", e);
		} 
		catch (ParserConfigurationException e) 
		{
			logger.error("Parser configuration", e);
		} 
		catch (IOException e) 
		{
			logger.error("IOException", e);
		} 
		catch (SAXException e) 
		{
			logger.error("SAXException", e);
		}
		
		logger.info("Validate Wowza URL = " + url + " equals web service URL =  " 
		    + ticketUrl + ": " + url.equalsIgnoreCase(ticketUrl));
		logger.info("Validate Wowza IP = " + ipAddress + " equals web service IP = " 
		    + ticketIpAddr + ": " + ipAddress.equalsIgnoreCase(ticketIpAddr));
		
		// Validate token
		if (url.equalsIgnoreCase(ticketUrl) && ipAddress.equalsIgnoreCase(ticketIpAddr)) 
		{
			logger.info("Stream storage directory: " + appInstance.getStreamStorageDir());
			
			// Save non-empty transcript text as SRT file
			if (!transcript.isEmpty()) 
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
					logger.error("FileWriter close IO exception", e);
				}
			}
			
			File mediaFile = new File(appInstance.getStreamStorageDir() + "/" + hashValue + ".mp4");
			BufferedInputStream mediaIn = null;
			FileOutputStream mediaFos = null;
			
			// Get existing filename from Wowza content directory
			if (mediaFile.exists()) 
			{
				logger.info("File already on Wowza server: " + mediaFile.getName());
				return "mp4:" + mediaFile.getName();
				
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
					logger.info("File streamed from store service: " + hashValue + ".mp4");
					return "mp4:" + hashValue + ".mp4";
				} 
				catch (MalformedURLException e) 
				{
					logger.error("Malformed URL", e);
				} 
				catch (FileNotFoundException e) 
				{
					logger.error("File not found", e);
				} 
				catch (IOException e) 
				{
					logger.error("IO exception", e);
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
							logger.error("Input stream close IO exception", e);
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
							logger.error("Output stream close IO exception", e);
						}
					}
				}
			}
		}
		// Ticket not validated, return empty filename
		return "";
	}
}

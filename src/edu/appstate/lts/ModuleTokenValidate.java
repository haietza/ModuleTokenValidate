package edu.appstate.lts;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.stream.*;
import com.wowza.wms.stream.livepacketizer.ILiveStreamPacketizer;
import com.wowza.wms.rtp.model.*;
import com.wowza.wms.httpstreamer.model.*;
import com.wowza.wms.mediacaster.IMediaCaster;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to validate tokens for file access/playback for Wowza streaming server.
 * 
 * @author Michelle Melton
 * @version Sep 2015
 */
public class ModuleTokenValidate extends ModuleBase implements IMediaStreamNameAliasProvider2 
{
	private ValidateToken validateToken;
	private TimerTask purgeFiles;
	private Timer timer;
	
	/**
	 * Constructor calls ModuleBase constructor.
	 */
	public ModuleTokenValidate() 
	{
		super();
	}
	
	/**
	 * API method for start of application; sets the stream alias based on token validation.
	 * 
	 * @param appInstance 
	 */
	public void onAppStart(IApplicationInstance appInstance) 
	{
		getLogger().info("onAppStart: " + appInstance.getApplication().getName() + "/" + appInstance.getName());
				
		validateToken = new ValidateToken(appInstance);
		
		// Call overridden resolvePlayAlias and play the stream returned from validateToken
		appInstance.setStreamNameAliasProvider(this);
		
		// Purge files thread/task to perform housekeeping on server
		// isDaemon set to true, so shutdown of application is not delayed by Timer
		timer = new Timer(true);
		purgeFiles = new PurgeFiles(appInstance);
		try 
		{
			// Start after 10 seconds, repeat every 3 minutes
			getLogger().info("File purge starting in 10 seconds.");
			timer.schedule(purgeFiles, 10000, 180000);
		}
		catch (IllegalArgumentException e)
		{
			getLogger().error("Delay time was negative.", e);
		}
		catch (IllegalStateException e)
		{
			getLogger().error("Task was already scheduled or cancelled.", e);
		}
		catch (NullPointerException e)
		{
			getLogger().error("Task was null.", e);
		}
	}
	
	/**
	 * On application stop, cancels the timer task to purge files and purges cancelled tasks.
	 * @param appInstance
	 */
	public void onAppStop(IApplicationInstance appInstance) {
		getLogger().info("onAppStop: " + appInstance.getApplication().getName() + "/" + appInstance.getName());
		
		//timer.cancel();
		//timer.purge();
	}
	
	@SuppressWarnings("unchecked")
	public void onConnect(IClient client, RequestFunction function, AMFDataList params) {
		getLogger().info("onConnect: " + client.getClientId());
		
		List<IMediaStream> streams = client.getPlayStreams();
		for (int i = 0; i < streams.size(); i++)
		{
			if (streams.get(i).getName().isEmpty())
			{
				client.rejectConnection("Invalid ticket.");
				client.shutdownClient();
			}
			else
			{
				client.acceptConnection();
			}
		}
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
		getLogger().info("RTMP resolvePlayAlias Wowza values: Token = " + name + ", URL = " 
		    + client.getPageUrl() + ", IP = " + client.getIp());
		
		try 
		{
			return validateToken.validateToken(name, client.getPageUrl(), client.getIp());
		} 
		catch (Exception e) 
		{
			getLogger().error("Validate token RTMP exception", e);

			return "";
		}
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
		getLogger().info("HTTP resolvePlayAlias Wowza values: Token = " + name + ", URL = " 
		    + httpSession.getReferrer() + ", IP = " + httpSession.getIpAddress());
		
		try 
		{
			return validateToken.validateToken(name, httpSession.getReferrer(), httpSession.getIpAddress());
		} 
		catch (Exception e) 
		{
			getLogger().error("Validate token HTTP exception", e);
			
			//httpSession.rejectSession();
			//httpSession.shutdown();
			return "";
		}
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

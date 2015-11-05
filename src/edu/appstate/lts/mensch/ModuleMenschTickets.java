package edu.appstate.lts.mensch;

import com.wowza.wms.amf.AMFDataList;
import com.wowza.wms.application.*;
import com.wowza.wms.client.*;
import com.wowza.wms.module.*;
import com.wowza.wms.request.RequestFunction;
import com.wowza.wms.stream.*;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Class to validate tokens for file access/playback for Wowza streaming server.
 * 
 * @author Michelle Melton
 * @version Sep 2015
 */
public class ModuleMenschTickets extends ModuleBase  
{
	private MenschAliasProvider aliasProvider;
	private TimerTask menschTimerTask;
	private Timer timer;
	
	/**
	 * Constructor calls ModuleBase constructor.
	 */
	public ModuleMenschTickets()
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
						
		// Create alias provider and play the stream returned after validating the token
		aliasProvider = new MenschAliasProvider(appInstance);
		appInstance.setStreamNameAliasProvider(aliasProvider);
		
		// Purge files thread/task to perform housekeeping on server
		// isDaemon set to true, so shutdown of application is not delayed by Timer
		timer = new Timer(true);
		menschTimerTask = new MenschTimerTask(appInstance);
		try 
		{
			// Start after 10 seconds, repeat every 3 minutes
			getLogger().info("File purge starting in 10 seconds.");
			timer.schedule(menschTimerTask, 10000, 180000);
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
}

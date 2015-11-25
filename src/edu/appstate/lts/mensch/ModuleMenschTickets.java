package edu.appstate.lts.mensch;

import com.wowza.wms.application.*;
import com.wowza.wms.module.*;
import java.util.Timer;

/**
 * Wowza Streaming Engine module to validate Appalachian State Mensch stream
 * tickets (query string tokens). Also provides media retrieval from store
 * and cache housekeeping.
 * 
 * @author Michelle Melton
 * @version 0.0.1
 */
public class ModuleMenschTickets extends ModuleBase  
{
	private static final String TIMER_DELAY = "timerDelay";
	private static final String TIMER_PERIOD = "timerPeriod";
	
	// Instance of the timer for periodic housekeeping
	// Create new Timer object once instead of creating a new object every time the application starts
	private Timer timer = new Timer(true);
	
	// Default timer task delay is 10 seconds.
	private long timerDelay = 10 * 1000;
	
	//Default timer task repeating period is 3 minutes;
	private long timerPeriod = 3 * 60 * 1000;
	
	private MenschTimerTask menschTimerTask;
	
	/**
	 * API method for start of application; sets the stream alias based on token validation.
	 * 
	 * @param appInstance 
	 */
	public void onAppStart(IApplicationInstance appInstance) 
	{
		getLogger().info(String.format("onAppStart: %s/%s", appInstance.getApplication().getName(), appInstance.getName()));
						
		// Notify application instance that MenschAliasProvider will provide stream name aliasing
		// to call our implementations of resolvePlayAlias
		appInstance.setStreamNameAliasProvider(new MenschAliasProvider(appInstance));
		
		// Purge files thread/task to perform housekeeping on server
		// isDaemon set to true, so shutdown of application is not delayed by Timer
		menschTimerTask = new MenschTimerTask(appInstance);
				
		try 
		{
			// Get and set timer delay and period from GUI app properties
			timerDelay = appInstance.getProperties().getPropertyInt(TIMER_DELAY, (int) timerDelay) * 1000;
			timerPeriod = appInstance.getProperties().getPropertyInt(TIMER_PERIOD, (int) timerPeriod) * 60 * 1000;
			getLogger().info(String.format("menschTimerTask starting in %d seconds, recurring every %d minutes.", (timerDelay / 1000), (timerPeriod / 60000)));
			timer.schedule(menschTimerTask, timerDelay, timerPeriod);
		}
		catch (IllegalArgumentException e)
		{
			getLogger().error("menschTimerTask", e);
		}
		catch (IllegalStateException e)
		{
			getLogger().error("menschTimerTask", e);
		}
		catch (NullPointerException e)
		{
			getLogger().error("menschTimerTask", e);
		}
	}
	
	/**
	 * On application stop, cancel the timer task to purge files and purge cancelled tasks.
	 * @param appInstance
	 */
	public void onAppStop(IApplicationInstance appInstance) {
		getLogger().info(String.format("onAppStop: %s/%s", appInstance.getApplication().getName(), appInstance.getName()));
		getLogger().info("Cancelling menschTimerTask.");
		menschTimerTask.cancel();
		timer.cancel();
	}
}

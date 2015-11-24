package edu.appstate.lts.mensch;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;

/**
 * Class to perform server housekeeping.
 * 
 * @author Michelle Melton
 * @version 0.0.1
 */
public class MenschTimerTask extends TimerTask 
{
	private static final String TIME_TO_LIVE = "daysToLive";
	private static final String USABLE_SPACE = "usableSpacePercent";
	
	// Set default number of days for files to stay on Wowza server.
	private int timeToLive = 30;
	
	// Set default percentage of usable space to start file purge.
	private int usableSpace = 25;
	
	private IApplicationInstance appInstance;
	private WMSLogger logger;
	private File[] files;
	private List<IMediaStream> mediaStreams;
	Date keepDate;
	Date modified;
	boolean isPlaying;
	
	/**
	 * Constructor to create instance of file purge thread/task.
	 * 
	 * @param appInstance 
	 */
	public MenschTimerTask(IApplicationInstance appInstance)
	{
		this.appInstance = appInstance;
		logger = WMSLoggerFactory.getLoggerObj(appInstance);
	}
	
	/**
	 * Overridden run method for TimerTask abstract class.
	 */
	@Override
	public void run() 
	{
		// Set days for files to stay on server based on GUI config
		timeToLive = appInstance.getProperties().getPropertyInt(TIME_TO_LIVE, timeToLive);
		usableSpace = appInstance.getProperties().getPropertyInt(USABLE_SPACE, usableSpace);
		
		// Traverse content directory to check for files older than TTL
		// If last modified date is more than TTL days ago, delete the file from Wowza
		File directory = new File(appInstance.getStreamStorageDir());
		
		// If usable space is less than GUI config, purge files older than TTL that are not playing
		if ((((double) directory.getUsableSpace() / directory.getTotalSpace()) * 100) < usableSpace)
		{			
			logger.info(String.format("Usable space is less than %d; purging files over %d days old and not playing.", usableSpace, timeToLive));
			
			files = directory.listFiles();
			
			// Get streams currently being played
			mediaStreams = appInstance.getStreams().getStreams();
			
			// Set date to keep files after
			keepDate = new Date();
			keepDate.setTime(keepDate.getTime() - ((long) timeToLive * (long) 86400000));
			
			for (int f = 0; f < files.length; f++)
			{
				isPlaying = false;
				modified = new Date(files[f].lastModified());
				for (int m = 0; m < mediaStreams.size() && !isPlaying; m++)
				{
					if (files[f].getName().equals(mediaStreams.get(m).getName())
						|| files[f].getName().equals(mediaStreams.get(m).getName().substring(0, mediaStreams.get(m).getName().length() - 4).concat(".srt")))
						{
							isPlaying = true;
						}
				}
				if (!isPlaying && modified.before(keepDate))
				{
					logger.info(String.format("Deleting %s: older than TTL and not in use.", files[f].getName()));
					files[f].delete();
				}
				else if (isPlaying)
				{
					logger.info(String.format("Not deleting %s: in use.", files[f].getName()));
				}
				else if (modified.after(keepDate))
				{
					logger.info(String.format("Not deleting %s: newer than TTL.", files[f].getName()));
				}
			}
		}
		
		// If usable space is still less than GUI config, purge files that are not playing
		if ((((double) directory.getUsableSpace() / directory.getTotalSpace()) * 100) < usableSpace)
		{			
			logger.info(String.format("Usable space is still less than %d; purging files not playing.", usableSpace));
			
			files = directory.listFiles();
			
			// Get streams currently being played
			mediaStreams = appInstance.getStreams().getStreams();
			
			for (int f = 0; f < files.length; f++)
			{
				isPlaying = false;
				for (int m = 0; m < mediaStreams.size() && !isPlaying; m++)
				{
					if (files[f].getName().equals(mediaStreams.get(m).getName())
						|| files[f].getName().equals(mediaStreams.get(m).getName().substring(0, mediaStreams.get(m).getName().length() - 4).concat(".srt")))
						{
							isPlaying = true;
						}
				}
				if (!isPlaying)
				{
					logger.info(String.format("Deleting %s: not in use.", files[f].getName()));
					files[f].delete();
				}
				else if (isPlaying)
				{
					logger.info(String.format("Not deleting %s: in use.", files[f].getName()));
				}
			}
		}
	}
}

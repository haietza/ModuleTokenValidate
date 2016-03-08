package edu.appstate.lts.mensch;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
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
	private Path file;
	private BasicFileAttributes attrs;
	private FileTime accessed = attrs.lastAccessTime();
	private Date current;
	private FileTime today;
	private FileTime keep ;
	private List<IMediaStream> mediaStreams;
	private Date keepDate;
	private boolean isPlaying;
	private boolean isRunning = false;
	
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
		isRunning = true;
		
		// Set days for files to stay on server based on GUI config
		timeToLive = appInstance.getProperties().getPropertyInt(TIME_TO_LIVE, timeToLive);
		usableSpace = appInstance.getProperties().getPropertyInt(USABLE_SPACE, usableSpace);
		
		// Traverse content directory to check for files older than TTL
		// If last modified date is more than TTL days ago, delete the file from Wowza
		File directory = new File(appInstance.getStreamStorageDir());
		
		// Testing access file time
		/*
		files = directory.listFiles();
		for (int f = 0; f < files.length; f++)
		{
			Path file = files[f].toPath();
			try {
				BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
				
				// File system of server must have last access enabled
				FileTime accessed = attrs.lastAccessTime();
				
				Date current = new Date();
				FileTime today = FileTime.fromMillis(current.getTime());
				FileTime keep = FileTime.fromMillis((long) timeToLive * (long) 86400000 + accessed.toMillis());
				
				if (keep.compareTo(today) < 0)
				{
					logger.info("File older than 30 days, will be deleted.");
				}
				else
				{
					logger.info("File will be kept.");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/
		
		// If usable space is less than GUI config, purge files older than TTL that are not playing
		if ((((double) directory.getUsableSpace() / directory.getTotalSpace()) * 100) < usableSpace)
		{			
			logger.info(String.format("Usable space under %d percent; purging closed files over %d days old.", usableSpace, timeToLive));
			
			files = directory.listFiles();
			
			// Get streams currently being played
			mediaStreams = appInstance.getStreams().getStreams();
			
			// Set date to keep files after
			keepDate = new Date();
			keepDate.setTime(keepDate.getTime() - ((long) timeToLive * (long) 86400000));
			
			for (int f = 0; f < files.length; f++)
			{
				isPlaying = false;
				
				file = files[f].toPath();
				
				try {
					attrs = Files.readAttributes(file, BasicFileAttributes.class);
					
					// File system of server must have last access enabled
					accessed = attrs.lastAccessTime();
					
					current = new Date();
					today = FileTime.fromMillis(current.getTime());
					keep = FileTime.fromMillis((long) timeToLive * (long) 86400000 + accessed.toMillis());
					
					for (int m = 0; m < mediaStreams.size() && !isPlaying; m++)
					{
						if (files[f].getName().equals(mediaStreams.get(m).getName())
							|| files[f].getName().equals(mediaStreams.get(m).getName().substring(0, mediaStreams.get(m).getName().length() - 4).concat(".srt")))
							{
								isPlaying = true;
							}
					}
					if (!isPlaying && keep.compareTo(today) < 0)
					{
						logger.info(String.format("Deleting %s: closed and older than TTL.", files[f].getName()));
						files[f].delete();
					}
					else if (isPlaying)
					{
						logger.info(String.format("Not deleting %s: open.", files[f].getName()));
					}
					else if (keep.compareTo(today) >= 0)
					{
						logger.info(String.format("Not deleting %s: newer than TTL.", files[f].getName()));
					}
				} catch (IOException e) {
					logger.error("MenschTimerTask run", e);
				}
			}
		}
		else
		{
			logger.info(String.format("Usable space over %d percent.", usableSpace));
		}
		
		// If usable space is still less than GUI config, purge files that are not playing
		if ((((double) directory.getUsableSpace() / directory.getTotalSpace()) * 100) < usableSpace)
		{			
			logger.info(String.format("Usable space still under %d percent; purging closed files.", usableSpace));
			
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
					logger.info(String.format("Deleting %s: closed.", files[f].getName()));
					files[f].delete();
				}
				else if (isPlaying)
				{
					logger.info(String.format("Not deleting %s: open.", files[f].getName()));
				}
			}
		}
		
		isRunning = false;
	}
	
	public boolean isRunning()
	{
		return isRunning;
	}
}

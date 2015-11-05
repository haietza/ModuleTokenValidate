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
 * Class to purge files on Wowza streaming server.
 * 
 * @author Michelle Melton
 * @version Oct 2015
 */
public class PurgeFiles extends TimerTask 
{
	private static final String TIME_TO_LIVE = "time-to-live";
	// Set default number of days for files to stay on Wowza server.
	private int timeToLive = 30;
	
	private IApplicationInstance appInstance;
	private WMSLogger logger;
	private File[] files;
	private Date modified;
	private long age;
	private long longDays;
	private List<IMediaStream> streams;
	
	/**
	 * Constructor to create instance of file purge thread/task.
	 * 
	 * @param appInstance 
	 */
	public PurgeFiles(IApplicationInstance appInstance)
	{
		this.appInstance = appInstance;
		logger = WMSLoggerFactory.getLoggerObj(appInstance);
	}
	
	/**
	 * Overridden run method for TimerTask abstract class.
	 */
	@Override
	public void run() {
		// Set days for files to stay on server based on GUI config
		timeToLive = appInstance.getProperties().getPropertyInt(TIME_TO_LIVE, timeToLive);
		
		// Traverse file directory to check for files last modified days over configured TTL
		// If last modified date is more than TTL days ago, delete the file from Wowza
		File directory = new File(appInstance.getStreamStorageDir());
		
		logger.info("Storage directory: " + directory.getName());
		logger.info("Total space: " + directory.getTotalSpace());
		logger.info("Usable space: " + directory.getUsableSpace());
		
		if (directory.getUsableSpace() < (directory.getTotalSpace() / 4))
		{
			logger.info("Usable space is less than 25% of total space.");
			logger.info("Purging files over " + timeToLive + " days old.");
			
			files = directory.listFiles();
			longDays = (long) timeToLive * 86400000;
			
			// Get streams currently being played
			streams = appInstance.getStreams().getStreams();
			
			// Iterate through current streams to get MP4 filename and SRT filename of each
			for (int i = 0; i < streams.size(); i++)
			{
				logger.info("Current stream " + i + ": " + streams.get(i).getName());
				logger.info("Current stream SRT " + i + ": " + streams.get(i).getName().substring(0,streams.get(i).getName().length() - 4).concat(".srt"));
				
				// Iterate through files in Wowza content directory
				// Check if a file, older than timeToLive, required file, or current stream MP4 or SRT
				for (int j = 0; j < files.length; j++)
				{
					age = files[j].lastModified();
					modified = new Date(age);
					if (age > longDays 
							&& files[j].isFile()
							&& !files[j].getName().equals("wowzalogo.png")
							&& !files[j].getName().equals("sample.mp4")
							&& !files[j].getName().equals(streams.get(i).getName())
							&& !files[j].getName().equals(streams.get(i).getName().substring(0,streams.get(i).getName().length() - 4).concat(".srt")))
					{
						logger.info("File " + files[j].getName() + " last modified " + modified.toString() + ". File will be deleted.");
						logger.info(files[j].getName() + " is " + files[j].length());
						files[j].delete();
					}
					else
					{
						logger.info("File " + files[j].getName() + " last modified " + modified.toString() + ", is in use, or it is required. File will not be deleted.");
						logger.info(files[j].getName() + " is " + files[j].length());
					}
				}
			}
		}
		
		if (directory.getUsableSpace() < (directory.getTotalSpace() / 4))
		{
			logger.info("Usable space is STILL less than 25% of total space.");
			logger.info("Purging all but required and current files.");
			
			files = directory.listFiles();
			streams = appInstance.getStreams().getStreams();
			
			for (int i = 0; i < streams.size(); i++)
			{
				logger.info("Stream " + i + ": " + streams.get(i).getName());
				
				for (int j = 0; j < files.length; j++)
				{
					if (files[j].isFile()
							&& !files[j].getName().equals("wowzalogo.png")
							&& !files[j].getName().equals("sample.mp4")
							&& !files[j].getName().equals(streams.get(i).getName())
							&& !files[j].getName().equals(streams.get(i).getName().substring(0,streams.get(i).getName().length() - 4).concat(".srt")))
					{
						logger.info("File " + files[j].getName() + " will be deleted.");
						logger.info(files[j].getName() + " is " + files[j].length());
						files[j].delete();
					}
					else
					{
						logger.info("File " + files[j].getName() + " is required or in use. File will not be deleted.");
						logger.info(files[j].getName() + " is " + files[j].length());
					}
				}
			}
		}
	}
}

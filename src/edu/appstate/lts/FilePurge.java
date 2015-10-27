package edu.appstate.lts;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;

public class FilePurge extends TimerTask 
{
	private IApplicationInstance appInstance;
	private WMSLogger logger;
	private File[] files;
	private Date modified;
	private Date today;
	private long age;
	private long longDays;
	
	// Set default number of days for files to stay on Wowza server.
	private int timeToLive = 30;
	
	public FilePurge(IApplicationInstance appInstance)
	{
		this.appInstance = appInstance;
		logger = WMSLoggerFactory.getLoggerObj(appInstance);
	}
	
	@Override
	public void run() {
		// Default days for files to stay on server is 30
		timeToLive = appInstance.getProperties().getPropertyInt("timeToLive", timeToLive);
		// Set days for files to stay on server based on GUI config
		timeToLive = appInstance.getProperties().getPropertyInt("validateTTL", timeToLive);
		
		// Traverse file directory to check for files last modified days over configured TTL
		// If last modified date is more than TTL days ago, delete the file from Wowza
		File directory = new File(appInstance.getStreamStorageDir());
		
		logger.info("Storage directory: " + directory.getName());
		logger.info("Total space: " + directory.getTotalSpace());
		logger.info("Usable space: " + directory.getUsableSpace());
		
		if (directory.getUsableSpace() < (directory.getTotalSpace() / 3))
		{
			logger.info("Usable space is less than 30% of total space.");
			
			logger.info("Purging files over " + timeToLive + " days old.");
			files = directory.listFiles();
			longDays = (long) timeToLive * 86400000;
			
			List<IMediaStream> streams = appInstance.getStreams().getStreams();
			for (int i = 0; i < streams.size(); i++)
			{
				logger.info("Stream " + i + ": " + streams.get(i).getName());
				
				for (int j = 0; j < files.length; j++)
				{
					if (age > longDays && files[i].isFile()
							&& !files[j].getName().equals("wowzalogo.png")
							&& !files[j].getName().equals("sample.mp4")
							&& !files[j].getName().equals("access-denied.srt")
							&& !files[j].getName().equals("access-denied.mp4")
							&& !files[j].getName().equals(streams.get(i).getName())
							&& !files[j].getName().equals(streams.get(i).getName().substring(0,streams.get(i).getName().length() - 4).concat(".srt")))
					{
						logger.info("File " + files[j].getName() + " last modified " + modified.toString() + ". File will be deleted.");
						logger.info(files[j].getName() + " is " + files[j].length());
						files[i].delete();
					}
					else
					{
						logger.info("File " + files[j].getName() + " last modified " + modified.toString() + ", is in use, or it is required. File will not be deleted.");
						logger.info(files[j].getName() + " is " + files[j].length());
					}
				}
			}
		}
		
		if (directory.getUsableSpace() < (directory.getTotalSpace() / 3))
		{
			logger.info("Usable space is STILL less than 30% of total space.");
			
			logger.info("Purging all but required and current files.");
			files = directory.listFiles();
			
			List<IMediaStream> streams = appInstance.getStreams().getStreams();
			for (int i = 0; i < streams.size(); i++)
			{
				logger.info("Stream " + i + ": " + streams.get(i).getName());
				
				for (int j = 0; j < files.length; j++)
				{
					if (files[i].isFile()
							&& !files[j].getName().equals("wowzalogo.png")
							&& !files[j].getName().equals("sample.mp4")
							&& !files[j].getName().equals("access-denied.srt")
							&& !files[j].getName().equals("access-denied.mp4")
							&& !files[j].getName().equals(streams.get(i).getName())
							&& !files[j].getName().equals(streams.get(i).getName().substring(0,streams.get(i).getName().length() - 4).concat(".srt")))
					{
						logger.info("File " + files[j].getName() + " will be deleted.");
						logger.info(files[j].getName() + " is " + files[j].length());
						files[i].delete();
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

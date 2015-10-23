package edu.appstate.lts;

import java.io.File;
import java.util.Date;
import java.util.TimerTask;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;

public class FilePurge extends TimerTask 
{
	private IApplicationInstance appInstance;
	private int timeToLive = 30;
	private WMSLogger logger;
	
	public FilePurge(IApplicationInstance appInstance)
	{
		this.appInstance = appInstance;
		logger = WMSLoggerFactory.getLoggerObj(appInstance);
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		// Default days for files to stay on server is 30
		this.timeToLive = appInstance.getProperties().getPropertyInt("timeToLive", this.timeToLive);
		// Set days for files to stay on server based on GUI config
		this.timeToLive = appInstance.getProperties().getPropertyInt("validateTTL", this.timeToLive);
		
		// Traverse file directory to check for files last modified days over configured TTL
		// If last modified date is more than TTL days ago, delete the file from Wowza
		File directory = new File(appInstance.getStreamStorageDir());
		
		logger.info("Storage directory: " + directory.getName());
		logger.info("Total space: " + directory.getTotalSpace());
		logger.info("Usable space: " + directory.getUsableSpace());
		if (directory.getUsableSpace() <= directory.getTotalSpace() / 3)
		{
			logger.info("Usable space is less than 30% of total space.");
			File[] files = directory.listFiles();
			
			for (int i = 0; i < files.length; i++)
			{
				if (files[i].isFile()
					&& !files[i].getName().equals("wowzalogo.png")
					&& !files[i].getName().equals("sample.mp4")
					&& !files[i].getName().equals("access-denied.srt")
					&& !files[i].getName().equals("access-denied.mp4"))
					{
						logger.info("File " + files[i].getName() + " will be deleted.");
						files[i].delete();
					}
					else
					{
						logger.info("File " + files[i].getName() + " is required. File will not be deleted.");
					}
			}
			
			
			/*
			Date modified;
			Date today = new Date();
			long age;
			long longDays = (long) this.timeToLive * 86400000;
			for (int i = 0; i < files.length; i++)
			{
				modified = new Date(files[i].lastModified());
				age = today.getTime() - modified.getTime();
				if (age > longDays && files[i].isFile()
					&& !files[i].getName().equals("wowzalogo.png")
					&& !files[i].getName().equals("sample.mp4")
					&& !files[i].getName().equals("access-denied.srt")
					&& !files[i].getName().equals("access-denied.mp4"))
				{
					logger.info("File " + files[i].getName() + " last modified " + modified.toString() + ". File will be deleted.");
					files[i].delete();
				}
				else
				{
					logger.info("File " + files[i].getName() + " last modified " + modified.toString() + ", or it is required. File will not be deleted.");
				}
			}
			*/
		}
	}
}

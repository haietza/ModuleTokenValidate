package edu.appstate.lts.mensch;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;

import com.wowza.wms.application.IApplicationInstance;
import com.wowza.wms.httpstreamer.model.IHTTPStreamerSession;
import com.wowza.wms.logging.WMSLogger;
import com.wowza.wms.logging.WMSLoggerFactory;
import com.wowza.wms.stream.IMediaStream;

/**
 * Class to purge files on Wowza streaming server.
 * 
 * @author Michelle Melton
 * @version Oct 2015
 */
public class MenschTimerTask extends TimerTask 
{
	private static final String TIME_TO_LIVE = "time-to-live";
	private static final String USABLE_SPACE = "usable-space-percentage";
	// Set default number of days for files to stay on Wowza server.
	private int timeToLive = 30;
	// Set default percentage of usable space to start file purge.
	private int usableSpace = 25;
	
	private IApplicationInstance appInstance;
	private WMSLogger logger;
	private File[] files;
	private Date modified;
	private long age;
	private long longDays;
	private List<IMediaStream> mediaStreams;
	private List<IHTTPStreamerSession> httpStreams;
	
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
	public void run() {
		// Set days for files to stay on server based on GUI config
		timeToLive = appInstance.getProperties().getPropertyInt(TIME_TO_LIVE, timeToLive);
		usableSpace = appInstance.getProperties().getPropertyInt(USABLE_SPACE, usableSpace);
		
		// Traverse file directory to check for files last modified days over configured TTL
		// If last modified date is more than TTL days ago, delete the file from Wowza
		File directory = new File(appInstance.getStreamStorageDir());
		
		logger.info(String.format("Storage directory: %s", directory.getName()));
		logger.info(String.format("Total space: %s", directory.getTotalSpace()));
		logger.info(String.format("Usable space: %s", directory.getUsableSpace()));
		
		if (directory.getUsableSpace() < (directory.getTotalSpace() / (usableSpace / 100)))
		{
			logger.info(String.format("Usable space is less than %d% of total space.", usableSpace));
			logger.info(String.format("Purging files over %d days old.", timeToLive));
			
			files = directory.listFiles();
			longDays = (long) timeToLive * 86400000;
			
			// Get streams currently being played
			mediaStreams = appInstance.getStreams().getStreams();
			httpStreams = appInstance.getHTTPStreamerSessions();
			
			// Iterate through current media streams to get MP4 filename and SRT filename of each
			for (int m = 0, h = 0; m < mediaStreams.size() && h < httpStreams.size(); m++, h++)
			{
				logger.info(String.format("Current media stream %d: %s", m, mediaStreams.get(m).getName()));
				logger.info(String.format("Current HTTP stream %d: %s", h, httpStreams.get(h).getStreamName()));
				logger.info(String.format("Current media stream SRT %d: %s.srt", m, mediaStreams.get(m).getName().substring(0,mediaStreams.get(m).getName().length() - 4)));
				logger.info(String.format("Current HTTP stream SRT %d: %s.srt", h, httpStreams.get(h).getStreamName().substring(0,httpStreams.get(h).getStreamName().length() - 4)));
				
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
							&& !files[j].getName().equals(mediaStreams.get(m).getName())
							&& !files[j].getName().equals(httpStreams.get(h).getStreamName())
							&& !files[j].getName().equals(mediaStreams.get(m).getName().substring(0,mediaStreams.get(m).getName().length() - 4).concat(".srt"))
							&& !files[j].getName().equals(httpStreams.get(h).getStreamName().substring(0,httpStreams.get(h).getStreamName().length() - 4).concat(".srt")))
					{
						logger.info(String.format("File %s last modified %s. File will be deleted.", files[j].getName(), modified.toString()));
						logger.info(String.format("%s is %d", files[j].getName(), files[j].length()));
						files[j].delete();
					}
					else
					{
						logger.info(String.format("File %s last modified %s, is in use, or it is required. File will not be deleted.", files[j].getName(), modified.toString()));
						logger.info(String.format("%s is %d", files[j].getName(), files[j].length()));
					}
				}
			}
		}
		
		if (directory.getUsableSpace() < (directory.getTotalSpace() / 4))
		{
			logger.info("Usable space is STILL less than 25% of total space.");
			logger.info("Purging all but required and current files.");
			
			// Get streams currently being played
			mediaStreams = appInstance.getStreams().getStreams();
			httpStreams = appInstance.getHTTPStreamerSessions();
			
			// Iterate through current media streams to get MP4 filename and SRT filename of each
			for (int m = 0, h = 0; m < mediaStreams.size() && h < httpStreams.size(); m++, h++)
			{
				logger.info(String.format("Current media stream %d: %s", m, mediaStreams.get(m).getName()));
				logger.info(String.format("Current HTTP stream %d: %s", h, httpStreams.get(h).getStreamName()));
				logger.info(String.format("Current media stream SRT %d: %s.srt", m, mediaStreams.get(m).getName().substring(0,mediaStreams.get(m).getName().length() - 4)));
				logger.info(String.format("Current HTTP stream SRT %d: %s.srt", h, httpStreams.get(h).getStreamName().substring(0,httpStreams.get(h).getStreamName().length() - 4)));
				
				// Iterate through files in Wowza content directory
				// Check if a file, older than timeToLive, required file, or current stream MP4 or SRT
				
				for (int j = 0; j < files.length; j++)
				{
					if (files[j].isFile()
						&& !files[j].getName().equals("wowzalogo.png")
						&& !files[j].getName().equals("sample.mp4")
						&& !files[j].getName().equals(mediaStreams.get(m).getName())
						&& !files[j].getName().equals(httpStreams.get(h).getStreamName())
						&& !files[j].getName().equals(mediaStreams.get(m).getName().substring(0,mediaStreams.get(m).getName().length() - 4).concat(".srt"))
						&& !files[j].getName().equals(httpStreams.get(h).getStreamName().substring(0,httpStreams.get(h).getStreamName().length() - 4).concat(".srt")))
					{
						logger.info(String.format("File %s will be deleted.", files[j].getName()));
						logger.info(String.format("%s is %d.", files[j].getName(), files[j].length()));
						files[j].delete();
					}
					else
					{
						logger.info(String.format("File %s is required or in use. File will not be deleted.", files[j].getName()));
						logger.info(String.format("%s is %d.", files[j].getName(), files[j].length()));
					}
				}
			}
		}
	}
}

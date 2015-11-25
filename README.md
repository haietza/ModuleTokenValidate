# wowza-ModuleMenschTickets

This module calls a ticket web service with a token to get a hash value, a publishing outlet, a transcript, an IP address, and a URL for a valid media request. 
It then validates the IP address and URL information obtained from the Wowza server play request against the information obtained from the ticket web service.
If the validation fails, the client or session is shutdown.
If the validation passes, and the media file exists on the Wowza server, the content is sent to the player.
If the media file does not exist, the Appalachian State Mensch store web service is called to retrieve the media file.

The module also includes a file purge thread/task for server housekeeping.

The storage directory should be configured as sub-directory of /content in application.

This module is tested to work for both RTMP/Flash and HTTP streaming for iOS.

Module Custom Properties

Configurable file purge days to live
* Path: /Root/Application
* Name: daysToLive
* Type: Integer
* Value: [number of days for files to stay on the Wowza server]
The default value is 30 days.

Configurable file purge usable space
* Path: /Root/Application
* Name: usableSpacePercent
* Type: Integer
* Value: [usable space as percentage of total space when file purging should take place]
The default value is 25%.

Configurable file purge task delay
* Path: /Root/Application
* Name: taskDelay
* Type: Integer
* Value: [delay in seconds before file purge task starts]
The default value is 10 seconds.

Configurable file purge task period
* Path: /Root/Application
* Name: taskPeriod
* Type: Integer
* Value: [period in minutes for the repeated task execution]
The default value is 3 minutes.

Configurable ticket web service URL
* Path: /Root/Application
* Name: ticketServiceUrl
* Type: String
* Value: [URL to ticket web service]
The ticket token will be appended to the ticket web service URL. Include trailing slash.

Configurable store web service URL
* Path: /Root/Application
* Name: storeServiceUrl
* Type: String
* Value: [URL to store web service]
The media hash value will be appended to the store web service URL. Include trailing slash.
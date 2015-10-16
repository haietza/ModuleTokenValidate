# wowza-ModuleTokenValidate

This module calls a ticket web service with a token to get a hash value, a publishing outlet, a transcript, an IP address, and a URL for a valid media request. 
It then validates the IP address and URL information obtained from the Wowza server play request against the information obtained from the ticket web service.
If the validation fails, an access denied video is sent to the player.
If the validation passes, and the media file exists on the Wowza server, the content is sent to the player.
If the media file does not exist, the store web service is called to retrieve the media file.

Module Custom Properties

Configurable file purge
* Path: /Root/Application
* Name: validateTTL
* Type: Integer
* Value: [desired number of days for files to stay on the Wowza server]
The default value is 30 days.

Configurable ticket web service URL
* Path: /Root/Application
* Name: validateTicketURL
* Type: String
* Value: [URL to ticket web service]
The ticket token will be appended to the ticket web service URL.

Configurable store web service URL
* Path: /Root/Application
* Name: validateStoreURL
* Type: String
* Value: [URL to store web service]
The media hash value will be appended to the store web service URL.

This module is tested to work for both RTMP/Flash and HTTP streaming for iOS.
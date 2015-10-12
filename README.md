# wowza-ModuleTokenValidate

This module calls a ticket web service with a token to get a mediaHashValue, a scriptHashValue, an IP address, and a URL for a valid media request. 
It then validates the IP address and URL information obtained from the Wowza server play request against the information obtained from the ticket web service.
If the validation fails, an access denied video is sent to the player.
If the validation passes, and the media file exists on the Wowza server, the content is sent to the player.
If the media file does not exist, the Mensch store web service is called to retrieve both the media file and the SRT transcription file, if it exists.

An additional component of the module is a configurable file purge. In the Wowza Application Custom Properties, add the following to set the configurable number of days for a file to be kept on the Wowza server:

* Path: /Root/Application
* Name: validateTTL
* Type: Integer
* Value: [desired number of days for files to stay on the Wowza server]

The default value is 30 days.

This module is tested to work for both RTMP/Flash and HTTP streaming for iOS.
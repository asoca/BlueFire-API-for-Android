# BlueFire-API-for-Android
Android API for direct connection to the BlueFire J1939/J1708 Bluetooth Data Adapters. Documentation is available upon request from [BlueFire Support](mailto:support@bluefire.llc.com).

Version 1:<ul>
	<li>Initial version.
</ul>
	
Version 2:<ul>
	<li>Code updates.
</ul>
	
Version 3:<ul>
	<li>Handles Adapter looping errors.
</ul>
	
Version 4:<ul>
	<li>Added Adapter Name and Password retrieval and update.
	<li>Connects to any adapter that starts with the Adapter Name.
	<li>Reboots the adapter on receiving adapter errors (firmware version 2.12).
	<li>Supports Adapter Firmware 2.13.
</ul>
	
Version 5:<ul>
	<li>Added Adapter Name update to Main page.
	<li>Added Truck Make, Model, Serial No, and Unit No to Next Pages.
	<li>Added App Settings class for BlueFire settings.
	<li>Added rebooting the adapter when disconnecting.
	<li>Added LastConnectedId and ConnectToLastAdapter Bluetooth settings.
	<li>Added Incompatible version check.</li>
	<ul>
		<li>Adapter Firmware 2.7 and less</li>
		<li>Adapter Firmware 3.0 and greater</li>
	</ul>
</ul>
	
Version 6:<ul>
	<li>Created an API document. Contact BlueFire Support for a copy.
	<li>Removed exposure to the Comm, J1939, and J1587 classes, and, moved all properties and methods to the Adapter class.
	<li>Added option to set the Interval on retrieving truck data (default is on change of data). This is useful when the data is coming in too fast (ie. RPM) and you want to slow it down.
	<li>Added SendPGN method and PGNData property for sending non-API defined PGNs.
	<li>Added sample code for SendPGN and MonitorPGN.
	<li>Added commons-codec-1.10.jar to the project libs folder. This is only required for the API Demo app.
	<li>Added a projects docs folder that contains the commons javadoc files. You must set the Javadoc Location project property to point to this folder.
</ul>
	
	

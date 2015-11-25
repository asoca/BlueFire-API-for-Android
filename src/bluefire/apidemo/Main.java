package bluefire.apidemo;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;

import com.bluefire.api.Adapter;
import com.bluefire.api.Truck;
import com.bluefire.api.Const;
import com.bluefire.api.Helper;
import com.bluefire.api.Adapter.ConnectionStates;

public class Main extends Activity 
{
    // Form controls	
	private TextView textView1;
	private TextView textView2;
	private TextView textView3;
	private TextView textView4;
	private TextView textView5;
	private TextView textView6;
	private TextView textView7;
	
	private TextView dataView1;
	private TextView dataView2;
	private TextView dataView3;
	private TextView dataView4;
	private TextView dataView5;
	private TextView dataView6;
	private TextView dataView7;
	
	private TextView textStatus;    
	private TextView textFaultCode;
	private EditText textLedBrightness;
	private EditText textAdapterName;
	private EditText textPGN;
	private EditText textPGNData;
	private CheckBox checkJ1939;
	private CheckBox checkJ1708;
	private Button buttonConnect;
	private Button buttonReset;
	private Button buttonUpdate;
	private Button buttonSendMonitor;
	private TextView textHeartbeat;
    
    private boolean isConnecting;
    private boolean isConnected;
    
    private int ledBrightness;
    private String adapterName = "";
	
    private int pgn;
    private boolean isSendingPGN;
    private boolean isMonitoringPGN;

    private boolean ignoreJ1939;
    private boolean ignoreJ1708;

	private int faultIndex;
	
	private int groupNo;
	private static final int maxGroupNo = 5;
    
    private Timer connectTimer;
    
    private ConnectionStates ConnectionState = ConnectionStates.NotConnected;

    // BlueFire adapter
    private Adapter blueFire;

    private Settings appSettings;
    
    private class Settings
    {
    	private static final String preferenceName = "BlueFire Demo";
    	
    	public String adapterName = blueFire.DefaultName;
    	
        public int sleepMode;
    	public int ledBrightness = 100;
    	
        public boolean ignoreJ1939 = false;
        public boolean ignoreJ1708 = false;
        
        public String lastConnectedId = "";
        public boolean connectToLastAdapter = false;
        
        public int discoveryTimeOut = 5 * Const.OneSecond;
        public int maxConnectRetrys;
       
        public void getSettings()
        {
        	SharedPreferences settings = getSharedPreferences(preferenceName, 0);

        	adapterName = settings.getString("adapterName", blueFire.DefaultName);
       	
        	sleepMode = settings.getInt("sleepMode", 0);
        	ledBrightness = settings.getInt("ledBrightness", 100);

        	ignoreJ1939 = settings.getBoolean("ignoreJ1939", false);
        	ignoreJ1708 = settings.getBoolean("ignoreJ1708", true); // set to true to improve performance

        	lastConnectedId = settings.getString("lastConnectedId", "");
        	connectToLastAdapter = settings.getBoolean("connectToLastAdapter", false);

        	discoveryTimeOut = settings.getInt("discoveryTimeOut", 5 * Const.OneSecond);
        	maxConnectRetrys = settings.getInt("maxConnectRetrys", 0);
        }
        
        public void saveSettings()
        {
        	SharedPreferences preferences = getSharedPreferences(preferenceName, MODE_PRIVATE);
        	SharedPreferences.Editor edit= preferences.edit();
        	
        	edit.putString("adapterName", adapterName);

          	edit.putInt("sleepMode", sleepMode);
        	edit.putInt("ledBrightness", ledBrightness);
        	
        	edit.putBoolean("ignoreJ1939", ignoreJ1939);
        	edit.putBoolean("ignoreJ1708", ignoreJ1708);
        	
        	edit.putString("lastConnectedId", lastConnectedId);
        	edit.putBoolean("connectToLastAdapter", connectToLastAdapter);

        	edit.putInt("discoveryTimeOut", discoveryTimeOut);
        	edit.putInt("maxConnectRetrys", maxConnectRetrys);

        	edit.commit();    
        }
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.main);
		
		blueFire = new Adapter(this, adapterHandler);
		
		this.setTitle("API Demo v-" + blueFire.Version);

        // Set to use an insecure connection.
        // Note, there are other Android devices that require this other than just ICS (4.0.x).
        if (android.os.Build.VERSION.RELEASE.startsWith("4.0."))
            blueFire.SetUseInsecureConnection(true);
        else
            blueFire.SetUseInsecureConnection(false);   	

        // Get app settings
        appSettings = new Settings();

        // Initialize adapter properties
        initializeAdapter();
        
        // Initialize the app startup form
        initializeForm();
	}
	
	private void initializeAdapter()
	{
        appSettings.getSettings();

        // Set the adapter name
		blueFire.AdapterName = appSettings.adapterName;
        
        // Set the Bluetooth discovery timeout.
        // Note, depending on the number of Bluetooth devices present on the mobile device,
        // discovery could take a long time.
        // Note, if this is set to a high value, the app needs to provide the user with the
        // capability of canceling the discovery.
        blueFire.SetDiscoveryTimeOut(appSettings.discoveryTimeOut);
        
        // Set number of Bluetooth connection attempts.
        // Note, if the mobile device does not connect, try setting this to a value that 
        // allows for a consistent connection. If you're using multiple adapters and have 
        // connection problems, un-pair all devices before connecting.
        blueFire.SetMaxConnectRetrys(appSettings.maxConnectRetrys);
        
        // Get the Bluetooth last connection id and the connect to last adapter setting
        blueFire.SetLastConnectedId(appSettings.lastConnectedId);
        blueFire.SetConnectToLastAdapter(appSettings.connectToLastAdapter);
		
		// Set to ignore data bus settings
		blueFire.SetIgnoreJ1939(appSettings.ignoreJ1939);
		blueFire.SetIgnoreJ1708(appSettings.ignoreJ1708);
	}

	private void initializeForm()
	{
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);
        textView6 = (TextView) findViewById(R.id.textView6);
        textView7 = (TextView) findViewById(R.id.textView7);
        
        dataView1 = (TextView) findViewById(R.id.dataView1);
        dataView2 = (TextView) findViewById(R.id.dataView2);
        dataView3 = (TextView) findViewById(R.id.dataView3);
        dataView4 = (TextView) findViewById(R.id.dataView4);
        dataView5 = (TextView) findViewById(R.id.dataView5);
        dataView6 = (TextView) findViewById(R.id.dataView6);
        dataView7 = (TextView) findViewById(R.id.dataView7);
        
        textStatus = (TextView) findViewById(R.id.textStatus);
        textFaultCode = (TextView) findViewById(R.id.textFaultCode);
        textLedBrightness = (EditText) findViewById(R.id.textLedBrightness);
        textAdapterName = (EditText) findViewById(R.id.textAdapterName);
        textPGN = (EditText) findViewById(R.id.textPGN);
        textPGNData = (EditText) findViewById(R.id.textPGNData);
        
        checkJ1939 = (CheckBox) findViewById(R.id.checkJ1939);
        checkJ1708 = (CheckBox) findViewById(R.id.checkJ1708);
        buttonConnect = (Button) findViewById(R.id.buttonConnect);
        buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonUpdate = (Button) findViewById(R.id.buttonUpdate);
        buttonSendMonitor = (Button) findViewById(R.id.buttonSendMonitor);
        textHeartbeat = (TextView) findViewById(R.id.textHeartbeat);
        
        clearForm();
       
        buttonConnect.setEnabled(true);
        buttonReset.setEnabled(false);
        buttonUpdate.setEnabled(false);
        textStatus.setText("Not Connected");
    	
        buttonConnect.setFocusable(true);
        buttonConnect.setFocusableInTouchMode(true);
        buttonConnect.requestFocus();
	}
	
	private void clearForm()
	{
		ShowTruckText();
		
		// Clear form
		dataView1.setText("0");
		dataView2.setText("0");
		dataView3.setText("0");
		dataView4.setText("0");
		dataView5.setText("0");
		dataView6.setText("0");
		dataView7.setText("NA");
		
		textFaultCode.setText("NA");
		textLedBrightness.setText("0");
		textAdapterName.setText("");
		textHeartbeat.setText("0");
		checkJ1939.setChecked(false);
		checkJ1708.setChecked(false);
		
		faultIndex = -1;
		ledBrightness = -1;
		
		ignoreJ1939 = false;
		ignoreJ1708 = false;
    	
    	// Show user settings
        textAdapterName.setText(appSettings.adapterName);
        textLedBrightness.setText(String.valueOf(appSettings.ledBrightness));
		checkJ1939.setChecked(!appSettings.ignoreJ1939); // checkJ1939 is the opposite of ignoreJ1939
		checkJ1708.setChecked(!appSettings.ignoreJ1708); // checkJ1708 is the opposite of ignoreJ1708
	}

	// Connect button
	public void onConnectClick(View view) 
	{
        if (!isConnected)
        {
        	clearForm();
    		
    		isConnecting = true;
    		isConnected = false;
       	
            textStatus.setText("Connecting...");
            
            checkJ1939.setEnabled(false);
            checkJ1708.setEnabled(false);
            
            buttonConnect.setEnabled(false);
            buttonReset.setEnabled(false);
            buttonUpdate.setEnabled(false);
            
            connectTimer = new Timer();
            connectTimer.schedule(new ConnectAdapter(), 1, Long.MAX_VALUE);
       }
        else
        	DisconnectAdapter();
	}
	
	private class ConnectAdapter extends TimerTask
	{
        @Override
        public void run()
        {
            // Initialize adapter properties (in case they were changed)
        	initializeAdapter();
        	
			blueFire.Connect(); // this is a blocking call
			connectTimer.cancel();
        }
	};

	private void DisconnectAdapter()
	{
		try
		{
	        buttonConnect.setEnabled(false);
	        buttonReset.setEnabled(false);
	        buttonUpdate.setEnabled(false);
	        
	        blueFire.Disconnect(true);
		}
		catch(Exception e) {}
	}
	
	private void AdapterConnected()
	{
	   	isConnected = true;
	   	isConnecting = false;

	   	buttonConnect.setText("Disconnect");
        
        buttonConnect.setEnabled(true);
        buttonUpdate.setEnabled(true);
    	
    	buttonConnect.requestFocus();
    	
		// Save settings
		appSettings.lastConnectedId = blueFire.GetLastConnectedId();
		appSettings.saveSettings();
        
		getData();
	}
	
	private void AdapterNotConnected()
	{
	   	isConnected = false;
	   	isConnecting = false;

        buttonConnect.setText("Connect");
        
        checkJ1939.setEnabled(true);
        checkJ1708.setEnabled(true);
        
        buttonConnect.setEnabled(true);
        buttonUpdate.setEnabled(false);
    	
    	buttonConnect.requestFocus();
       
        ShowStatus();
	}

    private void AdapterReconnecting()
    {
    	isConnected = false;
		isConnecting = true;
    	
        buttonConnect.setEnabled(false);
        buttonUpdate.setEnabled(false);

        LogNotifications("App reconnecting to the Adapter. Reason is " + blueFire.ReconnectReason + ".");
        
		Toast.makeText(this, "Lost connection to the Adapter.", Toast.LENGTH_LONG).show();
         
    	Toast.makeText(this, "Attempting to reconnect.", Toast.LENGTH_LONG).show();
    }
 
    private void AdapterReconnected()
    {
        // Note, AdapterConnected will be called when State changes to Connected.
 
        Toast.makeText(this, "Adapter reconnected.", Toast.LENGTH_LONG).show();
    }

    private void AdapterNotReconnected()
    {
        AdapterNotConnected();
        
        Toast.makeText(this, "Adapter did not reconnect.", Toast.LENGTH_LONG).show();
    }

	// Next Group click
    public void onNextGroupClick(View view)
    {
    	groupNo++;
    	if (groupNo > maxGroupNo)
    		groupNo = 0;
    	
    	ShowTruckText();    	
    }
    
	// Fault Text click
	public void onFaultClick(View view) 
	{	
		ShowFault();
	}
   
	// Reset button
	public void onResetClick(View view) 
	{	
		blueFire.ResetFaults();
	}
	
	// Update button
	public void onUpdateClick(View view) 
	{
		// Update LED Brightness
		int ledBrightness = -1;
		try
		{
			ledBrightness = Integer.parseInt(textLedBrightness.getText().toString().trim());
		}
		catch(Exception e){}
		
		if (ledBrightness < 1 || ledBrightness > 100)
		{
            Toast.makeText(this, "Led Brightness must be between 1 and 100", Toast.LENGTH_LONG).show();
            return;
		}
		if (ledBrightness != blueFire.LedBrightness)
		{
			blueFire.SetLedBrightness(ledBrightness);
	        Toast.makeText(this, "LED Brightness updated.", Toast.LENGTH_SHORT).show();
		}
		
		// Update Adapter Name
		String adapterName = textAdapterName.getText().toString().trim();
		if (adapterName.length() > 20)
		{
            Toast.makeText(this, "Adapter Name must be less than 20 characters.", Toast.LENGTH_LONG).show();
            return;
		}
		if (adapterName.equals(""))
			adapterName = blueFire.DefaultName;
		
		if (!adapterName.equals(blueFire.AdapterName))
		{
			blueFire.SetAdapterName(adapterName);	
			Toast.makeText(this, "Adapter Name updated.", Toast.LENGTH_SHORT).show();
		}
		
		// Save settings
		appSettings.ledBrightness = ledBrightness;
		appSettings.adapterName = adapterName;
		appSettings.saveSettings();
	}
	
	// Send/Monitor button
	public void onSendMonitorClick(View view) 
	{
		isSendingPGN = false;
		isMonitoringPGN = false;
		
		// Get PGN
		pgn = -1;
		try
		{
			pgn = Integer.parseInt("0"+textPGN.getText().toString().trim());
		}
		catch(Exception e){}
		
		if (pgn < 0)
		{
            Toast.makeText(this, "PGN must be numeric.", Toast.LENGTH_LONG).show();
            return;
		}
		
		// Ignore if no PGN
		if (pgn == 0)
			return;
		
		// Get PGN Data
	    byte[] pgnBytes = new byte[8];
	    
		String pgnData = textPGNData.getText().toString().trim();
		
		if (pgnData.length() == 0) // Monitor a PGN
		{
			int source = 0; // engine
			isMonitoringPGN = true;
			blueFire.MonitorPGN(source, pgn);
		}
		else // Send a PGN
		{
			// Edit the PGN Data to be 16 hex characters (8 bytes)
			if (pgnData.length() != 16) 
			{
	            Toast.makeText(this, "PGN Data must be 16 hex characters (8 bytes).", Toast.LENGTH_LONG).show();
	            return;
			}
			
			// Convert the PGN Data hex string to bytes
		    try 
		    {
				pgnBytes = Hex.decodeHex(pgnData.toCharArray());
			} catch (Exception e) {}
		    
			// Send the PGN
			isSendingPGN = true;
			blueFire.SendPGN(pgn,  pgnBytes);
		}
	}

	public void onJ1939Click(View view)
	{
		// Set to ignore J1939 (opposite of checkJ1939)
		ignoreJ1939 = !checkJ1939.isChecked();
		
		// Save settings
		appSettings.ignoreJ1939 = ignoreJ1939;
		appSettings.saveSettings();
		
		// Update BlueFire
		blueFire.SetIgnoreJ1939(ignoreJ1939);
	}

	public void onJ1708Click(View view)
	{
		// Set to ignore J708 (opposite of checkJ1708)
		ignoreJ1708 = !checkJ1708.isChecked();
		
		// Save settings
		appSettings.ignoreJ1708 = ignoreJ1708;
		appSettings.saveSettings();
		
		// Update BlueFire
		blueFire.SetIgnoreJ1708(ignoreJ1708);
	}
	
    // Data Changed Handler from the BlueFire Adapter
	private final Handler adapterHandler = new Handler() 
	{
		@Override
		@SuppressLint("HandlerLeak")
		public void handleMessage(Message msg) 
		{
			try 
			{
				ShowStatus();
				switch (blueFire.ConnectionState)
				{
					case NotConnected:
						// Initial state only
						break;
						
					case Connecting:
						// Status only
						if (blueFire.IsReconnecting)
							if (!isConnecting)
								AdapterReconnecting();
						break;
						
					case Connected:
						// Status only
						break;
						
					case AdapterConnected:
						if (!isConnected)
		                	AdapterConnected();
						break;
						
					case Disconnecting:
						// Status only
						break;
					
					case Disconnected:
						if (isConnected)
							AdapterNotConnected();
						break;
						
					case Reconnecting:
						if (!isConnecting)
							AdapterReconnecting();
						break;
						
					case Reconnected:
						if (isConnecting)
							AdapterReconnected();
						break;
						
					case NotReconnected:
						if (isConnecting)
							AdapterNotReconnected();
						break;
						
					case DataError:
						// Ignore, handled by Reconnecting
						break;
						
					case CommTimeout:
					case ConnectTimeout:
					case AdapterTimeout:
						if (isConnecting || isConnected)
						{
							blueFire.Disconnect();
							AdapterNotConnected();
							ShowMessage("Adapter Connection", "Adapter Timed Out");
						}
						break;
						
					case SystemError:
						if (isConnecting || isConnected)
						{
							blueFire.Disconnect();
							AdapterNotConnected();
							ShowSystemError();
						}
						break;
						
					case DataChanged:
						ShowData();
				}
				
				// Check reset button enable
				if (!isConnected)
		          	buttonReset.setEnabled(false); // because it's enabled in ShowData
			} 
			catch (Exception e) {} 
		}
	};
	
    // Start retrieving data after connecting to the adapter
    private void getData()
    {        
		// Note, version has already been retrieved.
		// Check for an incompatible version.
    	if (blueFire.IsVersionIncompatible)
    	{
            Toast.makeText(this, "The BlueFire Adapter is not compatible with this API.", Toast.LENGTH_LONG).show();
            DisconnectAdapter();
            return;
    	}
    	
       	blueFire.GetAdapterName(); // BlueFire or User Name
       	blueFire.GetPassword(); // User Password
    	
       	blueFire.GetSleepMode(); // Adapter Sleep Mode
      	blueFire.GetLedBrightness(); // Adapter LED Brightness
       	blueFire.GetMessages(); // Any Adapter Error Messages
     	
      	blueFire.GetVehicleData(); // VIN, Make, Model, Serial no
      	
     	blueFire.GetEngineData1(); // RPM, Percent Torque, Driver Torque, Torque Mode
     	blueFire.GetEngineData2(); // Percent Load, Accelerator Pedal Position
     	blueFire.GetEngineData3(); // Vehicle Speed, Max Set Speed, Brake Switch, Clutch Switch, Park Brake Switch, Cruise Control Settings and Switches
     	
      	blueFire.GetTemps(); // Oil Temp, Coolant Temp, Intake Manifold Temperature
      	blueFire.GetOdometer(); // Odometer (Engine Distance)
      	blueFire.GetFuelData(); // Fuel Used, Idle Fuel Used, Fuel Rate, Instant Fuel Economy, Avg Fuel Economy, Throttle Position
      	blueFire.GetBrakeData(); // Application Pressure, Primary Pressure, Secondary Pressure
      	blueFire.GetPressures(); // Oil Pressure, Coolant Pressure, Intake Manifold(Boost) Pressure
      	blueFire.GetEngineHours(); // Total Engine Hours, Total Idle Hours
      	blueFire.GetCoolantLevel(); // Coolant Level
      	blueFire.GetBatteryVoltage(); // Battery Voltage
      	
      	blueFire.GetFaults(); // Any Engine Faults
    }

	private void ShowStatus()
	{
		// Check for a change of the connection state
		if (ConnectionState != blueFire.ConnectionState)
		{
			ConnectionState = blueFire.ConnectionState;
        	textStatus.setText(ConnectionState.toString());
		}
		
        // Show any error message from the adapter
    	if (blueFire.NotificationMessage != "")
    	{
    		LogNotifications(blueFire.NotificationMessage);
    		blueFire.NotificationMessage = "";
    	}
	}
	
    private void LogNotifications(String Notification)
    {
 		Log.d("BlueFire", Notification);
     }
	
	private void ShowData()
	{       
        // Show truck data
        if (blueFire.IsTruckDataChanged)
        {
        	ShowTruckData();
        }
       
        if (blueFire.IsFaultDataChanged)
        {
	        if (Truck.FaultCount == 0)
	        {
	        	textFaultCode.setText("");
	          	buttonReset.setEnabled(false);
	        }
	        else
	        {
	        	if (faultIndex < 0) // show first fault only once.
	        	{
	        		faultIndex = 0;
		        	ShowFault();
		          	buttonReset.setEnabled(true);
	        	}
	        }
        }
        
        // Only change an input field if data has changed
         if (ledBrightness != blueFire.LedBrightness)
         {
        	ledBrightness = Integer.parseInt(textLedBrightness.getText().toString());
        	textLedBrightness.setText(String.valueOf(blueFire.LedBrightness));
         }
         
         // Only change an input field if data has changed
          if (!adapterName.equals(blueFire.AdapterName))
          {
        	adapterName = textAdapterName.getText().toString().trim();
        	textAdapterName.setText(String.valueOf(blueFire.AdapterName));
          }
         
         // Only change an input field if data has changed
          if (!ignoreJ1939 != blueFire.GetIgnoreJ1939())
          {
        	  ignoreJ1939 = blueFire.GetIgnoreJ1939();
        	  checkJ1939.setChecked(!ignoreJ1939); // checkJ1939 is the opposite of ignoreJ1939
          }
          if (!ignoreJ1708 != blueFire.GetIgnoreJ1708())
          {
        	  ignoreJ1708 = blueFire.GetIgnoreJ1708();
        	  checkJ1708.setChecked(!ignoreJ1708); // checkJ1708 is the opposite of ignoreJ1708
          }
          
          // Check for SendPGN response
          if ((isSendingPGN || isMonitoringPGN) && blueFire.PGNData.PGN== pgn)
          {
        	  isSendingPGN = false; // only show sending data once
        	  textPGNData.setText(new String(Hex.encodeHex(blueFire.PGNData.Data)).toUpperCase());       	  
           }
         
         // Show heartbeat
         textHeartbeat.setText(String.valueOf(blueFire.HeartbeatCount));
   }
	
	private void ShowTruckText()
	{
        switch (groupNo)
        {
        case 0:
            textView1.setText("RPM");
            textView2.setText("Speed");
            textView3.setText("Max Speed");
            textView4.setText("HiRes Max");
            textView5.setText("Accel Pedal");
            textView6.setText("Throttle Pos");
            textView7.setText("VIN");
         	break;
        	
        case 1:
            textView1.setText("Distance");
            textView2.setText("Odometer");
            textView3.setText("Total Hours");
            textView4.setText("Idle Hours");
            textView5.setText("Brake Pres");
            textView6.setText("Brake Air");
            textView7.setText("Make");
         	break;
        	
        case 2:
            textView1.setText("Fuel Rate");
            textView2.setText("Fuel Used");
            textView3.setText("HiRes Fuel");
            textView4.setText("Idle Fuel Used");
            textView5.setText("Avg Fuel Econ");
            textView6.setText("Inst Fuel Econ");
            textView7.setText("Model");
         	break;
        	
        case 3:
            textView1.setText("Pct Load");
            textView2.setText("Pct Torque");
            textView3.setText("Driver Torque");
            textView4.setText("Torque Mode");
            textView5.setText("Intake Temp");
            textView6.setText("Intake Pres");
            textView7.setText("Serial No");
         	break;
        	
        case 4:
            textView1.setText("Oil Temp");
            textView2.setText("Oil Pressure");
            textView3.setText("Coolant Temp");
            textView4.setText("Coolant Level");
            textView5.setText("Coolant Pres");
            textView6.setText("Battery Volts");
            textView7.setText("Unit No");
         	break;
        	
        case 5:
            textView1.setText("Brake Switch");
            textView2.setText("Clutch Switch");
            textView3.setText("Park Switch");
            textView4.setText("Cruise Switch");
            textView5.setText("Cruise Speed");
            textView6.setText("Cruise State");
            textView7.setText("VIN");
         	break;
        }
	}

	private void ShowTruckData()
	{
        switch (groupNo)
        {
        case 0:
            dataView1.setText(String.valueOf(Truck.RPM));
            dataView2.setText(roundString(Truck.Speed * Const.KphToMph,0));
            dataView3.setText(roundString(Truck.MaxSpeed * Const.KphToMph,0));
            dataView4.setText(roundString(Truck.HiResMaxSpeed * Const.KphToMph,0));
            dataView5.setText(roundString(Truck.AccelPedal,2));
            dataView6.setText(roundString(Truck.ThrottlePos,2));
            dataView7.setText(Truck.VIN);       
        	break;
        	
        case 1:
            dataView1.setText(roundString(Truck.Distance * Const.KmToMiles,0));
            dataView2.setText(roundString(Truck.Odometer * Const.MetersToMiles,0)); // HiRes Distance
            dataView3.setText(roundString(Truck.TotalHours,2));
            dataView4.setText(roundString(Truck.IdleHours,2));
            dataView5.setText(roundString(Truck.BrakeAppPressure * Const.kPaToPSI,2));
            dataView6.setText(roundString(Truck.Brake1AirPressure * Const.kPaToPSI,2));
            dataView7.setText(Truck.Make);       
         	break;
        	
        case 2:
            dataView1.setText(roundString(Truck.FuelRate * Const.LphToGalPHr,2));
            dataView2.setText(roundString(Truck.FuelUsed * Const.LitersToGal,2));
            dataView3.setText(roundString(Truck.HiResFuelUsed * Const.LitersToGal,2));
            dataView4.setText(roundString(Truck.IdleFuelUsed * Const.LitersToGal,2));
            dataView5.setText(roundString(Truck.AvgFuelEcon * Const.KplToMpg,2));
            dataView6.setText(roundString(Truck.InstFuelEcon * Const.KplToMpg,2));
            dataView7.setText(Truck.Model);       
         	break;
        	
        case 3:
            dataView1.setText(String.valueOf(Truck.PctLoad));
            dataView2.setText(String.valueOf(Truck.PctTorque));
            dataView3.setText(String.valueOf(Truck.DrvPctTorque));
            dataView4.setText(String.valueOf(Truck.TorqueMode));
            dataView5.setText(roundString(Helper.CelciusToFarenheit(Truck.IntakeTemp),2));
            dataView6.setText(roundString(Truck.IntakePressure * Const.kPaToPSI,2));
            dataView7.setText(Truck.SerialNo);       
         	break;
        	
        case 4:
            dataView1.setText(roundString(Helper.CelciusToFarenheit(Truck.OilTemp),2));
            dataView2.setText(roundString(Truck.OilPressure * Const.kPaToPSI,2));
            dataView3.setText(roundString(Helper.CelciusToFarenheit(Truck.CoolantTemp),2));
            dataView4.setText(roundString(Truck.CoolantLevel,2));
            dataView5.setText(roundString(Truck.CoolantPressure * Const.kPaToPSI,2));
            dataView6.setText(roundString(Truck.BatteryPotential,2));
            dataView7.setText(Truck.UnitNo);       
         	break;
        	
        case 5:
            dataView1.setText(String.valueOf(Truck.BrakeSwitch));
            dataView2.setText(String.valueOf(Truck.ClutchSwitch));
            dataView3.setText(String.valueOf(Truck.ParkBrakeSwitch));
            dataView4.setText(String.valueOf(Truck.CruiseOnOff));
            dataView5.setText(roundString(Truck.CruiseSetSpeed * Const.KphToMph,0));
            dataView6.setText(String.valueOf(Truck.CruiseState));
            dataView7.setText(Truck.VIN);       
         	break;
        }
	}

	private void ShowFault()
	{
		// Show the fault at the specified index. Note, faultIndex is relative to 0.
    	String FaultCode = String.valueOf(Truck.GetFaultSPN(faultIndex)) + " - " + String.valueOf(Truck.GetFaultFMI(faultIndex));
    	textFaultCode.setText(FaultCode);
    	
    	// Set to show next fault
    	faultIndex += 1;
    	if (faultIndex == Truck.FaultCount) // wrap to the beginning
    		faultIndex = 0;
	}
	
	private String roundString(float data, int precision)
	{
		String formatString = "#";
		if(precision > 0)
			formatString += "." + StringUtils.repeat("#", precision);
		
        return String.valueOf(NumberFormat.getNumberInstance(Locale.US).format(Double.valueOf(new DecimalFormat(formatString).format(data))));
	}

    private void ShowSystemError()
    {
		ShowMessage("System Error", "See System Log");
    }
    
	private void ShowMessage(String title, String message)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(title);
		alert.setMessage(message);
		alert.show();
	}

	@Override
	public void onBackPressed()
    {
		super.onBackPressed();
		try 
		{
			blueFire.Disconnect();
		}
		catch (Exception e) {} 
    }

	@Override
	protected void onDestroy()
    {
		super.onDestroy();
		
		blueFire.Dispose();
    }

}


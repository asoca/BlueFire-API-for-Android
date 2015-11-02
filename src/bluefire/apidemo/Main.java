package bluefire.apidemo;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

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
import com.bluefire.api.Adapter.MessageIds;
import com.bluefire.api.Truck;
import com.bluefire.api.Const;
import com.bluefire.api.Helper;
import com.bluefire.api.Adapter.AdapterErrors;
import com.bluefire.api.Adapter.ConnectionStates;
import com.bluefire.api.Adapter.IgnoreDatabuses;

public class Main extends Activity 
{
    // Form controls	
	private TextView textView1;
	private TextView textView2;
	private TextView textView3;
	private TextView textView4;
	private TextView textView5;
	private TextView textView6;
	
	private TextView dataView1;
	private TextView dataView2;
	private TextView dataView3;
	private TextView dataView4;
	private TextView dataView5;
	private TextView dataView6;
	
	private TextView textVIN;
	private TextView textStatus;    
	private TextView textFaultCode;
	private EditText textLedBrightness;
	private EditText textAdapterName;
	private CheckBox checkJ1939;
	private CheckBox checkJ1708;
	private Button buttonConnect;
	private Button buttonReset;
	private Button buttonUpdate;
	private TextView textHeartbeat;
    
    private boolean isConnecting;
    private boolean isConnected;
    private int ledBrightness; 
    private String adapterName = "";

	private int faultIndex;
	
	private boolean vinRetrieved;
	
	private int groupNo;
	private static final int maxGroupNo = 5;
    
    private Timer connectTimer;
    
    private ConnectionStates ConnectionState = ConnectionStates.NotConnected;

    // BlueFire adapter
    private Adapter blueFire;

    private Settings AppSettings;
    
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
            blueFire.Comm.UseInsecureConnection = true;
        else
            blueFire.Comm.UseInsecureConnection = false;   	

        // Get app settings
        AppSettings = new Settings();

        // Initialize adapter properties
        InitializeAdapter();
        
        // Initialize the app startup form
        initializeForm();
	}
	
	private void InitializeAdapter()
	{
        AppSettings.getSettings();

        // Set the adapter name
		blueFire.AdapterName = AppSettings.adapterName;
        
        // Set the Bluetooth discovery timeout.
        // Note, depending on the number of Bluetooth devices present on the mobile device,
        // discovery could take a long time.
        // Note, if this is set to a high value, the app needs to provide the user with the
        // capability of canceling the discovery.
        blueFire.Comm.DiscoveryTimeOut = AppSettings.discoveryTimeOut;
        
        // Set number of Bluetooth connection attempts.
        // Note, if the mobile device does not connect, try setting this to a value that 
        // allows for a consistent connection. If you're using multiple adapters and have 
        // connection problems, un-pair all devices before connecting.
        blueFire.Comm.MaxConnectRetrys = AppSettings.maxConnectRetrys;
        
        // Get the Bluetooth last connection id and the connect to last adapter setting
        blueFire.Comm.LastConnectedId = AppSettings.lastConnectedId;
        blueFire.Comm.ConnectToLastAdapter = AppSettings.connectToLastAdapter;
		
		// GEt to ignore data bus settings
		blueFire.IgnoreJ1939 = AppSettings.ignoreJ1939;
		blueFire.IgnoreJ1708 = AppSettings.ignoreJ1708;
	}

	private void initializeForm()
	{
        textView1 = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);
        textView6 = (TextView) findViewById(R.id.textView6);
        
        dataView1 = (TextView) findViewById(R.id.dataView1);
        dataView2 = (TextView) findViewById(R.id.dataView2);
        dataView3 = (TextView) findViewById(R.id.dataView3);
        dataView4 = (TextView) findViewById(R.id.dataView4);
        dataView5 = (TextView) findViewById(R.id.dataView5);
        dataView6 = (TextView) findViewById(R.id.dataView6);
        
        textVIN = (TextView) findViewById(R.id.textVIN);
        textStatus = (TextView) findViewById(R.id.textStatus);
        textFaultCode = (TextView) findViewById(R.id.textFaultCode);
        textLedBrightness = (EditText) findViewById(R.id.textLedBrightness);
        textAdapterName = (EditText) findViewById(R.id.textAdapterName);
        
        checkJ1939 = (CheckBox) findViewById(R.id.checkJ1939);
        checkJ1708 = (CheckBox) findViewById(R.id.checkJ1708);
        buttonConnect = (Button) findViewById(R.id.buttonConnect);
        buttonReset = (Button) findViewById(R.id.buttonReset);
        buttonUpdate = (Button) findViewById(R.id.buttonUpdate);
        textHeartbeat = (TextView) findViewById(R.id.textHeartbeat);
        
        clearForm();
       
		checkJ1939.setChecked(true);
		checkJ1708.setChecked(false);
       
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
		
		dataView1.setText("0");
		dataView2.setText("0");
		dataView3.setText("0");
		dataView4.setText("0");
		dataView5.setText("0");
		dataView6.setText("0");
		
		textVIN.setText("NA");
		textFaultCode.setText("NA");
		textLedBrightness.setText("0");
		textAdapterName.setText("");
		textHeartbeat.setText("0");

		faultIndex = -1;
		ledBrightness = -1;
    	vinRetrieved = false;
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
        	InitializeAdapter();
        	
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
	        
	        // Reset the adapter
	        blueFire.RebootAdapter();
	        Thread.sleep(200); // wait for reboot command to be sent 
	        
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
		AppSettings.lastConnectedId = blueFire.Comm.LastConnectedId;
		AppSettings.saveSettings();
        
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
		blueFire.J1939.ResetFaults();
	}

	public void onJ1939Click(View view)
	{
		// Do not allow unchecking
		if (!checkJ1939.isChecked())
		{
			checkJ1939.setChecked(true);
			return;
		}
		
		// J1939 is checked so uncheck J1708
		checkJ1708.setChecked(false);
		
		// Save settings
		AppSettings.ignoreJ1939 = !checkJ1939.isChecked();
		AppSettings.ignoreJ1708 = !checkJ1708.isChecked();
		AppSettings.saveSettings();
	}

	public void onJ1708Click(View view)
	{
		// Do not allow unchecking
		if (!checkJ1708.isChecked())
		{
			checkJ1708.setChecked(true);
			return;
		}
		
		// J1708 is checked so uncheck J1939
		checkJ1939.setChecked(false);
		
		// Save settings
		AppSettings.ignoreJ1939 = !checkJ1939.isChecked();
		AppSettings.ignoreJ1708 = !checkJ1708.isChecked();
		AppSettings.saveSettings();
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
		AppSettings.ledBrightness = ledBrightness;
		AppSettings.adapterName = adapterName;
		AppSettings.saveSettings();
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
    	
       	blueFire.GetSleepMode();
      	blueFire.GetLedBrightness();
       	blueFire.GetAdapterName();
       	blueFire.GetPassword();
       	blueFire.GetMessages();
     	
      	blueFire.GetVIN();
      	blueFire.GetOdometer();
      	blueFire.GetFuelData();
      	blueFire.GetEngineTemps();
      	blueFire.GetEngineHours();
      	blueFire.GetEngineData();
      	blueFire.GetEngineFluidData();
      	blueFire.GetCruiseControlData();
      	blueFire.GetBatteryVoltage();
      	blueFire.GetBrakeData();
      	blueFire.GetFaults();
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
        	//blueFire.IsTruckDataChanged = false;
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
         
         // Show heartbeat
         textHeartbeat.setText(String.valueOf(blueFire.HeartbeatCount));
         
         // Show data buses
         checkJ1939.setChecked(blueFire.IgnoreDatabus != IgnoreDatabuses.J1939);
         checkJ1708.setChecked(blueFire.IgnoreDatabus != IgnoreDatabuses.J1708);
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
         	break;
        	
        case 1:
            textView1.setText("Distance");
            textView2.setText("Odometer");
            textView3.setText("Total Hours");
            textView4.setText("Idle Hours");
            textView5.setText("Brake Pres");
            textView6.setText("Brake Air");
         	break;
        	
        case 2:
            textView1.setText("Fuel Rate");
            textView2.setText("Fuel Used");
            textView3.setText("HiRes Fuel");
            textView4.setText("Idle Fuel Used");
            textView5.setText("Avg Fuel Econ");
            textView6.setText("Inst Fuel Econ");
         	break;
        	
        case 3:
            textView1.setText("Pct Load");
            textView2.setText("Pct Torque");
            textView3.setText("Driver Torque");
            textView4.setText("Torque Mode");
            textView5.setText("Intake Temp");
            textView6.setText("Intake Pres");
         	break;
        	
        case 4:
            textView1.setText("Oil Temp");
            textView2.setText("Oil Pressure");
            textView3.setText("Coolant Temp");
            textView4.setText("Coolant Level");
            textView5.setText("Coolant Pres");
            textView6.setText("Battery Volts");
         	break;
        	
        case 5:
            textView1.setText("Brake Switch");
            textView2.setText("Clutch Switch");
            textView3.setText("Park Switch");
            textView4.setText("Cruise Switch");
            textView5.setText("Cruise Speed");
            textView6.setText("Cruise State");
         	break;
        }
	}

	private void ShowTruckData()
	{
        // Check for retrieving the VIN
        if (Truck.VIN != "" && !vinRetrieved)
        {
        	vinRetrieved = true;
        	blueFire.RemoveVIN();
            textVIN.setText(Truck.VIN);       
        }

        switch (groupNo)
        {
        case 0:
            dataView1.setText(String.valueOf(Truck.RPM));
            dataView2.setText(roundString(Truck.Speed * Const.KphToMph,0));
            dataView3.setText(roundString(Truck.MaxSpeed * Const.KphToMph,0));
            dataView4.setText(roundString(Truck.HiResMaxSpeed * Const.KphToMph,0));
            dataView5.setText(roundString(Truck.AccelPedal,2));
            dataView6.setText(roundString(Truck.ThrottlePos,2));
        	break;
        	
        case 1:
            dataView1.setText(roundString(Truck.Distance * Const.KmToMiles,0));
            dataView2.setText(roundString(Truck.Odometer * Const.MetersToMiles,0)); // HiRes Distance
            dataView3.setText(roundString(Truck.TotalHours,2));
            dataView4.setText(roundString(Truck.IdleHours,2));
            dataView5.setText(roundString(Truck.BrakeAppPressure * Const.kPaToPSI,2));
            dataView6.setText(roundString(Truck.Brake1AirPressure * Const.kPaToPSI,2));
         	break;
        	
        case 2:
            dataView1.setText(roundString(Truck.FuelRate * Const.LphToGalPHr,2));
            dataView2.setText(roundString(Truck.FuelUsed * Const.LitersToGal,2));
            dataView3.setText(roundString(Truck.HiResFuelUsed * Const.LitersToGal,2));
            dataView4.setText(roundString(Truck.IdleFuelUsed * Const.LitersToGal,2));
            dataView5.setText(roundString(Truck.AvgFuelEcon * Const.KplToMpg,2));
            dataView6.setText(roundString(Truck.InstFuelEcon * Const.KplToMpg,2));
         	break;
        	
        case 3:
            dataView1.setText(String.valueOf(Truck.PctLoad));
            dataView2.setText(String.valueOf(Truck.PctTorque));
            dataView3.setText(String.valueOf(Truck.DrvPctTorque));
            dataView4.setText(String.valueOf(Truck.TorqueMode));
            dataView5.setText(roundString(Helper.CelciusToFarenheit(Truck.IntakeTemp),2));
            dataView6.setText(roundString(Truck.IntakePressure * Const.kPaToPSI,2));
         	break;
        	
        case 4:
            dataView1.setText(roundString(Helper.CelciusToFarenheit(Truck.OilTemp),2));
            dataView2.setText(roundString(Truck.OilPressure * Const.kPaToPSI,2));
            dataView3.setText(roundString(Helper.CelciusToFarenheit(Truck.CoolantTemp),2));
            dataView4.setText(roundString(Truck.CoolantLevel,2));
            dataView5.setText(roundString(Truck.CoolantPressure * Const.kPaToPSI,2));
            dataView6.setText(roundString(Truck.BatteryPotential,2));
         	break;
        	
        case 5:
            dataView1.setText(String.valueOf(Truck.BrakeSwitch));
            dataView2.setText(String.valueOf(Truck.ClutchSwitch));
            dataView3.setText(String.valueOf(Truck.ParkBrakeSwitch));
            dataView4.setText(String.valueOf(Truck.CruiseSwitch));
            dataView5.setText(roundString(Truck.CruiseSetSpeed * Const.KphToMph,0));
            dataView6.setText(String.valueOf(Truck.CruiseState));
         	break;
        }
	}

	private void ShowFault()
	{
		// Show the fault at the specified index. Note, faultIndex is relative to 0.
    	String FaultCode = String.valueOf(blueFire.J1939.GetFaultSPN(faultIndex)) + " - " + String.valueOf(blueFire.J1939.GetFaultFMI(faultIndex));
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
		ShowMessage("System Error", blueFire.NotificationMessage);
		blueFire.NotificationMessage = "";
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
			blueFire.RebootAdapter();
			
			// Allow reboot command to be sent before disconnecting
			Thread.sleep(200); 
			
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


/**
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland. 
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version. 
 * 
 * Funf is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.mit.media.funf.funfohmage;

import static edu.mit.media.funf.AsyncSharedPrefs.async;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.joda.time.DateTimeZone;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ohmage.Config;
import org.ohmage.OhmageApi;
import org.ohmage.SharedPreferencesHelper;
import org.ohmage.Utilities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
//import android.util.Log;
import edu.mit.media.funf.IOUtils;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.configured.ConfiguredPipeline;
import edu.mit.media.funf.configured.FunfConfig;
import edu.mit.media.funf.probe.Probe;
import edu.mit.media.funf.storage.BundleSerializer;
import edu.mit.media.funf.storage.NameValueDatabaseService;
import edu.ucla.cens.systemlog.ISystemLog;
import edu.ucla.cens.systemlog.Log;


public class MainPipeline extends ConfiguredPipeline {
	
	public static String TAG = "FunfBGCollector";
	public static final String MAIN_CONFIG = "main_config";
	public static final String START_DATE_KEY = "START_DATE";

	public static final String ACTION_RUN_ONCE = "RUN_ONCE";
	public static final String RUN_ONCE_PROBE_NAME = "PROBE_NAME";
	
	//FunfOhmage
	//Stores data until data from all the sensors for the current dutycycle is received
	String locationProbe_data =  null;
	String wifiProbe_data = null;
	String accelerometerSensorProbe_data = null;
	private OhmageApi mApi;
	private SensorDataSource datasource;
	
	//FunfOhmage
	public void setOhmageApi(OhmageApi api) {
		mApi = api;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		if (ACTION_RUN_ONCE.equals(intent.getAction())) {
			String probeName = intent.getStringExtra(RUN_ONCE_PROBE_NAME);
			runProbeOnceNow(probeName);
		} else {
			super.onHandleIntent(intent);
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.setAppName("funfohmagetrial"); 
		//funfohmage
		if (!Log.isConnected()) 
		{
			Log.i("dony", "Connecting to Log");
			bindService(new Intent(ISystemLog.class.getName()),Log.SystemLogConnection, Context.BIND_AUTO_CREATE);
		}
		Log.i("dony", "Log Starting in service");
	}



	@Override
	public void onDestroy() {
		super.onDestroy();
		if (Log.isConnected()) 
		{
			unbindService(Log.SystemLogConnection);
		}
	}
	
	@Override
	public BundleSerializer getBundleSerializer() {
		return new BundleToJson();
	}
	
	public static class BundleToJson implements BundleSerializer {
		public String serialize(Bundle bundle) {
			return JsonUtils.getGson().toJson(Utils.getValues(bundle));
		}
		
	}

//	@Override
//	public void onDataReceived(Bundle data) {
//		super.onDataReceived(data);
//		incrementCount();
//	}

//	public static final String SCAN_COUNT_KEY = "SCAN_COUNT";
//	
//	public static long getScanCount(Context context) {
//		return getSystemPrefs(context).getLong(SCAN_COUNT_KEY, 0L);
//	}
	
	public static final String DATABASE_COUNT_KEY = "DATABASE_COUNT";
	
	public static long getdbCount(Context context) {
		return getSystemPrefs(context).getLong(DATABASE_COUNT_KEY, 0L);
	}
	
//	private void incrementCount() {
//		boolean success = false;
//		while(!success) { 
//			SharedPreferences.Editor editor = getSystemPrefs().edit();
//			editor.putLong(SCAN_COUNT_KEY, getScanCount(this) + 1L);
//			success = editor.commit();
//		}
//	}
	
	@Override
	public void onStatusReceived(Probe.Status status) {
		super.onStatusReceived(status);
		// Fill this in with extra behaviors on status received
	}
	
	@Override
	public void onDetailsReceived(Probe.Details details) {
		super.onDetailsReceived(details);
		// Fill this in with extra behaviors on details received
	}
	
	public static boolean isEnabled(Context context) {
		return getSystemPrefs(context).getBoolean(ENABLED_KEY, true);
	}
	
	@Override
	public SharedPreferences getSystemPrefs() {
		return getSystemPrefs(this);
	}
	
	public static SharedPreferences getSystemPrefs(Context context) {
		return async(context.getSharedPreferences(MainPipeline.class.getName() + "_system", MODE_PRIVATE));
	}
	
	@Override
	public FunfConfig getConfig() {
		return getMainConfig(this);
	}

	/**
	 * Easy access to Funf config.  
	 * As long as this service is running, changes will be automatically picked up.
	 * @param context
	 * @return
	 */
	public static FunfConfig getMainConfig(Context context) {
		FunfConfig config = getConfig(context, MAIN_CONFIG);
		if (config.getName() == null) {			
			String jsonString = getStringFromAsset(context, "default_config.json");
			if (jsonString == null) {
				Log.e(TAG, "Error loading default config.  Using blank config.");
				jsonString = "{}";
			}
			try {
				config.edit().setAll(jsonString).commit();
			} catch (JSONException e) {
				Log.e(TAG, "Error parsing default config", e);
			}
		}
		return config;
	}
	
	public static String getStringFromAsset(Context context, String filename) {
		InputStream is = null;
		try {
			is = context.getAssets().open(filename);
			return IOUtils.inputStreamToString(is, Charset.defaultCharset().name());
		} catch (IOException e) {
			Log.e(TAG, "Unable to read asset to string", e);
			return null;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					Log.e(TAG, "Unable to close asset input stream", e);
				}
			}
		}
	}
	
	public void runProbeOnceNow(final String probeName) {
		FunfConfig config = getMainConfig(this);
		ArrayList<Bundle> updatedRequests = new ArrayList<Bundle>();
		Bundle[] existingRequests = config.getDataRequests(probeName);
		if (existingRequests != null) {
			for (Bundle existingRequest : existingRequests) {
				updatedRequests.add(existingRequest);
			}
		}
		
		Bundle oneTimeRequest = new Bundle();
		oneTimeRequest.putLong(Probe.Parameter.Builtin.PERIOD.name, 0L);
		updatedRequests.add(oneTimeRequest);
		
		Intent request = new Intent(Probe.ACTION_REQUEST);
		request.setClassName(this, probeName);
		request.putExtra(Probe.CALLBACK_KEY, getCallback());
		request.putExtra(Probe.REQUESTS_KEY, updatedRequests);
		startService(request);
	}
	
	@Override
	public void onDataReceived(Bundle data) {
		String dataJson = getBundleSerializer().serialize(data);
		String probeName = data.getString(Probe.PROBE);
		long timestamp = data.getLong(Probe.TIMESTAMP, 0L);
		Bundle b = new Bundle();
		b.putString(NameValueDatabaseService.DATABASE_NAME_KEY, getPipelineName());
		b.putLong(NameValueDatabaseService.TIMESTAMP_KEY, timestamp);
		b.putString(NameValueDatabaseService.NAME_KEY, probeName);
		b.putString(NameValueDatabaseService.VALUE_KEY, dataJson);
		
		//Funf Ohmage
		//Log.w("dony","ts:"+timestamp);
		Log.w("dony","name:"+probeName);
		//Log.w("dony","data:"+dataJson);
		
		if(probeName.endsWith("LocationProbe"))
			locationProbe_data =  dataJson;
		else if(probeName.endsWith("WifiProbe"))
			wifiProbe_data = dataJson;
		else if(probeName.endsWith("AccelerometerSensorProbe"))
			accelerometerSensorProbe_data = dataJson;
		else
			Log.w("dony","Received:"+probeName);
		
		if(locationProbe_data !=null && wifiProbe_data !=null && accelerometerSensorProbe_data!=null)
		{
			Log.w("dony","ts: All data received");
			uploadRequest(accelerometerSensorProbe_data, locationProbe_data, wifiProbe_data); 
			Log.v("dony","location:"+locationProbe_data);
			Log.v("dony","wifi:"+wifiProbe_data);
			Log.v("dony","acc:"+accelerometerSensorProbe_data);
			locationProbe_data = null;
			wifiProbe_data = null;
			accelerometerSensorProbe_data = null;
		}
		
		//Intent i = new Intent(this, getDatabaseServiceClass());
		//i.setAction(DatabaseService.ACTION_RECORD);
		//i.putExtras(b);
		//startService(i);
	}
	
	public void uploadRequest(String accelerometer, String location, String wifi) 
	{
//		Intent intent = new Intent(MainPipeline.this, UploadService.class);
//		intent.setData(Responses.CONTENT_URI);
//		intent.putExtra(UploadService.EXTRA_UPLOAD_MOBILITY, true);
//		WakefulIntentService.sendWakefulWork(MobilityActivity.this, intent);
		uploadMobility(accelerometer, location, wifi);
	}
	
	private void uploadMobility(String accelerometer, String location, String wifi)
	{
		if(mApi == null)
			setOhmageApi(new OhmageApi(this));
		//sendBroadcast(new Intent(UploadService.MOBILITY_UPLOAD_STARTED));
		
		boolean uploadSensorData = true;
		
		SharedPreferencesHelper helper = new SharedPreferencesHelper(this);
		
		String username = helper.getUsername();
		String hashedPassword = helper.getHashedPassword();
		long uploadAfterTimestamp = helper.getLastMobilityUploadTimestamp();
		if (uploadAfterTimestamp == 0) {
			uploadAfterTimestamp = helper.getLoginTimestamp();
		}
		Log.w("dony","user name"+username);
		//Log.w("dony","hashed"+hashedPassword);
		//Log.w("dony","uploadTS"+uploadAfterTimestamp);

		
		Long now = System.currentTimeMillis();
		//Cursor c = MobilityInterface.getMobilityCursor(this, uploadAfterTimestamp);
		
		OhmageApi.UploadResponse response = new OhmageApi.UploadResponse(OhmageApi.Result.SUCCESS, null);
	
		JSONArray mobilityJsonArray = new JSONArray();
		JSONObject mobilityPointJson = new JSONObject();
		
		try 
		{
			JSONObject acc_json = new JSONObject(accelerometer);
			JSONObject location_json = new JSONObject(location);
			JSONObject wifi_json = new JSONObject(wifi);
			
			JSONObject location_inner_json = location_json.getJSONObject("LOCATION");
			

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//			Long time = Long.parseLong(c.getString(c.getColumnIndex(MobilityInterface.KEY_TIME)));
//			if (i == limit - 1) 
//			{
//				uploadAfterTimestamp = time;
//			}
			
			UUID id = UUID.randomUUID();
			mobilityPointJson.put("id", id.toString());
			mobilityPointJson.put("time", System.currentTimeMillis());
			mobilityPointJson.put("timezone", DateTimeZone.getDefault().getID());
			mobilityPointJson.put("mode", "still");
			if (uploadSensorData) 
			{
				mobilityPointJson.put("subtype", "sensor_data");
				JSONObject dataJson = new JSONObject();
				dataJson.put("mode", "still");
				
				if(location_inner_json.getBoolean("mHasSpeed"))
				{
					try {
						double speed = Double.parseDouble(location_inner_json.getString("mSpeed"));
						dataJson.put("speed", speed);
					} catch (NumberFormatException e) {
						dataJson.put("speed", (double)(-1));
					} catch (JSONException e) {
						dataJson.put("speed", (double)(-1));
					}
				}
				else
				{
					dataJson.put("speed", (float)(-1));
				}
				
				JSONArray x_array = acc_json.getJSONArray("X");
				JSONArray y_array = acc_json.getJSONArray("Y");
				JSONArray z_array = acc_json.getJSONArray("Z");
				JSONArray ts_array = acc_json.getJSONArray("EVENT_TIMESTAMP");
				
				JSONArray accel_data_array = new JSONArray();

				int start_index=0;
				long ts_end=0;
				if(ts_array.length()>0)
				{
					ts_end = ts_array.getLong(ts_array.length()-1);
				}
				
				for(int i= ts_array.length()-2; i>=0; i--)
				{
					if((ts_end - ts_array.getLong(i)) > 1000000000L)
					{
						start_index=i;
						//Log.i("dony","Stopping at : "+start_index);
						continue;
					}
					//Log.i("dony","diff "+(ts_end - ts_array.getLong(i))/1000000L);
				}
				
				//Log.i("dony","Number of values: "+(x_array.length()- start_index));
				//Log.i("dony","Total Number of values: "+x_array.length());
				
				for(int i=start_index; i< x_array.length(); i++)
				{
					JSONObject accel_data_json = new JSONObject();
					accel_data_json.put("x", x_array.getDouble(i));
					accel_data_json.put("y", y_array.getDouble(i));
					accel_data_json.put("z", z_array.getDouble(i));
					accel_data_array.put(accel_data_json);
				}
				
				dataJson.put("accel_data",accel_data_array);
				
				JSONObject wifiJson = new JSONObject();
//				wifiJson.put("time", wifi_json.getLong("TIMESTAMP"));
				wifiJson.put("time", System.currentTimeMillis());		
				wifiJson.put("timezone",  DateTimeZone.getDefault().getID());
				JSONArray scan_array = wifi_json.getJSONArray("SCAN_RESULTS");
				JSONArray scan_data_array = new JSONArray();
				
				for(int i=0;i<scan_array.length();i++)
				{
					JSONObject scan_element_json = new JSONObject();					
					JSONObject scan_input_json = scan_array.getJSONObject(i);
					
					scan_element_json.put("ssid", scan_input_json.getString("BSSID"));
					scan_element_json.put("strength", scan_input_json.getDouble("level"));
					scan_data_array.put(scan_element_json);
				}
				wifiJson.put("scan", scan_data_array);
				dataJson.put("wifi_data", wifiJson);
				
				mobilityPointJson.put("data", dataJson);
			} 
			else {
				mobilityPointJson.put("subtype", "mode_only");
				mobilityPointJson.put("mode", "still");
			}
			
			//String locationStatus = c.getString(c.getColumnIndex(MobilityInterface.KEY_STATUS));
			
			JSONObject locationJson = new JSONObject();
			
			if(location_inner_json.getBoolean("mHasAccuracy"))
			{
				mobilityPointJson.put("location_status", "valid");
				locationJson.put("accuracy", location_inner_json.getDouble("mAccuracy"));
			}
			else
			{
				mobilityPointJson.put("location_status", "inaccurate");
				locationJson.put("accuracy", "NaN");
			}
			
			try {
				locationJson.put("latitude", location_inner_json.getDouble("mLatitude"));
			} catch (NumberFormatException e) {
				locationJson.put("latitude", "NaN");
			} catch (JSONException e) {
				locationJson.put("latitude", "NaN");
			}
			
			try {
				locationJson.put("longitude", location_inner_json.getDouble("mLongitude"));
			} catch (NumberFormatException e) {
				locationJson.put("longitude", "NaN");
			}  catch (JSONException e) {
				locationJson.put("longitude", "NaN");
			}
			
			locationJson.put("provider",location_inner_json.getString("mProvider"));
			
			try {
				locationJson.put("accuracy", location_inner_json.getDouble("mAccuracy"));
			} catch (NumberFormatException e) {
				locationJson.put("accuracy", "NaN");
			} catch (JSONException e) {
				locationJson.put("accuracy", "NaN");
			}
			
//			locationJson.put("time",  location_json.getLong("TIMESTAMP"));
			locationJson.put("time", location_inner_json.getDouble("mTime"));
			locationJson.put("timezone", DateTimeZone.getDefault().getID());
			
			mobilityPointJson.put("location", locationJson);
			mobilityJsonArray.put(mobilityPointJson);
			
		} catch (JSONException e) {
			Log.e(TAG, "error creating mobility json", e);
//			if(isBackground)
//				NotificationHelper.showMobilityErrorNotification(this);
			throw new RuntimeException(e);
		}
		
		TAG ="dony";
		
		//Database Test
		datasource = new SensorDataSource(this);
		datasource.open();
		String dataToUpload = mobilityJsonArray.toString();
		Boolean currentData_bool = true;
				
		SensorData tempData = null;
		while(true)
		{
			response = mApi.mobilityUpload(Config.DEFAULT_SERVER_URL, username, hashedPassword, OhmageApi.CLIENT_NAME, dataToUpload);
		
			SharedPreferences.Editor editor = getSystemPrefs().edit();
			editor.putLong(DATABASE_COUNT_KEY, datasource.count());
			editor.commit();
			Log.i(TAG,"Number of database points:"+datasource.count());
			
			
			if (response.getResult().equals(OhmageApi.Result.SUCCESS)) 
			{
				Log.i(TAG, "Successfully uploaded 1 mobility points:"+dataToUpload);
				if(currentData_bool)
				{
					currentData_bool = false;
					Log.i(TAG, "Finished current. Starting past");
				}
				else
				{
					Log.i(TAG, " data to be deleted: "+tempData.getData());
					datasource.deleteData(tempData);
					Log.i(TAG, "Successfully uploaded 1 past");
				}
					
				helper.putLastMobilityUploadTimestamp(uploadAfterTimestamp);
				List<SensorData> values = datasource.getAllData();
				if(values == null || values.size()==0)
					break;
				else
				{
					tempData = values.get(0);
					dataToUpload = tempData.getData();
				}
				Log.i(TAG, "number of values:"+values.size() + " data: "+tempData.getData());
			} 
			else 
			{
				Log.e(TAG, "Failed to upload mobility points. Cancelling current round of mobility uploads.");
				
				switch (response.getResult()) 
				{
					case FAILURE:
						Log.e(TAG, "Upload failed due to error codes: " + Utilities.stringArrayToString(response.getErrorCodes(), ", "));
	
						boolean isAuthenticationError = false;
						boolean isUserDisabled = false;
	
						for (String code : response.getErrorCodes()) 
						{
							if (code.charAt(1) == '2') {
								isAuthenticationError = true;
	
								if (code.equals("0201")) {
									isUserDisabled = true;
								}
							}
						}
	
						if (isUserDisabled) {
							new SharedPreferencesHelper(this).setUserDisabled(true);
						}
	
	//					if (isBackground) 
	//					{
	//						if (isAuthenticationError) {
	//							NotificationHelper.showAuthNotification(this);
	//						} else {
	//							NotificationHelper.showMobilityErrorNotification(this);
	//						}
	//					}
	
						break;
	
					case INTERNAL_ERROR:
						Log.e(TAG, "Upload failed due to unknown internal error");
	//					if (isBackground)
	//						NotificationHelper.showMobilityErrorNotification(this);
						break;
	
					case HTTP_ERROR:
						Log.e(TAG, "Upload failed due to network error");
						break;
				}
				
				if(currentData_bool)
				{
					datasource.createData(dataToUpload);
					Log.i(TAG, "Storing in database");
				}
				Log.i(TAG, "Breaking due to Error");

				break;
			}
		}
		
		datasource.close();
		
		//sendBroadcast(new Intent(UploadService.MOBILITY_UPLOAD_FINISHED));
	}
}

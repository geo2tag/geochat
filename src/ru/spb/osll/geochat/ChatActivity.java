package ru.spb.osll.geochat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ru.spb.osll.json.Errno;
import ru.spb.osll.json.JsonApplyMarkRequest;
import ru.spb.osll.json.JsonApplyMarkResponse;
import ru.spb.osll.json.JsonAvailableChannelRequest;
import ru.spb.osll.json.JsonAvailableChannelResponse;
import ru.spb.osll.json.JsonBaseRequest;
import ru.spb.osll.json.JsonLoadTagsRequest;
import ru.spb.osll.json.JsonLoadTagsResponse;
import ru.spb.osll.json.JsonSubscribeRequest;
import ru.spb.osll.json.JsonSubscribeResponse;
import ru.spb.osll.json.JsonSubscribedChannelsRequest;
import ru.spb.osll.json.JsonSubscribedChannelsResponse;
import ru.spb.osll.objects.Channel;
import ru.spb.osll.objects.Mark;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.text.format.DateFormat;
import android.text.method.DateTimeKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;

public class ChatActivity extends Activity {

	private ListView m_tagsView;
	private Spinner m_channelsSpinner;
	private EditText m_tagEdit;
	private Button m_sendTagButton;
	
	private Handler m_handler = new Handler();
	
	
	private String m_authString;
	
	private Location m_location = null;
	
	Runnable m_statusChecker = new Runnable()
	{
	     @Override 
	     public void run() {
	          updateTagView();
	          m_handler.postDelayed(m_statusChecker, 5000);
	     }
	};
	
	
	private OnItemSelectedListener m_channelSelected = new OnItemSelectedListener(){

		@Override
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			// TODO Auto-generated method stub
			String channel = m_channelsSpinner.getSelectedItem().toString();
			try {
				subscribeChannel(channel);
				Log.d(Constants.GEO_CHAT_LOG, "Successfuly subscribed for "+ channel);
			} catch (GeoChatException e) {
				// TODO Auto-generated catch block
				Log.d(Constants.GEO_CHAT_LOG, "Error during subscribing "+ channel);
			}
		}

		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			// TODO Auto-generated method stub
			
		}
		
	};
	
	
	private OnClickListener m_sendListener = new OnClickListener(){

		@Override
		public void onClick(View arg0) {
			// TODO Auto-generated method stub
			try{ 
				writeTag();	
				Log.d(Constants.GEO_CHAT_LOG, "Tag added successfuly, updating list");
				updateTagView();
				
			}catch(GeoChatException e) {
				Log.d(Constants.GEO_CHAT_LOG, "Error during tag sending");
			}
					
		}
		
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_chat);
		
		m_authString =  getIntent().getExtras().getString(Constants.AUTH_STRING);
		Log.d(Constants.GEO_CHAT_LOG, "AuthString = " + m_authString);
		
	
		m_tagsView = (ListView)findViewById(R.id.tags_view);
		m_channelsSpinner = (Spinner)findViewById(R.id.channels_spinner);
		m_tagEdit = (EditText)findViewById(R.id.tag_message_edit) ;
		m_sendTagButton = (Button)findViewById(R.id.send_tag_button);
		
		m_sendTagButton.setOnClickListener(m_sendListener);
		m_channelsSpinner.setOnItemSelectedListener(m_channelSelected);
		
		
		m_statusChecker.run(); 
		
		setupLocationUpdates();
		
		initChannelsAndTags();
		
	}

	protected void subscribeChannel(String channel) throws GeoChatException {
		// TODO Auto-generated method stub
		JsonBaseRequest req = new JsonSubscribeRequest(
				m_authString, channel, Constants.SERVER_URL);
		JsonSubscribeResponse res = new  JsonSubscribeResponse();
		
		RequestSender.sendRequest(req, res, Errno.SUCCESS, Errno.CHANNEL_ALREADY_SUBSCRIBED_ERROR);
	}

	protected void updateTagView() {
		// TODO Auto-generated method stub
		
		List<String> tags;
		try {
			tags = loadTags();
			setupTagsViewAdapter(tags);
		} catch (GeoChatException e) {
			// TODO Auto-generated catch block
			Log.d(Constants.GEO_CHAT_LOG, "Error during tag loading");
		}			
		
		
	}

	private void initChannelsAndTags(){
		// Do subscribedChannels - fill spinner
		// Take first subscribed channel and fill ListView

		try {
		
			List<String> availableChannels = availableChannels();
			setupChannelsSpinnerAdapter(availableChannels);
			
		
		}catch (GeoChatException e){
			Log.d(Constants.GEO_CHAT_LOG, "Error during initing channels and tags");
			
		}
		
	}
	
	private void setupChannelsSpinnerAdapter(List<String> subscribedChannels){
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, subscribedChannels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        m_channelsSpinner.setAdapter(adapter);
        m_channelsSpinner.setPrompt("Title");
        //m_channelsSpinner.setSelection(0);
	}
	

	private void setupTagsViewAdapter(List<String> tags){
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, tags);
		
		m_tagsView.setAdapter(adapter);

	}
	
	private List<String> availableChannels() throws GeoChatException{
		JsonBaseRequest req = new JsonAvailableChannelRequest(
				m_authString, Constants.SERVER_URL);
		JsonAvailableChannelResponse res = new  JsonAvailableChannelResponse();
		
		RequestSender.sendRequest(req, res, Errno.SUCCESS);
		
		List<Channel> channels = res.getChannels();
		List<String> result = new ArrayList<String>();
		for(Channel c:channels){
			result.add(c.getName());
		}
		
		return result;
	}
	
	private List<String> loadTags() throws GeoChatException{
		List<String> result = new ArrayList<String>();
	
		
		if (m_location == null) {
			Log.d(Constants.GEO_CHAT_LOG, "Location is not determined, cant load tags");
			return result;
		}
		
		
		double lat = m_location.getLatitude();
		double lon = m_location.getLongitude();
		
		JsonBaseRequest req = new JsonLoadTagsRequest(
				m_authString, lat, lon, Constants.RADIUS, Constants.SERVER_URL);
		JsonLoadTagsResponse res = new  JsonLoadTagsResponse();
		
		RequestSender.sendRequest(req, res, Errno.SUCCESS);
		
		
		List<Channel> channels = res.getChannels();
		

		for(Channel c: channels){
			
			List<Mark> marks= c.getMarks();
			for (Mark m: marks ){
				String representation = m.getUser() + " : "+m.getTitle();
				result.add(representation);
			}

		}
		return result;
	}
	
	
	private void setupLocationUpdates(){
		LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

		// Define a listener that responds to location updates
		LocationListener locationListener = new LocationListener() {
		    public void onLocationChanged(Location location) {
		      // Called when a new location is found by the network location provider.
		    	m_location = location;
		    }

		    public void onStatusChanged(String provider, int status, Bundle extras) {}

		    public void onProviderEnabled(String provider) {}

		    public void onProviderDisabled(String provider) {}
		  };

		// Register the listener with the Location Manager to receive location updates
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
	}
	
	
	private void writeTag() throws GeoChatException{
		String content = m_tagEdit.getText().toString();
		Object obj= m_channelsSpinner.getSelectedItem();
		if (obj == null){
			Log.d(Constants.GEO_CHAT_LOG, "No channel selected, cant send tag");
			return;
		}

		if (m_location == null) {
			Log.d(Constants.GEO_CHAT_LOG, "Location is not determined, cant send tag");
			return;
		}
		
		String channelName = obj.toString();
		double lat = m_location.getLatitude();
		double lon = m_location.getLongitude();
		double alt = m_location.getAltitude();
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd MM yyyy HH:MM:ss.SSS");
		String time = dateFormat.format(new Date());
		
		
		JsonBaseRequest req = new JsonApplyMarkRequest(m_authString, channelName,
				content,"","",lat, lon, alt, time, Constants.SERVER_URL);

		JsonApplyMarkResponse res = new  JsonApplyMarkResponse();
		

		RequestSender.sendRequest(req, res, Errno.SUCCESS);
	}

}

package ru.spb.osll.geochat;

import org.json.JSONObject;

import ru.spb.osll.json.Errno;
import ru.spb.osll.json.JsonBaseRequest;
import ru.spb.osll.json.JsonLoginRequest;
import ru.spb.osll.json.JsonLoginResponse;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends Activity {

	private EditText m_loginEdit;
	private EditText m_passwordEdit;
	private Button m_loginButton;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		m_loginEdit = (EditText)findViewById(R.id.login_edit);
		m_passwordEdit = (EditText)findViewById(R.id.password_edit);
		m_loginButton = (Button)findViewById(R.id.login_button);
		
		m_loginButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub

				try{
		        	String authString = login();
		        	Log.d(Constants.GEO_CHAT_LOG, "Recieved authString =" + authString);
		        	moveToChatActivity(authString);
		        	
		        }catch (GeoChatException e){
		        	Log.d(Constants.GEO_CHAT_LOG, "Error sending login");
		        }
		        
		        
		        
			}
		});
	}

	private void moveToChatActivity(String authString)
	{
     	Intent intent = new Intent(this, ChatActivity.class);
     	Bundle b = new Bundle();
    	b.putString(Constants.AUTH_STRING, authString);

     	
     	intent.putExtras(b);
     	startActivity(intent);
	}
	
	private String login() throws GeoChatException
	{
		String login = m_loginEdit.getText().toString();
		String password = m_passwordEdit.getText().toString();
		JsonBaseRequest req = new JsonLoginRequest(
				login, password, Constants.SERVER_URL);
		JsonLoginResponse res = new  JsonLoginResponse();

		RequestSender.sendRequest(req, res, Errno.SUCCESS);
		
		return res.getAuthString();
	}	
	
}

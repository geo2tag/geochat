package ru.spb.osll.geochat;

import org.json.JSONObject;

import android.util.Log;

import ru.spb.osll.json.JsonBaseRequest;
import ru.spb.osll.json.JsonBaseResponse;

public class RequestSender {
	
	
	public static void sendRequest(JsonBaseRequest request, 
	        JsonBaseResponse response, int... possibleErrnos)
			throws GeoChatException
	
	{
		
		JSONObject JSONResponse = null;
        JSONResponse = request.doRequest();
      
        if (JSONResponse == null){
            throw new GeoChatException();
        }
        Log.d(Constants.GEO_CHAT_LOG, JSONResponse.toString());
        response.parseJson(JSONResponse);
        int errno =  response.getErrno();
        for (int err : possibleErrnos) {
            if (err == errno) return;
        }
        
        throw new GeoChatException();
	}
}

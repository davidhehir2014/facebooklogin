package com.fbloginsample.app;

import com.android.volley.AuthFailureError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.JsonObjectRequest;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class JsonObjectRequestHeader extends JsonObjectRequest {
	private Map<String, String> headers;

	public JsonObjectRequestHeader(int method, String url, JSONObject jsonRequest, Listener<JSONObject> listener,
								   ErrorListener errorListener, String token) {
		super(method, url, (String)null, listener, errorListener);
				if (token!=null) {
					headers=new HashMap<String, String>();
					headers.put("X-Auth-Token", token);
				}
	}

	@Override
	public Map<String, String> getHeaders() throws AuthFailureError {
		return headers;
	}
}

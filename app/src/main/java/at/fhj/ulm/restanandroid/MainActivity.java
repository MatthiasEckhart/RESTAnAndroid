package at.fhj.ulm.restanandroid;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    String apiKey = "";

    /////////////////////// UI Elements /////////////////////////
    TextView tvStatus;
    EditText etUserName;
    EditText etEmail;
    EditText etPassword;

    /////////////////////// Receiver for HTTP Response /////////////////////////
    private ResultReceiver mReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // initialize UI Elements
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        etUserName = (EditText) findViewById(R.id.etUserName);
        etEmail = (EditText) findViewById(R.id.etEmail);
        etPassword = (EditText) findViewById(R.id.etPassword);

        // initialized Receiver to handle Response
        mReceiver = new ResultReceiver(new Handler()) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                if (resultData != null && resultData.containsKey(RESTService.REST_RESULT)) {
                    onRESTResult(resultCode, resultData.getString(RESTService.REST_RESULT));
                }
                else {
                    onRESTResult(resultCode, null);
                }
            }
        };
    }


    /////////////////////// Handle Register Button /////////////////////////
    public void onClickRegister(View v){
        tvStatus.setText("Register Requested");

        // Create Params Bundle
        Bundle params = new Bundle();
        params.putString("password", etPassword.getText().toString());
        params.putString("email", etEmail.getText().toString());
        params.putString("name", etUserName.getText().toString());

        // Define Intent to start an Request with a REST Service
        Intent service = new Intent(this, RESTService.class);

        // Define URL
        String method = RESTService.SERVICE_URI + RESTService.SERVICE_METHOD_REGISTER;

        service.setData(Uri.parse(method));


        // Config REST Request with params
        service.putExtra(RESTService.EXTRA_PARAMS, params);
        service.putExtra(RESTService.EXTRA_HTTP_VERB, RESTService.POST);
        service.putExtra(RESTService.EXTRA_RESULT_RECEIVER, getResultReceiver());

        startService(service);
    }

    /////////////////////// Handle Login Button /////////////////////////
    public void onClickLogin(View v){
        tvStatus.setText("Login Requested");

        // Create Params Bundle
        Bundle params = new Bundle();
        params.putString("password", etPassword.getText().toString());
        params.putString("email", etEmail.getText().toString());

        // Define Intent to start an Request with a REST Service
        Intent service = new Intent(this, RESTService.class);

        // Define URL
        String method = RESTService.SERVICE_URI + RESTService.SERVICE_METHOD_LOGIN;
        service.setData(Uri.parse(method));

        // Config REST Request with params
        service.putExtra(RESTService.EXTRA_PARAMS, params);
        service.putExtra(RESTService.EXTRA_HTTP_VERB, RESTService.POST);
        service.putExtra(RESTService.EXTRA_RESULT_RECEIVER, getResultReceiver());

        startService(service);
    }

    /////////////////////// Handle Users Button /////////////////////////
    public void onClickUsers(View v){
        tvStatus.setText("Users Requested");

        // Create Params Bundle
        Bundle params = new Bundle();
        params.putString("apiKey", apiKey);

        // Define Intent to start an Request with a REST Service
        Intent service = new Intent(this, RESTService.class);

        // Define URL
        String method = RESTService.SERVICE_URI + RESTService.SERVICE_METHOD_USERS;
        service.setData(Uri.parse(method));

        // Config REST Request with params
        service.putExtra(RESTService.EXTRA_PARAMS, params);
        service.putExtra(RESTService.EXTRA_HTTP_VERB, RESTService.GET);
        service.putExtra(RESTService.EXTRA_RESULT_RECEIVER, getResultReceiver());

        startService(service);
    }

    public ResultReceiver getResultReceiver() {
        return mReceiver;
    }


    public void onRESTResult(int code, String result) {
        // Here is where we handle our REST response
        // Check to see if we got an HTTP 200 code and have some data.

        JSONObject jsonObj;
        try {
            // just for logging
            Log.i("REST DEMO", result);
            jsonObj = new JSONObject(result);
        } catch (JSONException e) {
            tvStatus.setText("No Server Response");
            return;
        }

        if (code == 202 && result != null) { // 202 = accepted

            try {
                if(jsonObj.getString("error").equals("false")){
                    tvStatus.setText("Logged In");
                    apiKey = jsonObj.getString("apiKey");

                    Log.i("REST DEMO", "apiKey: " + apiKey);
                } else {
                    tvStatus.setText(jsonObj.getString("message"));
                }
            } catch (JSONException e) {
                // just for logging
                Log.i("REST DEMO Error", e.toString());
                tvStatus.setText("Failed! " + code);
            }
        } else if(code == 201 && result != null) { // 202 = accepted

            try {
                if(jsonObj.getString("error").equals("false")){
                    tvStatus.setText(jsonObj.getString("message"));
                } else {
                    tvStatus.setText(jsonObj.getString("message"));
                }
            } catch (JSONException e) {
                // just for logging
                Log.i("REST DEMO Error", e.toString());
                tvStatus.setText("Failed! " + code);
            }
        } else if (code == 200 && result != null){
            try {
                String resp = jsonObj.getString("response");

                if(resp.equals("users")){ // to identify the response type
                    JSONArray ja = jsonObj.getJSONArray("users");
                    for (int i = 0; i < ja.length(); i++) {
                        JSONObject user = ja.getJSONObject(i);
                        //Log.i("REST DEMO", user.getString("name"));
                    }
                    tvStatus.setText(ja.length() + " Users found in Database");
                }
            } catch (JSONException e) {
                tvStatus.setText("Failed!" + code);
            }
        }
        else {
            try {
                String msg = jsonObj.getString("message");
                tvStatus.setText(msg);
            } catch (JSONException e) {
                tvStatus.setText("Failed with Status Code " + code);
            }
            Log.i("REST DEMO", result);
        }
    }
}

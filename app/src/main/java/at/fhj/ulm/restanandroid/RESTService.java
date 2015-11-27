package at.fhj.ulm.restanandroid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.StatusLine;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpDelete;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpPut;
import cz.msebera.android.httpclient.client.methods.HttpRequestBase;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;
import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Base64;
import android.util.Log;

public class RESTService extends IntentService {
    private static final String TAG = RESTService.class.getName();

    public static final int GET = 0x1;
    public static final int POST = 0x2;
    public static final int PUT = 0x3;
    public static final int DELETE = 0x4;

    public static final String EXTRA_HTTP_VERB = "at.fhj.ims.rest.EXTRA_HTTP_VERB";
    public static final String EXTRA_PARAMS = "at.fhj.ims.rest.EXTRA_PARAMS";
    public static final String EXTRA_RESULT_RECEIVER = "at.fhj.ims.rest.EXTRA_RESULT_RECEIVER";
    public static final String REST_RESULT = "at.fhj.ims.rest.REST_RESULT";

    public final static String SERVICE_URI = "http://www.codepower.at/fhj/moswdev/demorest/v4/";
    public final static String SERVICE_METHOD_LOGIN = "login";
    public final static String SERVICE_METHOD_REGISTER = "register";
    public final static String SERVICE_METHOD_USERS = "users";

    private final static String PRIVATE_KEY = "secret-of-mike";
    private String apiKey = "";

    public void setApiKey(String apiKey){
        this.apiKey = apiKey;
    }

    public RESTService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // When an intent is received by this Service, this method
        // is called on a new thread.
        Uri action = intent.getData();

        Bundle extras = intent.getExtras();

        if (extras == null || action == null
                || !extras.containsKey(EXTRA_RESULT_RECEIVER)) {
            // Extras contain our ResultReceiver and data is our REST action.
            // So, without these components we can't do anything useful.
            Log.e(TAG, "You did not pass extras or data with the Intent.");
            return;
        }
        // We default to GET if no verb was specified.
        int verb = extras.getInt(EXTRA_HTTP_VERB, GET);
        Bundle params = extras.getParcelable(EXTRA_PARAMS);
        params.putString("hash", hmacSha1(params, PRIVATE_KEY));
        String tmpKey = params.getString("apiKey");
        if(tmpKey != null && !tmpKey.isEmpty())
            setApiKey(tmpKey);

        ResultReceiver receiver = extras.getParcelable(EXTRA_RESULT_RECEIVER);
        try {
            // Here we define our base request object which we will
            // send to our REST service via HttpClient.
            HttpRequestBase request = null;
            // Let's build our request based on the HTTP verb we were
            // given.
            switch (verb) {
                case GET: {
                    request = new HttpGet();
                    if(!apiKey.isEmpty()) // set ApiKey if User already is loggedIn
                        request.setHeader("Apikey",apiKey);
                    attachUriWithQuery(request, action, params);
                }
                break;
                case DELETE: {
                    request = new HttpDelete();
                    if(!apiKey.isEmpty()) // set ApiKey if User already is loggedIn
                        request.setHeader("Apikey",apiKey);
                    attachUriWithQuery(request, action, params);
                }
                break;
                case POST: {
                    request = new HttpPost();
                    if(!apiKey.isEmpty()) // set ApiKey if User already is loggedIn
                        request.setHeader("Apikey",apiKey);
                    request.setURI(new URI(action.toString()));
                    // Attach form entity if necessary. Note: some REST APIs
                    // require you to POST JSON. This is easy to do, simply use
                    // postRequest.setHeader('Content-Type', 'application/json')
                    // and StringEntity instead. Same thing for the PUT case
                    // below.
                    HttpPost postRequest = (HttpPost) request;
                    if (params != null) {
                        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(
                                paramsToList(params));
                        postRequest.setEntity(formEntity);
                    }
                }
                break;
                case PUT: {
                    request = new HttpPut();
                    request.setURI(new URI(action.toString()));
                    if(!apiKey.isEmpty()) // set ApiKey if User already is loggedIn
                        request.setHeader("Apikey",apiKey);
                    // Attach form entity if necessary.
                    HttpPut putRequest = (HttpPut) request;
                    if (params != null) {
                        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(
                                paramsToList(params));
                        putRequest.setEntity(formEntity);
                    }
                }
                break;
            }
            if (request != null) {
                HttpClient client = new DefaultHttpClient();
                // Let's send some useful debug information so we can monitor
                // things in LogCat.
                Log.d(TAG, "Executing request: " + verbToString(verb) + ": "
                        + action.toString());
                // Finally, we send our request using HTTP. This is the
                // synchronous long operation that we need to run on this thread.
                HttpResponse response = client.execute(request);
                HttpEntity responseEntity = response.getEntity();
                StatusLine responseStatus = response.getStatusLine();
                int statusCode = responseStatus != null ? responseStatus
                        .getStatusCode() : 0;
                // Our ResultReceiver allows us to communicate back the results
                // to the caller. This class has a method named send() that can send back a code and
                // a Bundle of data. ResultReceiver and IntentService abstract away all
                // the IPC code we would need to write to normally make this work.
                if (responseEntity != null) {
                    Bundle resultData = new Bundle();
                    resultData.putString(REST_RESULT,
                            EntityUtils.toString(responseEntity));
                    receiver.send(statusCode, resultData);
                } else {
                    receiver.send(statusCode, null);
                }
            }
        } catch (URISyntaxException e) {
            Log.e(TAG, "URI syntax was incorrect. " + verbToString(verb) + ": " + action.toString(), e);
            receiver.send(0, null);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "A UrlEncodedFormEntity was created with an unsupported encoding.", e);
            receiver.send(0, null);
        } catch (ClientProtocolException e) {
            Log.e(TAG, "There was a problem when sending the request.", e);
            receiver.send(0, null);
        } catch (IOException e) {
            Log.e(TAG, "There was another problem when sending the request.", e);
            receiver.send(0, null);
        }
    }

    private static void attachUriWithQuery(HttpRequestBase request, Uri uri,
                                           Bundle params) {

        try {
            if (params == null) {
                // No params were given or they have already been
                // attached to the Uri.
                request.setURI(new URI(uri.toString()));
            } else {
                Uri.Builder uriBuilder = uri.buildUpon();
                // Loop through our params and append them to the Uri.
                for (BasicNameValuePair param : paramsToList(params)) {
                    uriBuilder.appendQueryParameter(param.getName(),
                            param.getValue());
                }
                uri = uriBuilder.build();
                request.setURI(new URI(uri.toString()));
            }
        } catch (URISyntaxException e) {
            Log.e(TAG, "URI syntax was incorrect: " + uri.toString(), e);
        }
    }

    private static String verbToString(int verb) {
        switch (verb) {
            case GET:
                return "GET";
            case POST:
                return "POST";
            case PUT:
                return "PUT";
            case DELETE:
                return "DELETE";
        }
        return "";
    }

    private static List<BasicNameValuePair> paramsToList(Bundle params) {
        ArrayList<BasicNameValuePair> formList = new ArrayList<BasicNameValuePair>(
                params.size());
        for (String key : params.keySet()) {
            Object value = params.get(key);
            // We can only put Strings in a form entity, so we call the
            // toString() method to enforce. We also probably don't need to check for null
            // here but we do anyway because Bundle.get() can return null.
            if (value != null)
                formList.add(new BasicNameValuePair(key, value.toString()));
        }
        return formList;
    }


    public static String hmacSha1(Bundle params, String secret64) {

        String valuesToHash = "";

        Set<String> keys = params.keySet();
        ArrayList<String> list = new ArrayList<String>(keys);
        Collections.sort(list);

        for (String keyString : list) {
            valuesToHash += params.getString(keyString);
            //Log.i("REST DEMO KEY", keyString);
        }

        Log.i("REST DEMO VALUES	", valuesToHash);


        try {
            byte[] secret = Base64.decode(secret64, Base64.DEFAULT);
            SecretKeySpec key = new SecretKeySpec(secret, "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(key);
            byte[] bytes = mac.doFinal(valuesToHash.getBytes("UTF-8"));
            String hash = Base64.encodeToString(bytes, Base64.DEFAULT);
            Log.i("REST DEMO HASH", hash);
            return hash;
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        return "";
    }

}

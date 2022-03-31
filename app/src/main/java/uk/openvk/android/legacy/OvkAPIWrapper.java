package uk.openvk.android.legacy;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

import static java.lang.Thread.sleep;

public class OvkAPIWrapper {

    public Context ctx;
    public String server;
    public String api_method;
    public String token;
    public String send_request;
    public boolean isConnected;
    public String state;
    public final static String CONNECTION_STATE = new String();
    public final static String API_METHOD = new String();
    public final static String JSON_RESPONSE = new String();
    public TimerTask updUI;
    public Boolean inputStream_isClosed;

    public TimerTask HTTPtoHTTPS;
    public Thread socketThread;
    public Thread sslSocketThread;

    public HttpURLConnection httpConnection;
    public HttpsURLConnection httpsConnection;
    public HttpURLConnection httpRawConnection;
    public HttpsURLConnection httpsRawConnection;

    public StringBuilder response_sb;
    public String jsonResponseString;
    public JSONObject jsonResponse;
    public String connectionErrorString;
    public boolean allowHTTPS;
    public Handler handler;

    public OvkAPIWrapper(Context context, String instance, String access_token, JSONObject json, boolean allowSecureConnection) {
        ctx = context;
        server = instance;
        inputStream_isClosed = new Boolean(true);
        response_sb = new StringBuilder();
        allowHTTPS = allowSecureConnection;
        token = access_token;
        HTTPtoHTTPS = new switchToHTTPS();
        handler = new Handler();
        connectionErrorString = "";
    }

    public void sendMethod(String method, String args) {
        api_method = method;
        if(token == null) {
            send_request = "/token?" + args;
        } else {
            send_request = "/method/" + method + "?access_token=" + token + "&" + args;
        }
        new Thread(new socketThread()).start();
    }

    class socketThread implements Runnable {
        @Override
        public void run() {
            try {
                Log.d("OpenVK Legacy", "Connecting to " + server + "...\r\nMethod: " + api_method);
                String url_addr = new String();
                url_addr = "http://" + server + send_request;
                URL url = new URL(url_addr);
                httpConnection = (HttpURLConnection) url.openConnection();
                isConnected = true;
                httpConnection.setRequestMethod("GET");
                httpConnection.setRequestProperty("Host", server);
                httpConnection.setRequestProperty("Accept","application/json");
                httpConnection.setRequestProperty("Accept-Charset", "UTF-8");
                httpConnection.setConnectTimeout(240000);
                httpConnection.setReadTimeout(240000);
                httpConnection.setDoInput(true);
                httpConnection.setDoOutput(true);
                httpConnection.connect();
                isConnected = true;
                BufferedReader in;
                int status = -1;
                inputStream_isClosed = false;
                status = httpConnection.getResponseCode();
                Log.d("OpenVK Legacy", "Connected!");
                String response = new String();
                Log.d("OpenVK","Response code: " + status);
                if(status == 200) {
                    in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream(), "utf-8"));
                    while ((response = in.readLine()) != null) {
                        sleep(20);
                        if (response.length() > 0) {
                            Log.d("OpenVK Legacy", "Getting response from " + server + ": [" + response_sb.toString() + "]");
                            response_sb.append(response).append("\n");
                        }
                    }
                    jsonResponseString = response_sb.toString();
                    jsonResponse = new JSONObject(response_sb.toString());
                    response_sb = new StringBuilder();
                    httpConnection.getInputStream().close();
                    inputStream_isClosed = true;
                    isConnected = false;
                    state = "getting_response";
                    sendMessageToParent();
                    Log.e("OpenVK Legacy", "InputStream closed");
                } else if(status == 301) {
                    if(allowHTTPS == true) {
                        Log.d("OpenVK Legacy", "Creating SSL connection...");
                        state = "creating_ssl_connection";
                        HTTPtoHTTPS.run();
                    } else {
                        connectionErrorString = "HTTPS required";
                        state = "no_connection";
                        sendMessageToParent();
                    }
                } else {
                    if (httpConnection.getErrorStream() != null) {
                        in = new BufferedReader(new InputStreamReader(httpConnection.getErrorStream()));
                        while ((response = in.readLine()) != null) {
                            response_sb.append(response).append("\n");
                        }
                        jsonResponseString = response_sb.toString();
                        Log.d("OpenVK Legacy", "Getting response from " + server + ": [" + response_sb.toString() + "]");
                        jsonResponse = new JSONObject(response_sb.toString());
                        response_sb = new StringBuilder();
                        httpConnection.getErrorStream().close();
                        isConnected = false;
                        inputStream_isClosed = true;
                        state = "getting_response";
                        sendMessageToParent();
                    }
                }
            } catch(SocketTimeoutException ex) {
                connectionErrorString = "SocketTimeoutException";
                state = "timeout";
                sendMessageToParent();
                Log.e("OpenVK Legacy", ex.getMessage());
            } catch(UnknownHostException ex) {
                connectionErrorString = "UnknownHostException";
                state = "no_connection";
                sendMessageToParent();
                Log.e("OpenVK Legacy", ex.getMessage());
            } catch(SocketException ex) {
                connectionErrorString = "UnknownHostException";
                state = "no_connection";
                sendMessageToParent();
                Log.e("OpenVK Legacy", ex.getMessage());
            } catch(NullPointerException ex) {
                ex.printStackTrace();
            } catch(ProtocolException ex) {
                ex.printStackTrace();
            } catch(JSONException ex) {
                if(ctx.getClass().getSimpleName().equals("AppActivity")) {
                    connectionErrorString = ((AppActivity) ctx).getResources().getString(R.string.unable_to_parse_error);
                } else if(ctx.getClass().getSimpleName().equals("AuthenticationActivity")) {
                    connectionErrorString = ((AuthenticationActivity) ctx).getResources().getString(R.string.unable_to_parse_error);
                } else if(ctx.getClass().getSimpleName().equals("AppIntentActivity")) {
                    connectionErrorString = ((AuthenticationActivity) ctx).getResources().getString(R.string.unable_to_parse_error);
                }
                state = "no_connection";
                sendMessageToParent();
                Log.e("OpenVK Legacy", ex.getMessage());
            } catch(Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void sendMessageToParent() {
        if(ctx.getClass().getSimpleName().equals("AppActivity")) {
            Message msg = handler.obtainMessage(AppActivity.UPDATE_UI);
            Bundle bundle = new Bundle();
            bundle.putString("State", state);
            bundle.putString("API_method", "/method/" + api_method);
            bundle.putString("JSON_response", "" + jsonResponseString);
            msg.setData(bundle);
            AppActivity.handler.sendMessage(msg);
        } else if(ctx.getClass().getSimpleName().equals("AuthenticationActivity")) {
            Message msg = handler.obtainMessage(AuthenticationActivity.UPDATE_UI);
            Bundle bundle = new Bundle();
            bundle.putString("State", state);
            bundle.putString("API_method", "/method/" + api_method);
            bundle.putString("JSON_response", "" + jsonResponseString);
            bundle.putString("Error_message", connectionErrorString);
            msg.setData(bundle);
            AuthenticationActivity.handler.sendMessage(msg);
        } else if(ctx.getClass().getSimpleName().equals("NewPostActivity")) {
            Message msg = handler.obtainMessage(NewPostActivity.UPDATE_UI);
            Bundle bundle = new Bundle();
            bundle.putString("State", state);
            bundle.putString("API_method", "/method/" + api_method);
            bundle.putString("JSON_response", "" + jsonResponseString);
            bundle.putString("Error_message", connectionErrorString);
            msg.setData(bundle);
            NewPostActivity.handler.sendMessage(msg);
        }
    }

    class sslSocketThread implements Runnable {
        @Override
        public void run() {
            try {
                Log.d("OpenVK Legacy", "Connecting to " + server + "... (Secured)");
                String url_addr = new String();
                url_addr = "https://" + server + send_request;
                URL url = new URL(url_addr);
                httpsConnection = (HttpsURLConnection) url.openConnection();
                httpsConnection.setRequestMethod("GET");
                httpsConnection.setRequestProperty("Host", server);
                httpsConnection.setRequestProperty("Accept","application/json");
                httpsConnection.setRequestProperty("Accept-Charset", "UTF-8");
                httpsConnection.setConnectTimeout(60000);
                httpsConnection.setReadTimeout(60000);
                httpsConnection.setDoInput(true);
                httpsConnection.setDoOutput(true);
                httpsConnection.connect();
                isConnected = true;
                BufferedReader in;
                int status = -1;
                inputStream_isClosed = false;
                status = httpsConnection.getResponseCode();
                Log.d("OpenVK Legacy", "Connected!");
                String response = new String();
                Log.d("OpenVK","Response code: " + status);
                if(status == 200) {
                    in = new BufferedReader(new InputStreamReader(httpsConnection.getInputStream(), "utf-8"));
                    while ((response = in.readLine()) != null) {
                        sleep(20);
                        if (response.length() > 0) {
                            response_sb.append(response).append("\n");
                            Log.d("OpenVK Legacy", "Getting response from " + server + ": [" + response_sb.toString() + "]");
                        }
                    }
                    jsonResponseString = response_sb.toString();
                    jsonResponse = new JSONObject(response_sb.toString());
                    response_sb = new StringBuilder();
                    httpsConnection.getInputStream().close();
                    inputStream_isClosed = true;
                    isConnected = false;
                    state = "getting_response";
                    sendMessageToParent();
                    Log.e("OpenVK Legacy", "InputStream closed");
                } else if(status == 301) {

                } else {
                    if (httpsConnection.getErrorStream() != null) {
                        in = new BufferedReader(new InputStreamReader(httpsConnection.getErrorStream()));
                        while ((response = in.readLine()) != null) {
                            response_sb.append(response).append("\n");
                        }
                        jsonResponseString = response_sb.toString();
                        Log.d("OpenVK Legacy", "Getting response from " + server + ": [" + response_sb.toString() + "]");
                        jsonResponse = new JSONObject(response_sb.toString());
                        response_sb = new StringBuilder();
                        httpsConnection.getErrorStream().close();
                        inputStream_isClosed = true;
                        isConnected = false;
                        state = "getting_response";
                        sendMessageToParent();
                    }
                }
            } catch(SocketTimeoutException ex) {
                connectionErrorString = "SocketTimeoutException";
                state = "timeout";
                sendMessageToParent();
            } catch(UnknownHostException uhEx) {
                connectionErrorString = "UnknownHostException";
                state = "no_connection";
                sendMessageToParent();
            } catch(JSONException jEx) {
                connectionErrorString = "JSONException";
                state = "no_connection";
                sendMessageToParent();
            } catch(NullPointerException ex) {
                ex.printStackTrace();
                state = "no_connection";
                sendMessageToParent();
            } catch(Exception ex) {
                ex.printStackTrace();
                isConnected = true;
            }
        }
    }

    class switchToHTTPS extends TimerTask {
        @Override
        public void run() {
            if(state == "creating_ssl_connection") {
                socketThread = new Thread(new socketThread());
                sslSocketThread = new Thread(new sslSocketThread());
                sslSocketThread.start();
            }
        }
    }
}
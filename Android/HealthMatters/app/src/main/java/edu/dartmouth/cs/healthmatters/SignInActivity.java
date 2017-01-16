package edu.dartmouth.cs.healthmatters;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class SignInActivity extends Activity {

    private String TAG = "Sign In";
    private String email;
    private String password;
    private String errorMessage;
    private ProgressDialog progressDialog;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == 0) {
                // registration failed
                errorMessage = "Registration failed. Please try again later.\n"
                        + errorMessage;
                progressDialog.dismiss();
                Toast.makeText(getApplicationContext(), errorMessage,
                        Toast.LENGTH_LONG).show();
            } else {
                // registration successful
                Toast.makeText(getApplicationContext(),
                        "Sign in successful!\nThank you!", Toast.LENGTH_LONG)
                        .show();
                SharedPreferences.Editor spe = getSharedPreferences(
                        Globals.LOGIN_PREFERENCES, MODE_PRIVATE).edit();
                spe.putString(Globals.USERNAME_KEY, email);
                spe.putString(Globals.PASSWORD_KEY, password);
                spe.putBoolean("ISREGISTERED", true);
                spe.commit();
                progressDialog.dismiss();

                Intent in = new Intent(getApplicationContext(), LandingActivity.class);
                startActivity(in);
                finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        Button btn = (Button) findViewById(R.id.SignIn);
        btn.setOnClickListener(new View.OnClickListener() {
            EditText usernameText = (EditText) findViewById(R.id.UserNameET);
            EditText passwordText = (EditText) findViewById(R.id.PasswordET);

            @Override
            public void onClick(View v) {

                email = usernameText.getText().toString();
                password = passwordText.getText().toString();
                Log.d(TAG, "username is " + email + " password is " + password);

                progressDialog = ProgressDialog.show(SignInActivity.this, "",
                        "Signing in, please wait...");

                Thread registerThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        errorMessage = "";
                        boolean result = checkAccount(email, password);
                        Message m = handler.obtainMessage();
                        if (result) {
                            // sign in successful
                            m.arg1 = 1;
                        } else {
                            // sign in failed
                            m.arg1 = 0;
                        }
                        handler.sendMessage(m);
                    }
                });
                registerThread.start();
            }
        });
    }

    /* returns true on successful registration */
    private boolean checkAccount(String email, String password) {

		/* get the web service address */
        String service = "https://biorhythm.cs.dartmouth.edu/remoteLogin";
        Log.d(TAG, "Service is " + service);
        JSONObject holder = new JSONObject();
        try {
            holder.put("email", email);
            holder.put("password", password);
        } catch (JSONException e) {
            // Should never happen
            e.printStackTrace();
        }
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost httpPost = new HttpPost(service);
        StringEntity se = null;
        try {
            se = new StringEntity(holder.toString());
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        httpPost.setEntity(se);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-type", "application/json");
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        String response;
        try {
            response = httpClient.execute(httpPost, responseHandler);
        } catch (ClientProtocolException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            return false;
        }
        JSONObject result;
        try {
            result = new JSONObject(response);
        } catch (JSONException e) {
			/* server response is not a JSON object: something went wrong */
            return false;
        }

        Log.i(TAG, "Server response: " + result.toString());
        try {
            if (result.getString("result").equals("SUCCESS")) {
                return true;
            } else {
                if (result.getString("result").equals("ERROR")) {
                    errorMessage = result.getString("message");
                }

                return false;
            }
        } catch (Exception e) {
			/* malformed server response */
            return false;
        }
    }

}

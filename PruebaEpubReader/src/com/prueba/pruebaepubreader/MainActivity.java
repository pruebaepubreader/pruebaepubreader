package com.prueba.pruebaepubreader;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

public class MainActivity extends Activity {
	
	private MainActivityHandler handler = new MainActivityHandler(this);
	
	String emailWritten;
	String passwordWritten;
	boolean rememberedTicked;
	
	private EditText email;
	private EditText password;
	private Button loginButton;
	private CheckBox rememberCheckBox;
	private ProgressDialog dialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		
		email = (EditText) findViewById(R.id.email);
		password = (EditText) findViewById(R.id.password);
		loginButton = (Button) findViewById(R.id.loginButton);
		rememberCheckBox = (CheckBox) findViewById(R.id.rememberButton);
		
		SharedPreferences preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
		boolean recordar = preferences.getBoolean("remembered", false);
		String rememberedEmail = preferences.getString("email", "");
		String rememberedPassword = preferences.getString("password", "");
		if(recordar){
			email.setText(rememberedEmail);
			password.setText(rememberedPassword);
			rememberCheckBox.setChecked(recordar);
		}
		
		loginButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				emailWritten = email.getText().toString().trim();
				passwordWritten = password.getText().toString().trim();
				rememberedTicked = rememberCheckBox.isChecked();
				
				Log.d("PruebaEpubReader", "email: " + emailWritten + " password: " + passwordWritten);
				
				dialog = ProgressDialog.show(MainActivity.this, "","Identifing user...", true, true);
				Thread dropboxLogin = new Thread(){
					public void run(){
						SharedPreferences preferences = getSharedPreferences("user", Context.MODE_PRIVATE);
						SharedPreferences.Editor editor = preferences.edit();
						editor.putString("email", emailWritten);
						editor.putString("password", passwordWritten);
						editor.putBoolean("remembered", rememberedTicked);
						editor.commit();
						
						MainActivity.this.handler.sendEmptyMessage(0);
					}
				};
				dropboxLogin.start();
			}
		});
		
		return true;
	}

	
	private void handleMessage(Message msg) {
        switch(msg.what) {
	        case 0:
	        	dialog.dismiss();
	        break;
        }
    }
	

	static private class MainActivityHandler extends Handler {
        private final MainActivity parent;
		
        public MainActivityHandler(MainActivity parent) {
            this.parent = parent;
        }

        public void handleMessage(Message msg) {
        	if(parent != null){
        		parent.handleMessage(msg);
        	}
        }
    }
}

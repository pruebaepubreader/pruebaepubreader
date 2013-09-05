package com.prueba.pruebaepubreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity {

	private static final String appKey = "6kqi2xfu21urmde";
	private static final String appSecret = "etd89oey5ebpiwz";
	private static final int REQUEST_LINK_TO_DBX = 0;
	private TextView output;

	private Button mLinkButton;
	private DbxAccountManager mDbxAcctMgr;
	DbxFileSystem dbxFs;
	List<DbxFileInfo> infos;
	
	private MainActivityHandler handler = new MainActivityHandler(this);
	private ProgressDialog dialogo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		infos = new LinkedList<DbxFileInfo>();
		output = (TextView) findViewById(R.id.output);
		mLinkButton = (Button) findViewById(R.id.link_button);
		mLinkButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickLinkToDropbox();
			}
		});
		mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), appKey, appSecret);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mDbxAcctMgr.hasLinkedAccount()) {
			showLinkedView();
			doDropboxTest();
		} else {
			showUnlinkedView();
		}
	}
	
	private void onClickLinkToDropbox() {
        mDbxAcctMgr.startLink((Activity)this, REQUEST_LINK_TO_DBX);
    }

	private void showLinkedView() {
		mLinkButton.setVisibility(View.GONE);
		output.setVisibility(View.VISIBLE);
		
	}

	private void showUnlinkedView() {
		mLinkButton.setVisibility(View.VISIBLE);
		output.setVisibility(View.GONE);
	}
	
	private void recursiveFileSearch(DbxPath path){
		try {
			List<DbxFileInfo> infosCurrent = dbxFs.listFolder(path);
			for (DbxFileInfo infoCurrent : infosCurrent) {
	        	if(infoCurrent.isFolder){
	        		recursiveFileSearch(infoCurrent.path);
	        	}else if(infoCurrent.path.toString().endsWith(".epub")){
	        		infos.add(infoCurrent);
	        	}
	        }
		} catch (DbxException e) {
			e.printStackTrace();
		}
	}
	
	private void doDropboxTest() {
        try {
            // Create DbxFileSystem for synchronized file access.
            dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

            // Print the contents of the root folder.  This will block until we can
            // sync metadata the first time.
            
            dialogo = ProgressDialog.show(MainActivity.this, "","Synchronizing dropbox...", true, true);
			Thread comunicaciones = new Thread(){
				public void run(){
					recursiveFileSearch(DbxPath.ROOT);
		            
					//Enviamos al servidor los datos del usuario
					MainActivity.this.handler.sendEmptyMessage(0);
				}
			};
			comunicaciones.start();
            
        } catch (IOException e) {
        	output.setText("Dropbox test failed: " + e);
        }
    }
	
	/**
	 * Método que recibe el mensaje del handler
	 * @param msg
	 */
	private void handleMessage(Message msg) {
        switch(msg.what) {
	        case 0:
	        	dialogo.dismiss();
	        	printFiles();
	        break;
        }
    }
	
	private void printFiles(){
		output.setText("\nContents of app folder:\n");
        for (DbxFileInfo info : infos) {
        	output.append("    " + info.path + ", " + info.modifiedTime + '\n');
        }
	}
	
	/**
	 * Implementa el handler para las comunicaciones
	 * @author Álvaro González Prieto
	 *
	 */
	static private class MainActivityHandler extends Handler {
        private final MainActivity parent;
		/**
		 * Constructor
		 * @param parent
		 */
        public MainActivityHandler(MainActivity parent) {
            this.parent = parent;
        }
        /**
         * Handler del mensaje
         */
        public void handleMessage(Message msg) {
        	if(parent != null){
        		parent.handleMessage(msg);
        	}
        }
    }
}

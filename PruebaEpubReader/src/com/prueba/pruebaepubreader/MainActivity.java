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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity implements OnItemSelectedListener  {

	private static final String appKey = "6kqi2xfu21urmde";
	private static final String appSecret = "etd89oey5ebpiwz";
	private static final int REQUEST_LINK_TO_DBX = 0;
	
	private ViewHolder [] viewHolders;
	
	private Spinner spinner;
	private ListView bookList;
	private Button mLinkButton;
	private DbxAccountManager mDbxAcctMgr;
	private DbxFileSystem dbxFs;
	private List<DbxFileInfo> infos;
	
	private MainActivityHandler handler = new MainActivityHandler(this);
	private ProgressDialog dialogo;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		infos = new LinkedList<DbxFileInfo>();
		mLinkButton = (Button) findViewById(R.id.link_button);
		bookList = (ListView) findViewById(R.id.bookList);
		mLinkButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickLinkToDropbox();
			}
		});
		mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), appKey, appSecret); //TODO
		
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_array, android.R.layout.simple_spinner_item);
		spinner = (Spinner) findViewById(R.id.spinner);
		spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
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
			getEpubFiles();
		} else {
			showUnlinkedView();
		}
	}
	
	private void onClickLinkToDropbox() {
        mDbxAcctMgr.startLink((Activity)this, REQUEST_LINK_TO_DBX);
    }

	private void showLinkedView() {
		mLinkButton.setVisibility(View.GONE);
		bookList.setVisibility(View.VISIBLE);
		spinner.setVisibility(View.VISIBLE);
	}

	private void showUnlinkedView() {
		mLinkButton.setVisibility(View.VISIBLE);
		bookList.setVisibility(View.GONE);
		spinner.setVisibility(View.GONE);
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
	
	private void getEpubFiles() {
        try {
            // Create DbxFileSystem for synchronized file access.
            dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

            // Print the contents of the root folder.  This will block until we can
            // sync metadata the first time.
            
            dialogo = ProgressDialog.show(MainActivity.this, "","Synchronizing dropbox...", true, true);
			Thread comunicaciones = new Thread(){
				public void run(){
					recursiveFileSearch(DbxPath.ROOT);
		            MainActivity.this.handler.sendEmptyMessage(0);
				}
			};
			comunicaciones.start();
            
        } catch (IOException e) {
        	e.printStackTrace();
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
	        	//printFiles();
	        	listFiles();
	        break;
        }
    }
	
	private void listFiles(){
		viewHolders = new ViewHolder[infos.size()];
		BookListAdapter bookListAdapter = new BookListAdapter(this);
		bookList.setAdapter(bookListAdapter);
	}
	/*private void printFiles(){
		for (DbxFileInfo info : infos) {
			output.append("    " + info.path + ", " + info.modifiedTime + '\n');
        }
	}*/
	
	private class BookListAdapter extends ArrayAdapter<Object>{
		Activity context;
		
		public BookListAdapter(Activity context){
			super(context, R.layout.listelement, viewHolders);
			this.context = context;
		}
		
		public View getView(int position, View convertView, ViewGroup parent){
			View item = convertView;
			ViewHolder holder;
			
			if(item ==null){
				LayoutInflater inflater = context.getLayoutInflater();
				item = inflater.inflate(R.layout.listelement, null);
				
				holder = new ViewHolder();
				holder.bookicon = (ImageButton) item.findViewById(R.id.bookicon);
				holder.bookname = (TextView) item.findViewById(R.id.bookname);
				holder.date = (TextView) item.findViewById(R.id.date);
				
				item.setTag(holder);
			}else{
				holder = (ViewHolder)item.getTag();
			}
			holder.bookname.setText(infos.get(position).path.toString());
			holder.date.setText(infos.get(position).modifiedTime.toString());
			return (item);
		}
	}
	
	private static class ViewHolder{
		ImageButton bookicon;
		TextView bookname;
		TextView date;
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
	

	@Override
	public void onItemSelected(AdapterView<?> parent, View arg1, int position, long arg3) {
		// TODO Auto-generated method stub
		setOrder((String) parent.getItemAtPosition(position));
	}
	
	private void setOrder(String order){
		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// TODO Auto-generated method stub
		
	}
	
	
}

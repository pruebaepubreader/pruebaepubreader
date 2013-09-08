package com.prueba.pruebaepubreader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxException;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileInfo;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity implements OnItemSelectedListener {

	private static final String appKey = "6kqi2xfu21urmde";
	private static final String appSecret = "etd89oey5ebpiwz";
	private static final int REQUEST_LINK_TO_DBX = 0;
	private static final String logTag = "PruebaEpubReader";
	private ViewHolder[] viewHolders;
	private ArrayAdapter<CharSequence> adapter;
	private Spinner spinner;
	private ListView bookList;
	private Button mLinkButton;
	private DbxAccountManager mDbxAcctMgr;
	private DbxFileSystem dbxFs;
	private List<BookInfo> bookInfoList = null;
	private boolean bookInfoListWasRead = false;
	private boolean isReadingBookCover = false;
	private MainActivityHandler handler = new MainActivityHandler(this);
	private ProgressDialog dialogo;
	private DbxPath pathLastSelectedBook = new DbxPath("");
	private Bitmap coverImage = null;
	private PopupWindow mPopupWindow;

	/**
	 * Executed at app creation
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mLinkButton = (Button) findViewById(R.id.link_button);
		bookList = (ListView) findViewById(R.id.bookList);
		mLinkButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onClickLinkToDropbox();
			}
		});
		mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(),
				appKey, appSecret);
		adapter = ArrayAdapter.createFromResource(this, R.array.spinner_array,
				android.R.layout.simple_spinner_item);
		spinner = (Spinner) findViewById(R.id.spinner);
		spinner.setAdapter(adapter);
		spinner.setOnItemSelectedListener(this);
	}

	/**
	 * Check if the app has linked with dropbox, if it is linked and the books
	 * from dropbox have not been read, the app proceeds to read them
	 */
	private void checkLink() {
		if (mDbxAcctMgr.hasLinkedAccount()) {
			showLinkedView();
			if (!bookInfoListWasRead) {
				getEpubFiles();
			}
		} else {
			showUnlinkedView();
		}
	}

	/**
	 * Menu not used
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * Then the Activity starts running for the first time or again, the link
	 * with dropbox is checked.
	 */
	@Override
	protected void onResume() {
		super.onResume();
		checkLink();
	}

	/**
	 * Create link to Dropbox
	 */
	private void onClickLinkToDropbox() {
		mDbxAcctMgr.startLink((Activity) this, REQUEST_LINK_TO_DBX);
	}

	/**
	 * Create the view used when the link to dropbox was successful
	 */
	private void showLinkedView() {
		mLinkButton.setVisibility(View.GONE);
		bookList.setVisibility(View.VISIBLE);
		spinner.setVisibility(View.VISIBLE);
	}

	/**
	 * Create the view used when the link to dropbox has not been done yet
	 */
	private void showUnlinkedView() {
		mLinkButton.setVisibility(View.VISIBLE);
		bookList.setVisibility(View.GONE);
		spinner.setVisibility(View.GONE);
	}

	/**
	 * Recursive epub file search
	 * 
	 * @param path
	 */
	private void recursiveFileSearch(DbxPath path) {
		try {
			List<DbxFileInfo> infosCurrent = dbxFs.listFolder(path);
			for (DbxFileInfo infoCurrent : infosCurrent) {
				if (infoCurrent.isFolder) {
					recursiveFileSearch(infoCurrent.path);
				} else if (infoCurrent.path.toString().endsWith(".epub")) {
					DbxFile currentFile = dbxFs.open(infoCurrent.path);
					Book book = (new EpubReader()).readEpub(currentFile
							.getReadStream());
					String bookTitle;
					try {
						bookTitle = book.getTitle();
					} catch (NullPointerException e) {
						bookTitle = "* No Title for " + infoCurrent.path;
						Log.d(logTag, bookTitle);
					}
					bookInfoList.add(new BookInfo(infoCurrent.path, bookTitle,
							infoCurrent.modifiedTime));
					currentFile.close();
					Log.d(logTag, "Add book: " + bookTitle + "\n  Path: "
							+ infoCurrent.path);
				}
			}
		} catch (DbxException e) {
			Log.e(logTag, e.toString());
		} catch (IOException e) {
			Log.e(logTag, e.toString());
		} catch (OutOfMemoryError e) {
			Log.e(logTag, e.toString());
		}
	}

	/**
	 * Short the book list by name
	 */
	private void sortByName() {
		Collections.sort(bookInfoList, new Comparator<BookInfo>() {
			public int compare(BookInfo file1, BookInfo file2) {
				if (file1.title.compareTo(file2.title) < 0) {
					return -1;
				} else {
					return 1;
				}
			}
		});

	}

	/**
	 * Short the book list by date
	 */
	private void sortByDate() {
		Collections.sort(bookInfoList, new Comparator<BookInfo>() {
			public int compare(BookInfo file1, BookInfo file2) {
				if (file1.date.compareTo(file2.date) < 0) {
					return -1;
				} else {
					return 1;
				}
			}
		});

	}

	/**
	 * Get all the epub files from dropbox A new thread must be created because
	 * listFolder used to get the files from dropbox will block to wait for the
	 * fist sync to be completed. Not using a different thread will make the UI
	 * thread unresponsive if the dropbox file system is big and takes long to
	 * synchronize.
	 */
	private void getEpubFiles() {
		try {
			bookInfoList = new ArrayList<BookInfo>();
			// Create DbxFileSystem for synchronized file access.
			dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

			// Print the contents of the root folder. This will block until
			// metadata synchronized the first time.

			dialogo = ProgressDialog.show(MainActivity.this, "",
					"Synchronizing dropbox...", true, false);
			bookInfoListWasRead = true;
			Thread comunicaciones = new Thread() {
				public void run() {
					recursiveFileSearch(DbxPath.ROOT);
					sortByName();
					MainActivity.this.handler.sendEmptyMessage(0);
				}
			};
			comunicaciones.start();

		} catch (IOException e) {
			Log.e(logTag, e.toString());
		}
	}

	/**
	 * Displays the files in a list with a generic icon, name and date
	 */
	private void listFiles() {
		viewHolders = new ViewHolder[bookInfoList.size()];
		BookListAdapter bookListAdapter = new BookListAdapter(this);
		bookList.setAdapter(bookListAdapter);
	}

	/**
	 * Adapter for displaying the List as a ListView
	 * 
	 * @author Alvaro
	 * 
	 */
	private class BookListAdapter extends ArrayAdapter<Object> {
		Activity context;

		public BookListAdapter(Activity context) {
			super(context, R.layout.listelement, viewHolders);
			this.context = context;
		}

		/**
		 * Get View for each position in the list
		 */
		public View getView(int position, View convertView, ViewGroup parent) {
			View item = convertView;
			ViewHolder holder;

			if (item == null) {
				LayoutInflater inflater = context.getLayoutInflater();
				item = inflater.inflate(R.layout.listelement, null);

				holder = new ViewHolder();
				holder.bookname = (TextView) item.findViewById(R.id.bookname);
				holder.date = (TextView) item.findViewById(R.id.date);
				holder.bookicon = (ImageButton) item
						.findViewById(R.id.bookicon);
				item.setTag(holder);
			} else {
				holder = (ViewHolder) item.getTag();
			}
			holder.path = bookInfoList.get(position).path;
			holder.bookicon
					.setOnClickListener(new MyOnClickListenerWithListPosition(
							holder.path));
			holder.bookname.setText(bookInfoList.get(position).title);
			holder.date.setText(bookInfoList.get(position).date.toString());
			return (item);
		}
	}

	/**
	 * Customized On click listener. This is needed because the path of the file
	 * is necessary for latter getting the cover image of the book.
	 * 
	 * @author Alvaro
	 * 
	 */
	private class MyOnClickListenerWithListPosition implements OnClickListener {
		private DbxPath path;

		public MyOnClickListenerWithListPosition(DbxPath path) {
			this.path = path;
		}

		@Override
		public void onClick(View v) {
			if (path.compareTo(pathLastSelectedBook) != 0) {
				pathLastSelectedBook = path;
				Log.d(logTag, "Selected = " + pathLastSelectedBook.toString());
				Toast.makeText(MainActivity.this,
						"Push again to see the book cover", Toast.LENGTH_SHORT)
						.show();
			} else {
				Log.d(logTag,
						"Selected second time = "
								+ pathLastSelectedBook.toString());
				if(isReadingBookCover == false){
					getBookCoverImage();
				}
			}
		}

	}

	/**
	 * Get cover image
	 * 
	 * @param path
	 */
	private void getBookCoverImage() {
		dialogo = ProgressDialog.show(MainActivity.this, "",
				"Searching cover image...", true, false);
		coverImage = null;
		isReadingBookCover = true;
		Thread comunicaciones = new Thread() {
			public void run() {
				try {
					DbxFile currentCoverFile = dbxFs.open(pathLastSelectedBook);
					Book book = (new EpubReader()).readEpub(currentCoverFile
							.getReadStream());
					try {
						coverImage = BitmapFactory.decodeStream(book
								.getCoverImage().getInputStream());
					} catch (NullPointerException e) {
						Log.d(logTag, "* No Cover Image for: "
								+ pathLastSelectedBook.toString());
					} catch (OutOfMemoryError E) {
						Log.d(logTag, "* OutOfMemoryError: "
								+ pathLastSelectedBook.toString());
					}
					currentCoverFile.close();
				} catch (IOException E) {
					Log.e(logTag, "* IOException getting cover image: "
							+ pathLastSelectedBook.toString());
				}
				pathLastSelectedBook = new DbxPath("");
				MainActivity.this.handler.sendEmptyMessage(1);
			}
		};
		comunicaciones.start();

	}

	/**
	 * Overrides the back key press so that when the image Popup is shown the
	 * back key
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Override back button
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (mPopupWindow != null && mPopupWindow.isShowing()) {
				mPopupWindow.dismiss();
				return false;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	/**
	 * Create Image Cover Activity
	 */
	private void createImageCoverActivity() {
		if (coverImage != null) {
			LayoutInflater inflater = this.getLayoutInflater();
			View mView = inflater.inflate(R.layout.bookcoverimage, null);
			mPopupWindow = new PopupWindow(mView, LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT, false);
			mPopupWindow.setAnimationStyle(android.R.style.Animation_Toast);
			ImageView imageView = (ImageView) mView
					.findViewById(R.id.bookcoverimage);
			imageView.setImageBitmap(coverImage);
			mPopupWindow.showAtLocation(imageView, Gravity.CENTER, 45, 0);
		} else {
			Toast.makeText(MainActivity.this, "Cover image not found",
					Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * View Holder with the three elements of the list.
	 * 
	 * @author Alvaro
	 * 
	 */
	private static class ViewHolder {
		DbxPath path; // Path is used to uniquely identify the book file
		ImageButton bookicon;
		TextView bookname;
		TextView date;
	}

	/**
	 * Element of the book info list that will keep track of the relevant book
	 * data
	 * 
	 * @author Alvaro
	 * 
	 */
	private static class BookInfo {
		DbxPath path; // Path is used to uniquely identify the book file
		String title;
		Date date;

		BookInfo(DbxPath path, String title, Date date) {
			this.path = path;
			this.title = title;
			this.date = date;
		}
	}

	/**
	 * Handle message of dropbox file system communication thread
	 * 
	 * @param msg
	 */
	private void handleMessage(Message msg) {
		switch (msg.what) {
		case 0:
			dialogo.dismiss();
			spinner.setSelection(0, true);
			listFiles();
			break;
		case 1:
			dialogo.dismiss();
			isReadingBookCover = false;
			createImageCoverActivity();
			break;
		}
	}

	/**
	 * Handler for receiving the message of the dropbox file system
	 * communication thread
	 * 
	 * @author Alvaro
	 * 
	 */
	static private class MainActivityHandler extends Handler {
		private final MainActivity parent;

		/**
		 * Constructor
		 * 
		 * @param parent
		 */
		public MainActivityHandler(MainActivity parent) {
			this.parent = parent;
		}

		/**
		 * Message Handler
		 */
		public void handleMessage(Message msg) {
			if (parent != null) {
				parent.handleMessage(msg);
			}
		}
	}

	/**
	 * Spinner item selection
	 */
	@Override
	public void onItemSelected(AdapterView<?> parent, View arg1, int position,
			long arg3) {
		// Auto-generated method stub
		setOrder((String) parent.getItemAtPosition(position));
	}

	/**
	 * Set name or date order depending on the item selection
	 * 
	 * @param order
	 */
	private void setOrder(String order) {
		if (order.equals(getString(R.string.name))) {
			this.sortByName();
			this.listFiles();
		} else if (order.equals(getString(R.string.date))) {
			this.sortByDate();
			this.listFiles();
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// Auto-generated method stub

	}
}
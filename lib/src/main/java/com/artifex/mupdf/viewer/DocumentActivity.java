package com.artifex.mupdf.viewer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.core.text.HtmlCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;
import com.artifex.mupdf.viewer.gp.MuPDFLibrary;
import com.artifex.mupdf.viewer.gp.CropAndShareActivity;
import com.artifex.mupdf.viewer.gp.OutlineAdapter;
import com.artifex.mupdf.viewer.gp.RecyclerAdapter;
import com.artifex.mupdf.viewer.gp.models.GPContent;
import com.artifex.mupdf.viewer.gp.models.GPReaderSearchResult;
import com.artifex.mupdf.viewer.gp.models.PagePreview;
import com.artifex.mupdf.viewer.gp.util.ThemeColor;
import com.artifex.mupdf.viewer.gp.util.ThemeFont;
import com.artifex.mupdf.viewer.gp.util.ThemeIcon;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;



public class DocumentActivity extends Activity
{
	public final static String EXTRA_THEME_TYPE = "themeType";
	public final static String EXTRA_FOREGROUND_THEME_COLOR = "foregroundThemeColor";
	/* The core rendering instance */
	enum TopBarMode {Main, Search}

	/**
	 *  App: use app search methods and GalePress design
	 *  Lib: use mupdf-android-viewer search
	 */
	enum SearchMode {App, Lib};

	private final int    OUTLINE_REQUEST=0;
	private MuPDFCore    core;
	private String       mFileName;
	private ReaderView   mDocView;
	private View         mButtonsView;
	private boolean      mButtonsVisible;
	private EditText     mPasswordView;
	private TextView     mFilenameView;
	private ImageButton  mSearchButton;
	private ImageButton  mOutlineButton;
	private ImageButton  mShareButton;
	private ViewAnimator mTopBarSwitcher;
	private ImageButton  mLinkButton;
	private TopBarMode   mTopBarMode = TopBarMode.Main;
	private ImageButton  mSearchBack;
	private ImageButton  mSearchFwd;
	private ImageButton  mSearchClose;
	private EditText     mSearchText;
	private SearchTask   mSearchTask;
	private AsyncThumb   asyncThumb;

	// GP drawer
	private DrawerLayout mDrawerLayout;

	// Search mode GalePress or mupdf lib
	private SearchMode mSearchMode = SearchMode.Lib;

	// GP custom model
	private GPContent content;

	// GP search
	private SearchResultAdapter searchAdapter;
	private ListView mSearhResultListView;
	private LinearLayout mSearchProgressBaseView;
	private LinearLayout mSearchClearBaseView;
	private PopupWindow mSearchPopup;
	private EditText mPopupSearchEditText;
	private String mReaderSearchWord;
	public ProgressDialog mSearchDialog;

	// GP reader search result
	private ArrayList<GPReaderSearchResult> mReaderSearchResult;

	// GP reader show buttons button
	private RelativeLayout mReaderShowPageThumbnailsButton;
    private ProgressBar ProgressRecycler;


	// GP recycler page preview
	private RecyclerView mRecyclerPagePreview;
	private RecyclerAdapter mRecyclerPagePreviewAdapter;
	private LinearLayoutManager mRecylerPagePreviewLayoutManager;
    private boolean isPagePreviewActive = false;


	private AlertDialog.Builder mAlertBuilder;
	private boolean    mLinkHighlight = false;


	protected int mDisplayDPI;
	private int mLayoutEM = 10;
	private int mLayoutW = 312;
	private int mLayoutH = 504;

	// page thumbnail show button scale animation replay count
	private int scaleAnimationReplayCount = 3;

	protected View mReflowButton;
	protected PopupMenu mLayoutPopupMenu;

	// GP orientation
	private int mOrientation;
	private boolean deviceType;

	private MuPDFCore openFile(String path)
	{
		int lastSlashPos = path.lastIndexOf('/');
		mFileName = lastSlashPos == -1
				? path
				: path.substring(lastSlashPos + 1);
		System.out.println("Trying to open " + path);
		try
		{
			core = new MuPDFCore(path);
		}
		catch (Exception | OutOfMemoryError e)
		{
			System.out.println(e);
			return null;
		}
		//  out of memory is not an Exception, so we catch it separately.

		return core;
	}

	private MuPDFCore openBuffer(byte[] buffer, String magic)
	{
		System.out.println("Trying to open byte buffer");
		try
		{
			core = new MuPDFCore(buffer, magic);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		return core;
	}

	/** Called when the activity is first created. */
	@SuppressLint({"StaticFieldLeak", "SourceLockedOrientationActivity"})
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Commented out for disabling full screen in GalePress
		// getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// GP Orientation
		mOrientation = getResources().getConfiguration().orientation;
		deviceType = getResources().getBoolean(R.bool.isTablet);
		if(!deviceType)
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDisplayDPI = metrics.densityDpi;

		mAlertBuilder = new AlertDialog.Builder(this);

		prepareDocument(savedInstanceState);
		createUI(savedInstanceState);

		ProgressRecycler = findViewById(R.id.ProgressRecycler);

		asyncThumb = new AsyncThumb();
		if (asyncThumb.getStatus() != AsyncTask.Status.RUNNING){
			asyncThumb.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		}
	}

	private void saveImage(Bitmap finalBitmap, int index, String ContentId) {
		String root = getFilesDir().getAbsolutePath();
		File myDir = new File(root + "/saved_images/" + ContentId);
		myDir.mkdirs();
		String fname = "Image-"+ index +".jpg";
		File file = new File (myDir, fname);
		if (file.exists ()) file.delete();
		try {
			FileOutputStream out = new FileOutputStream(file);
			finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
			out.flush();
			out.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Bitmap readImage(int index){
		String root = getFilesDir().getAbsolutePath();
		String fileName = root + "/saved_images/" + getContentId() + "/Image-" + index + ".jpg";
		File file = new File(fileName);
		return BitmapFactory.decodeFile(file.getAbsolutePath());
	}


	@SuppressLint("StaticFieldLeak")
	public class AsyncThumb extends  AsyncTask<Void,Void,Void>{

		@Override
		protected Void doInBackground(Void... voids) {
			final PagePreview[] ppArray = new PagePreview[core.countPages()];
			String root = getFilesDir().getAbsolutePath();
			String Cache = root + "/saved_images/" + getContentId();

			File file = new File(Cache);
			if(!file.exists()){
				ArrayList<Bitmap> bm_images = core.getPDFThumbnails(120, 180);
				for (int i = 0; i < core.countPages(); i++){
					ppArray[i] = new PagePreview(i, bm_images.get(i));
					saveImage(bm_images.get(i), i, getContentId());
				}
			}
			else {
				for (int i = 0; i < core.countPages(); i++) {
					Bitmap bitmap = readImage(i);
					ppArray[i] = new PagePreview(i, bitmap);
				}
			}
			mRecyclerPagePreviewAdapter = new RecyclerAdapter(ppArray, DocumentActivity.this);
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			isPagePreviewActive = true;
			mRecyclerPagePreview.setAdapter(mRecyclerPagePreviewAdapter);
			int displayingPageIndex = mDocView.getDisplayedViewIndex();
			mRecyclerPagePreviewAdapter.setSelectedIndex(displayingPageIndex);
			scrollToThumbnailPagePreviewIndex(displayingPageIndex);
			ProgressRecycler.setVisibility(View.INVISIBLE);
		}
	}


	private void prepareDocument(Bundle savedInstanceState) {
		if (core == null) {
			if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
				mFileName = savedInstanceState.getString("FileName");
			}
		}

		if (core == null) {
			Intent intent = getIntent();

			//---------- GalePress Integration [Start]

			// if application instance registered, use its methods for search, set etc.
			if (MuPDFLibrary.getAppInstance() != null) {
				MuPDFLibrary.getAppInstance().setMuPDFActivity(this);
				mSearchMode = SearchMode.App;
			}
			else {
				Log.i(MuPDFLibrary.TAG, "if you want to implement a custom search or etc. you can register your app instance which implements ApplicationInterface using MuPDFLibrary class");
			}

			content = (GPContent) intent.getSerializableExtra("content");

			// if content is not sent as intent extra set create one with default values
			// this is just for not requiring content in viewer.app
			if (content == null) {
				content = new GPContent() {
					@Override
					public Integer getId() {
						return 0;
					}

					@Override
					public String getName() {
						return core.getTitle();
					}

					@Override
					public Integer getContentOrientation() {
						return getResources().getConfiguration().orientation;
					}
				};
				Log.i(MuPDFLibrary.TAG, "content is used in custom methods of ApplicationInterface instance, if you registered your app, you'd prefer to set GPContent implemented object, as a serializable extra when starting DocumentActivity");
			}

			String foregroundColor = intent.getStringExtra(EXTRA_FOREGROUND_THEME_COLOR);
			if (foregroundColor != null) {
				ThemeColor.getInstance().setForegroundColor(foregroundColor);
			}

			int themeType = intent.getIntExtra(EXTRA_THEME_TYPE, 1);
			ThemeColor.getInstance().setThemeType(themeType);

			//---------- GalePress Integration [End]

			byte[] buffer;

			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				Uri uri = intent.getData();
				System.out.println("URI to open is: " + uri);
				if (uri.getScheme().equals("file")) {
					String path = uri.getPath();
					core = openFile(path);
				} else {
					try {
						InputStream is = getContentResolver().openInputStream(uri);
						int len;
						ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
						byte[] data = new byte[16384];
						while ((len = is.read(data, 0, data.length)) != -1) {
							bufferStream.write(data, 0, len);
						}
						bufferStream.flush();
						buffer = bufferStream.toByteArray();
						is.close();
					}
					catch (IOException e) {
						String reason = e.toString();
						Resources res = getResources();
						AlertDialog alert = mAlertBuilder.create();
						setTitle(String.format(Locale.ROOT, res.getString(R.string.cannot_open_document_Reason), reason));
						alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int which) {
										finish();
									}
								});
						alert.show();
						return;
					}
					core = openBuffer(buffer, intent.getType());
				}
				SearchTaskResult.set(null);
			}
			if (core != null && core.needsPassword()) {
				requestPassword(savedInstanceState);
				return;
			}
			if (core != null && core.countPages() == 0)
			{
				core = null;
			}
		}
		if (core == null)
		{
			AlertDialog alert = mAlertBuilder.create();
			alert.setTitle(R.string.cannot_open_document);
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			alert.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			});
			alert.show();
		}
	}

	public void requestPassword(final Bundle savedInstanceState) {
		mPasswordView = new EditText(this);
		mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = mAlertBuilder.create();
		alert.setTitle(R.string.enter_password);
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.okay),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						if (core.authenticatePassword(mPasswordView.getText().toString())) {
							createUI(savedInstanceState);
						} else {
							requestPassword(savedInstanceState);
						}
					}
				});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		alert.show();
	}

	public void relayoutDocument() {
		int loc = core.layout(mDocView.mCurrent, mLayoutW, mLayoutH, mLayoutEM);
		mDocView.mHistory.clear();
		mDocView.refresh();
		mDocView.setDisplayedViewIndex(loc);
	}

	/**
	 * GalePress integration manage layout of custom views on orientation change
	 * @param currentPageIndex
	 */
	public void relayoutCustomViews(int currentPageIndex) {
		searchModeOff();
		scrollToThumbnailPagePreviewIndex(currentPageIndex);
	}

	@SuppressLint("StaticFieldLeak")
    public void createUI(Bundle savedInstanceState) {
		if (core == null)
			return;

		// Now create the UI.
		// First create the document view
		mDocView = new ReaderView(this) {
			@Override
			protected void onMoveToChild(int i) {
				if (core == null)
					return;
                if(isPagePreviewActive){
                    mRecyclerPagePreviewAdapter.setSelectedIndex(i);
                    scrollToThumbnailPagePreviewIndex(i);
                }
				super.onMoveToChild(i);
			}

			@Override
			protected void onTapMainDocArea() {
				if (!mButtonsVisible) {
					showButtons();
				} else {
					if (mTopBarMode == TopBarMode.Main)
						hideButtons();
				}
			}

			@Override
			protected void onDocMotion() {
				hideButtons();
			}

			@Override
			public void onSizeChanged(int w, int h, int oldw, int oldh) {
				if (core.isReflowable()) {
					mLayoutW = w * 72 / mDisplayDPI;
					mLayoutH = h * 72 / mDisplayDPI;
					relayoutDocument();
				} else {
					refresh();
				}
			}
			@SuppressLint("SourceLockedOrientationActivity")
			@Override
			public void onConfigurationChanged(Configuration newConfig) {
				super.onConfigurationChanged(newConfig);
			// GalePress integration: manage layout of custom views on orientation change
				if (mOrientation != newConfig.orientation && deviceType) {
					relayoutCustomViews(mCurrent);
					displayPages = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE ? DisplayPages.TWO : DisplayPages.SINGLE;
					mOrientation = newConfig.orientation;
				}else
					setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			}
		};
		mDocView.setAdapter(new PageAdapter(this, core));

		// GalePress set background theme color
		mDocView.setBackgroundColor(ThemeColor.getInstance().getThemeColor());

		mSearchTask = new SearchTask(this, core) {
			@Override
			protected void onTextFound(SearchTaskResult result) {
				SearchTaskResult.set(result);
				// Ask the ReaderView to move to the resulting page
				mDocView.setDisplayedViewIndex(result.pageNumber);
				// Make the ReaderView act on the change to SearchTaskResult
				// via overridden onChildSetup method.
				mDocView.resetupChildren();
			}
		};

		// Make the buttons overlay, and store all its
		// controls in variables
		makeButtonsView();

		// Set the file-name text
		String docTitle = core.getTitle();
		if (docTitle != null)
			mFilenameView.setText(docTitle);
		else
			mFilenameView.setText(mFileName);

		//---------- GalePress recycle page preview [Start]

		mRecylerPagePreviewLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);

		mRecyclerPagePreview = mButtonsView.findViewById(R.id.recyclerPagePreview);
	    mRecyclerPagePreview.setLayoutManager(mRecylerPagePreviewLayoutManager);
		mRecyclerPagePreview.setBackgroundColor(ThemeColor.getInstance().getThemeColor());


		//---------- GalePress recycle page preview [End]

		// Activate the search-preparing button
		mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOn();
			}
		});

		if (mSearchMode == SearchMode.Lib) {
			createLibSearchUI();
		}
		else {
			removeLibSearchUI();
		}

		mShareButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String filePath = getApplicationContext().getFilesDir().getAbsolutePath() + File.separator + "capturedImage.png";
				cropAndShareCurrentPage(filePath);
			}
		});

		mLinkButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setLinkHighlight(!mLinkHighlight);
			}
		});

		if (core.isReflowable()) {
			mReflowButton.setVisibility(View.VISIBLE);
			mLayoutPopupMenu = new PopupMenu(this, mReflowButton);
			mLayoutPopupMenu.getMenuInflater().inflate(R.menu.layout_menu, mLayoutPopupMenu.getMenu());
			mLayoutPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
				public boolean onMenuItemClick(MenuItem item) {
					float oldLayoutEM = mLayoutEM;
					int id = item.getItemId();
					if (id == R.id.action_layout_6pt) mLayoutEM = 6;
					else if (id == R.id.action_layout_7pt) mLayoutEM = 7;
					else if (id == R.id.action_layout_8pt) mLayoutEM = 8;
					else if (id == R.id.action_layout_9pt) mLayoutEM = 9;
					else if (id == R.id.action_layout_10pt) mLayoutEM = 10;
					else if (id == R.id.action_layout_11pt) mLayoutEM = 11;
					else if (id == R.id.action_layout_12pt) mLayoutEM = 12;
					else if (id == R.id.action_layout_13pt) mLayoutEM = 13;
					else if (id == R.id.action_layout_14pt) mLayoutEM = 14;
					else if (id == R.id.action_layout_15pt) mLayoutEM = 15;
					else if (id == R.id.action_layout_16pt) mLayoutEM = 16;
					if (oldLayoutEM != mLayoutEM)
						relayoutDocument();
					return true;
				}
			});
			mReflowButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mLayoutPopupMenu.show();
				}
			});
		}

		// Reenstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		if(content !=null){
			mDocView.setDisplayedViewIndex(prefs.getInt("page" + getContentId(), 0));
		}else{
			mDocView.setDisplayedViewIndex(prefs.getInt("page" + mFileName, 0));
		}


		// GalePress don't show buttons in first open instead show a button to let user know there are buttons
		 if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
			scaleAnimation();

		if(savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
			searchModeOn();

		//---------- GalePress Integration - Drawer [Start]

		// Stick the document view and the buttons overlay into a parent view
		if (core.hasOutline()) {

			mDrawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.pdf_reader, null);

			final RelativeLayout layout = mDrawerLayout.findViewById(R.id.reader);
			layout.setBackgroundColor(Color.DKGRAY);
			layout.addView(mDocView);
			layout.addView(mButtonsView);
			setContentView(mDrawerLayout);

			ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,R.string.open, R.string.close) {
				@Override
				public void onDrawerSlide(View drawerView, float slideOffset) {
					super.onDrawerSlide(drawerView, slideOffset);
					float slideX = drawerView.getWidth() * slideOffset;
					layout.setTranslationX(slideX);
				}
			};

			mDrawerLayout.addDrawerListener(actionBarDrawerToggle);
			mDrawerLayout.setScrimColor(Color.TRANSPARENT);

			// left menu title
			TextView menuTitle = findViewById(R.id.reader_left_menu_title);
			menuTitle.setText(this.content.getName());
			menuTitle.setTextColor(ThemeColor.getInstance().getThemeColor());
			menuTitle.setTypeface(ThemeFont.getInstance().getSemiBoldItalicFont(this));
			menuTitle.setBackgroundColor(ThemeColor.getInstance().getForegroundColor());

			ListView leftList = findViewById(R.id.reader_left_menu_listView);
			leftList.setBackgroundColor(ThemeColor.getInstance().getForegroundColor());
			leftList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
					mDocView.pushHistory();
					int resultPageIndex = core.getOutline().get(position).page;
					mDocView.setDisplayedViewIndex(resultPageIndex);

					// refresh page preview bar
                    if(isPagePreviewActive){
                        scrollToThumbnailPagePreviewIndex(resultPageIndex);
                        mRecyclerPagePreviewAdapter.notifyDataSetChanged();
                    }
				}
			});

			leftList.setAdapter(new OutlineAdapter(this, getLayoutInflater(), core.getOutline()));

			mOutlineButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mDrawerLayout.openDrawer(GravityCompat.START);
				}
			});
		}
		else {

			// hide outline button
			mOutlineButton.setVisibility(View.GONE);

			// set content view
			RelativeLayout layout = new RelativeLayout(this);
			layout.setBackgroundColor(Color.DKGRAY);
			layout.addView(mDocView);
			layout.addView(mButtonsView);
			setContentView(layout);
		}

		//---------- GalePress Integration - Drawer [End]
	}

	public void createLibSearchUI() {
		mButtonsView.findViewById(R.id.searchBar).setBackgroundColor(ThemeColor.getInstance().getStrongThemeColor());

		mSearchClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOff();
			}
		});
		mSearchClose.setColorFilter(ThemeColor.getInstance().getOppositeThemeColorFilter());

		// Search invoking buttons are disabled while there is no text specified
		mSearchBack.setEnabled(false);
		mSearchFwd.setEnabled(false);
		mSearchBack.setColorFilter(ThemeColor.getInstance().getOppositeThemeColorFilter());
		mSearchFwd.setColorFilter(ThemeColor.getInstance().getOppositeThemeColorFilter());

		mSearchText.setTextColor(ThemeColor.getInstance().getOppositeThemeColor());

		// React to interaction with the text widget
		mSearchText.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				boolean haveText = s.toString().length() > 0;
				setButtonEnabled(mSearchBack, haveText);
				setButtonEnabled(mSearchFwd, haveText);

				// Remove any previous search results
				if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
					SearchTaskResult.set(null);
					mDocView.resetupChildren();
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count,
										  int after) {}
			public void onTextChanged(CharSequence s, int start, int before,
									  int count) {}
		});

		// React to Done button on keyboard
		mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					search(1);
				return false;
			}
		});

		mSearchText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
					search(1);
				return false;
			}
		});

		// Activate search invoking buttons
		mSearchBack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(-1);
			}
		});
		mSearchFwd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(1);
			}
		});
	}

	public void removeLibSearchUI() {
		LinearLayout searchBar = mButtonsView.findViewById(R.id.searchBar);

		if (searchBar == null) return;

		ViewGroup parent = (ViewGroup) searchBar.getParent();

		if (parent == null) return;

		parent.removeView(searchBar);
	}

	//---------- GalePress Integration [Start]

	/**
	 * get current content id for custom GPContent
	 */
	public String getContentId() {
		return content.getId().toString();
	}

	public ReaderView getReaderView() {
		return mDocView;
	}

	//---------- GalePress Integration [End]

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == OUTLINE_REQUEST) {
			if (resultCode >= RESULT_FIRST_USER) {
				mDocView.pushHistory();
				mDocView.setDisplayedViewIndex(resultCode - RESULT_FIRST_USER);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		mOrientation = getResources().getConfiguration().orientation;

		if (mFileName != null && mDocView != null) {
			outState.putString("FileName", mFileName);
			// Store current page in the prefs against the file name,
			// so that we can pick it up each time the file is loaded
			// Other info is needed only for screen-orientation change,
			// so it can go in the bundle
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+mFileName, mDocView.getDisplayedViewIndex());
			edit.apply();
		}
		if (!mButtonsVisible)
			outState.putBoolean("ButtonsHidden", true);

		if (mTopBarMode == TopBarMode.Search)
			outState.putBoolean("SearchMode", true);
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (mSearchTask != null)
			mSearchTask.stop();
		if (mFileName != null && mDocView != null) {
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			if(content !=null){
				edit.putInt("page" + getContentId(), mDocView.getDisplayedViewIndex());
				edit.apply();
			}
			else{
				edit.putInt("page", mDocView.getDisplayedViewIndex());
				edit.apply();
			}
		}
	}

	public void onDestroy()
	{
		if (mDocView != null) {
			mDocView.applyToChildren(new ReaderView.ViewMapper() {
				void applyToView(View view) {
					((PageView)view).releaseBitmaps();
				}
			});
		}
		if (core != null)
			core.onDestroy();
		core = null;
		super.onDestroy();
	}

	private void setButtonEnabled(ImageButton button, boolean enabled) {
		button.setEnabled(enabled);
		button.setColorFilter(enabled ? Color.argb(255, 255, 255, 255) : Color.argb(255, 128, 128, 128));
	}

	private void setLinkHighlight(boolean highlight) {
		mLinkHighlight = highlight;
		// LINK_COLOR tint
		mLinkButton.setColorFilter(highlight ? Color.argb(0xFF, 0x00, 0x66, 0xCC) : Color.argb(0xFF, 255, 255, 255));
		// Inform pages of the change.
		mDocView.setLinksEnabled(highlight);
	}

	/**
	 * set search result
	 * @param searchResult
	 */
	public void setReaderSearchResult(ArrayList<GPReaderSearchResult> searchResult) {
		mReaderSearchResult = searchResult;
	}

	/**
	 * get search result
	 */
	public ArrayList<GPReaderSearchResult> getReaderSearchResult() {
		return mReaderSearchResult;
	}

	/*
	 * scroll to index of item in thumbnail recycle view
	 * */
	private void scrollToThumbnailPagePreviewIndex( int index) {
        if(isPagePreviewActive)
            mRecylerPagePreviewLayoutManager.scrollToPositionWithOffset(index, mRecyclerPagePreview.getRight()/2-80);
    }


	/*
	 * display page of the given index and center it
	 * */
	public void jumpToPageAtIndex(int index) {
		// display selected page
		mDocView.pushHistory();
		mDocView.setDisplayedViewIndex(index);

		// scroll to selected page
		scrollToThumbnailPagePreviewIndex(index);
	}

	private void showButtons() {
		if (core == null)
			return;
		if (!mButtonsVisible) {
			mButtonsVisible = true;
			// Update page number text and slider
			int index = mDocView.getDisplayedViewIndex();

			// GP scroll to displaying page
            if(isPagePreviewActive){
                scrollToThumbnailPagePreviewIndex(index);
                mRecyclerPagePreviewAdapter.notifyDataSetChanged();
            }

			if (mTopBarMode == TopBarMode.Search) {
				if (mSearchMode == SearchMode.Lib) {
					mSearchText.requestFocus();
				}
				showKeyboard();
			}

			// GalePress animate show reader bottom thumbnail (up arrow) button
			Animation anim = new TranslateAnimation(0, 0, 0, mReaderShowPageThumbnailsButton.getHeight());
			anim.setDuration(250);
			anim.setAnimationListener(new Animation.AnimationListener() {

				public void onAnimationStart(Animation animation) {
					mReaderShowPageThumbnailsButton.setVisibility(View.GONE);
				}

				public void onAnimationRepeat(Animation animation) {}

				public void onAnimationEnd(Animation animation) {
					if(!isPagePreviewActive)
					ProgressRecycler.setVisibility(View.VISIBLE);
				}
			});
			mReaderShowPageThumbnailsButton.startAnimation(anim);

			anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
			anim.setStartOffset(200);
			anim.setDuration(250);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mTopBarSwitcher.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
				}
			});
			mTopBarSwitcher.startAnimation(anim);

			anim = new TranslateAnimation(0, 0, mRecyclerPagePreview.getHeight(), 0);
			anim.setStartOffset(200);
			anim.setDuration(250);
			anim.setAnimationListener(new Animation.AnimationListener() {

				public void onAnimationStart(Animation animation) {
					mRecyclerPagePreview.setVisibility(View.VISIBLE);
				}

				public void onAnimationRepeat(Animation animation) {}

				public void onAnimationEnd(Animation animation) {

				}
			});
			mRecyclerPagePreview.startAnimation(anim);
		}
	}

	private void hideButtons() {
		if (mButtonsVisible) {
			mButtonsVisible = false;
			hideKeyboard();

			Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
			anim.setDuration(250);
			anim.setAnimationListener(new Animation.AnimationListener() {

				public void onAnimationStart(Animation animation) {}

				public void onAnimationRepeat(Animation animation) {}

				public void onAnimationEnd(Animation animation) {
					mTopBarSwitcher.setVisibility(View.INVISIBLE);
				}
			});
			mTopBarSwitcher.startAnimation(anim);

			anim = new TranslateAnimation(0, 0, 0, mRecyclerPagePreview.getHeight());
			anim.setDuration(250);
			anim.setAnimationListener(new Animation.AnimationListener() {

				public void onAnimationStart(Animation animation) {
					ProgressRecycler.setVisibility(View.INVISIBLE);
				}

				public void onAnimationRepeat(Animation animation) {}

				public void onAnimationEnd(Animation animation) {
					mRecyclerPagePreview.setVisibility(View.INVISIBLE);
				}
			});
			mRecyclerPagePreview.startAnimation(anim);

			// GalePress animate show reader bottom thumbnail (up arrow) button
			anim = new TranslateAnimation(0, 0, mReaderShowPageThumbnailsButton.getHeight(), 0);
			anim.setStartOffset(250);
			anim.setDuration(250);
			anim.setAnimationListener(new Animation.AnimationListener() {

				public void onAnimationStart(Animation animation) {
					mReaderShowPageThumbnailsButton.setVisibility(View.VISIBLE);
				}

				public void onAnimationRepeat(Animation animation) {}

				public void onAnimationEnd(Animation animation) {
					scaleAnimation();
				}
			});
			mReaderShowPageThumbnailsButton.startAnimation(anim);
		}
	}

	private void searchModeOn() {
		if (mTopBarMode != TopBarMode.Search) {
			mTopBarMode = TopBarMode.Search;
			//Focus on EditTextWidget
			if (mSearchMode == SearchMode.App) {
				openSearchPopup();
			}
			else {
				mSearchText.requestFocus();
			}
			showKeyboard();
			mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
		}
	}

	private void searchModeOff() {
		if (mTopBarMode == TopBarMode.Search) {
			mTopBarMode = TopBarMode.Main;
			hideKeyboard();
			mTopBarSwitcher.setDisplayedChild(mTopBarMode.ordinal());
			SearchTaskResult.set(null);
			// Make the ReaderView act on the change to mSearchTaskResult
			// via overridden onChildSetup method.
			mDocView.resetupChildren();
		}

		if (mSearchMode == SearchMode.App && mSearchPopup != null && mSearchPopup.isShowing()) {
			mSearchPopup.dismiss();
		}
	}

	private void makeButtonsView() {
		mButtonsView = getLayoutInflater().inflate(R.layout.document_activity, null);
		mFilenameView = mButtonsView.findViewById(R.id.docNameText);
		mRecyclerPagePreview = mButtonsView.findViewById(R.id.recyclerPagePreview); // GP recycler view
		mSearchButton = mButtonsView.findViewById(R.id.searchButton);
		mOutlineButton = mButtonsView.findViewById(R.id.outlineButton);
		mShareButton = mButtonsView.findViewById(R.id.shareButton); // GP share button
		mTopBarSwitcher = mButtonsView.findViewById(R.id.switcher);
		mSearchBack = mButtonsView.findViewById(R.id.searchBack);
		mSearchFwd = mButtonsView.findViewById(R.id.searchForward);
		mSearchClose = mButtonsView.findViewById(R.id.searchClose);
		mSearchText = mButtonsView.findViewById(R.id.searchText);
		mLinkButton = mButtonsView.findViewById(R.id.linkButton);
		mReflowButton = mButtonsView.findViewById(R.id.reflowButton);

		// GalePress crop and share button
		Drawable icon = ThemeIcon.getInstance().paintIcon(getApplicationContext(), R.drawable.reader_share, ThemeIcon.OPPOSITE_THEME_COLOR_FILTER);
		mShareButton.setBackground(icon);

		// GalePress reader show page thumbnails button
		mReaderShowPageThumbnailsButton = mButtonsView.findViewById(R.id.readerShowPageThumbnailsButton);





		mReaderShowPageThumbnailsButton.setVisibility(View.VISIBLE);
		mReaderShowPageThumbnailsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showButtons();
			}
		});

		mTopBarSwitcher.setVisibility(View.INVISIBLE);

		// GP recycler page preview
		mRecyclerPagePreview.setVisibility(View.INVISIBLE);

		// GP search button
		mSearchButton.setBackground(ThemeIcon.getInstance().paintIcon(getApplicationContext(),R.drawable.reader_search,ThemeIcon.OPPOSITE_THEME_COLOR_FILTER));

		// GP table of contens
		mOutlineButton.setBackground(ThemeIcon.getInstance().paintIcon(getApplicationContext(),R.drawable.reader_contents,ThemeIcon.OPPOSITE_THEME_COLOR_FILTER));

		// GP switcher
		mTopBarSwitcher.setBackgroundColor(ThemeColor.getInstance().getStrongThemeColor());

	}

	private void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

		if (imm == null)
			return;

		if (mSearchMode == SearchMode.App && mPopupSearchEditText != null) {
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
			return;
		}

		if (mSearchText != null)
			imm.toggleSoftInput(InputMethodManager.SHOW_FORCED,0);
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

		if (imm == null)
			return;

		if (mSearchMode == SearchMode.App && mPopupSearchEditText != null) {
			imm.hideSoftInputFromWindow(mPopupSearchEditText.getWindowToken(),0);
			return;
		}

		if (mSearchText != null) {
			imm.hideSoftInputFromWindow(mSearchText.getWindowToken(),0);
		}
	}

	private void search(int direction) {
		hideKeyboard();
		int displayPage = mDocView.getDisplayedViewIndex();
		SearchTaskResult r = SearchTaskResult.get();
		int searchPage = r != null ? r.pageNumber : -1;
		mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
	}

	public void cropAndShareCurrentPage(String b) {

		hideButtons();
		mButtonsView.setVisibility(View.INVISIBLE);
		try {
			if (!savePic(takeScreenShot(DocumentActivity.this), b)) {
				Toast.makeText(DocumentActivity.this, DocumentActivity.this.getResources().getString(R.string.cannot_open_crop), Toast.LENGTH_SHORT).show();
				return;
			}
			Intent intent = new Intent(DocumentActivity.this, CropAndShareActivity.class);
			int display_mode = getResources().getConfiguration().orientation;
			intent.putExtra("displayMode", display_mode);
			startActivity(intent);
		} catch (Exception e) {
			Toast.makeText(DocumentActivity.this, DocumentActivity.this.getResources().getString(R.string.cannot_open_crop), Toast.LENGTH_SHORT).show();
		}
		mButtonsView.setVisibility(View.VISIBLE);
		showButtons();

	}

	private static Bitmap takeScreenShot(Activity activity) {
		View view = activity.getWindow().getDecorView();
		view.setDrawingCacheEnabled(true);
		view.buildDrawingCache();
		Bitmap b1 = view.getDrawingCache();
		Rect frame = new Rect();
		activity.getWindow().getDecorView().getWindowVisibleDisplayFrame(frame);

		int statusBarHeight = frame.top;
		int width = activity.getWindowManager().getDefaultDisplay().getWidth();
		int height = activity.getWindowManager().getDefaultDisplay()
				.getHeight();
		// Bitmap b = Bitmap.createBitmap(b1, 0, 25, 320, 455);
		Bitmap b = Bitmap.createBitmap(b1, 0, statusBarHeight, width, height
				- statusBarHeight);
		view.destroyDrawingCache();

		return b;
	}

	private static boolean savePic(Bitmap b, String strFileName) {
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(strFileName);
			b.compress(Bitmap.CompressFormat.PNG, 90, fos);
			fos.flush();
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	/*
	 * GalePress show thumbnails button scale animation
	 * */
	private void scaleAnimation() {
		final int startTime = 0;
		final int durationTime = 600;

		ScaleAnimation s11 = new ScaleAnimation(1f, 0.8f, 1f, 0.8f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		s11.setFillAfter(true);
		s11.setStartOffset(startTime);
		s11.setDuration(durationTime);
		s11.setAnimationListener(new Animation.AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {}

			@Override
			public void onAnimationEnd(Animation animation) {
				ScaleAnimation s12 = new ScaleAnimation(0.8f, 1f, 0.8f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
				s12.setFillAfter(true);
				s12.setStartOffset(startTime);
				s12.setDuration(durationTime);
				s12.setAnimationListener(new Animation.AnimationListener() {
					@Override
					public void onAnimationStart(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						scaleAnimationReplayCount--;
						if (scaleAnimationReplayCount > 0) {
							scaleAnimation();
						}
						else {
							scaleAnimationReplayCount = 3;
						}
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}
				});
				 mReaderShowPageThumbnailsButton.startAnimation(s12);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {}
		});
		    mReaderShowPageThumbnailsButton.startAnimation(s11);


	}

	@Override
	public boolean onSearchRequested() {
		if (mButtonsVisible && mTopBarMode == TopBarMode.Search) {
			hideButtons();
		} else {
			showButtons();
			searchModeOn();
		}
		return super.onSearchRequested();
	}

	//---------- GalePress Integration - Search [Start]

	public void openSearchPopup() {
		// Inflate the popup_layout.xml
		RelativeLayout viewGroup = findViewById(R.id.reader_search_popup);
		LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		final View layout = layoutInflater.inflate(R.layout.reader_search_popup, viewGroup);

		// Creating the PopupWindow
		mSearchPopup = new PopupWindow(this);
		mSearchPopup.setContentView(layout);
		mSearchPopup.setWidth(RelativeLayout.LayoutParams.MATCH_PARENT);
		mSearchPopup.setHeight(RelativeLayout.LayoutParams.MATCH_PARENT);
		mSearchPopup.setFocusable(true);
		mSearchPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				searchModeOff();
			}
		});
		mSearchPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		mPopupSearchEditText = layout.findViewById(R.id.popup_searchText);
		if (mReaderSearchWord != null)
			mPopupSearchEditText.setText(mReaderSearchWord);

		mPopupSearchEditText.setTextColor(ThemeColor.getInstance().getStrongOppositeThemeColor());
		mPopupSearchEditText.setHintTextColor(ThemeColor.getInstance().getOppositeThemeColor());
		mPopupSearchEditText.requestFocus();
		mPopupSearchEditText.setTypeface(ThemeFont.getInstance().getLightFont(this));
		mPopupSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE  && mPopupSearchEditText.getText().length() > 0) {
					if(mSearchPopup != null && mSearchPopup.isShowing() && mSearchProgressBaseView != null && mSearchClearBaseView != null) {
						mSearchProgressBaseView.setVisibility(View.VISIBLE);
						mSearchClearBaseView.setVisibility(View.GONE);
					}
					mReaderSearchWord = mPopupSearchEditText.getText().toString();
					MuPDFLibrary.getAppInstance().fullTextSearchForReader(mPopupSearchEditText.getText().toString(), getContentId(), DocumentActivity.this);
				}
				return false;
			}
		});
		mPopupSearchEditText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER && mPopupSearchEditText.getText().length() > 0) {
					if (mSearchPopup != null && mSearchPopup.isShowing() && mSearchProgressBaseView != null && mSearchClearBaseView != null) {
						mSearchProgressBaseView.setVisibility(View.VISIBLE);
						mSearchClearBaseView.setVisibility(View.GONE);
					}
					mReaderSearchWord = mPopupSearchEditText.getText().toString();
					MuPDFLibrary.getAppInstance().fullTextSearchForReader(mPopupSearchEditText.getText().toString(), getContentId(), DocumentActivity.this);
				}
				return false;
			}
		});

		// clear butonu
		layout.findViewById(R.id.popup_clearSearch).setBackground(ThemeIcon.getInstance().paintIcon(getApplicationContext(), R.drawable.reader_search_clear, ThemeIcon.THEME_COLOR_FILTER));

		// search progress
		mSearchProgressBaseView = layout.findViewById(R.id.popup_progress_search_base);
		((ProgressBar)layout.findViewById(R.id.popup_search_progress)).getIndeterminateDrawable().setColorFilter(ThemeColor.getInstance().getStrongOppositeThemeColor(), android.graphics.PorterDuff.Mode.MULTIPLY);

		// textview background
		((RelativeLayout) mPopupSearchEditText.getParent()).setBackgroundColor(ThemeColor.getInstance().getThemeColor());

		// input background
		((LinearLayout) mPopupSearchEditText.getParent().getParent()).setBackgroundColor(ThemeColor.getInstance().getStrongThemeColor());

		mSearchClearBaseView = layout.findViewById(R.id.popup_clear_search_base);
		mSearchClearBaseView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mSearchPopup != null && mSearchPopup.isShowing() && mSearchProgressBaseView != null && mSearchClearBaseView != null) {
					mSearchProgressBaseView.setVisibility(View.GONE);
					mSearchClearBaseView.setVisibility(View.VISIBLE);
				}
				mPopupSearchEditText.setText("");
				mReaderSearchWord = mPopupSearchEditText.getText().toString();
				setReaderSearchResult(new ArrayList<GPReaderSearchResult>());
				completeSearch(false);
			}
		});


		mSearhResultListView = layout.findViewById(R.id.reader_search_list);
		if(mReaderSearchResult != null) {
			searchAdapter = new SearchResultAdapter(mReaderSearchResult, DocumentActivity.this);
		}
		else {
			searchAdapter = new SearchResultAdapter(new ArrayList<GPReaderSearchResult>(), this);
		}

		mSearhResultListView.setAdapter(searchAdapter);
		mSearhResultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(mReaderSearchResult != null && mReaderSearchResult.size() > position) {
					GPReaderSearchResult result = mReaderSearchResult.get(position);
					int resultPageIndex = result.getPage() - 1;

					// Get the original search text from the search box
					String searchText = mPopupSearchEditText.getText().toString().trim();

					if(!searchText.isEmpty()) {
						// Move to page first
						mDocView.setDisplayedViewIndex(resultPageIndex);

						// Create a SearchTask to find and highlight the text
						SearchTask searchTask = new SearchTask(DocumentActivity.this, core) {
							@Override
							protected void onTextFound(SearchTaskResult result) {
								// Set the search result which will trigger highlighting
								SearchTaskResult.set(result);
								mDocView.resetupChildren();
							}
						};
						// Start searching on the specific page
						searchTask.go(searchText, 0, resultPageIndex, resultPageIndex);
					}

					// refresh page preview bar
					if(isPagePreviewActive){
						scrollToThumbnailPagePreviewIndex(resultPageIndex);
						mRecyclerPagePreviewAdapter.notifyDataSetChanged();
					}
					searchModeOff();
				}
			}
		});

		layout.findViewById(R.id.reader_search_popup).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				searchModeOff();
			}
		});

		// reader popup background
		layout.findViewById(R.id.reader_search_popup_base).setBackgroundColor(ThemeColor.getInstance().getStrongThemeColor());

		mSearchPopup.showAsDropDown(mTopBarSwitcher, 0, -mTopBarSwitcher.getHeight());
	}

	public void completeSearch(boolean showNotFoundMessage) {
		if(mSearchPopup == null || !mSearchPopup.isShowing())
			return;

		if(mSearchProgressBaseView != null && mSearchClearBaseView != null) {
			mSearchProgressBaseView.setVisibility(View.GONE);
			mSearchClearBaseView.setVisibility(View.VISIBLE);
		}

		if (mSearchDialog != null) {
			mSearchDialog.dismiss();
		}

		if (searchAdapter != null && mReaderSearchResult != null
				&& mReaderSearchResult.size() > 0) {
			searchAdapter.textSearchList = mReaderSearchResult;
			searchAdapter.notifyDataSetChanged();
		} else {
			if(searchAdapter != null && mSearhResultListView != null) {
				searchAdapter.textSearchList = new ArrayList<>();
				searchAdapter.notifyDataSetChanged();
			}
			if (showNotFoundMessage) {
				Toast.makeText(DocumentActivity.this, getResources().getString(R.string.text_not_found), Toast.LENGTH_SHORT).show();
			}
		}
	}

	public class SearchResultAdapter extends BaseAdapter {

		private ArrayList<GPReaderSearchResult> textSearchList;
		private Context mContext;

		SearchResultAdapter(ArrayList<GPReaderSearchResult> textSearchList, Context mContext) {
			this.mContext = mContext;
			this.textSearchList = textSearchList;
		}

		@Override
		public int getCount() {
			return textSearchList.size();
		}

		@Override
		public Object getItem(int position) {
			return textSearchList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@SuppressLint("SetTextI18n")
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				LayoutInflater mInflater = (LayoutInflater)
						mContext.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
				convertView = mInflater.inflate(R.layout.reader_search_listview_item, null);
			}

			GPReaderSearchResult pageItem = textSearchList.get(position);
			TextView title = convertView.findViewById(R.id.reader_search_result_page_title);
			title.setText(HtmlCompat.fromHtml(pageItem.getText(), HtmlCompat.FROM_HTML_MODE_LEGACY));
			title.setTextColor(ThemeColor.getInstance().getStrongOppositeThemeColor());
			title.setTypeface(ThemeFont.getInstance().getRegularFont(mContext));


			TextView page = convertView.findViewById(R.id.reader_search_result_page_page);
			page.setText("" + (pageItem.getPage()));
			page.setTextColor(ThemeColor.getInstance().getStrongOppositeThemeColor());
			page.setTypeface(ThemeFont.getInstance().getLightFont(mContext));


			return convertView;
		}
	}

	//---------- GalePress Integration - Search [End]



	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mButtonsVisible && mTopBarMode != TopBarMode.Search) {
			hideButtons();
		} else {
			showButtons();
			searchModeOff();
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onStop() {
		if (MuPDFLibrary.getAppInstance() != null) {
			MuPDFLibrary.getAppInstance().setMuPDFActivity(null);
		}
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		asyncThumb.cancel(true);
		finish();
	}
}

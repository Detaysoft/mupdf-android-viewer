package com.artifex.mupdf.viewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.artifex.mupdf.viewer.gp.RecyclerAdapter;
import com.artifex.mupdf.viewer.gp.models.PagePreview;
import com.artifex.mupdf.viewer.gp.util.ThemeColor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

public class DocumentActivity extends Activity
{
	public final static String EXTRA_THEME_TYPE = "themeType";
	public final static String EXTRA_FOREGROUND_THEME_COLOR = "foregroundThemeColor";
	/* The core rendering instance */
	enum TopBarMode {Main, Search, More};

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
	private ViewAnimator mTopBarSwitcher;
	private ImageButton  mLinkButton;
	private TopBarMode   mTopBarMode = TopBarMode.Main;
	private ImageButton  mSearchBack;
	private ImageButton  mSearchFwd;
	private ImageButton  mSearchClose;
	private EditText     mSearchText;
	private SearchTask   mSearchTask;

	// GP reader show buttons button
	private RelativeLayout mReaderShowPageThumbnailsButton;

	// GP recycler page preview
	private RecyclerView mRecyclerPagePreview;
	private RecyclerAdapter mRecyclerPagePreviewAdapter;
	private LinearLayoutManager mRecylerPagePreviewLayoutManager;

	private AlertDialog.Builder mAlertBuilder;
	private boolean    mLinkHighlight = false;
	private final Handler mHandler = new Handler();
	private boolean mAlertsActive= false;
	private AlertDialog mAlertDialog;
	private ArrayList<OutlineActivity.Item> mFlatOutline;

	protected int mDisplayDPI;
	private int mLayoutEM = 10;
	private int mLayoutW = 312;
	private int mLayoutH = 504;

	// page thumbnail show button scale animation replay count
	private int scaleAnimationReplayCount = 3;

	protected View mLayoutButton;
	protected PopupMenu mLayoutPopupMenu;

	private MuPDFCore openFile(String path)
	{
		int lastSlashPos = path.lastIndexOf('/');
		mFileName = new String(lastSlashPos == -1
					? path
					: path.substring(lastSlashPos+1));
		System.out.println("Trying to open " + path);
		try
		{
			core = new MuPDFCore(path);
		}
		catch (Exception e)
		{
			System.out.println(e);
			return null;
		}
		catch (java.lang.OutOfMemoryError e)
		{
			//  out of memory is not an Exception, so we catch it separately.
			System.out.println(e);
			return null;
		}
		return core;
	}

	private MuPDFCore openBuffer(byte buffer[], String magic)
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
	@Override
	public void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mDisplayDPI = (int)metrics.densityDpi;

		mAlertBuilder = new AlertDialog.Builder(this);

		if (core == null) {
			if (savedInstanceState != null && savedInstanceState.containsKey("FileName")) {
				mFileName = savedInstanceState.getString("FileName");
			}
		}

		if (core == null) {
			Intent intent = getIntent();

			//---------- GalePress Integration [Start]
			String foregroundColor = intent.getStringExtra(EXTRA_FOREGROUND_THEME_COLOR);
			if (foregroundColor != null) {
				ThemeColor.getInstance().setForegroundColor(foregroundColor);
			}

			int themeType = intent.getIntExtra(EXTRA_THEME_TYPE, 1);
			ThemeColor.getInstance().setThemeType(themeType);

			//---------- GalePress Integration [End]

			byte buffer[] = null;

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
			return;
		}

		createUI(savedInstanceState);
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
		mFlatOutline = null;
		mDocView.mHistory.clear();
		mDocView.refresh();
		mDocView.setDisplayedViewIndex(loc);
	}

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

				mRecyclerPagePreviewAdapter.setSelectedIndex(i);
				scrollToThumbnailPagePreviewIndex(i);

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
		mRecyclerPagePreview = (RecyclerView) mButtonsView.findViewById(R.id.recyclerPagePreview);
		mRecyclerPagePreview.setLayoutManager(mRecylerPagePreviewLayoutManager);

		mRecyclerPagePreview.setBackgroundColor(ThemeColor.getInstance().getThemeColor());

		PagePreview[] ppArray = new PagePreview[core.countPages()];

		// TODO: make width and height dynamic
		Bitmap[] pageThumbnails = core.getPDFThumbnails(60, 90);

		for (int i = 0; i < core.countPages(); i++) {
			ppArray[i] = new PagePreview(i, pageThumbnails[i]);
		}

		mRecyclerPagePreviewAdapter = new RecyclerAdapter(ppArray, this);

		mRecyclerPagePreview.setAdapter(mRecyclerPagePreviewAdapter);

		//---------- GalePress recycle page preview [End]

		// Activate the search-preparing button
		mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOn();
			}
		});

		mSearchClose.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOff();
			}
		});

		// Search invoking buttons are disabled while there is no text specified
		mSearchBack.setEnabled(false);
		mSearchFwd.setEnabled(false);
		mSearchBack.setColorFilter(Color.argb(255, 128, 128, 128));
		mSearchFwd.setColorFilter(Color.argb(255, 128, 128, 128));

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

		//React to Done button on keyboard
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

		mLinkButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				setLinkHighlight(!mLinkHighlight);
			}
		});

		if (core.isReflowable()) {
			mLayoutButton.setVisibility(View.VISIBLE);
			mLayoutPopupMenu = new PopupMenu(this, mLayoutButton);
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
			mLayoutButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mLayoutPopupMenu.show();
				}
			});
		}

		if (core.hasOutline()) {
			mOutlineButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if (mFlatOutline == null)
						mFlatOutline = core.getOutline();
					if (mFlatOutline != null) {
						Intent intent = new Intent(DocumentActivity.this, OutlineActivity.class);
						Bundle bundle = new Bundle();
						bundle.putInt("POSITION", mDocView.getDisplayedViewIndex());
						bundle.putSerializable("OUTLINE", mFlatOutline);
						intent.putExtras(bundle);
						startActivityForResult(intent, OUTLINE_REQUEST);
					}
				}
			});
		} else {
			mOutlineButton.setVisibility(View.GONE);
		}

		// Reenstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		mDocView.setDisplayedViewIndex(prefs.getInt("page"+mFileName, 0));

		// GalePress don't show buttons in first open instead show a button to let user know there are buttons
		 if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false))
			scaleAnimation();

		if(savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false))
			searchModeOn();

		// Stick the document view and the buttons overlay into a parent view
		RelativeLayout layout = new RelativeLayout(this);
		layout.setBackgroundColor(Color.DKGRAY);
		layout.addView(mDocView);
		layout.addView(mButtonsView);
		setContentView(layout);
	}

	//---------- GalePress Integration [Start]

	// Temporary content id for test
	public String getContentId() {
		return "100069";
	}

	public ReaderView getReaderView() {
		return mDocView;
	}

	//---------- GalePress Integration [End]

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case OUTLINE_REQUEST:
			if (resultCode >= RESULT_FIRST_USER) {
				mDocView.pushHistory();
				mDocView.setDisplayedViewIndex(resultCode-RESULT_FIRST_USER);
			}
			break;
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

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
			edit.putInt("page"+mFileName, mDocView.getDisplayedViewIndex());
			edit.apply();
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

	/*
	 * scroll to index of item in thumbnail recycle view
	 * */
	private void scrollToThumbnailPagePreviewIndex(int index) {
		// TODO: offset parameter needs to be dynamic
		mRecylerPagePreviewLayoutManager.scrollToPositionWithOffset(index, mRecyclerPagePreview.getRight()/2-80);
	}

	/*
	 * display page of the given index and center it
	 * */
	public void onPagePreviewItemClick(int index) {
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
			scrollToThumbnailPagePreviewIndex(index);
			mRecyclerPagePreviewAdapter.notifyDataSetChanged();

			if (mTopBarMode == TopBarMode.Search) {
				mSearchText.requestFocus();
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

				public void onAnimationEnd(Animation animation) {}
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
				public void onAnimationEnd(Animation animation) {}
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

				public void onAnimationEnd(Animation animation) {}
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

				public void onAnimationStart(Animation animation) {}

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
			mSearchText.requestFocus();
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
	}

	private void makeButtonsView() {
		mButtonsView = getLayoutInflater().inflate(R.layout.document_activity, null);
		mFilenameView = (TextView)mButtonsView.findViewById(R.id.docNameText);
		mRecyclerPagePreview = (RecyclerView) mButtonsView.findViewById(R.id.recyclerPagePreview);
		mSearchButton = (ImageButton)mButtonsView.findViewById(R.id.searchButton);
		mOutlineButton = (ImageButton)mButtonsView.findViewById(R.id.outlineButton);
		mTopBarSwitcher = (ViewAnimator)mButtonsView.findViewById(R.id.switcher);
		mSearchBack = (ImageButton)mButtonsView.findViewById(R.id.searchBack);
		mSearchFwd = (ImageButton)mButtonsView.findViewById(R.id.searchForward);
		mSearchClose = (ImageButton)mButtonsView.findViewById(R.id.searchClose);
		mSearchText = (EditText)mButtonsView.findViewById(R.id.searchText);
		mLinkButton = (ImageButton)mButtonsView.findViewById(R.id.linkButton);
		mLayoutButton = mButtonsView.findViewById(R.id.layoutButton);

		// GalePress reader show page thumbnails button
		mReaderShowPageThumbnailsButton = (RelativeLayout)mButtonsView.findViewById(R.id.readerShowPageThumbnailsButton);

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
	}

	private void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.showSoftInput(mSearchText, 0);
	}

	private void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
	}

	private void search(int direction) {
		hideKeyboard();
		int displayPage = mDocView.getDisplayedViewIndex();
		SearchTaskResult r = SearchTaskResult.get();
		int searchPage = r != null ? r.pageNumber : -1;
		mSearchTask.go(mSearchText.getText().toString(), direction, displayPage, searchPage);
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
		super.onStop();
	}

	@Override
	public void onBackPressed() {
		finish();
	}
}

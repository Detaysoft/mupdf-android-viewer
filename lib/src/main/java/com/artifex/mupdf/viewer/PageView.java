package com.artifex.mupdf.viewer;

import com.artifex.mupdf.fitz.Cookie;
import com.artifex.mupdf.fitz.Link;
import com.artifex.mupdf.fitz.Quad;
import com.artifex.mupdf.viewer.gp.models.GPAnnotationInfo;
import com.artifex.mupdf.viewer.gp.CustomPulseProgress;
import com.artifex.mupdf.viewer.gp.util.ModuleConfig;
import com.artifex.mupdf.viewer.gp.webviews.ViewAnnotation;
import com.artifex.mupdf.viewer.gp.webviews.WebViewAnnotation;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap.Config;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.os.AsyncTask;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

// Make our ImageViews opaque to optimize redraw
class OpaqueImageView extends androidx.appcompat.widget.AppCompatImageView {

	public OpaqueImageView(Context context) {
		super(context);
	}

	@Override
	public boolean isOpaque() {
		return true;
	}
}

@SuppressLint("ViewConstructor")
public class PageView extends ViewGroup {
	private final MuPDFCore mCore;

	private static final int HIGHLIGHT_COLOR = 0x80cc6600;
	private static final int LINK_COLOR = 0x800066cc;
	// private static final int BOX_COLOR = 0xFF4444FF;
	private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
	private static final int PROGRESS_DIALOG_DELAY = 200;

	protected final Context mContext;

	protected     int       mPageNumber;
	private       Point     mParentSize;
	protected     Point     mSize;   // Size of page at minimum zoom
	protected     float     mSourceScale;

	private       ImageView mEntire; // Image rendered at minimum zoom
	private       Bitmap    mEntireBm;
	private       Matrix    mEntireMat;
	private       AsyncTask<Void,Void,Link[]> mGetLinkInfo;
	private       CancellableAsyncTask<Void, Void> mDrawEntire;

	private       Point     mPatchViewSize; // View size on the basis of which the patch was created
	private       Rect      mPatchArea;
	private       ImageView mPatch;
	private       Bitmap    mPatchBm;
	private       CancellableAsyncTask<Void,Void> mDrawPatch;
	private       Quad      mSearchBoxes[];
	protected     Link      mLinks[];
	protected	  ArrayList<GPAnnotationInfo> mGPLinks; // GalePress annotation links
	private       View      mSearchView;
	private       boolean   mIsBlank;
	private       boolean   mHighlightLinks;

	private       ProgressBar mBusyIndicator;
	private final Handler   mHandler = new Handler();

	AtomicInteger atomicInteger = new AtomicInteger(); // GalePress generates view ids

	public PageView(Context c, MuPDFCore core, Point parentSize, Bitmap sharedHqBm) {
		super(c);
		mContext = c;
		mCore = core;
		mParentSize = parentSize;
		setBackgroundColor(BACKGROUND_COLOR);
		mEntireBm = Bitmap.createBitmap(parentSize.x, parentSize.y, Config.ARGB_8888);
		mPatchBm = sharedHqBm;
		mEntireMat = new Matrix();
	}

	private void reinit() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel();
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel();
			mDrawPatch = null;
		}

		if (mGetLinkInfo != null) {
			mGetLinkInfo.cancel(true);
			mGetLinkInfo = null;
		}

		mIsBlank = true;
		mPageNumber = 0;

		if (mSize == null)
			mSize = mParentSize;

		if (mEntire != null) {
			mEntire.setImageBitmap(null);
			mEntire.invalidate();
		}

		if (mPatch != null) {
			mPatch.setImageBitmap(null);
			mPatch.invalidate();
		}

		mPatchViewSize = null;
		mPatchArea = null;

		mSearchBoxes = null;
		mLinks = null;
		mGPLinks = null; // GalePress links
	}

	public void releaseResources() {
		reinit();

		if (mBusyIndicator != null) {
			removeView(mBusyIndicator);
			mBusyIndicator = null;
		}
	}

	public void releaseBitmaps() {
		reinit();

		// recycle bitmaps before releasing them.

		if (mEntireBm!=null)
			mEntireBm.recycle();
		mEntireBm = null;

		if (mPatchBm!=null)
			mPatchBm.recycle();
		mPatchBm = null;
	}

	public void blank(int page) {
		reinit();
		mPageNumber = page;

		if (mBusyIndicator == null) {
			mBusyIndicator = new ProgressBar(mContext);
			mBusyIndicator.setIndeterminate(true);
			addView(mBusyIndicator);
		}

		setBackgroundColor(BACKGROUND_COLOR);
	}

	@SuppressLint("StaticFieldLeak")
	public void setPage(int page, PointF size) {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel();
			mDrawEntire = null;
		}

		mIsBlank = false;
		// Highlights may be missing because mIsBlank was true on last draw
		if (mSearchView != null)
			mSearchView.invalidate();

		mPageNumber = page;
		if (mEntire == null) {
			mEntire = new OpaqueImageView(mContext);
			mEntire.setScaleType(ImageView.ScaleType.MATRIX);
			addView(mEntire);
		}

		// Calculate scaled size that fits within the screen limits
		// This is the size at minimum zoom
		mSourceScale = Math.min(mParentSize.x/size.x, mParentSize.y/size.y);
		mSize = new Point((int)(size.x*mSourceScale), (int)(size.y*mSourceScale));

		mEntire.setImageBitmap(null);
		mEntire.invalidate();

		// needed for get link info
		final PageView pageView = this;

		// Get the link info in the background
		mGetLinkInfo = new AsyncTask<Void,Void,Link[]>() {
			@SuppressLint("WrongThread")
			protected Link[] doInBackground(Void... v) {
				return getLinkInfo();
			}

			@Override
			protected void onPreExecute() {
				// clear last annotations, it needed to prevent adding annotations over and over in different pages
				clearSomeCoolAnnotationStaff(pageView);
			}

			protected void onPostExecute(Link[] v) {
				mLinks = v;
				someCoolAnnotationStaff();
				if (mSearchView != null)
					mSearchView.invalidate();
			}
		};

		mGetLinkInfo.execute();

		// Render the page in the background
		mDrawEntire = new CancellableAsyncTask<Void, Void>(getDrawPageTask(mEntireBm, mSize.x, mSize.y, 0, 0)) {

			@Override
			public void onPreExecute() {
				setBackgroundColor(BACKGROUND_COLOR);
				mEntire.setImageBitmap(null);
				mEntire.invalidate();

				if (mBusyIndicator == null) {
					mBusyIndicator = new ProgressBar(mContext);
					mBusyIndicator.setIndeterminate(true);
					addView(mBusyIndicator);
					mBusyIndicator.setVisibility(INVISIBLE);
					mHandler.postDelayed(new Runnable() {
						public void run() {
							if (mBusyIndicator != null)
								mBusyIndicator.setVisibility(VISIBLE);
						}
					}, PROGRESS_DIALOG_DELAY);
				}
			}

			@Override
			public void onPostExecute(Void result) {
				removeView(mBusyIndicator);
				mBusyIndicator = null;
				mEntire.setImageBitmap(mEntireBm);
				mEntire.invalidate();
				setBackgroundColor(Color.TRANSPARENT);

			}
		};

		mDrawEntire.execute();

		if (mSearchView == null) {
			mSearchView = new View(mContext) {
				@Override
				protected void onDraw(final Canvas canvas) {
					super.onDraw(canvas);
					// Work out current total scale factor
					// from source to view
					final float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
					@SuppressLint("DrawAllocation") final Paint paint = new Paint();

					if (!mIsBlank && mSearchBoxes != null) {
						paint.setColor(HIGHLIGHT_COLOR);
						for (Quad q : mSearchBoxes) {
							@SuppressLint("DrawAllocation") Path path = new Path();
							path.moveTo(q.ul_x * scale, q.ul_y * scale);
							path.lineTo(q.ll_x * scale, q.ll_y * scale);
							path.lineTo(q.lr_x * scale, q.lr_y * scale);
							path.lineTo(q.ur_x * scale, q.ur_y * scale);
							path.close();
							canvas.drawPath(path, paint);
						}
					}

					if (!mIsBlank && mLinks != null && mHighlightLinks) {
						paint.setColor(LINK_COLOR);
						for (Link link : mLinks)
							canvas.drawRect(link.bounds.x0*scale, link.bounds.y0*scale,
									link.bounds.x1*scale, link.bounds.y1*scale,
									paint);
					}
				}
			};

			addView(mSearchView);
		}
		requestLayout();
	}

	public void setSearchBoxes(Quad[] searchBoxes) {
		mSearchBoxes = searchBoxes;
		if (mSearchView != null)
			mSearchView.invalidate();
	}

	public void setLinkHighlighting(boolean f) {
		mHighlightLinks = f;
		if (mSearchView != null)
			mSearchView.invalidate();
	}

	//---------- GalePress Integration [Start]

	private void bringAnnotationsToFront(PageView pageView) {
		for (int i = 0; i < pageView.getChildCount(); i++) {
			View view = pageView.getChildAt(i);
			if (view instanceof WebView) {
				view.bringToFront();
			}
		}
	}

	private void clearSomeCoolAnnotationStaff(PageView pageView) {
		clearGPWebAnnotations(pageView);
		clearGPModals(pageView);
		clearGPPageLinks(pageView);
		clearGPWebLinks(pageView);
		clearCustomProgress(pageView);

		if (mGPLinks != null) {
			mGPLinks.clear();
		}
	}

	public ArrayList<View> getGPAnnotations(PageView pageView) {
		ArrayList<View> gpAnnotations = new ArrayList<>();
		for (int i = 0; i < pageView.getChildCount(); i++) {
			View view = pageView.getChildAt(i);
			if (view instanceof WebView) {
				gpAnnotations.add(view);
			}
		}
		return gpAnnotations;
	}


	public ArrayList<View> getGPCustomProgress(PageView pageView) {
		ArrayList<View> gpprogress = new ArrayList<>();
		for (int i = 0; i < pageView.getChildCount(); i++) {
			View view = pageView.getChildAt(i);
			if (view instanceof CustomPulseProgress) {
				gpprogress.add(view);
			}
		}
		return gpprogress;
	}

	public ArrayList<View> getGPModals(PageView pageView) {
		ArrayList<View> modals = new ArrayList<>();
		for (int i = 0; i < pageView.getChildCount(); i++) {
			View view = pageView.getChildAt(i);
			if (view.getTag() != null && view.getTag().toString().compareTo("modal") == 0) {
				modals.add(view);
			}
		}
		return modals;
	}

	public ArrayList<View> getGPPageLinks(PageView pageView) {
		ArrayList<View> modals = new ArrayList<>();
		for (int i = 0; i < pageView.getChildCount(); i++) {
			View view = pageView.getChildAt(i);
			if (view.getTag() != null && view.getTag().toString().compareTo("pagelink") == 0) {
				modals.add(view);
			}
		}
		return modals;
	}

	public ArrayList<View> getGPWebLinks(PageView pageView) {
		ArrayList<View> modals = new ArrayList<>();
		for (int i = 0; i < pageView.getChildCount(); i++) {
			View view = pageView.getChildAt(i);
			if (view.getTag() != null && view.getTag().toString().compareTo("weblink") == 0) {
				modals.add(view);
			}
		}
		return modals;
	}

	/*
	 * Interaktif iceriklerin temizlenmesi
	 **/
	private void clearGPWebAnnotations(PageView pageView) {
		ArrayList<View> gpAnnotations = getGPAnnotations(pageView);

		for (int i = 0; i < gpAnnotations.size(); i++) {

			View view = gpAnnotations.get(i);

			if (view instanceof WebView) {
				WebView webView = (WebView) view;
				webView.loadUrl("");
				webView.stopLoading();

				try {
					Class.forName("android.webkit.WebView")
							.getMethod("onPause", (Class[]) null)
							.invoke(webView, (Object[]) null);

				} catch (Exception cnfe) {
					cnfe.printStackTrace();
				}

				webView.destroy();
				pageView.removeView(view);
				pageView.invalidate();
			}

		}
	}

	/*
	 * Modallarin sayfadan temizlenmesi
	 * */
	public void clearGPModals(PageView pageView){
		ArrayList<View> modals = getGPModals(pageView);
		for(int i=0; i < modals.size(); i++){
			View view = modals.get(i);
			pageView.removeView(view);
			pageView.invalidate();
		}
	}

	/*
	 * PageLinklerin sayfadan temizlenmesi
	 * */
	public void clearGPPageLinks(PageView pageView){
		ArrayList<View> pageLinks = getGPPageLinks(pageView);
		for(int i=0; i < pageLinks.size(); i++){
			View view = pageLinks.get(i);
			pageView.removeView(view);
			pageView.invalidate();
		}
	}

	/*
	 * WebLinklerin sayfadan temizlenmesi
	 * */
	public void clearGPWebLinks(PageView pageView){
		ArrayList<View> webLinks = getGPWebLinks(pageView);
		for(int i=0; i < webLinks.size(); i++){
			View view = webLinks.get(i);
			pageView.removeView(view);
			pageView.invalidate();
		}
	}

	/*
	 * Custom loading animasyonunun temizlenmesi
	 * */
	public void clearCustomProgress(PageView pageView){
		ArrayList<View> gpAnnotations = getGPCustomProgress(pageView);
		for(int i=0; i < gpAnnotations.size(); i++){
			View view = gpAnnotations.get(i);
			pageView.removeView(view);
			pageView.invalidate();
		}
	}

	/*
	 * Interaktif iceriklerin sayfaya eklenmesi
	 * */
	private void someCoolAnnotationStaff() {
		/*
		 * interaktif iceriklerin bilgileri alinana kadar activity'nin kapatilmasi durumu
		 * ve Finteraktif iceriklerin varligi kontrol ediliyor.
		 * */
		if (mContext == null || mLinks == null)
			return;

		if (mGPLinks == null) {
			mGPLinks = new ArrayList<GPAnnotationInfo>();
		}

		final float scale = mSourceScale*(float)getWidth()/(float)mSize.x;

		for (Link l : mLinks){

			final GPAnnotationInfo link = new GPAnnotationInfo(l);
			mGPLinks.add(link);

			final int left = (int)(l.bounds.x0 * scale);
			final int top = (int) (l.bounds.y0 * scale);
			int right = (int) (l.bounds.x1 * scale);
			int bottom = (int) (l.bounds.y1 * scale);

			CustomPulseProgress progressBar;

			if(!link.isInternal && !link.isModal) {
				Log.e("mGetLinkInfo", "Not a modal and internal link");
				int progressSize = 40;
				progressBar = new CustomPulseProgress(mContext);
				progressBar.layout((left+right)/2 - progressSize/2, (top+bottom)/2 - progressSize/2, (left+right)/2+progressSize, (top+bottom)/2+progressSize);

			}
			else {
				Log.e("mGetLinkInfo", "modal or internal link");
				progressBar = null;
			}

			if (link.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_BOOKMARK) {
				ViewAnnotation view = new ViewAnnotation(mContext, link);
				view.layout(left,top,right,bottom);
				view.setBackgroundColor(Color.TRANSPARENT);
				view.setTag("pagelink");
				view.readerView = ((DocumentActivity) mContext).getReaderView();
				view.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						((DocumentActivity) mContext).jumpToPageAtIndex(link.internalLinkPageIndex);
					}
				});
				addView(view);
			}
			else if((link.isWebAnnotation())){
				if(link.isModal){
					Button modalButton = new Button(mContext);
					modalButton.layout(left,top,right,bottom);
					modalButton.setBackgroundColor(Color.TRANSPARENT);
					modalButton.setTag("modal");
					modalButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(link.url));
                            mContext.startActivity(intent);
                        }
                    });
                    addView(modalButton);
				}
				else{
					String url = link.getSourceUrlPath(mContext);

					// Web Annotations
					final WebViewAnnotation web = new WebViewAnnotation(mContext, link, progressBar);
					web.layout(left, top, right, bottom);
					web.setLayoutParams(new ViewGroup.LayoutParams(right - left, bottom - top));
					web.readerView = ((DocumentActivity) mContext).getReaderView();
					web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

					web.setId(atomicInteger.incrementAndGet());
					link.webViewId = web.getId();

					if(!link.isInternal && !link.isModal)
						progressBar.setId(link.webViewId);

					if (link.isWebAnnotation()) {
						web.loadUrl(url);
					}
					addView(web);

				}
			}
			else if((link.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_MAP) ){
				// Map Annotations
				// http://adem.me/map/index.html?lat=41.033621&lon=28.952785&zoom=16&w=400&h=300&mapType=0
				String authority = !ModuleConfig.isTest ? "www.galepress.com" : "test.galepress.com";
				Uri.Builder builder = new Uri.Builder();
				builder.scheme("https");
				builder.authority(authority);
				builder.appendPath("api");
				builder.appendPath("files");
				builder.appendPath("map_html");
				builder.appendPath("index.html");
				builder.appendQueryParameter("lat",String.valueOf(link.location.getLatitude()));
				builder.appendQueryParameter("lon",String.valueOf(link.location.getLongitude()));
				builder.appendQueryParameter("zoom",String.valueOf(link.zoom));
				builder.appendQueryParameter("w",String.valueOf(right-left));
				builder.appendQueryParameter("h",String.valueOf(bottom-top));
				builder.appendQueryParameter("mapType",String.valueOf(link.mapType));
				String mapUrl = builder.build().toString();


				// Web Annotations
				final WebViewAnnotation web = new WebViewAnnotation(mContext, link, progressBar);
				web.layout(left, top, right, bottom);
				web.setLayoutParams(new ViewGroup.LayoutParams(right - left, bottom - top));
				web.readerView = ((DocumentActivity) mContext).getReaderView();
				web.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

				web.setId(atomicInteger.incrementAndGet());
				link.webViewId = web.getId();
				web.loadUrl(mapUrl);
				addView(web);
			}
			else if(link.componentAnnotationTypeId == GPAnnotationInfo.COMPONENT_TYPE_ID_WEBLINK){
				final boolean isMailto = link.isMailto;
				ViewAnnotation view = new ViewAnnotation(mContext, link);
				view.layout(left,top,right,bottom);
				view.setBackgroundColor(Color.TRANSPARENT);
				view.setTag("weblink");
				view.readerView = ((DocumentActivity) mContext).getReaderView();
				view.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isMailto) {
                            Intent intent = new Intent(Intent.ACTION_SEND);
                            intent.setType("message/rfc822");
                            //intent.setType("text/html");
                            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{link.url});
                            intent.putExtra(Intent.EXTRA_SUBJECT, " ");
                            intent.putExtra(Intent.EXTRA_TEXT, " ");
                            try {
                                mContext.startActivity(Intent.createChooser(intent, "Send mail..."));
                            } catch (android.content.ActivityNotFoundException ex) {
                                Toast.makeText(mContext, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                            }
                        } else if (link.url.contains("http")) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(link.url));
                            mContext.startActivity(intent);
                        } else {
							int number = Integer.parseInt(link.url.substring(link.url.indexOf('#') + 1, link.url.indexOf(',')));
							if (link.url.contains("#")) {
								((DocumentActivity) mContext).jumpToPageAtIndex(number - 1);
							}
						}
                    }
                });
                addView(view);
			}

			if(!link.isInternal && !link.isModal) {
				addView(progressBar);
			}
		}

		if (mSearchView != null)
			mSearchView.invalidate();
	}

	//---------- GalePress Integration [END]

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int x, y;
		if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
			x = mSize.x;
		} else {
			x = MeasureSpec.getSize(widthMeasureSpec);
		}
		if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED) {
			y = mSize.y;
		} else {
			y = MeasureSpec.getSize(heightMeasureSpec);
		}

		setMeasuredDimension(x, y);

		if (mBusyIndicator != null) {
			int limit = Math.min(mParentSize.x, mParentSize.y)/2;
			mBusyIndicator.measure(View.MeasureSpec.AT_MOST | limit, View.MeasureSpec.AT_MOST | limit);
		}
	}



	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int w = right-left;
		int h = bottom-top;

		if (mEntire != null) {
			if (mEntire.getWidth() != w || mEntire.getHeight() != h) {
				mEntireMat.setScale(w/(float)mSize.x, h/(float)mSize.y);
				mEntire.setImageMatrix(mEntireMat);
				mEntire.invalidate();
			}
			mEntire.layout(0, 0, w, h);
		}

		if (mSearchView != null) {
			mSearchView.layout(0, 0, w, h);
		}

		if (mPatchViewSize != null) {
			if (mPatchViewSize.x != w || mPatchViewSize.y != h) {
				// Zoomed since patch was created
				mPatchViewSize = null;
				mPatchArea = null;
				if (mPatch != null) {
					mPatch.setImageBitmap(null);
					mPatch.invalidate();
				}
			} else {
				mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
			}
		}

		if (mBusyIndicator != null) {
			int bw = mBusyIndicator.getMeasuredWidth();
			int bh = mBusyIndicator.getMeasuredHeight();

			mBusyIndicator.layout((w-bw)/2, (h-bh)/2, (w+bw)/2, (h+bh)/2);
		}

		//---------- GalePress Integration [Start]
		// Scale annotations

		if (mGPLinks != null) {
			{
				PageView pageView = this;
				for (int i = 0; i < pageView.getChildCount(); i++) {
					View view = pageView.getChildAt(i);
					if (view instanceof WebView) { //Annotation viewlar WebView
						float original_x;
						float original_y;
						if (pageView.mGPLinks != null) {
							for (GPAnnotationInfo link : pageView.mGPLinks) {
								if (link.webViewId == view.getId()) {
									original_x = link.muPdfLink.bounds.x0 * pageView.mSourceScale;
									original_y = link.muPdfLink.bounds.y0 * pageView.mSourceScale;
									view.setPivotX(0);
									view.setPivotY(0);
									view.setX(original_x * w/(float)mSize.x);
									view.setY(original_y * h/(float)mSize.y);
									view.setScaleX(w/(float)mSize.x);
									view.setScaleY(h/(float)mSize.y);
									view.invalidate();
								}
							}
						}
					} else if (view instanceof CustomPulseProgress) { //interaktif icerikler uzerindeki animasyon view
						  float original_x;
						  float original_y;
						  int progressSize = 40;
						  CustomPulseProgress progress = (CustomPulseProgress) view;
						  if (pageView.mGPLinks != null) {
							  for (GPAnnotationInfo link : pageView.mGPLinks) {
                                  if (link.webViewId == view.getId()) {
                                      original_x = (link.muPdfLink.bounds.x0 + link.muPdfLink.bounds.x1) / 2 * pageView.mSourceScale - (float)progressSize / 2;
                                      original_y = (link.muPdfLink.bounds.y0 + link.muPdfLink.bounds.y1) / 2 * pageView.mSourceScale - (float)progressSize / 2;
									  progress.setPivotX(0);
									  progress.setPivotY(0);
									  progress.setX(original_x * w / (float) mSize.x);
									  progress.setY(original_y * h / (float) mSize.y);
									  progress.setScaleX(w / (float) mSize.x);
									  progress.setScaleY(h / (float) mSize.y);
									  progress.invalidate();
								  }
							  }
						  }
					  }
				}
			}
			//---------- GalePress Integration [End]
		}
	}

    public void updateHq(boolean update) {
		Rect viewArea = new Rect(getLeft(),getTop(),getRight(),getBottom());
		if (viewArea.width() == mSize.x || viewArea.height() == mSize.y) {
			// If the viewArea's size matches the unzoomed size, there is no need for an hq patch
			if (mPatch != null) {
				mPatch.setImageBitmap(null);
				mPatch.invalidate();
			}
		} else {
			final Point patchViewSize = new Point(viewArea.width(), viewArea.height());
			final Rect patchArea = new Rect(0, 0, mParentSize.x, mParentSize.y);

			// Intersect and test that there is an intersection
			if (!patchArea.intersect(viewArea))
				return;

			// Offset patch area to be relative to the view top left
			patchArea.offset(-viewArea.left, -viewArea.top);

			boolean area_unchanged = patchArea.equals(mPatchArea) && patchViewSize.equals(mPatchViewSize);

			// If being asked for the same area as last time and not because of an update then nothing to do
			if (area_unchanged && !update)
				return;

			boolean completeRedraw = !area_unchanged;

			// Stop the drawing of previous patch if still going
			if (mDrawPatch != null) {
				mDrawPatch.cancel();
				mDrawPatch = null;
			}

			// Create and add the image view if not already done
			if (mPatch == null) {
				mPatch = new OpaqueImageView(mContext);
				mPatch.setScaleType(ImageView.ScaleType.MATRIX);
				addView(mPatch);
				mSearchView.bringToFront();
				bringAnnotationsToFront(this);

			}

			CancellableTaskDefinition<Void, Void> task;

			if (completeRedraw)
				task = getDrawPageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
								patchArea.left, patchArea.top);
			else
				task = getUpdatePageTask(mPatchBm, patchViewSize.x, patchViewSize.y,
						patchArea.left, patchArea.top);

			mDrawPatch = new CancellableAsyncTask<Void,Void>(task) {

				public void onPostExecute(Void result) {
					mPatchViewSize = patchViewSize;
					mPatchArea = patchArea;
					mPatch.setImageBitmap(mPatchBm);
					mPatch.invalidate();
					//requestLayout();
					// Calling requestLayout here doesn't lead to a later call to layout. No idea
					// why, but apparently others have run into the problem.
					mPatch.layout(mPatchArea.left, mPatchArea.top, mPatchArea.right, mPatchArea.bottom);
				}
			};

			mDrawPatch.execute();
		}
	}

	/*
	public void update() {
		// Cancel pending render task
		if (mDrawEntire != null) {
			mDrawEntire.cancel();
			mDrawEntire = null;
		}

		if (mDrawPatch != null) {
			mDrawPatch.cancel();
			mDrawPatch = null;
		}

		// Render the page in the background
		mDrawEntire = new CancellableAsyncTask<Void, Void>(getUpdatePageTask(mEntireBm, mSize.x, mSize.y, 0, 0, mSize.x, mSize.y)) {

			public void onPostExecute(Void result) {
				mEntire.setImageBitmap(mEntireBm);
				mEntire.invalidate();
			}
		};

		mDrawEntire.execute();

		updateHq(true);
	}
	*/

	public void removeHq() {
			// Stop the drawing of the patch if still going
			if (mDrawPatch != null) {
				mDrawPatch.cancel();
				mDrawPatch = null;
			}

			// And get rid of it
			mPatchViewSize = null;
			mPatchArea = null;
			if (mPatch != null) {
				mPatch.setImageBitmap(null);
				mPatch.invalidate();
			}
	}

	public int getPage() {
		return mPageNumber;
	}

	@Override
	public boolean isOpaque() {
		return true;
	}

	public int hitLink(Link link) {
		if (link.isExternal()) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link.uri));
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); // API>=21: FLAG_ACTIVITY_NEW_DOCUMENT
			mContext.startActivity(intent);
			return 0;
		} else {
			return mCore.resolveLink(link);
		}
	}

	public int hitLink(float x, float y) {
		// Since link highlighting was implemented, the super class
		// PageView has had sufficient information to be able to
		// perform this method directly. Making that change would
		// make MuPDFCore.hitLinkPage superfluous.
		float scale = mSourceScale*(float)getWidth()/(float)mSize.x;
		float docRelX = (x - getLeft())/scale;
		float docRelY = (y - getTop())/scale;

		if (mLinks != null)
			for (Link l: mLinks)
				if (l.bounds.contains(docRelX, docRelY))
					return hitLink(l);
		return 0;
	}

	protected CancellableTaskDefinition<Void, Void> getDrawPageTask(final Bitmap bm, final int sizeX, final int sizeY,
			final int patchX, final int patchY) {
		return new MuPDFCancellableTaskDefinition<Void, Void>() {
			@Override
			public Void doInBackground(Cookie cookie, Void ... params) {
				// Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
				// is not incremented when drawing.
				// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
				//		Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				//	bm.eraseColor(0);
				mCore.drawPage(bm, mPageNumber, sizeX, sizeY, patchX, patchY,cookie);
				return null;
			}
		};

	}

	protected CancellableTaskDefinition<Void, Void> getUpdatePageTask(final Bitmap bm, final int sizeX, final int sizeY,
			final int patchX, final int patchY)
	{
		return new MuPDFCancellableTaskDefinition<Void, Void>() {
			@Override
			public Void doInBackground(Cookie cookie, Void ... params) {
				// Workaround bug in Android Honeycomb 3.x, where the bitmap generation count
				// is not incremented when drawing.
				// if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB &&
				//		Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				//	bm.eraseColor(0);
				mCore.updatePage(bm, mPageNumber, sizeX, sizeY, patchX, patchY, cookie);
				return null;
			}
		};
	}

	protected Link[] getLinkInfo() {
		return mCore.getPageLinks(mPageNumber);
	}
}

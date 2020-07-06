package com.artifex.mupdf.viewer;

import android.annotation.SuppressLint;
import android.os.Build;
import android.view.View;

class Stepper {
	private final View mPoster;
	private final Runnable mTask;
	private boolean mPending;

	Stepper(View v, Runnable r) {
		mPoster = v;
		mTask = r;
		mPending = false;
	}

	@SuppressLint("NewApi")
	void prod() {
		if (!mPending) {
			mPending = true;
			mPoster.postOnAnimation(new Runnable() {
				@Override
				public void run() {
					mPending = false;
					mTask.run();
				}
			});
		}
	}
}

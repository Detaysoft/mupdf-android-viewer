package com.artifex.mupdf.viewer;

import com.artifex.mupdf.fitz.Quad;

public class SearchTaskResult {
	final String txt;
	final int pageNumber;
	final Quad[] searchBoxes;
	static private SearchTaskResult singleton;

	SearchTaskResult(String _txt, int _pageNumber, Quad[] _searchBoxes) {
		txt = _txt;
		pageNumber = _pageNumber;
		searchBoxes = _searchBoxes;
	}

	static public SearchTaskResult get() {
		return singleton;
	}

	static void set(SearchTaskResult r) {
		singleton = r;
	}
}

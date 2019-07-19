package com.artifex.mupdf.viewer.gp.webviews;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.artifex.mupdf.viewer.gp.models.GPAnnotationInfo;
import com.artifex.mupdf.viewer.ReaderView;

/**
 * Created by gunes on 18/08/2017.
 */

public class ViewAnnotation extends View {

    public float x1 , x2, y1 , y2;
    public float left, top ;
    public ReaderView readerView;
    public GPAnnotationInfo linkInfoExternal;
    public GPAnnotationInfo linkInfoInternal;
    private Context context;
    public boolean isHorizontalScrolling, isDummyAction;
    private MotionEvent previousMotionEvent;
    final ViewAnnotation viewAnnotation;
    private GestureDetector gestureDetector;

    public ViewAnnotation(Context context) {
        super(context);
        this.context = context;
        viewAnnotation = this;
        setCustomTouchListener();
    }

    public ViewAnnotation(Context context, GPAnnotationInfo linkInfoExternal) {
        super(context);
        this.context = context;
        this.linkInfoExternal = linkInfoExternal;
        viewAnnotation = this;
        setCustomTouchListener();
    }
    
    public void setCustomTouchListener(){
        this.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float dx,dy;
                if(event.getAction() == MotionEvent.ACTION_DOWN){
                    // Action DOWN
                    viewAnnotation.x1 = event.getX();
                    viewAnnotation.y1 = event.getY();
                    viewAnnotation.setPreviousMotionEvent(event);
                    viewAnnotation.isHorizontalScrolling = false;
                    if(viewAnnotation.isDummyAction){
                        return false;
                    }
                    else{
                        return true;
                    }

                }
                else if(event.getAction() == MotionEvent.ACTION_MOVE){
                    // Action MOVE
                    viewAnnotation.x2 = event.getX();
                    viewAnnotation.y2 = event.getY();
                    dx = viewAnnotation.x2 - viewAnnotation.x1;
                    dy = viewAnnotation.y2 - viewAnnotation.y1;
                    if(Math.abs(dx) > 10 || Math.abs(dy) > 10){
                        if(!(Math.abs(dx) >  Math.abs(dy))) {
                            // vertical
                            viewAnnotation.isHorizontalScrolling = false;
                            return false;
                        }else {
                            // horizontal
                            viewAnnotation.isHorizontalScrolling = true;
                            if(viewAnnotation.getPreviousMotionEvent()!=null && viewAnnotation.getPreviousMotionEvent().getAction() != MotionEvent.ACTION_MOVE) {
                                MotionEvent previousEvent = viewAnnotation.getPreviousMotionEvent();
                                viewAnnotation.setPreviousMotionEvent(null);
                                previousEvent.setLocation(previousEvent.getX() + left, previousEvent.getY() + top);
                                readerView.onTouchEvent(previousEvent);
                            }
                            event.setLocation(event.getX() + left, event.getY() + top); // viewAnnotationview size is not equal to page size. Optimize the location for page.
                            readerView.onTouchEvent(event);
                            return true;
                        }
                    }
                }
                else if(event.getAction() == MotionEvent.ACTION_UP){
                    // Action UP
                    if(viewAnnotation.isHorizontalScrolling){
                        viewAnnotation.isHorizontalScrolling = false;
                        event.setLocation(event.getX() + left, event.getY() + top); // viewAnnotationview size is not equal to page size. Optimize the location for page.
                        readerView.onTouchEvent(event);
                        return true;
                    }
                    else{
                        if(viewAnnotation.getPreviousMotionEvent()!=null) {
                            MotionEvent previousEvent = viewAnnotation.getPreviousMotionEvent();
                            viewAnnotation.setPreviousMotionEvent(null);
                            viewAnnotation.isDummyAction = true;
                            viewAnnotation.onTouchEvent(previousEvent);
                        }
                        return false;
                    }

                }



                return false;
            }
        });
    }

    public MotionEvent getPreviousMotionEvent() {
        return previousMotionEvent;
    }

    public void setPreviousMotionEvent(MotionEvent event) {
        if(event == null)
            this.previousMotionEvent = null;
        else
            this.previousMotionEvent = MotionEvent.obtain(event);
    }
}

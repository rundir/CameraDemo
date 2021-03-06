package com.boyko.videorecorder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.widget.FrameLayout;

public class PreviewTextureView extends FrameLayout {

	private TextureView textureView;
	private boolean isRecording;

	public PreviewTextureView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	public PreviewTextureView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public PreviewTextureView(Context context) {
		super(context);
		init();
	}

	private void init() {
		textureView = new TextureView(getContext());
		addView(textureView);
	}

	public boolean isRecording() {
		return isRecording;
	}

	public void setRecording(boolean isRecording) {
		this.isRecording = isRecording;
		invalidate();
	}

	public void setSurfaceTextureListener(SurfaceTextureListener listener) {
		textureView.setSurfaceTextureListener(listener);
		
	}
	
	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		Log.d(VIEW_LOG_TAG, "dispatchDraw " + isRecording);
		if(isRecording)
			drawIndicator(canvas);
	}
	
	private void drawIndicator(Canvas c){
		Path borderPath = new Path();
		borderPath.lineTo(c.getWidth(), 0);
		borderPath.lineTo(c.getWidth(), c.getHeight());
		borderPath.lineTo(0, c.getHeight());
		borderPath.lineTo(0, 0);
		Paint paint = new Paint();
		paint.setColor(0xffCC171E);
		paint.setStrokeWidth(Convenience.dpToPx(getContext(), 2));
		paint.setStyle(Paint.Style.STROKE);
		c.drawPath(borderPath, paint);
		paint.setColor(0xffCC171E);
		paint.setStyle(Paint.Style.FILL);
		c.drawCircle(Convenience.dpToPx(getContext(), 13), Convenience.dpToPx(getContext(), 13), 
				Convenience.dpToPx(getContext(), 4), paint);
		paint.setAntiAlias(true);
		paint.setTextSize(Convenience.dpToPx(getContext(), 13)); //some size
		paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
		paint.setTextAlign(Align.CENTER);
		c.drawText("Recording...", c.getWidth()/2, c.getHeight() - 13, paint);
	}
}

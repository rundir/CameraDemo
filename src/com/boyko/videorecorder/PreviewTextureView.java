package com.boyko.videorecorder;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.FrameLayout;

public class PreviewTextureView extends FrameLayout {

	private TextureView textureView;
	private View overlay;
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
		LayoutInflater.from(getContext()).inflate(R.layout.preview_textureview, this, true);
		textureView = (TextureView)findViewById(R.id.textureView);
		overlay = findViewById(R.id.overlay);
	}

	public boolean isRecording() {
		return isRecording;
	}

	public void setRecording(boolean isRecording) {
		this.isRecording = isRecording;
		overlay.setVisibility(isRecording?View.VISIBLE:View.GONE);
	}

	public void setSurfaceTextureListener(SurfaceTextureListener listener) {
		textureView.setSurfaceTextureListener(listener);
		
	}
	

}

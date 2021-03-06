package com.boyko.videorecorder;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

/**
 * A simple wrapper around a Camera and a SurfaceView that renders a
 * centered preview of the Camera to the surface. We need to center the
 * SurfaceView because not all devices have cameras that support preview
 * sizes at the same aspect ratio as the device's display.
 */
public class Preview extends ViewGroup implements SurfaceHolder.Callback{
	private final String TAG = "Preview";

	private SurfaceView surfaceView;
	private SurfaceHolder holder;
	private Size previewSize;
	private List<Size> supportedPreviewSizes;
	private Camera camera;

	private boolean isRecording;

	private Paint paint = new Paint();

	public Preview(Context context) {
		super(context);

		surfaceView = new SurfaceView(context);
		addView(surfaceView);

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		holder = surfaceView.getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setExposure(int new_exposure) {
		Parameters parameters = this.camera.getParameters();
		parameters.setExposureCompensation(new_exposure);
	}

	public void setCamera(Camera camera) {
		this.camera = camera;
		if (this.camera != null) {
			supportedPreviewSizes = this.camera.getParameters().getSupportedPreviewSizes();
			requestLayout();
		}
	}

	public void switchCamera(Camera camera) {
		setCamera(camera);
		try {
			camera.setPreviewDisplay(holder);
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}
		Camera.Parameters parameters = camera.getParameters();
		parameters.setPreviewSize(previewSize.width, previewSize.height);
		requestLayout();

		camera.setParameters(parameters);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// We purposely disregard child measurements because act as a
		// wrapper to a SurfaceView that centers the camera preview instead
		// of stretching it.
		final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
		final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
		setMeasuredDimension(width, height);

		if (supportedPreviewSizes != null) {
			previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
		}
		
		Log.d(TAG, "onMeasure");
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if (changed && getChildCount() > 0) {
			final View child = getChildAt(0);

			final int width = r - l;
			final int height = b - t;

			int previewWidth = width;
			int previewHeight = height;
			if (previewSize != null) {
				previewWidth = previewSize.height;
				previewHeight = previewSize.width;
			}

			// Center the child SurfaceView within the parent.
			if (width * previewHeight > height * previewWidth) {
				final int scaledChildWidth = previewWidth * height / previewHeight;
				child.layout((width - scaledChildWidth) / 2, 0, (width + scaledChildWidth) / 2, height);
			} else {
				final int scaledChildHeight = previewHeight * width / previewWidth;
				child.layout(0, (height - scaledChildHeight) / 2, width, (height + scaledChildHeight) / 2);
			}
		}
		
		Log.d(TAG, "onLayout");
	}

	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated " + holder + ", " + camera);
		// The Surface has been created, acquire the camera and tell it
		// where
		// to draw.
		try {
			if (this.camera != null) {
				this.camera.setPreviewDisplay(holder);
				this.camera.setDisplayOrientation(90);
			}
		} catch (IOException exception) {
			Log.e(TAG, "IOException caused by setPreviewDisplay()", exception);
		}

		setWillNotDraw(false);
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed " + holder + ", " + camera);
		// Surface will be destroyed when we return, so stop the preview.
		if (this.camera != null) {
			//this.camera.stopPreview();
		}
	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the
		// requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		Log.d(TAG, "surfaceChanged " + holder + ", " + camera);
		// Now that the size is known, set up the camera parameters and
		// begin
		// the preview.
		Camera.Parameters parameters = this.camera.getParameters();
		parameters.setPreviewSize(previewSize.width, previewSize.height);
		parameters.setRecordingHint(true);

		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		List<int[]> fps_ranges = parameters.getSupportedPreviewFpsRange();
		int selected_min_fps = -1, selected_max_fps = -1, selected_diff = -1;
		for (int[] fps_range : fps_ranges) {
			int min_fps = fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
			int max_fps = fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
			if (min_fps <= profile.videoFrameRate * 1000 && max_fps >= profile.videoFrameRate * 1000) {
				int diff = max_fps - min_fps;
				if (selected_diff == -1 || diff < selected_diff) {
					selected_min_fps = min_fps;
					selected_max_fps = max_fps;
					selected_diff = diff;
				}
			}
		}
		if (selected_min_fps == -1) {
			selected_diff = -1;
			int selected_dist = -1;
			for (int[] fps_range : fps_ranges) {
				int min_fps = fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
				int max_fps = fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
				int diff = max_fps - min_fps;
				int dist = -1;
				if (max_fps < profile.videoFrameRate * 1000)
					dist = profile.videoFrameRate * 1000 - max_fps;
				else
					dist = min_fps - profile.videoFrameRate * 1000;
				if (selected_dist == -1 || dist < selected_dist || (dist == selected_dist && diff < selected_diff)) {
					selected_min_fps = min_fps;
					selected_max_fps = max_fps;
					selected_dist = dist;
					selected_diff = diff;
				}
			}
			parameters.setPreviewFpsRange(selected_min_fps, selected_max_fps);

		} else {
			parameters.setPreviewFpsRange(selected_min_fps, selected_max_fps);
		}

		int maxExposureCompensation = parameters.getMaxExposureCompensation();
		parameters.setExposureCompensation(maxExposureCompensation);

		// parameters.setAutoExposureLock(true);
		// parameters.setAutoWhiteBalanceLock(true);


		this.camera.setParameters(parameters);

		this.camera.startPreview();

		requestLayout();
		invalidate();
	}

	public SurfaceHolder getHolder() {
		return holder;
	}

	public void setRecording(boolean isRecording) {
		this.isRecording = isRecording;
		invalidate();
	}
	
	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		if(isRecording){
			drawIndicator(canvas);
		}
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
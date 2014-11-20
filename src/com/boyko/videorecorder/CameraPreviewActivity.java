/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.boyko.videorecorder;

import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.example.android.common.media.CameraHelper;

public class CameraPreviewActivity extends Activity {
	private Preview preview;
	Camera camera;

	private MediaRecorder mediaRecorder;
	protected boolean isRecording;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	       getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			setContentView(R.layout.activity_main);

	       
		SensorManager mySensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

		Sensor lightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
		if (lightSensor != null) {
			final float maxvalue = lightSensor.getMaximumRange(); 
			
			mySensorManager.registerListener(new SensorEventListener() {
				@Override
				public void onSensorChanged(SensorEvent event) {
					float f = event.values[0];
					Logger.d(f + "/" + maxvalue);
					if(preview != null){
						int exp;
						if(f<20)
							exp = 12;
						else if(f>20 && f<40)
							exp = 0;
						else
							exp = -12;
						preview.setExposure(exp );
					}
				}

				@Override
				public void onAccuracyChanged(Sensor sensor, int accuracy) {}
				
			}, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
		}

		// Create a RelativeLayout container that will hold a SurfaceView,
		// and set it as the content of our activity.
		preview = new Preview(this);
		((ViewGroup) findViewById(R.id.preview)).addView(preview);
		preview.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				if (isRecording) {
					Logger.d("STOP RECORD");
					// stop recording and release camera
					mediaRecorder.stop(); // stop the recording
					releaseMediaRecorder(); // release the MediaRecorder object
					camera.lock(); // take camera access back from
									// MediaRecorder

					// inform the user that recording has stopped
					isRecording = false;
				} else {
					Logger.d("START RECORD");
					new MediaPrepareTask().execute();
				}
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Open the default i.e. the first rear facing camera.
		camera = CameraHelper.getDefaultFrontFacingCameraInstance();
		if (camera == null)
			camera = CameraHelper.getDefaultCameraInstance();
		preview.setCamera(camera);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isRecording) {
			mediaRecorder.stop();
			releaseMediaRecorder();
			camera.lock();
		}
		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
		if (camera != null) {
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
	}

	void prepareRecorder() {
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		Camera.Parameters parameters = camera.getParameters();

		List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
		Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes, preview.getWidth(),
				preview.getHeight());

		// Use the same size for recording profile.
		profile.videoFrameWidth = optimalSize.width;
		profile.videoFrameHeight = optimalSize.height;

		// likewise for the camera object itself.
		parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);

		camera.setParameters(parameters);

		// BEGIN_INCLUDE (configure_media_recorder)
		mediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		camera.startPreview();
		camera.unlock();
		mediaRecorder.setCamera(camera);

		// Step 2: Set sources
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		mediaRecorder.setProfile(profile);

		// Step 4: Set output file
		mediaRecorder.setOutputFile(CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO).toString());
		// END_INCLUDE (configure_media_recorder)

		mediaRecorder.setOnInfoListener(new OnInfoListener() {

			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				Logger.d("what = " + what + ", extra = " + extra);
			}
		});
		mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
			public void onError(MediaRecorder mr, int what, int extra) {
				Logger.d("MediaRecorder error: " + what + " extra: " + extra);
			}
		});

		mediaRecorder.setPreviewDisplay(preview.getHolder().getSurface());
		mediaRecorder.setOrientationHint(90);

		// Step 5: Prepare configured MediaRecorder
		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			Logger.d("IllegalStateException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
		} catch (IOException e) {
			Logger.d("IOException preparing MediaRecorder: " + e.getMessage());
			releaseMediaRecorder();
		}
	}

	private void releaseMediaRecorder() {
		if (mediaRecorder != null) {
			// clear recorder configuration
			mediaRecorder.reset();
			// release the recorder object
			mediaRecorder.release();
			mediaRecorder = null;
			// Lock camera for later use i.e taking it back from MediaRecorder.
			// MediaRecorder doesn't need it anymore and we will release it if
			// the activity pauses.
			camera.lock();
		}
	}

	/**
	 * Asynchronous task for preparing the {@link android.media.MediaRecorder}
	 * since it's a long blocking operation.
	 */
	class MediaPrepareTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... voids) {
			prepareRecorder();
			mediaRecorder.start();
			isRecording = true;
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
		}
	}

}

package com.boyko.videorecorder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView.SurfaceTextureListener;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.Toast;
import android.widget.VideoView;

import com.boyko.videorecorder.CustomAdapterView.OnItemTouchListener;
import com.example.android.common.media.CameraHelper;

public class GridViewFragment extends Fragment {

	private Camera camera;

	private MediaRecorder mediaRecorder;
	private boolean isRecording;
	private boolean isPlaying;
	private int currentItemPlayed = -1;
	
	private CustomAdapterView gridView;
	private List<FriendStub> list;

	private FriendsAdapter adapter;

	private VideoView videoView;

	private View videoBody;

	private SurfaceTexture surface;

	private int surfaceWidth;

	private int surfaceHeight;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.video_gridview_fragment, null);
		
		videoBody = v.findViewById(R.id.video_body);
		videoView = (VideoView)v.findViewById(R.id.video_view);
		videoView.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.d(getTag(), videoView.getX()+", " +videoView.getX() + ", " + videoView.getWidth() +", " + videoView.getHeight());
				videoBody.setVisibility(View.INVISIBLE);
				
			}
		});
	
		gridView = (CustomAdapterView)v.findViewById(R.id.grid_view);
		gridView.setItemClickListener(new OnItemTouchListener() {
			@Override
			public boolean onItemClick(CustomAdapterView parent, View view, int position, long id) {
				if(id == -1)
					return false;
				
				videoView.stopPlayback();
				if(isPlaying && currentItemPlayed == position){
					videoBody.setVisibility(View.INVISIBLE);
					currentItemPlayed = -1;
				}else{
					FriendStub fs = (FriendStub)((FriendsAdapter)parent.getAdapter()).getItem(position);
					videoView.setVideoURI(fs.videoPath);
					LayoutParams params = new FrameLayout.LayoutParams(view.getWidth(), view.getHeight());
					videoBody.setLayoutParams(params);
					videoBody.setX(view.getX());
					videoBody.setY(view.getY());
					videoBody.setVisibility(View.VISIBLE);
					videoView.start();
					currentItemPlayed = position;
				}
				isPlaying = !isPlaying;
				return true;
			}
			@Override
			public boolean onItemLongClick(CustomAdapterView parent, View view, int position, long id) {
				Logger.d("START RECORD");
				if(id == -1)
					return false;

				adapter.setRecording(true);
				new MediaPrepareTask().execute();
				return true;
			}
			@Override
			public boolean onItemStopTouch() {
				if (isRecording) {
					Logger.d("STOP RECORD");
					adapter.setRecording(false);
					// stop recording and release camera
					try{
						mediaRecorder.stop(); // stop the recording
					}catch(RuntimeException e){
						Toast.makeText(getActivity(), "Video is too short", Toast.LENGTH_SHORT).show();
					}
					releaseMediaRecorder(); // release the MediaRecorder object
					camera.lock(); // take camera access back from
									// MediaRecorder

					// inform the user that recording has stopped
					isRecording = false;
				}
				return false;
			}

			@Override
			public boolean onCancelTouch() {
				Log.d(getTag(), "onCancelTouch");
				if (isRecording) {
					Logger.d("STOP RECORD");
					adapter.setRecording(false);
					// stop recording and release camera
					try{
						mediaRecorder.stop(); // stop the recording
					}catch(RuntimeException e){
						Toast.makeText(getActivity(), "Video is too short", Toast.LENGTH_SHORT).show();
					}
					releaseMediaRecorder(); // release the MediaRecorder object
					camera.lock(); // take camera access back from
									// MediaRecorder

					// inform the user that recording has stopped
					isRecording = false;
				}
				return false;
			}
		});
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		list = new ArrayList<FriendStub>(8);
		
		for (int i = 1; i<9; i++) {
			FriendStub stub = new FriendStub();
			stub.name = "friend "+ (i);
			stub.videoPath = Uri.parse("android.resource://" + getActivity().getPackageName() + "/" + R.raw.small);
			stub.imagePath = i+".jpg";
			list.add(stub);
		}
		
		adapter = new FriendsAdapter(getActivity(), list);
		gridView.setAdapter(adapter);
		
		adapter.setListener(new SurfaceTextureListener() {
			
			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			}
			
			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
				Log.d(getTag(), "onSurfaceTextureSizeChanged");
			}
			
			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				Log.d(getTag(), "onSurfaceTextureDestroyed");
				camera.stopPreview();
					camera.release();
					camera = null;
		          return true;
			}
			
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
				Log.d(getTag(), "onSurfaceTextureAvailable");

				
				GridViewFragment.this.surface = surface;
				GridViewFragment.this.surfaceWidth = width;
				GridViewFragment.this.surfaceHeight = height;
				
		          try {
		        	  camera = CameraHelper.getDefaultFrontFacingCameraInstance();
		        	  camera.setPreviewTexture(surface);
		              camera.setDisplayOrientation(90);
		              camera.startPreview();
		          } catch (IOException ioe) {
		              // Something bad happened
		          }
				
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.d(getTag(), "onResume");
		// Open the default i.e. the first rear facing camera.
//		if (camera == null)
//			camera = CameraHelper.getDefaultCameraInstance();
//		adapter.setCamera(camera);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		if (isRecording) {
			mediaRecorder.stop();
			releaseMediaRecorder();
			camera.lock();
		}
		// Because the Camera object is a shared resource, it's very
		// important to release it when the activity is paused.
	}
	
	void prepareRecorder() {
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		Camera.Parameters parameters = camera.getParameters();

		List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
		Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes, surfaceWidth,
				surfaceHeight);

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

		//mediaRecorder.setPreviewDisplay(surface);
		//mediaRecorder.setOrientationHint(90);

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

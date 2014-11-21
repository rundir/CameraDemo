package com.boyko.videorecorder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.boyko.videorecorder.CustomAdapterView.OnItemClickListener;
import com.boyko.videorecorder.CustomAdapterView.OnItemLongClickListener;
import com.example.android.common.media.CameraHelper;

public class GridViewFragment extends Fragment {

	Camera camera;

	private MediaRecorder mediaRecorder;
	protected boolean isRecording;
	
	private CustomAdapterView gridView;
	private List<FriendStub> list;

	private VideosAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.video_gridview_fragment, null);
	
		gridView = (CustomAdapterView)v.findViewById(R.id.grid_view);
		gridView.setItemClickListener(new OnItemClickListener() {
			@Override
			public boolean onItemClick(CustomAdapterView parent, View view, int position, long id) {
				Toast.makeText(getActivity(), "touch " + id, Toast.LENGTH_SHORT).show();
				for (int i = 0; i<list.size(); i++) {
					FriendStub fs = list.get(i);
					if(id == i){
						fs.isPlaying = true;
					}else{
						fs.isPlaying = false;
					}
				}
				adapter.notifyDataSetChanged();
				return true;
			}
		});
		gridView.setLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(CustomAdapterView parent, View view, int position, long id) {
				Toast.makeText(getActivity(), "touch long", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		
		return v;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		
		list = new ArrayList<GridViewFragment.FriendStub>(8);
		
		for (int i = 1; i<9; i++) {
			FriendStub stub = new FriendStub();
			stub.name = "friend "+ (i);
			list.add(stub);
		}
		
		
		adapter = new VideosAdapter(getActivity(), list);
		gridView.setAdapter(adapter);
	}

	@Override
	public void onResume() {
		super.onResume();
		// Open the default i.e. the first rear facing camera.
		camera = CameraHelper.getDefaultFrontFacingCameraInstance();
		if (camera == null)
			camera = CameraHelper.getDefaultCameraInstance();
		adapter.setCamera(camera);
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
		if (camera != null) {
			adapter.setCamera(null);
			camera.release();
			camera = null;
		}
	}
	
	void prepareRecorder() {
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		Camera.Parameters parameters = camera.getParameters();

		List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
		Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes, adapter.getPreviewWidth(),
				adapter.getPreviewHeight());

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

		mediaRecorder.setPreviewDisplay(adapter.getPreviewSurface());
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

	
	private class FriendStub{
		String name;
		boolean isPlaying;
	}
	
	private class VideosAdapter extends BaseAdapter{

		private Context context;
		private List<FriendStub> list;
		private Preview preview;
		private Camera camera;

		public VideosAdapter(Context context, List<FriendStub> list) {
			this.context = context;
			this.list = list;
		}

		public Surface getPreviewSurface() {
			return preview.getHolder().getSurface();
		}

		public int getPreviewWidth() {
			return preview.getHeight();
		}

		public int getPreviewHeight() {
			return preview.getWidth();
		}

		public void setCamera(Camera camera) {
			if(preview!=null)
				preview.setCamera(camera);
			this.camera = camera;
		}

		@Override
		public int getCount() {
			return list.size() + 1;
		}

		@Override
		public FriendStub getItem(int position) {
			if(position < (int)(getCount()/2))
				return list.get(position);
			else
				return list.get(position-1);
		}

		@Override
		public long getItemId(int position) {
			if(position < (int)(getCount()/2))
				return position;
			else
				return position - 1;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v;
			if(position == getCount()/2){
				v = getUserView(position, convertView, parent);
			}else{
				v = getFriendView(position, convertView, parent);
			}
			
			return v;
		}

		private View getUserView(int position, View convertView, ViewGroup parent) {
			preview = new Preview(context);
			if(camera != null)
				preview.setCamera(camera);
			return preview;
		}

		private View getFriendView(int position, View convertView, ViewGroup parent) {
			View v = LayoutInflater.from(context).inflate(R.layout.friendview_item, null);
			
			TextView tw_name = (TextView) v.findViewById(R.id.textView1);
			final VideoView videoView = (VideoView) v.findViewById(R.id.videoView1);
			
			FriendStub st = getItem(position);
			tw_name.setText(st.name);
			videoView.setVideoURI(Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.small));
			
			if(st.isPlaying){
				videoView.start();
				Log.w("adapter", "isPlay " + position);
			}else
				Log.w("adapter", "isnt Play " + position);
			
			v.setTag("friend");
			return v;
		}
	}
		
}

package com.boyko.videorecorder;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FriendsAdapter extends BaseAdapter {

	private Context context;
	private List<FriendStub> list;
	private PreviewTextureView preview;
	private boolean isRecording;
	
	private TextureView.SurfaceTextureListener listener;

	public FriendsAdapter(Context context, List<FriendStub> list) {
		this.context = context;
		this.list = list;
	}

	public void setListener(TextureView.SurfaceTextureListener listener) {
		this.listener = listener;
	}
	
	public void setRecording(boolean b) {
		isRecording = b;
		if(preview!=null)
			preview.setRecording(b);
	}

	@Override
	public int getCount() {
		return list.size() + 1;
	}

	@Override
	public FriendStub getItem(int position) {
		if (position < (int) (getCount() / 2))
			return list.get(position);
		else
			return list.get(position - 1);
	}

	@Override
	public long getItemId(int position) {
		if (position == getCount() / 2)
			return -1;
		if (position < (int) (getCount() / 2))
			return position;
		else
			return position - 1;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v;
		if (position == getCount() / 2) {
			v = getUserView(position, convertView, parent);
		} else {
			v = getFriendView(position, convertView, parent);
		}

		return v;
	}

	private View getUserView(int position, View convertView, ViewGroup parent) {
		preview = new PreviewTextureView(context);
		preview.setSurfaceTextureListener(listener);
		preview.setRecording(isRecording);
		return preview;
	}

	private View getFriendView(int position, View convertView, ViewGroup parent) {
		View v = LayoutInflater.from(context).inflate(R.layout.friendview_item, null);

		TextView tw_name = (TextView) v.findViewById(R.id.textView1);
		ImageView img_thumb = (ImageView) v.findViewById(R.id.img_thumb);

		FriendStub st = getItem(position);
		try {
			Drawable d = Drawable.createFromStream(context.getAssets().open(st.imagePath), null);
			img_thumb.setImageDrawable(d);
		} catch (IOException e) {
			e.printStackTrace();
		}

		tw_name.setText(st.name);
		return v;
	}
}
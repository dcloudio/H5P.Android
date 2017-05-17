package io.dcloud.js.map.amap.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

class PopViewLayout extends LinearLayout {
	private ImageView mImageView;
	private TextView mTextView;

	public PopViewLayout(Context context, String label, Drawable icon) {
		super(context);
		mTextView = new TextView(context);
		if (!TextUtils.isEmpty(label)) {
			mTextView.setText(label);
		}
		mImageView = new ImageView(context);
		if (icon != null) {
			mImageView.setImageDrawable(icon);
		}
		addView(mImageView);
		addView(mTextView);
	}

	void setBubbleLabel(String label) {
		if (!TextUtils.isEmpty(label)) {
			mTextView.setText(label);
		}
	}

	void setBubbleIcon(Drawable drawable) {
		if ( drawable != null) {
			mImageView.setImageDrawable(drawable);
		}
	}

}

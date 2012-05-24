package com.jldroid.twook.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

public class UnderlineDrawable extends Drawable {

	private int mThickness;
	private int mColor;
	
	private static Paint sPaint = new Paint();
	
	public UnderlineDrawable(int thickness, int color) {
		super();
		mThickness = thickness;
		mColor = color;
	}
	
	@Override
	public void draw(Canvas pCanvas) {
		sPaint.setColor(mColor);
		Rect bounds = getBounds();
		pCanvas.drawRect(bounds.left, bounds.bottom - mThickness, bounds.right, bounds.bottom, sPaint);
	}

	@Override
	public void setAlpha(int pAlpha) {
	}

	@Override
	public void setColorFilter(ColorFilter pCf) {
	}

	@Override
	public int getOpacity() {
		return PixelFormat.TRANSPARENT;
	}

}

/*
 * Copyright (C) 2007-2014 Geometer Plus <contact@geometerplus.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301, USA.
 */

package org.geometerplus.zlibrary.ui.android.view;

import org.geometerplus.zlibrary.core.view.ZLView;

import android.graphics.Bitmap;
import android.util.Log;

class BitmapManager {
	private final int SIZE = 2;
	private final Bitmap[] myBitmaps = new Bitmap[SIZE];
    //一个Index对应一张Bitmap
	private final ZLView.PageIndex[] myIndexes = new ZLView.PageIndex[SIZE];

	private int myWidth;
	private int myHeight;

	private final ZLAndroidWidget myWidget;

	BitmapManager(ZLAndroidWidget widget) {
		myWidget = widget;
	}

	void setSize(int w, int h) {
		if (myWidth != w || myHeight != h) {
			myWidth = w;
			myHeight = h;
			for (int i = 0; i < SIZE; ++i) {
				myBitmaps[i] = null;
				myIndexes[i] = null;
			}
			System.gc();
			System.gc();
			System.gc();
		}
	} 
    
    public Bitmap getBitmap(ZLView.PageIndex index){
        Log.i("ZYStudio", "getBitmap  start:"+"|"+index.toString()+"|"+System.currentTimeMillis()%10000);
        Bitmap bit=getBitmapInternal(index);
        Log.i("ZYStudio", "getBitmap  end:"+"|"+index.toString()+"|"+System.currentTimeMillis()%10000);
        return bit;
    }
	
	private Bitmap getBitmapInternal(ZLView.PageIndex index) {
		for (int i = 0; i < SIZE; ++i) {
            //myBitmap只存了两张图片，但是有去取第三张图片的时候
			if (index == myIndexes[i]) {
				return myBitmaps[i];
			}
		}
        //getInternalIndex中有转换,这个index不会大于１的.
		final int iIndex = getInternalIndex(index);
		myIndexes[iIndex] = index;
		if (myBitmaps[iIndex] == null) {
			try {
				myBitmaps[iIndex] = Bitmap.createBitmap(myWidth, myHeight, Bitmap.Config.RGB_565);
			} catch (OutOfMemoryError e) {
				System.gc();
				System.gc();
				myBitmaps[iIndex] = Bitmap.createBitmap(myWidth, myHeight, Bitmap.Config.RGB_565);
			}
		}
        myWidget.drawOnBitmap(myBitmaps[iIndex], index);
		return myBitmaps[iIndex];
	}

	private int getInternalIndex(ZLView.PageIndex index) {
		for (int i = 0; i < SIZE; ++i) {
			if (myIndexes[i] == null) {
				return i;
			}
		}
		for (int i = 0; i < SIZE; ++i) {
            //就是哪一个不是当前页面的cache，哪一个就是要返回的那个index了
			if (myIndexes[i] != ZLView.PageIndex.current) {
				return i;
			}
		}
		throw new RuntimeException("That's impossible");
	}

	void reset() {
		for (int i = 0; i < SIZE; ++i) {
			myIndexes[i] = null;
		}
	}

	void shift(boolean forward) {
		for (int i = 0; i < SIZE; ++i) {
			if (myIndexes[i] == null) {
				continue;
			}
			myIndexes[i] = forward ? myIndexes[i].getPrevious() : myIndexes[i].getNext();
		}
	}
}

/*
 * Copyright (C) 2010-2014 Geometer Plus <contact@geometerplus.com>
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

package org.geometerplus.android.fbreader.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.geometerplus.fbreader.tree.FBTree;

import zystudio.debug.FBDebug;
import android.util.Log;
import android.widget.BaseAdapter;

public abstract class TreeAdapter extends BaseAdapter {

    private final TreeActivity myActivity;
    private final List<FBTree> myItems;

    protected TreeAdapter(TreeActivity activity) {
        myActivity = activity;
        myItems = Collections.synchronizedList(new ArrayList<FBTree>());
        activity.setListAdapter(this);
    }

    protected TreeActivity getActivity() {
        return myActivity;
    }

    public void remove(final FBTree item) {
       FBDebug.logLibTree("TreeAdapter.remove"); 
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myItems.remove(item);
                notifyDataSetChanged();
            }
        });
    }

    public void add(final FBTree item) {
       FBDebug.logLibTree("TreeAdapter.add()"); 
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myItems.add(item);
                notifyDataSetChanged();
            }
        });
    }

    public void add(final int index, final FBTree item) {
       FBDebug.logLibTree("TreeAdapter.add(index,FBTree)"); 
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                myItems.add(index, item);
                notifyDataSetChanged();
            }
        });
    }

    public void replaceAll(final Collection<FBTree> items, final boolean invalidateViews) {
       FBDebug.logLibTree("TreeAdapter.replaceAll(items, invalidateViews():"+items.size()+"|"+invalidateViews); 
       for(FBTree myItem:items){
           FBDebug.logLibTree("TreeAdapter.replaceAll() each item:"+myItem.getName()); 
       }
        myActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (myItems) {
                    myItems.clear();
                    myItems.addAll(items);
                }
                notifyDataSetChanged();
                if (invalidateViews) {
                    myActivity.getListView().invalidateViews();
                }
            }
        });
    }

    @Override
    public void notifyDataSetChanged() {
       FBDebug.logLibTree("TreeAdapter.notifyDataSetChanged"); 
        Log.i("ZYStudio", "notifyDataSetChanged occured");
        super.notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return myItems.size();
    }

    @Override
    public FBTree getItem(int position) {
        return myItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getIndex(FBTree item) {
        return myItems.indexOf(item);
    }

    public FBTree getFirstSelectedItem() {
        synchronized (myItems) {
            for (FBTree t : myItems) {
                if (myActivity.isTreeSelected(t)) {
                    return t;
                }
            }
        }
        return null;
    }

}
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

package org.geometerplus.android.fbreader.libraryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.geometerplus.android.fbreader.api.FBReaderIntents;
import org.geometerplus.fbreader.book.AbstractBookCollection;
import org.geometerplus.fbreader.book.Author;
import org.geometerplus.fbreader.book.Book;
import org.geometerplus.fbreader.book.BookEvent;
import org.geometerplus.fbreader.book.BookQuery;
import org.geometerplus.fbreader.book.Bookmark;
import org.geometerplus.fbreader.book.BookmarkQuery;
import org.geometerplus.fbreader.book.Filter;
import org.geometerplus.fbreader.book.HighlightingStyle;
import org.geometerplus.fbreader.book.SerializerUtil;
import org.geometerplus.fbreader.book.Tag;
import org.geometerplus.fbreader.book.UID;
import org.geometerplus.zlibrary.core.filesystem.ZLFile;
import org.geometerplus.zlibrary.core.image.ZLImage;
import org.geometerplus.zlibrary.core.options.Config;
import org.geometerplus.zlibrary.text.view.ZLTextFixedPosition;
import org.geometerplus.zlibrary.text.view.ZLTextPosition;
import org.geometerplus.zlibrary.ui.android.image.ZLBitmapImage;

import zystudio.debug.FBDebug;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class BookCollectionShadow extends AbstractBookCollection implements ServiceConnection {

    private volatile Context myContext;
    private volatile LibraryInterface myInterface;
    private final List<Runnable> myOnBindActions = new LinkedList<Runnable>();

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!hasListeners()) {
                return;
            }

            try {
                final String type = intent.getStringExtra("type");
                if (LibraryService.BOOK_EVENT_ACTION.equals(intent.getAction())) {
                    final Book book = SerializerUtil.deserializeBook(intent.getStringExtra("book"));
                    fireBookEvent(BookEvent.valueOf(type), book);
                } else {
                    fireBuildEvent(Status.valueOf(type));
                }
            } catch (Exception e) {
                // ignore
            }
        }
    };

    public synchronized boolean bindToService(Context context, Runnable onBindAction) {
        if (myInterface != null && myContext == context) {
            if (onBindAction != null) {
                Config.Instance().runOnConnect(onBindAction);
            }
            return true;
        } else {
            if (onBindAction != null) {
                myOnBindActions.add(onBindAction);
            }
            // 这个intent对应的service是org.geometerplus.android.fbreder.libraryService.LibraryService
            final boolean result = context.bindService(FBReaderIntents.internalIntent(FBReaderIntents.Action.LIBRARY_SERVICE), this,
                    LibraryService.BIND_AUTO_CREATE);
            if (result) {
                myContext = context;
            }
            return result;
        }
    }

    public synchronized void unbind() {
        if (myContext != null && myInterface != null) {
            try {
                myContext.unregisterReceiver(myReceiver);
            } catch (IllegalArgumentException e) {
                // called before regisration, that's ok
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                myContext.unbindService(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
            myInterface = null;
            myContext = null;
        }
    }

    public synchronized void reset(boolean force) {
        FBDebug.logCollectionCall("BookCollectionShadow.reset(): "+force);
        if (myInterface != null) {
            try {
                myInterface.reset(force);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public synchronized int size() {
        FBDebug.logCollectionCall("BookCollectionShadow.size() ");
        if (myInterface == null) {
            return 0;
        }
        try {
            return myInterface.size();
        } catch (RemoteException e) {
            return 0;
        }
    }

    @Override
    public synchronized Status status() {
        FBDebug.logCollectionCall("BookCollectionShadow.status() ");
        if (myInterface == null) {
            return Status.NotStarted;
        }
        try {
            return Status.valueOf(myInterface.status());
        } catch (Throwable t) {
            return Status.NotStarted;
        }
    }

    @Override
    public synchronized List<Book> books(BookQuery query) {
        FBDebug.logCollectionCall("BookCollectionShadow.books(): ");
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            return SerializerUtil.deserializeBookList(myInterface.books(SerializerUtil.serialize(query)));
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized boolean hasBooks(Filter filter) {
        FBDebug.logCollectionCall("BookCollectionShadow.hasBooks() ");
        if (myInterface == null) {
            return false;
        }
        try {
            return myInterface.hasBooks(SerializerUtil.serialize(new BookQuery(filter, 1)));
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public synchronized List<Book> recentlyAddedBooks(int count) {
        FBDebug.logCollectionCall("BookCollectionShadow.recentlyAddedBooks() :"+count);
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            return SerializerUtil.deserializeBookList(myInterface.recentlyAddedBooks(count));
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized List<Book> recentlyOpenedBooks(int count) {
        FBDebug.logCollectionCall("BookCollectionShadow.recentlyOpenedBooks() :"+count);
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            return SerializerUtil.deserializeBookList(myInterface.recentlyOpenedBooks(count));
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    // 最近的上一本书的话，index是0
    @Override
    public synchronized Book getRecentBook(int index) {
        FBDebug.logCollectionCall("BookCollectionShadow.getRecentBook() :"+index);
        if (myInterface == null) {
            return null;
        }
        try {
            return SerializerUtil.deserializeBook(myInterface.getRecentBook(index));
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public synchronized Book getBookByFile(ZLFile file) {
        FBDebug.logCollectionCall("BookCollectionShadow.getBookByFile()  path:"+file.getPath());
        if (myInterface == null) {
            return null;
        }
        try {
            return SerializerUtil.deserializeBook(myInterface.getBookByFile(file.getPath()));
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public synchronized Book getBookById(long id) {
        FBDebug.logCollectionCall("BookCollectionShadow.getBookById:"+id);
        if (myInterface == null) {
            return null;
        }
        try {
            return SerializerUtil.deserializeBook(myInterface.getBookById(id));
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public synchronized Book getBookByUid(UID uid) {
        FBDebug.logCollectionCall("BookCollectionShadow.getBookByUid:");
        if (myInterface == null) {
            return null;
        }
        try {
            return SerializerUtil.deserializeBook(myInterface.getBookByUid(uid.Type, uid.Id));
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public synchronized Book getBookByHash(String hash) {
        FBDebug.logCollectionCall("BookCollectionShadow.getBookByHash");
        if (myInterface == null) {
            return null;
        }
        try {
            return SerializerUtil.deserializeBook(myInterface.getBookByHash(hash));
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public synchronized List<Author> authors() {
        FBDebug.logCollectionCall("BookCollectionShadow.authors");
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            final List<String> strings = myInterface.authors();
            final List<Author> authors = new ArrayList<Author>(strings.size());
            for (String s : strings) {
                authors.add(Util.stringToAuthor(s));
            }
            return authors;
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized List<Tag> tags() {
        FBDebug.logCollectionCall("BookCollectionShadow.tags");
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            final List<String> strings = myInterface.tags();
            final List<Tag> tags = new ArrayList<Tag>(strings.size());
            for (String s : strings) {
                tags.add(Util.stringToTag(s));
            }
            return tags;
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized boolean hasSeries() {
        FBDebug.logCollectionCall("BookCollectionShadow.hasSeries");
        if (myInterface != null) {
            try {
                return myInterface.hasSeries();
            } catch (RemoteException e) {
            }
        }
        return false;
    }

    @Override
    public synchronized List<String> series() {
        FBDebug.logCollectionCall("BookCollectionShadow.series");
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            return myInterface.series();
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized List<String> titles(BookQuery query) {
        FBDebug.logCollectionCall("BookCollectionShadow.titles");
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            return myInterface.titles(SerializerUtil.serialize(query));
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized List<String> firstTitleLetters() {
        FBDebug.logCollectionCall("BookCollectionShadow.firstTitleLetters");
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            return myInterface.firstTitleLetters();
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized boolean saveBook(Book book) {
        FBDebug.logCollectionCall("BookCollectionShadow.saveBook");
        if (myInterface == null) {
            return false;
        }
        try {
            return myInterface.saveBook(SerializerUtil.serialize(book));
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public synchronized boolean canRemoveBook(Book book, boolean deleteFromDisk) {
        FBDebug.logCollectionCall("BookCollectionShadow.canRemoveBook");
        if (myInterface == null) {
            return false;
        }
        try {
            return myInterface.canRemoveBook(SerializerUtil.serialize(book), deleteFromDisk);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public synchronized void removeBook(Book book, boolean deleteFromDisk) {
        FBDebug.logCollectionCall("BookCollectionShadow.removeBook");
        if (myInterface != null) {
            try {
                myInterface.removeBook(SerializerUtil.serialize(book), deleteFromDisk);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public synchronized void addToRecentlyOpened(Book book) {
        FBDebug.logCollectionCall("BookCollectionShadow.addToRecentlyOpened");
        if (myInterface != null) {
            try {
                myInterface.addToRecentlyOpened(SerializerUtil.serialize(book));
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public synchronized void removeFromRecentlyOpened(Book book) {
        FBDebug.logCollectionCall("BookCollectionShadow.removeFromRecentlyOpened");
        if (myInterface != null) {
            try {
                myInterface.removeFromRecentlyOpened(SerializerUtil.serialize(book));
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public synchronized List<String> labels() {
        FBDebug.logCollectionCall("BookCollectionShadow.labels");
        if (myInterface != null) {
            try {
                return myInterface.labels();
            } catch (RemoteException e) {
            }
        }
        return Collections.emptyList();
    }

    @Override
    public String getHash(Book book, boolean force) {
        FBDebug.logCollectionCall("BookCollectionShadow.getHash");
        if (myInterface == null) {
            return null;
        }
        try {
            return myInterface.getHash(SerializerUtil.serialize(book), force);
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public void setHash(Book book, String hash) {
        FBDebug.logCollectionCall("BookCollectionShadow.setHash");
        if (myInterface == null) {
            return;
        }
        try {
            myInterface.setHash(SerializerUtil.serialize(book), hash);
        } catch (RemoteException e) {
        }
    }

    @Override
    public synchronized ZLTextFixedPosition.WithTimestamp getStoredPosition(long bookId) {
        FBDebug.logCollectionCall("BookCollectionShadow.getStoredPosition");
        if (myInterface == null) {
            return null;
        }

        try {
            final PositionWithTimestamp pos = myInterface.getStoredPosition(bookId);
            if (pos == null) {
                return null;
            }

            return new ZLTextFixedPosition.WithTimestamp(pos.ParagraphIndex, pos.ElementIndex, pos.CharIndex, pos.Timestamp);
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public synchronized void storePosition(long bookId, ZLTextPosition position) {
        FBDebug.logCollectionCall("BookCollectionShadow.storePosition");
        if (position != null && myInterface != null) {
            try {
                myInterface.storePosition(bookId, new PositionWithTimestamp(position));
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public synchronized boolean isHyperlinkVisited(Book book, String linkId) {
        FBDebug.logCollectionCall("BookCollectionShadow.isHyperlinkVisited");
        if (myInterface == null) {
            return false;
        }

        try {
            return myInterface.isHyperlinkVisited(SerializerUtil.serialize(book), linkId);
        } catch (RemoteException e) {
            return false;
        }
    }

    @Override
    public synchronized void markHyperlinkAsVisited(Book book, String linkId) {
        FBDebug.logCollectionCall("BookCollectionShadow.markHyperlinkAsVisited");
        if (myInterface != null) {
            try {
                myInterface.markHyperlinkAsVisited(SerializerUtil.serialize(book), linkId);
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public synchronized ZLImage getCover(Book book, int maxWidth, int maxHeight) {
        FBDebug.logCollectionCall("BookCollectionShadow.getCover");
        if (myInterface == null) {
            return null;
        }
        try {
            final boolean[] delayed = new boolean[1];
            return new ZLBitmapImage(myInterface.getCover(SerializerUtil.serialize(book), maxWidth, maxHeight, delayed));
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public String getCoverUrl(Book book) {
        FBDebug.logCollectionCall("BookCollectionShadow.getCoverUrl");
        if (myInterface == null) {
            return null;
        }
        try {
            return myInterface.getCoverUrl(book.File.getPath());
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public String getDescription(Book book) {
        FBDebug.logCollectionCall("BookCollectionShadow.getDescription");
        if (myInterface == null) {
            return null;
        }
        try {
            return myInterface.getDescription(SerializerUtil.serialize(book));
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public synchronized List<Bookmark> bookmarks(BookmarkQuery query) {
        FBDebug.logCollectionCall("BookCollectionShadow.bookmarks");
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            return SerializerUtil.deserializeBookmarkList(myInterface.bookmarks(SerializerUtil.serialize(query)));
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized void saveBookmark(Bookmark bookmark) {
        FBDebug.logCollectionCall("BookCollectionShadow.saveBookmark");
        if (myInterface != null) {
            try {
                bookmark.update(SerializerUtil.deserializeBookmark(myInterface.saveBookmark(SerializerUtil.serialize(bookmark))));
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public synchronized void deleteBookmark(Bookmark bookmark) {
        FBDebug.logCollectionCall("BookCollectionShadow.deleteBookmark");
        if (myInterface != null) {
            try {
                myInterface.deleteBookmark(SerializerUtil.serialize(bookmark));
            } catch (RemoteException e) {
            }
        }
    }

    @Override
    public synchronized HighlightingStyle getHighlightingStyle(int styleId) {
        FBDebug.logCollectionCall("BookCollectionShadow.getHighlightingStyle");
        if (myInterface == null) {
            return null;
        }
        try {
            return SerializerUtil.deserializeStyle(myInterface.getHighlightingStyle(styleId));
        } catch (RemoteException e) {
            return null;
        }
    }

    @Override
    public synchronized List<HighlightingStyle> highlightingStyles() {
        FBDebug.logCollectionCall("BookCollectionShadow.highlightingStyles");
        if (myInterface == null) {
            return Collections.emptyList();
        }
        try {
            return SerializerUtil.deserializeStyleList(myInterface.highlightingStyles());
        } catch (RemoteException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public synchronized void saveHighlightingStyle(HighlightingStyle style) {
        FBDebug.logCollectionCall("BookCollectionShadow.saveHighlightingStyle");
        if (myInterface != null) {
            try {
                myInterface.saveHighlightingStyle(SerializerUtil.serialize(style));
            } catch (RemoteException e) {
                // ignore
            }
        }
    }

    @Override
    public synchronized void rescan(String path) {
        FBDebug.logCollectionCall("BookCollectionShadow.rescan:"+path);
        if (myInterface != null) {
            try {
                myInterface.rescan(path);
            } catch (RemoteException e) {
                // ignore
            }
        }
    }

    // method from ServiceConnection interface
    @Override
    public synchronized void onServiceConnected(ComponentName name, IBinder service) {
        myInterface = LibraryInterface.Stub.asInterface(service);
        while (!myOnBindActions.isEmpty()) {
            Config.Instance().runOnConnect(myOnBindActions.remove(0));
        }
        if (myContext != null) {
            myContext.registerReceiver(myReceiver, new IntentFilter(LibraryService.BOOK_EVENT_ACTION));
            myContext.registerReceiver(myReceiver, new IntentFilter(LibraryService.BUILD_EVENT_ACTION));
        }
    }

    // method from ServiceConnection interface
    @Override
    public synchronized void onServiceDisconnected(ComponentName name) {
    }
}

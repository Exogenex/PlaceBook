package com.raywenderlich.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.raywenderlich.placebook.db.BookmarkDao
import com.raywenderlich.placebook.db.PlaceBookDatabase
import com.raywenderlich.placebook.model.Bookmark

class BookmarkRepo(context: Context) {
    private val db = PlaceBookDatabase.getInstance(context)
    private val bookmarkDao: BookmarkDao = db.bookmarkDao()

    fun addBookmark(bookmark: Bookmark): Long? {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }

    fun updateBookmark(bookmark: Bookmark) { bookmarkDao.updateBookmark(bookmark) }

    fun getBookmark(bookmarkId: Long): Bookmark { return bookmarkDao.loadBookmark(bookmarkId) }

    fun createBookmark(): Bookmark { return Bookmark() }

    fun getLiveBookmark(bookmarkId: Long): LiveData<Bookmark> = bookmarkDao.loadLiveBookmark(bookmarkId)

    val allBookmarks: LiveData<List<Bookmark>> get() { return bookmarkDao.loadAll() }
}
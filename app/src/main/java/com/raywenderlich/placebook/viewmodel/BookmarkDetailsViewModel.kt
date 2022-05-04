package com.raywenderlich.placebook.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.raywenderlich.placebook.model.Bookmark
import com.raywenderlich.placebook.repository.BookmarkRepo
import com.raywenderlich.placebook.util.ImageUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    private val bookmarkRepo = BookmarkRepo(getApplication())

    fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
        if (bookmarkDetailsView == null) { mapBookmarkToBookmarkView(bookmarkId) }
        return bookmarkDetailsView
    }

    fun getCategoryResourceId(category: String): Int? { return bookmarkRepo.getCategoryResourceId(category) }

    fun getCategories(): List<String> { return bookmarkRepo.categories }

    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        GlobalScope.launch {
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            bookmark?.let { bookmarkRepo.updateBookmark(it) }
        }
    }

    fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {
        GlobalScope.launch {
            val bookmark = bookmarkDetailsView.id?.let { bookmarkRepo.getBookmark(it) }
            bookmark?.let { bookmarkRepo.deleteBookmark(it) }
        }
    }

    private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView): Bookmark? {
        val bookmark = bookmarkView.id?.let { bookmarkRepo.getBookmark(it) }
        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
            bookmark.category = bookmarkView.category
        }
        return bookmark
    }

    fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark) { repoBookmark -> repoBookmark?.let { repoBookmark -> bookmarkToBookmarkView(repoBookmark) } }
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkDetailsView {
        return BookmarkDetailsView(
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes,
            bookmark.category,
            bookmark.longitude,
            bookmark.latitude,
            bookmark.placeId
        )
    }

    data class BookmarkDetailsView(
        var id: Long? = null,
        var name: String = "",
        var phone: String = "",
        var address: String = "",
        var notes: String = "",
        var category: String = "",
        var longitude: Double = 0.0,
        var latitude: Double = 0.0,
        var placeId: String? = null
    ) {
        fun getImage(context: Context) = id?.let { ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it)) }
        fun setImage(context: Context, image: Bitmap) { id?.let { ImageUtils.saveBitmapToFile(context, image, Bookmark.generateImageFilename(it)) } }
    }
}
package com.raywenderlich.placebook.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.raywenderlich.placebook.R
import com.raywenderlich.placebook.databinding.ActivityBookmarkDetailsBinding
import com.raywenderlich.placebook.util.ImageUtils
import com.raywenderlich.placebook.viewmodel.BookmarkDetailsViewModel
import java.io.File
import java.net.URLEncoder

class BookmarkDetailsActivity : AppCompatActivity(), PhotoOptionDialogFragment.PhotoOptionDialogListener {
    private lateinit var databinding: ActivityBookmarkDetailsBinding

    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null
    private var photoFile: File? = null

    override fun onCaptureClick() {
        photoFile = null
        try { photoFile = ImageUtils.createUniqueImageFile(this) }
        catch (ex: java.io.IOException) { return }

        photoFile?.let { photoFile ->
            val photoUri = FileProvider.getUriForFile(this, "com.raywenderlich.placebook.fileprovider", photoFile)
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            captureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)
            val intentActivities = packageManager.queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY)
            intentActivities.map { it.activityInfo.packageName }
                .forEach { grantUriPermission(it, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)
        }
    }
    override fun onPickClick() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
    }

    private fun getImageWithAuthority(uri: Uri) =
        ImageUtils.decodeUriStreamToSize(
            uri, resources.getDimensionPixelSize(R.dimen.default_image_width),
            resources.getDimensionPixelSize(R.dimen.default_image_height), this
        )

    private fun populateCategoryList() {
        val bookmarkView = bookmarkDetailsView ?: return

        val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(bookmarkView.category)
        resourceId?.let{ databinding.imageViewCategory.setImageResource(it) }

        val categories = bookmarkDetailsViewModel.getCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        databinding.spinnerCategory.adapter = adapter

        val placeCategory = bookmarkView.category
        databinding.spinnerCategory.setSelection(adapter.getPosition(placeCategory))

        databinding.spinnerCategory.post {
            databinding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    val category = parent.getItemAtPosition(position) as String
                    val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(category)
                    resourceId?.let { databinding.imageViewCategory.setImageResource(it) }
                }
                override fun onNothingSelected(parent: AdapterView<*>) { /*NOTE: This method is required but not used.*/ }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == android.app.Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CAPTURE_IMAGE -> {
                    val photoFile = photoFile ?: return
                    val uri = FileProvider.getUriForFile(this, "com.raywenderlich.placebook.fileprovider", photoFile)
                    revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val image = getImageWithPath(photoFile.absolutePath)
                    val bitmap = ImageUtils.rotateImageIfRequired(this, image , uri)
                    updateImage(bitmap)
                }
                REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null) {
                    val imageUri = data.data as Uri
                    val image = getImageWithAuthority(imageUri)
                    image?.let {
                        val bitmap = ImageUtils.rotateImageIfRequired(this, it, imageUri)
                        updateImage(bitmap)
                    }
                }
            }
        }
    }

    private fun sharePlace() {
        val bookmarkView = bookmarkDetailsView ?: return
        var mapUrl = ""

        if (bookmarkView.placeId == null) {
            val location = URLEncoder.encode("${bookmarkView.latitude},${bookmarkView.longitude}", "utf-8")
            mapUrl = "https://www.google.com/maps/dir/?api=1&destination=$location"
        } else {
            val name = URLEncoder.encode(bookmarkView.name, "utf-8")
            mapUrl = "https://www.google.com/maps/dir/?api=1&destination=$name&destination_place_id=${bookmarkView.placeId}"
        }

        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out ${bookmarkView.name} at:\n$mapUrl")
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing ${bookmarkView.name}")
        sendIntent.type = "text/plain"

        startActivity(sendIntent)
    }

    private fun deleteBookmark() {
        val bookmarkView = bookmarkDetailsView ?: return
        AlertDialog.Builder(this)
            .setMessage("Delete?")
            .setPositiveButton("Ok") { _, _ ->
                bookmarkDetailsViewModel.deleteBookmark(bookmarkView)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .create().show()
    }

    private fun updateImage(image: Bitmap) {
        bookmarkDetailsView?.let {
            databinding.imageViewPlace.setImageBitmap(image)
            it.setImage(this, image)
        }
    }

    private fun getImageWithPath(filePath: String) = ImageUtils.decodeFileToSize(
        filePath,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height)
    )

    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    private fun populateImageView() {
        bookmarkDetailsView?.let { bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let { databinding.imageViewPlace.setImageBitmap(placeImage) }
        }
        databinding.imageViewPlace.setOnClickListener { replaceImage() }
    }

    private fun saveChanges() {
        val name = databinding.editTextName.text.toString()
        if (name.isEmpty()) { return }
        bookmarkDetailsView?.let { bookmarkView ->
            bookmarkView.name = databinding.editTextName.text.toString()
            bookmarkView.notes = databinding.editTextNotes.text.toString()
            bookmarkView.address = databinding.editTextAddress.text.toString()
            bookmarkView.phone = databinding.editTextPhone.text.toString()
            bookmarkView.category = databinding.spinnerCategory.selectedItem as String
            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_save -> { saveChanges();true }
            R.id.action_delete -> { deleteBookmark();true }
            else -> super.onOptionsItemSelected(item)
        }

    // NOTE: Moved lambda out of parentheses
    private fun getIntentData() {
        val bookmarkId = intent.getLongExtra(MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0)

        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(this) {
            it?.let {
                bookmarkDetailsView = it
                databinding.bookmarkDetailsView = it
                populateImageView()
                populateCategoryList()
            }
        }
    }

    override fun onCreate(savedInstanceState: android.os.Bundle?)
    {
        super.onCreate(savedInstanceState)
        databinding = DataBindingUtil.setContentView(this, R.layout.activity_bookmark_details)
        setupToolbar()
        getIntentData()
        setupFab()
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    private fun setupToolbar() { setSupportActionBar(databinding.toolbar) }

    private fun setupFab() { databinding.fab.setOnClickListener { sharePlace() } }

    companion object {
        private const val REQUEST_CAPTURE_IMAGE = 1
        private const val REQUEST_GALLERY_IMAGE = 2
    }
}
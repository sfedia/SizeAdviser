package com.sizeadviser.sizeadviser

import android.Manifest
import android.app.Activity
import android.app.ActivityOptions
import android.app.AlertDialog
import android.content.ClipData.Item
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.auth.FirebaseAuth
import com.sizeadviser.sizeadviser.databinding.ActivityCustomBrandBinding
import com.sizeadviser.sizeadviser.databinding.ActivityFittingRoomBinding
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import kotlinx.android.synthetic.main.item_standards.view.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.random.Random


class CustomBrandActivity() : SettingsProvidingActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    View.OnClickListener {

    private lateinit var binding: ActivityCustomBrandBinding

    var firstLoad: Boolean = true
    var api: SizeAdviserApi = SizeAdviserApi()
    var fitValue: Int = -1
    var fittingID: String? = null
    var submittedFID: MutableList<String> = mutableListOf()
    var selectedSize: String? = null
    var selectedStandard: String? = null

    val REQUEST_CODE = 200
    var permissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    var imageToUploadUri: Uri? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomBrandBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.tooSmall.setOnClickListener(this)
        binding.sizeDown.setOnClickListener(this)
        binding.idealFit.setOnClickListener(this)
        binding.tooBig.setOnClickListener(this)
        binding.sizeUp.setOnClickListener(this)
        binding.gotIt.setOnClickListener(this)
        binding.buttonAddPhoto.setOnClickListener(this)
        binding.otherBrand.setOnClickListener(this)

        binding.navigation.navProfile.setOnClickListener(this)
        binding.navigation.navFittingRoom.setOnClickListener(this)
        binding.navigation.navMyCollection.setOnClickListener(this)

        binding.progressBar.visibility = View.INVISIBLE

        setupSharedPreferences()

    }

    var mCurrentPhotoPath: String? = null

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun addPhoto() {
        if (askForPermissions()) {
            if (getCurrentFittingID() !in submittedFID) {
                Toast.makeText(
                    this,
                    "Please save your fit rate first.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else {
                CustomFittingSessionHolder.myBrandValue = getSavedBrandValue()

                val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                
                if (takePictureIntent.resolveActivity(packageManager) != null) {
                    var photoFile: File? = null
                    try {
                        photoFile = createImageFile();
                    } catch (e: IOException) {}


                    if (photoFile != null) {
                        var photoURI: Uri = FileProvider.getUriForFile(this,
                            "com.sizeadviser.sizeadviser.fileprovider",
                            photoFile)
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(takePictureIntent, REQUEST_CODE);
                    }
                }
            }
        }
    }
        
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE) {

            val file = File(mCurrentPhotoPath)

            val photoLocalID = Random.nextInt(
                10.toDouble().pow(6).toInt(),
                10.toDouble().pow(8).toInt()
            ).toString()
            
            binding.progressBar.visibility = View.VISIBLE

            getCurrentFittingID().let {
                api.uploadPhoto(
                    it,
                    photoLocalID,
                    file
                ) {
                    Toast.makeText(
                        baseContext, "Image uploaded",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.INVISIBLE
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.navigation.navFittingRoom.setTypeface(null, Typeface.BOLD)
        resetFittingSession()
    }

    private fun isPermissionsAllowed(): Boolean {
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) !=
                PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun askForPermissions(): Boolean {
        if (!isPermissionsAllowed()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this as Activity, permissions[0])) {
                showPermissionDeniedDialog()
            } else {
                ActivityCompat.requestPermissions(
                    this as Activity,
                    permissions.toTypedArray(),
                    REQUEST_CODE
                )
            }
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // no action added yet
                } else {
                    Log.d("PHOTO_PERMISSIONS", "not granted")
                }
                return
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Permission is denied, Please allow permissions from App Settings.")
            .setPositiveButton("App Settings",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                })
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getSavedBrandValue() : String? {
        return findViewById<TextView>(R.id.brand_name).text.toString()
    }

    private fun getSavedSizeValue() : String? {
        return findViewById<TextView>(R.id.my_brand_size).text.toString()
    }

    private fun getSavedStandardValue() : String? {
        return findViewById<TextView>(R.id.my_brand_standard).text.toString()
    }

    private fun getCurrentFittingID(): String {
        if (CustomFittingSessionHolder.fittingID != null) {
            return CustomFittingSessionHolder.fittingID!!
        }
        else {
            CustomFittingSessionHolder.fittingID = Random.nextInt(
                10.toDouble().pow(6).toInt(),
                10.toDouble().pow(8).toInt()
            ).toString()
            return CustomFittingSessionHolder.fittingID!!
        }
    }

    private fun resetFittingSession() {
        CustomFittingSessionHolder.fittingID = null
    }

    fun setBackgroundToButtons(resourceIdeal: Int, resourceOther: Int) {
        binding.tooSmall.setBackgroundResource(resourceOther)
        binding.sizeDown.setBackgroundResource(resourceOther)
        binding.tooBig.setBackgroundResource(resourceOther)
        binding.sizeUp.setBackgroundResource(resourceOther)
        binding.idealFit.setBackgroundResource(resourceIdeal)

    }
    
    private fun gotItRun() {
        if (fitValue == -1) {
            Toast.makeText(
                baseContext, "But how it fits you?",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        if (getCurrentFittingID() in submittedFID) {
            resetFittingSession()
        } else {
            submittedFID.add(getCurrentFittingID())
        }
        binding.progressBar.visibility = View.VISIBLE
        api.tryWithSize(
            getCurrentFittingID(),
            getSavedBrandValue()!!,
            getSavedSizeValue()!!,
            getSavedStandardValue()!!,
            fitValue
        ) {
            Toast.makeText(
                baseContext, "Your results saved",
                Toast.LENGTH_LONG
            ).show()
            binding.progressBar.visibility = View.INVISIBLE
            fitValue = -1
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            setBackgroundToButtons(R.drawable.fitting_r_button_ideal, R.drawable.fitting_r_button_other)
            when (v.id) {
                R.id.tooSmall -> {
                    binding.tooSmall.setBackgroundResource(R.drawable.fitting_r_button_other_pressed)
                    fitValue = 1
                }
                R.id.sizeDown -> {
                    binding.sizeDown.setBackgroundResource(R.drawable.fitting_r_button_other_pressed)
                    fitValue = 2
                }
                R.id.idealFit -> {
                    binding.idealFit.setBackgroundResource(R.drawable.fitting_r_button_ideal_pressed)
                    fitValue = 3
                }
                R.id.tooBig -> {
                    binding.tooBig.setBackgroundResource(R.drawable.fitting_r_button_other_pressed)
                    fitValue = 4
                }
                R.id.sizeUp -> {
                    binding.sizeUp.setBackgroundResource(R.drawable.fitting_r_button_other_pressed)
                    fitValue = 5
                }
                R.id.gotIt -> {
                    gotItRun()
                }
                R.id.button_add_photo -> {
                    addPhoto()
                }
                R.id.nav_profile -> {
                    val intent = Intent(applicationContext, ProfileActivity::class.java)
                    startActivity(intent)
                }
                R.id.other_brands, R.id.nav_fitting_room -> {
                    val intent = Intent(applicationContext, FittingRoomActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_my_collection -> {
                    val intent = Intent(applicationContext, MyCollectionActivity::class.java)
                    startActivity(intent)
                }
            }
        }
    }
}


object CustomFittingSessionHolder {
    init {
        println("CustomFittingSessionHolder added")
    }
    var fittingID: String? = null
    var myBrandValue: String? = null
}

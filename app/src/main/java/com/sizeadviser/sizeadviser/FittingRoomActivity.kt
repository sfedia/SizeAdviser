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


class FittingRoomActivity() : SettingsProvidingActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    AdapterView.OnItemSelectedListener,
    DiscreteScrollView.ScrollStateChangeListener<FittingRoomViewHolder>,
    DiscreteScrollView.OnItemChangedListener<FittingRoomViewHolder>, View.OnClickListener {

    private lateinit var binding: ActivityFittingRoomBinding
    private lateinit var slideToLeft: Slide
    private lateinit var slideToRight: Slide
    
    var firstLoad: Boolean = true
    var api: SizeAdviserApi = SizeAdviserApi()
    var fittingData: BoundFittingData? = null
    var fitValue: Int = -1
    var fittingID: String? = null
    var submittedFID: MutableList<String> = mutableListOf()
    var selectedSize: String? = null
    var selectedStandard: String? = null
    var thisBrandValue: String? = null

    var systemsFocusIndex: Int? = null
    var sizesFocusIndex: Int? = null

    val REQUEST_CODE = 200
    var permissions = listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    var imageToUploadUri: Uri? = null

    private lateinit var brandOptions: Spinner
    private lateinit var recommendedView: DiscreteScrollView
    private lateinit var standardsView: DiscreteScrollView
    private lateinit var sizesView: DiscreteScrollView
    private lateinit var auth: FirebaseAuth

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFittingRoomBinding.inflate(layoutInflater)

        setContentView(binding.root)

        slideToLeft = Slide()
        slideToLeft.slideEdge = Gravity.START
        slideToLeft.excludeTarget(R.id.action_bar_container, true)
        slideToLeft.excludeTarget(android.R.id.statusBarBackground, true)
        slideToLeft.excludeTarget(findViewById<View>(R.id.navigation), true)
        slideToRight = Slide()
        slideToRight.slideEdge = Gravity.END
        slideToRight.excludeTarget(R.id.action_bar_container, true)
        slideToRight.excludeTarget(android.R.id.statusBarBackground, true)
        slideToRight.excludeTarget(R.id.fr_local_tabs, true)

        window.allowEnterTransitionOverlap = true

        brandOptions = binding.brandOptions
        brandOptions.onItemSelectedListener = this
        recommendedView = binding.recommendedSizes
        standardsView = binding.brandStandards
        sizesView = binding.brandSizes

        standardsView.addOnItemChangedListener(this)
        standardsView.addScrollStateChangeListener(this)

        sizesView.addOnItemChangedListener(this)
        sizesView.addScrollStateChangeListener(this)

        binding.tooSmall.setOnClickListener(this)
        binding.sizeDown.setOnClickListener(this)
        binding.idealFit.setOnClickListener(this)
        binding.tooBig.setOnClickListener(this)
        binding.sizeUp.setOnClickListener(this)
        binding.gotIt.setOnClickListener(this)
        binding.newBrand.setOnClickListener(this)
        binding.addPhotoButton.setOnClickListener(this)

        binding.navigation.navProfile.setOnClickListener(this)
        binding.navigation.navMyCollection.setOnClickListener(this)

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
                FittingSessionHolder.selectedBrandValue = fittingData?.thisBrand

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

        uiLoadBrand()

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
                    // no action added
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
        if (FittingSessionHolder.selectedBrandValue != null) {
            return FittingSessionHolder.selectedBrandValue
        }
        else if (intent.extras != null) {
            return intent.extras?.getString("brandSelected")
        }
        else if (fittingData?.thisBrand != null) {
            return fittingData?.thisBrand
        }
        return null
    }

    private fun getCurrentFittingID(): String {
        if (FittingSessionHolder.fittingID != null) {
            return FittingSessionHolder.fittingID!!
        }
        else {
            FittingSessionHolder.fittingID = Random.nextInt(
                10.toDouble().pow(6).toInt(),
                10.toDouble().pow(8).toInt()
            ).toString()
            return FittingSessionHolder.fittingID!!
        }
    }

    private fun resetFittingSession() {
        FittingSessionHolder.fittingID = null
    }

    private fun uiLoadBrand(localPB: Boolean = false, globalPB: Boolean = true) {
        if (globalPB) {
            binding.progressBar.visibility = View.VISIBLE
            binding.sub1Layout.visibility = View.INVISIBLE
        }
        else if (localPB) {
            binding.localProgressBar.visibility = View.VISIBLE
            binding.recommendedSizesLayout.visibility = View.GONE
            binding.brandStandardsLayout.visibility = View.GONE
            binding.brandSizesLayout.visibility = View.GONE
            binding.fitButtonsGrid.visibility = View.GONE
        }

        var currentBrand: String? = getSavedBrandValue()
        if (currentBrand == null) {
            currentBrand = "Adidas"
        }
        sizesFocusIndex = null
        api.boundLoadFittingData(
            sharedPreferences, currentBrand
        ) { boundFittingData: BoundFittingData ->
            FittingSessionHolder.selectedBrandValue = boundFittingData.thisBrand
            if (brandOptions.adapter == null) {
                loadBrandOptions(boundFittingData.brandOptions?.listBrands!!)
            }

            fittingData = boundFittingData

            val defaultSystem = sharedPreferences.getString(
                "profile_system_of_size", "US"
            ).toString()
            val lstRecommendations: MutableList<Item> = mutableListOf()
            val lstStandards: MutableList<Item> = mutableListOf()


            boundFittingData.recommended?.recommendations?.forEachIndexed { i, it ->
                lstStandards.add(Item(it.standard))
                lstRecommendations.add(Item("${it.standard};${it.value}"))
                if (it.standard == defaultSystem) {
                    systemsFocusIndex = i
                }
            }

            loadRecommendedSizes(lstRecommendations)
            loadBrandStandards(lstStandards.toMutableList())

            // Load sizes
            val lstSizes: MutableList<Item> = mutableListOf()
            loadBrandSizes(lstSizes to false)
            loadFromStandard(boundFittingData.standardsObject?.defaultStandard, boundFittingData)

            // Show all stuff again
            if (globalPB) {
                binding.progressBar.visibility = View.INVISIBLE
                binding.sub1Layout.visibility = View.VISIBLE
            }
            else if (localPB) {
                binding.localProgressBar.visibility = View.GONE
                binding.recommendedSizesLayout.visibility = View.VISIBLE
                binding.brandStandardsLayout.visibility = View.VISIBLE
                binding.brandSizesLayout.visibility = View.VISIBLE
                binding.fitButtonsGrid.visibility = View.VISIBLE
            }
        }
    }

    private fun generateSizeItems(lst: List<String>?): 
            Pair<MutableList<Item>, Boolean> {
        var containsFractions: Boolean = false
        if (lst == null)
            return (mutableListOf<Item>() to false);
        val items: MutableList<Item> = mutableListOf()
        lst.forEach { it ->
            if (it.contains("/"))
                containsFractions = true;
            items.add(Item(it))
        }
        return items to containsFractions
    }

    private fun loadBrandOptions(brands: List<String?>) {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, brands)
        adapter.also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            brandOptions.adapter = adapter
        }
    }

    private fun loadRecommendedSizes(recSplitted: List<Item>) {
        recommendedView.setSlideOnFling(true)
        recommendedView.adapter = RecommendedAdapter(recSplitted)

        val cWidth: Int = applicationContext.resources.getDimensionPixelSize(R.dimen.rec_card_width)
        recommendedView.scrollToPosition((recSplitted.size - 1) / 2)
        recommendedView.scrollBy(cWidth / 2, 0)
    }
    
    private fun loadBrandStandards(lst: MutableList<Item>) {
        standardsView.setSlideOnFling(true)

        if (systemsFocusIndex != null) {
            try {
                val x = lst[2]
                lst[2] = lst[systemsFocusIndex!!]
                lst[systemsFocusIndex!!] = x
                systemsFocusIndex = 2
            }
            catch (e: IndexOutOfBoundsException) {}
        }
        else if (lst.size > 2) {
            systemsFocusIndex = 2
        }

        standardsView.adapter = StandardsAdapter(
            lst, applicationContext.resources.getColor(R.color.paletteBlue), standardsView
        )

        standardsView.scrollToPosition(systemsFocusIndex!!)

        standardsView.setItemTransformer(
            ScaleTransformer.Builder()
                .setMinScale(0.6f)
                .setMaxScale(1.0f)
                .build()
        )
    }
    
    private fun loadBrandSizes(sizeData: Pair<List<Item>, Boolean>) {
        println(sizeData)
        sizesView.setSlideOnFling(true)
        sizesView.adapter = SizesAdapter(sizeData.first, sizesView)

        sizesView.setItemTransformer(
            ScaleTransformer.Builder()
                .setMinScale(if (!sizeData.second) 0.8f else 0.4f)
                .setMaxScale(if (!sizeData.second) 1.5f else 1.1f)
                .build()
        )
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        FittingSessionHolder.selectedBrandValue = parent?.getItemAtPosition(position).toString()
        resetFittingSession()
        uiLoadBrand(localPB = true, globalPB = false)
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }

    override fun onScrollStart(currentItemHolder: FittingRoomViewHolder, adapterPosition: Int) {
        currentItemHolder.defaultColor()
    }

    override fun onScrollEnd(currentItemHolder: FittingRoomViewHolder, adapterPosition: Int) {

    }

    override fun onScroll(
        scrollPosition: Float,
        currentPosition: Int,
        newPosition: Int,
        currentHolder: FittingRoomViewHolder?,
        newCurrent: FittingRoomViewHolder?
    ) {

    }

    private fun loadFromStandard(standard: CharSequence?, ftData: BoundFittingData) {
        ftData.standardsObject?.standards?.forEach {
            if (it.standard == standard) {
                if (sizesFocusIndex == null) {
                    sizesFocusIndex = it.sizes?.indexOf(ftData.recommended?.forStandard(standard!!)!!)
                }
                loadBrandSizes(generateSizeItems(it.sizes))
                sizesView.scrollToPosition(sizesFocusIndex!!)
            }
        }
    }

    override fun onCurrentItemChanged(viewHolder: FittingRoomViewHolder?, adapterPosition: Int) {
        viewHolder?.selectedColor()
        when (viewHolder?.getType()) {

            viewHolder?.STANDARDS -> {
                fittingData?.let { loadFromStandard(viewHolder?.itemView?.currentStandard?.text, it) }
                selectedStandard = viewHolder?.itemView?.currentStandard?.text.toString()
                setBackgroundToButtons(R.drawable.fitting_r_button_ideal, R.drawable.fitting_r_button_other)
                fitValue = -1
            }

            viewHolder?.SIZES -> {
                sizesFocusIndex = viewHolder?.adapterPosition
                val curSize: TextView? = viewHolder?.itemView?.findViewById(R.id.currentSize)
                selectedSize = curSize?.text.toString()
                setBackgroundToButtons(R.drawable.fitting_r_button_ideal, R.drawable.fitting_r_button_other)
                fitValue = -1
            }

        }
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
            getCurrentFittingID(), getSavedBrandValue()!!, selectedSize!!,
            selectedStandard!!, fitValue
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
                R.id.new_brand -> {
                    val intent = Intent(applicationContext, CustomBrandActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                R.id.add_photo_button -> {
                    addPhoto()
                }
                R.id.nav_profile -> {
                    window.exitTransition = slideToRight
                    val intent = Intent(applicationContext, ProfileActivity::class.java)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                    overridePendingTransition(0, 0)
                }
                R.id.nav_my_collection -> {
                    window.exitTransition = slideToLeft
                    val intent = Intent(applicationContext, MyCollectionActivity::class.java)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                    overridePendingTransition(0, 0)
                }

            }
        }
    }
}


object FittingSessionHolder {
    init {
        println("FittingSessionHolder initialized")
    }
    var fittingID: String? = null
    var selectedBrandValue: String? = null
}

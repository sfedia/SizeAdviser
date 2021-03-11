package com.sizeadviser.sizeadviser

import android.app.ActivityOptions
import android.content.ClipData.Item
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.opengl.Visibility
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.google.android.material.shape.CornerFamily
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.sizeadviser.sizeadviser.databinding.ActivityProfileBinding
import com.squareup.picasso.Picasso
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.yarolegovich.discretescrollview.transform.ScaleTransformer
import kotlinx.android.synthetic.main.activity_profile.*


class ProfileActivity : SettingsProvidingActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener,
    DiscreteScrollView.ScrollStateChangeListener<ProfileViewHolder>,
    DiscreteScrollView.OnItemChangedListener<ProfileViewHolder>,
    View.OnClickListener, TextWatcher {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var api: SizeAdviserApi
    private lateinit var standardsView: DiscreteScrollView
    //var prefixSetter: EditText = binding.editTextTextBrandName as EditText
    var currentPrefix: String = ""
    var ctStandard: String? = null
    var loadedRows: DataForGender? = null
    var onlyTested = false
    var keyboardSize = 0
    var cachedKeyboardSize = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        val radius = resources.getDimension(R.dimen.image_corner_radius)
        profileAvatar.shapeAppearanceModel = profileAvatar.shapeAppearanceModel
            .toBuilder()
            .setTopRightCorner(CornerFamily.ROUNDED, radius)
            .setTopLeftCorner(CornerFamily.ROUNDED, radius)
            .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
            .setBottomRightCorner(CornerFamily.ROUNDED, radius)
            .build()

        api = SizeAdviserApi()
        scanCurrentProfile(setupSharedPreferences())
        api.registerCurrentUser(sharedPreferences)

        binding.profileAvatar.setOnClickListener(this)
        binding.toggleTested.setOnClickListener(this)
        binding.navigation.navFittingRoom.setOnClickListener(this)
        binding.navigation.navMyCollection.setOnClickListener(this)

        binding.progressBar.visibility = View.VISIBLE
        binding.profileSubLayout.visibility = View.INVISIBLE

        binding.profileSubLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            this.window.decorView.getWindowVisibleDisplayFrame(r)
            val screenHeight: Int = this.window.decorView.rootView.height
            val heightDifference: Int = screenHeight - (r.bottom - r.top)
            if (cachedKeyboardSize == 0) {
                cachedKeyboardSize = heightDifference
            } else if (cachedKeyboardSize != heightDifference) {
                if (cachedKeyboardSize < heightDifference &&
                    (heightDifference.toDouble() / cachedKeyboardSize.toDouble()) > 4.0) {
                    binding.toggleTested.visibility = View.INVISIBLE
                    binding.navigation.navButtonHolder.visibility = View.GONE
                    binding.navTabsLocalHolder.visibility = View.GONE
                } else {
                    binding.toggleTested.visibility = View.VISIBLE
                    binding.navigation.navButtonHolder.visibility = View.VISIBLE
                    binding.navTabsLocalHolder.visibility = View.VISIBLE
                    currentPrefix = binding.editTextTextBrandName.text.toString()
                    loadBrandRows()
                }
                cachedKeyboardSize = heightDifference
            }
            Log.d("CACHED_KB", "cached keyboard size: $cachedKeyboardSize")
        }
    }

    override fun onStart() {
        super.onStart()

        binding.navigation.navProfile.setTypeface(null, Typeface.BOLD)

        val user = auth.currentUser!!
        Log.d("PARSE_PROFILE:1", user.photoUrl.toString())
        if (user.photoUrl != null) {
            getLargerPicture(user.photoUrl.toString())
            { largerPic: String -> Picasso.get().load(largerPic).into(profileAvatar) }

        }
        if (user.displayName != null) {
            binding.avatarUserName.text = user.displayName
        }

        standardsView = binding.brandStandards
        val listStandards: MutableList<Item> = mutableListOf()
        api.getAllStandards().forEach {
            listStandards.add(Item(it))
        }
        loadBrandStandards(listStandards)

        api.getDataForUser(sharedPreferences) { it ->
            loadedRows = it
            binding.totalNumber.text = it.data?.size.toString()
            var tested = 0
            it.data?.forEach { pbr ->
                if (pbr.triedOn!!) {
                    tested++
                }
            }
            binding.testedNumber.text = tested.toString()
            loadBrandRows()

            binding.progressBar.visibility = View.INVISIBLE
            binding.profileSubLayout.visibility = View.VISIBLE
        }

        val pla = ProfileListAdapter(
            this, R.layout.item_profile_list, mutableListOf(), getColor(R.color.palettePink))

        binding.listView.adapter = pla
        for (i in 1..50) {
            pla.add(ProfileRowWrapper("empty"))
        }
        pla.notifyDataSetChanged()
    }

    private fun loadBrandStandards(lst: MutableList<Item>) {
        var systemsFocusIndex: Int? = null
        val defaultSystem = sharedPreferences.getString(
            "profile_system_of_size", "US"
        ).toString()

        standardsView.setSlideOnFling(true)
        standardsView.addOnItemChangedListener(this)
        standardsView.addScrollStateChangeListener(this)

        lst.forEachIndexed { index, item ->
            if (item.text == defaultSystem) {
                systemsFocusIndex = index
            }
        }

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

        standardsView.adapter = ProfileStandardsAdapter(
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

    private fun getLargerPicture(photoUrl: String, hook: (String) -> Unit) : Boolean {
        if (photoUrl.contains("graph.facebook.com")) {
            for (profile in auth.currentUser!!.providerData) {
                if (FacebookAuthProvider.PROVIDER_ID == profile.providerId) {
                    val largerUrl: String = "https://graph.facebook.com/" +
                            profile.uid + "/picture?height=500&width=500"
                    Log.d("PARSE_PROFILE:1", largerUrl)
                    hook(largerUrl)
                    return true
                }
            }
        }
        else if (photoUrl.contains("googleusercontent.com")) {
            for (profile in auth.currentUser!!.providerData) {
                if (GoogleAuthProvider.PROVIDER_ID == profile.providerId) {
                    var largerUrl: String = photoUrl
                    largerUrl = largerUrl.replace("96", "500")
                    Log.d("PARSE_PROFILE:1", largerUrl)
                    hook(largerUrl)
                    return true
                }
            }
        }
        return false
    }

    private fun scanCurrentProfile(sharedPreferences: SharedPreferences?) {
        val user = auth.currentUser!!
        val curName: String? = sharedPreferences!!.getString("profile_user_name", "default")
        if (curName != user.displayName.toString()) {
            val editor: SharedPreferences.Editor = sharedPreferences.edit()
            editor.putString("profile_user_name", user.displayName.toString())
            editor.commit()
        }
    }

    private fun loadBrandRows() {
        println("prefix: '$currentPrefix'")
        println("standard: '$ctStandard'")
        println("data: '$loadedRows'")
        val finalRows: MutableList<ProfileRowWrapper> = mutableListOf()
        var lastLetter: String? = null
        loadedRows?.data?.forEach {
            if (it.brand?.toLowerCase()?.startsWith(currentPrefix.toLowerCase())!!) {
                var standardMatched = false
                it.systemsOfSize?.forEach { standardSizeUnit ->
                    if (standardSizeUnit.standard.toString().toLowerCase() == ctStandard.toString().toLowerCase()) {
                        standardMatched = true
                        if (!onlyTested || it.triedOn!!) {
                            if (lastLetter == null || lastLetter != it.brand.toLowerCase()[0].toString()) {
                                lastLetter = it.brand.toLowerCase().get(0).toString()
                                finalRows.add(
                                        ProfileRowWrapper("capital", capitalLetter = lastLetter!!.toUpperCase())
                                )
                            }
                            finalRows.add(
                                ProfileRowWrapper(
                                    "default", SelectiveProfileBrandRow(
                                        it.brand,
                                        standardSizeUnit,
                                        it.triedOn
                                    )
                                )
                            )
                        }
                    }
                }
                if (!standardMatched && (!onlyTested || it.triedOn!!)) {
                    if (lastLetter == null || lastLetter != it.brand.toLowerCase()[0].toString()) {
                        lastLetter = it.brand.toLowerCase()[0].toString()
                        finalRows.add(
                                ProfileRowWrapper("capital", capitalLetter = lastLetter!!.toUpperCase())
                        )
                    }
                    finalRows.add(
                        ProfileRowWrapper(
                            "wo_ss_unit",
                            SelectiveProfileBrandRow(it.brand, triedOn = it.triedOn)
                        )
                    )
                }
            }
        }

        println("finalRows: $finalRows")
        binding.listView.adapter = ProfileListAdapter(
            this, R.layout.item_profile_list, finalRows, getColor(R.color.palettePink))
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when (key) {
            "profile_user_name" -> {
                val user = auth.currentUser!!
                val newName: String = sharedPreferences?.getString(
                    "profile_user_name", user.displayName!!
                ).toString()
                binding.avatarUserName.text = newName
            }
        }
    }

    override fun onScrollStart(currentItemHolder: ProfileViewHolder, adapterPosition: Int) {
        currentItemHolder.defaultColor()
        Log.d("SCROLLING_STARTED", "yes")
    }

    override fun onScrollEnd(currentItemHolder: ProfileViewHolder, adapterPosition: Int) {

    }

    override fun onScroll(
        scrollPosition: Float,
        currentPosition: Int,
        newPosition: Int,
        currentHolder: ProfileViewHolder?,
        newCurrent: ProfileViewHolder?
    ) {

    }

    override fun onCurrentItemChanged(viewHolder: ProfileViewHolder?, adapterPosition: Int) {
        viewHolder?.selectedColor()
        if (viewHolder?.getType() == viewHolder?.STANDARDS) {
            ctStandard = viewHolder?.getFocusElement()?.text.toString()
            loadBrandRows()
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            println(v.id)
            when (v.id) {
                R.id.toggleTested -> {
                    onlyTested = !onlyTested
                    if (onlyTested) {
                        binding.toggleTested.text = applicationContext.getText(R.string.show_all)
                    } else {
                        binding.toggleTested.text = applicationContext.getText(R.string.only_tested)
                    }
                    loadBrandRows()
                }
                R.id.profileAvatar -> {
                    val intent = Intent(applicationContext, SettingsActivity::class.java)
                    startActivity(intent)
                }
                R.id.nav_fitting_room -> {
                    val intent = Intent(applicationContext, FittingRoomActivity::class.java)
                    startActivity(intent)
                    finish()
                    overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
                }
                R.id.nav_my_collection -> {
                    val intent = Intent(applicationContext, MyCollectionActivity::class.java)
                    startActivity(intent)
                    finish()
                    overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left)
                }
            }
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }

    override fun afterTextChanged(s: Editable?) {
        currentPrefix = binding.editTextTextBrandName.text.toString()
        loadBrandRows()
        binding.toggleTested.visibility = View.INVISIBLE
    }
}


data class ProfileRowWrapper(
    val itemType: String,
    val brandData: SelectiveProfileBrandRow? = null,
    val capitalLetter: String? = null
)
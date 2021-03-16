package com.sizeadviser.sizeadviser

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.ActivityOptions
import android.app.Dialog
import android.content.ClipData.Item
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.transition.Slide
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.gson.Gson
import com.sizeadviser.sizeadviser.databinding.ActivityCalibrationBinding
import com.sizeadviser.sizeadviser.databinding.ActivityEmailPasswordBinding
import com.sizeadviser.sizeadviser.databinding.ActivityMyCollectionBinding
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import com.yarolegovich.discretescrollview.DiscreteScrollView
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_my_collection.*


data class CollectionExternal(
    val minColor: Int,
    val medColor: Int,
    val idealColor: Int,
    val resizeDimen: Float,
    val cardDimen: Float,
    val bgDrawable: Drawable
)


class FittingActionsDialogFragment(
    private val fittingID: String,
    private val parentActivity: MyCollectionActivity
): DialogFragment() {
    private val api = SizeAdviserApi()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.fitting_actions)
                .setItems(R.array.fitting_dialog_actions
                ) { dialog, which ->
                    if (which == 0) {
                        api.removeFitting(fittingID) {
                            parentActivity.loadCollection()
                        }
                    }
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}


class PhotoActionsDialogFragment(
    private val fittingID: String,
    private val photoIndex: Int,
    private val itemPosition: Int,
    private val parentActivity: MyCollectionActivity
): DialogFragment() {
    private val api = SizeAdviserApi()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.photo_actions)
                .setItems(R.array.photo_dialog_actions
                ) { dialog, which ->
                    if (which == 0) {
                        api.removePhoto(fittingID, photoIndex) {
                            parentActivity.loadCollection(toPosition=itemPosition)
                        }
                    }
                }
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}


class MyCollectionActivity : SettingsProvidingActivity(),
    SharedPreferences.OnSharedPreferenceChangeListener,
        View.OnClickListener {

    private lateinit var binding: ActivityMyCollectionBinding
    private lateinit var slideToRight: Slide
    private lateinit var auth: FirebaseAuth
    lateinit var collectionView: DiscreteScrollView
    private lateinit var ext: CollectionExternal
    private var currentAnimator: Animator? = null
    private var shortAnimationDuration: Int = 0
    private var zoomedState: Boolean = false
    var api: SizeAdviserApi = SizeAdviserApi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyCollectionBinding.inflate(layoutInflater)
        var api: SizeAdviserApi = SizeAdviserApi()
        setContentView(binding.root)

        slideToRight = Slide()
        slideToRight.slideEdge = Gravity.END
        slideToRight.excludeTarget(R.id.action_bar_container, true)
        slideToRight.excludeTarget(android.R.id.statusBarBackground, true)
        slideToRight.excludeTarget(findViewById<View>(R.id.navigation), true)
        window.exitTransition = slideToRight
        window.allowEnterTransitionOverlap = true

        shortAnimationDuration = resources.getInteger(android.R.integer.config_shortAnimTime)

        auth = FirebaseAuth.getInstance()

        setupSharedPreferences()

        collectionView = binding.collectionItems
        binding.progressBar.visibility = View.VISIBLE
        binding.collSubLayout.visibility = View.INVISIBLE

        binding.navigation.navProfile.setOnClickListener(this)
        binding.navigation.navFittingRoom.setOnClickListener(this)

        ext = CollectionExternal(
            applicationContext.resources.getColor(R.color.palettePink),
            applicationContext.resources.getColor(R.color.otherFitPressedColor),
            applicationContext.resources.getColor(R.color.idealFitColor),
            applicationContext.resources.getDimension(R.dimen._90sdp),
            applicationContext.resources.getDimension(R.dimen._50sdp),
            applicationContext.resources.getDrawable(R.drawable.shoe2_placeholder)
        )

    }

    override fun onStart() {
        super.onStart()

        binding.navigation.navMyCollection.setTypeface(null, Typeface.BOLD)
        loadCollection()

    }

    fun loadCollection(toPosition: Int = 1) {
        api.getCollection(sharedPreferences) { myCollectionData ->
            val items: MutableList<Item> = mutableListOf()
            myCollectionData.items?.forEach {
                items.add(Item(Gson().toJson(it)))
            }
            when (myCollectionData.items?.size) {
                0 -> {
                    binding.emptyCollection.visibility = View.VISIBLE
                }
                1, 2 -> {
                    for (i in myCollectionData.items.size..3) {
                        items.add(Item(Gson().toJson(
                            CollectionItem(is_empty = true)
                        )))
                    }
                }
            }
            collectionView.adapter = CollectionItemsAdapter(items, ext, this)
            collectionView.scrollToPosition(toPosition)
            collectionView.setSlideOnFling(true)

            binding.progressBar.visibility = View.INVISIBLE
            binding.collSubLayout.visibility = View.VISIBLE
        }
    }

    fun loadZoomed(thumbView: View, fittingID: String, photoIndex: Int) {
        val expandedImageView: ImageView = findViewById(R.id.expanded_image)
        expandedImageView.visibility = View.INVISIBLE
        binding.progressBar.visibility = View.VISIBLE
        Glide.with(this).load(api.getItemPhotoURL(fittingID, photoIndex)).apply(
            RequestOptions().fitCenter()
        ).listener(object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>?,
                isFirstResource: Boolean
            ): Boolean {
                binding.progressBar.visibility = View.INVISIBLE
                return true
            }

            override fun onResourceReady(
                resource: Drawable?,
                model: Any?,
                target: Target<Drawable>?,
                dataSource: DataSource?,
                isFirstResource: Boolean
            ): Boolean {
                zoomImageFromThumb(thumbView)
                binding.progressBar.visibility = View.INVISIBLE
                return false
            }

        }).into(expandedImageView)
    }

    fun zoomImageFromThumb(thumbView: View) {
        zoomedState = true
        currentAnimator?.cancel()

        val expandedImageView: ImageView = findViewById(R.id.expanded_image)
        expandedImageView.visibility = View.INVISIBLE

        val startBoundsInt = Rect()
        val finalBoundsInt = Rect()
        val globalOffset = Point()

        thumbView.getGlobalVisibleRect(startBoundsInt)
        findViewById<View>(R.id.main_layout)
            .getGlobalVisibleRect(finalBoundsInt, globalOffset)
        startBoundsInt.offset(-globalOffset.x, -globalOffset.y)
        finalBoundsInt.offset(-globalOffset.x, -globalOffset.y)

        val startBounds = RectF(startBoundsInt)
        val finalBounds = RectF(finalBoundsInt)

        val startScale: Float
        if ((finalBounds.width() / finalBounds.height() > startBounds.width() / startBounds.height())) {
            startScale = startBounds.height() / finalBounds.height()
            val startWidth: Float = startScale * finalBounds.width()
            val deltaWidth: Float = (startWidth - startBounds.width()) / 2
            startBounds.left -= deltaWidth.toInt()
            startBounds.right += deltaWidth.toInt()
        } else {
            startScale = startBounds.width() / finalBounds.width()
            val startHeight: Float = startScale * finalBounds.height()
            val deltaHeight: Float = (startHeight - startBounds.height()) / 2f
            startBounds.top -= deltaHeight.toInt()
            startBounds.bottom += deltaHeight.toInt()
        }

        thumbView.alpha = 0f
        expandedImageView.visibility = View.VISIBLE

        expandedImageView.pivotX = 0f
        expandedImageView.pivotY = 0f

        currentAnimator = AnimatorSet().apply {
            play(ObjectAnimator.ofFloat(
                expandedImageView,
                View.X,
                startBounds.left,
                finalBounds.left)
            ).apply {
                with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f))
            }
            duration = shortAnimationDuration.toLong()
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {

                override fun onAnimationEnd(animation: Animator) {
                    currentAnimator = null
                }

                override fun onAnimationCancel(animation: Animator) {
                    currentAnimator = null
                }
            })
            start()
        }

        expandedImageView.setOnClickListener {
            zoomedState = false
            currentAnimator?.cancel()

            currentAnimator = AnimatorSet().apply {
                play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left)).apply {
                    with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale))
                    with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale))
                }
                duration = shortAnimationDuration.toLong()
                interpolator = DecelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {

                    override fun onAnimationEnd(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        currentAnimator = null
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        thumbView.alpha = 1f
                        expandedImageView.visibility = View.GONE
                        currentAnimator = null
                    }
                })
                start()
            }
        }
    }

    fun showFittingActions(fittingID: String) {
        val newFragment = FittingActionsDialogFragment(fittingID, this)
        newFragment.show(supportFragmentManager, "fitting_actions")
    }

    fun showPhotoActions(fittingID: String, photoIndex: Int, itemPosition: Int) {
        val photoFragment = PhotoActionsDialogFragment(fittingID, photoIndex, itemPosition, this)
        photoFragment.show(supportFragmentManager, "photo_actions")
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        super.onSharedPreferenceChanged(sharedPreferences, key)
        when (key) {
            "profile_user_name" -> {
                val user = auth.currentUser!!
                val newName: String = sharedPreferences?.getString(
                    "profile_user_name", user.displayName!!
                ).toString()
            }
        }
    }

    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {
                R.id.nav_profile -> {
                    val intent = Intent(applicationContext, ProfileActivity::class.java)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                    overridePendingTransition(0, 0)
                }
                R.id.nav_fitting_room -> {
                    val intent = Intent(applicationContext, FittingRoomActivity::class.java)
                    startActivity(intent, ActivityOptions.makeSceneTransitionAnimation(this).toBundle())
                    overridePendingTransition(0,0)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (zoomedState) {
            findViewById<View>(R.id.expanded_image).performClick()
        }
        else {
            super.onBackPressed()
        }
    }
}
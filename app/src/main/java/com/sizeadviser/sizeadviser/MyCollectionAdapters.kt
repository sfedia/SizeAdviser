package com.sizeadviser.sizeadviser

import android.app.Application
import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.marginLeft
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_profile.*

class CollectionItemsAdapter(
    private val data: List<ClipData.Item>,
    private val external: CollectionExternal,
    private val parentActivity: MyCollectionActivity
) : RecyclerView.Adapter<CollectionItemsAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.item_collection_improved, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val colItem = Gson().fromJson(data[position].text.toString(),
            CollectionItem::class.java)

        if (colItem.is_empty) {
            holder.parentNode.visibility = View.INVISIBLE
            return
        }

        val api = SizeAdviserApi()
        holder.parentNode.invalidate()
        holder.brandElement.text = colItem.brand
        holder.sizeElement.text = colItem.size?.replace(".", ",")
        holder.standardElement.text = colItem.standard
        holder.dateElement.text = colItem.date
        when (colItem.fit_value) {
            1 -> {
                holder.fitStringElement.text = "1 size down"
                holder.fitStringElement.setTextColor(external.minColor)
            }
            2 -> {
                holder.fitStringElement.text = "Too small"
                holder.fitStringElement.setTextColor(external.medColor)
            }
            3 -> {
                holder.fitStringElement.text = "Ideal"
                holder.fitStringElement.setTextColor(external.idealColor)
            }
            4 -> {
                holder.fitStringElement.text = "Too big"
                holder.fitStringElement.setTextColor(external.medColor)
            }
            5 -> {
                holder.fitStringElement.text = "1 size up"
                holder.fitStringElement.setTextColor(external.minColor)
            }
        }
        holder.idHidden.text = colItem.fittingID!!

        holder.itemView.setOnClickListener {
            colItem?.let { it1 ->
                parentActivity.showFittingActions(colItem.fittingID)
            }
        }

        if (colItem.has_photos == true) {
            holder.imgOne.setOnClickListener {
                parentActivity.zoomImageFromThumb(holder.imgOne)
                parentActivity.loadZoomed(holder.imgOne, colItem.fittingID, 0)
            }
            holder.imgOne.setOnLongClickListener {
                parentActivity.showPhotoActions(colItem.fittingID, 0, position)
                true
            }
            holder.imgTwo.setOnClickListener {
                parentActivity.zoomImageFromThumb(holder.imgTwo)
                parentActivity.loadZoomed(holder.imgTwo, colItem.fittingID, 1)
            }
            holder.imgTwo.setOnLongClickListener {
                parentActivity.showPhotoActions(colItem.fittingID, 1, position)
                true
            }
            holder.imgThree.setOnClickListener {
                parentActivity.zoomImageFromThumb(holder.imgThree)
                parentActivity.loadZoomed(holder.imgThree, colItem.fittingID, 2)
            }
            holder.imgThree.setOnLongClickListener {
                parentActivity.showPhotoActions(colItem.fittingID, 2, position)
                true
            }

            Glide.with(parentActivity).load(api.getItemPhotoURL(colItem.fittingID, 0, true)).apply(
                RequestOptions().fitCenter().centerCrop()).into(holder.imgOne)

            Glide.with(parentActivity).load(api.getItemPhotoURL(colItem.fittingID, 1, true)).apply(
                RequestOptions().fitCenter().centerCrop()).into(holder.imgTwo)

            Glide.with(parentActivity).load(api.getItemPhotoURL(colItem.fittingID, 2, true)).apply(
                RequestOptions().fitCenter().centerCrop()).into(holder.imgThree)
        }
        else {
            holder.imgTwo.background = external.bgDrawable
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val parentNode: View = itemView
        val brandElement: TextView = itemView.findViewById(R.id.thisBrand)
        val sizeElement: TextView = itemView.findViewById(R.id.thisSize)
        val fitStringElement: TextView = itemView.findViewById(R.id.thisFitString)
        val dateElement: TextView = itemView.findViewById(R.id.itemDate)
        val standardElement: TextView = itemView.findViewById(R.id.itemStandard)
        val idHidden: TextView = itemView.findViewById(R.id.fittingIDHidden)
        val imgOne: ImageView = itemView.findViewById(R.id.imageView)
        val imgTwo: ImageView = itemView.findViewById(R.id.imageView2)
        val imgThree: ImageView = itemView.findViewById(R.id.imageView3)
    }
}
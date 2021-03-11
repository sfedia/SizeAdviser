package com.sizeadviser.sizeadviser

import android.content.ClipData
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlin.coroutines.coroutineContext

class SizesAdapter(
    private val data: List<ClipData.Item>,
    private val superView: RecyclerView
) : RecyclerView.Adapter<SizesAdapter.ViewHolder>() {

    private lateinit var recyclerView: RecyclerView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.item_brand_sizes, parent, false)
        return ViewHolder(v)
    }

    override fun onAttachedToRecyclerView(rView: RecyclerView) {
        super.onAttachedToRecyclerView(rView)
        recyclerView = rView
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.curSize.text = data[position].text
        holder.pos = position

        holder.itemView.setOnClickListener {
            superView.smoothScrollToPosition(holder.pos!!)
        }
        /*Glide.with(holder.itemView.context)
            .load(data[position].getImage())
            .into(holder.image)*/
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : FittingRoomViewHolder(itemView) {
        var curSize: TextView = itemView.findViewById(R.id.currentSize)
        var defaultColor: Int? = null
        var pos: Int? = null

        override fun getFocusElement(): TextView? {
            return itemView.findViewById(R.id.currentSize)
        }

        override fun selectedColor() {
            if (defaultColor == null) {
                defaultColor = getFocusElement()?.currentTextColor
            }
            getFocusElement()?.setTextColor(Color.BLACK)
        }

        override fun defaultColor() {
            getFocusElement()?.setTextColor(defaultColor!!)
        }
    }
}

class StandardsAdapter(
    private val data: List<ClipData.Item>,
    private val selectionColor: Int,
    private val superView: RecyclerView
) : RecyclerView.Adapter<StandardsAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.item_standards, parent, false)

        return ViewHolder(v, selectionColor)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.curStandard.text = data[position].text
        holder.pos = position
        holder.itemView.setOnClickListener {
            superView.smoothScrollToPosition(holder.pos!!)
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View, selectionColor: Int) : FittingRoomViewHolder(itemView) {
        var curStandard: TextView = itemView.findViewById(R.id.currentStandard)
        var defaultColor: Int? = null
        val selColor: Int = selectionColor
        var pos: Int? = null
        override fun getFocusElement(): TextView? {
            return itemView.findViewById(R.id.currentStandard)
        }

        override fun selectedColor() {
            if (defaultColor == null) {
                defaultColor = getFocusElement()?.currentTextColor
            }
            getFocusElement()?.setTextColor(selColor)
        }

        override fun defaultColor() {
            getFocusElement()?.setTextColor(defaultColor!!)
        }
    }
}

class RecommendedAdapter(private val data: List<ClipData.Item>) : RecyclerView.Adapter<RecommendedAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v: View = inflater.inflate(R.layout.item_recommended_sizes, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val recValues: List<String> = data[position].text.split(";")
        holder.recStandard.text = recValues[0]
        holder.recValue.text = recValues[1]
        if (position == data.size - 1) {
            holder.container.background = null
        }

        /*Glide.with(holder.itemView.context)
            .load(data[position].getImage())
            .into(holder.image)*/
    }

    override fun getItemCount(): Int {
        return data.size
    }

    class ViewHolder(itemView: View) : FittingRoomViewHolder(itemView) {
        val recStandard: TextView = itemView.findViewById(R.id.sizeStandard)
        val recValue: TextView = itemView.findViewById(R.id.sizeValue)
        val container: View = itemView.findViewById(R.id.container)

        override fun getFocusElement(): TextView? {
            return null
        }
    }
}

open class FittingRoomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val SIZES: Int = 1
    val STANDARDS: Int = 2
    val RECOMMENDATIONS: Int = 3

    open fun getFocusElement(): TextView? {
        return itemView.findViewById(R.id.currentSize)
    }

    open fun selectedColor(): Unit {
        getFocusElement()?.setTextColor(Color.RED)
    }

    open fun defaultColor(): Unit {
        getFocusElement()?.setTextColor(Color.BLACK)
    }

    fun getType(): Int {
        val sizeCheck: TextView? = itemView.findViewById(R.id.currentSize)
        val stCheck: TextView? = itemView.findViewById(R.id.currentStandard)
        val recCheck: TextView? = itemView.findViewById(R.id.sizeStandard)
        return when {
            sizeCheck != null -> {
                SIZES
            }
            stCheck != null -> {
                STANDARDS
            }
            recCheck != null -> {
                RECOMMENDATIONS
            }
            else -> {
                -1
            }
        }
    }

}
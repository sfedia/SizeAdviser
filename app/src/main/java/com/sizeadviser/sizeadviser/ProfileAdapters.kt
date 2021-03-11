package com.sizeadviser.sizeadviser

import android.app.Application
import android.content.ClipData
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import kotlinx.android.synthetic.main.item_profile_list.view.*


class ProfileStandardsAdapter(
    private val data: List<ClipData.Item>,
    private val selectionColor: Int,
    private val superView: RecyclerView
) : RecyclerView.Adapter<ProfileStandardsAdapter.ViewHolder>() {


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

    class ViewHolder(itemView: View, private val selectionColor: Int) : ProfileViewHolder(itemView) {
        var curStandard: TextView = itemView.findViewById(R.id.currentStandard)
        var defaultColor: Int? = null
        var pos: Int? = null

        override fun getFocusElement(): TextView? {
            return itemView.findViewById(R.id.currentStandard)
        }

        override fun selectedColor() {
            if (defaultColor == null) {
                defaultColor = getFocusElement()?.currentTextColor
            }
            getFocusElement()?.setTextColor(selectionColor)
        }

        override fun defaultColor() {
            getFocusElement()?.setTextColor(defaultColor!!)
        }
    }
}

open class ProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val STANDARDS: Int = 2
    val BRANDS: Int = 3

    open fun getFocusElement(): TextView? {
        return itemView.findViewById(R.id.currentSize)
    }

    open fun selectedColor(): Unit {
        getFocusElement()?.setTextColor(Color.RED)
    }

    open fun defaultColor(): Unit {
        getFocusElement()?.setTextColor(Color.BLACK)
    }

    open fun getType(): Int {
        return STANDARDS
    }

}

class ProfileListAdapter(
    context: Context,
    @LayoutRes private val layoutResource: Int,
    private val brands: MutableList<ProfileRowWrapper>,
    private val palettePinkColor: Int
):
    ArrayAdapter<ProfileRowWrapper>(context, layoutResource, brands) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return createViewFromResource(position, convertView, parent)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup?): View{
        val view: CardView = convertView as CardView? ?: LayoutInflater.from(context).inflate(layoutResource, parent, false) as CardView
        when (brands[position].itemType) {
            "default" -> {
                view.brandPosition.setTypeface(null, Typeface.NORMAL)
                view.brandPosition.text = brands[position].brandData?.brand
                view.sizePosition.text = brands[position].brandData?.SSUnit?.size
                if (brands[position].brandData?.triedOn!!) {
                    view.brandPosition.setTextColor(palettePinkColor)
                    view.sizePosition.setTextColor(palettePinkColor)
                }
                else {
                    view.brandPosition.setTextColor(Color.BLACK)
                    view.sizePosition.setTextColor(Color.BLACK)
                }
            }
            "wo_ss_unit" -> {
                view.brandPosition.setTypeface(null, Typeface.NORMAL)
                view.brandPosition.text = brands[position].brandData?.brand
                view.sizePosition.text = null
                if (brands[position].brandData?.triedOn!!) {
                    view.brandPosition.setTextColor(palettePinkColor)
                    view.sizePosition.setTextColor(palettePinkColor)
                }
                else {
                    view.brandPosition.setTextColor(Color.BLACK)
                    view.sizePosition.setTextColor(Color.BLACK)
                }
            }
            "capital" -> {
                view.brandPosition.text = brands[position].capitalLetter
                view.brandPosition.setTypeface(null, Typeface.BOLD)
                view.brandPosition.setTextColor(Color.BLACK)
                view.sizePosition.text = null
            }
        }
        return view
    }
}
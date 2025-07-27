package com.example.fasttickets


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.wm.fasttickets.R
import android.widget.ImageView


class SlideAdapter(private val context: Context, private val slides: List<SlideData>) : 
    RecyclerView.Adapter<SlideAdapter.SlideViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.slide_layout, parent, false)
        return SlideViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val slide = slides[position]
        holder.titleTextView.text = slide.title
        holder.messageTextView.text = slide.message
        slide.imageResId?.let {
            holder.imageView.setImageResource(it)
        }
    }

    override fun getItemCount() = slides.size

    class SlideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.dialogTitle)
        val messageTextView: TextView = view.findViewById(R.id.dialogMessage)
        val imageView: ImageView = view.findViewById(R.id.slideImage)
    }

    data class SlideData(
        val title: String,
        val message: String,
        val imageResId: Int? = null
    )
}

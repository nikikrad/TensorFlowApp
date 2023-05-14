package com.example.tensorflowapp.presentation.main.images.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.tensorflowapp.R
import com.example.tensorflowapp.presentation.main.images.ImageWithText
import com.example.tensorflowapp.presentation.main.`object`.model.ModelFirebase
import com.squareup.picasso.Picasso

class ImagesAdapter(
    private val animeList: List<ModelFirebase>
) : RecyclerView.Adapter<ImagesAdapter.MainViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MainViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view = layoutInflater.inflate(R.layout.item_image, parent, false)
        return MainViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.bind(animeList[position])
        holder.constraint.animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.scale_anim)
    }

    override fun getItemCount(): Int = animeList.size

    class MainViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.iv_Image)
        private val text: TextView = itemView.findViewById(R.id.tv_Text)
        val constraint: ConstraintLayout = itemView.findViewById(R.id.constraint)
        fun bind(item: ModelFirebase) {
//            Glide.with(itemView.context)
//                .load(item.url)
//                .into(image)
            Picasso.get()
                .load(item.url)
                .placeholder(R.drawable.ic_search)
                .into(image)
            text.text = item.text
        }
    }
}
package com.example.ukonnect2.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ukonnect2.R
import com.example.ukonnect2.network.AlatDto

class AlatAdapter(
    private var listAlat: List<AlatDto>,
    private val onItemClick: ((AlatDto) -> Unit)? = null
) : RecyclerView.Adapter<AlatAdapter.AlatViewHolder>() {

    inner class AlatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        val tvNamaAlat: TextView = itemView.findViewById(R.id.tvNamaAlat)
        val tvStok: TextView = itemView.findViewById(R.id.tvStok)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alat, parent, false)
        return AlatViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlatViewHolder, position: Int) {
        val alat = listAlat[position]
        holder.tvNamaAlat.text = alat.nama
        holder.tvStok.text = "Stok: ${alat.stokTersedia}/${alat.stokTotal}"

        // Jika ada iconKey, bisa pakai Glide/Picasso untuk load image
        // Contoh: Glide.with(holder.itemView).load(alat.iconKey).into(holder.ivIcon)

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(alat)
        }
    }

    override fun getItemCount(): Int = listAlat.size

    fun updateData(newList: List<AlatDto>) {
        listAlat = newList
        notifyDataSetChanged()
    }
}

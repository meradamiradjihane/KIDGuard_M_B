package com.example.kidguard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SecurezoneAdapter(
    private val zones: List<SafeZone>,
    private val onItemClick: (SafeZone) -> Unit,
    private val onDeleteClick: (SafeZone) -> Unit
) : RecyclerView.Adapter<SecurezoneAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val adresseTextView: TextView = view.findViewById(R.id.zoneAdresse)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)

        init {
            view.setOnClickListener {
                onItemClick(zones[adapterPosition])
            }
            deleteButton.setOnClickListener {
                onDeleteClick(zones[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_zone, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = zones.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.adresseTextView.text = zones[position].adresse
    }
}

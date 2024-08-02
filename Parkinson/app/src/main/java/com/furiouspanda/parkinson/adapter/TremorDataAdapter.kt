package com.harsh.parkinson.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.harsh.parkinson.R
import com.harsh.parkinson.database.ResultTremor

class TremorDataAdapter : RecyclerView.Adapter<TremorDataAdapter.TremorViewHolder>() {

    private var tremorList = listOf<ResultTremor>()

    fun updateData(newList: List<ResultTremor>) {
        tremorList = newList
        notifyDataSetChanged()
    }
    fun clearData() {
        tremorList = emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TremorViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_tremor_data, parent, false)
        return TremorViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TremorViewHolder, position: Int) {
        val tremor = tremorList[position]
        holder.bind(tremor)
    }

    override fun getItemCount(): Int {
        return tremorList.size
    }

    inner class TremorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val maxValueTextView: TextView = itemView.findViewById(R.id.textViewMaxValue)
        private val currentDayTextView: TextView = itemView.findViewById(R.id.textViewDay)

        fun bind(tremor: ResultTremor) {
            maxValueTextView.text = tremor.maxValue.toString()
            currentDayTextView.text = tremor.currentDay
        }
    }
}


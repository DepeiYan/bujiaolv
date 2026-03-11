package com.screentracker.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screentracker.data.db.DailyStats
import com.screentracker.databinding.ItemDailyStatBinding
import com.screentracker.ui.home.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

class DailyStatsAdapter(
    private val viewModel: HomeViewModel,
    private val onItemClick: (dateStr: String) -> Unit
) : ListAdapter<DailyStats, DailyStatsAdapter.ViewHolder>(DiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.CHINESE)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyStatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemDailyStatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position).dateStr)
                }
            }
        }

        fun bind(stats: DailyStats) {
            val date = dateFormat.parse(stats.dateStr) ?: Date()
            val cal = Calendar.getInstance().apply { time = date }

            binding.tvDayOfWeek.text = dayOfWeekFormat.format(date)
            binding.tvDayNumber.text = cal.get(Calendar.DAY_OF_MONTH).toString()
            binding.tvCount.text = "${stats.count}次"
            binding.tvDuration.text = viewModel.formatDuration(stats.totalDuration)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DailyStats>() {
        override fun areItemsTheSame(oldItem: DailyStats, newItem: DailyStats): Boolean {
            return oldItem.dateStr == newItem.dateStr
        }
        override fun areContentsTheSame(oldItem: DailyStats, newItem: DailyStats): Boolean {
            return oldItem == newItem
        }
    }
}

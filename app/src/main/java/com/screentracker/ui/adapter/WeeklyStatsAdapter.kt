package com.screentracker.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screentracker.databinding.ItemWeeklyStatBinding
import com.screentracker.ui.home.HomeViewModel

/**
 * 每周统计数据模型
 */
data class WeeklyStat(
    val weekNumber: Int,
    val startDateStr: String,
    val endDateStr: String,
    val count: Int,
    val totalDuration: Long
)

class WeeklyStatsAdapter(
    private val viewModel: HomeViewModel,
    private val onItemClick: (weekNumber: Int) -> Unit
) : ListAdapter<WeeklyStat, WeeklyStatsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWeeklyStatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemWeeklyStatBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position).weekNumber)
                }
            }
        }

        fun bind(stat: WeeklyStat) {
            binding.tvWeekNumber.text = stat.weekNumber.toString()
            binding.tvWeekRange.text = formatDateRange(stat.startDateStr, stat.endDateStr)
            binding.tvCount.text = "${stat.count}次点亮"
            binding.tvDuration.text = viewModel.formatDuration(stat.totalDuration)
        }

        private fun formatDateRange(start: String, end: String): String {
            // start/end 格式: 2026-03-03
            val startParts = start.split("-")
            val endParts = end.split("-")
            return if (startParts.size == 3 && endParts.size == 3) {
                "${startParts[1].toInt()}月${startParts[2].toInt()}日 - ${endParts[1].toInt()}月${endParts[2].toInt()}日"
            } else {
                "$start - $end"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WeeklyStat>() {
        override fun areItemsTheSame(oldItem: WeeklyStat, newItem: WeeklyStat): Boolean {
            return oldItem.weekNumber == newItem.weekNumber
        }
        override fun areContentsTheSame(oldItem: WeeklyStat, newItem: WeeklyStat): Boolean {
            return oldItem == newItem
        }
    }
}

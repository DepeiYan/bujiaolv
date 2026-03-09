package com.screentracker.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.screentracker.data.db.ScreenEvent
import com.screentracker.databinding.ItemScreenEventBinding
import com.screentracker.ui.home.HomeViewModel

/**
 * 屏幕事件列表适配器
 */
class ScreenEventAdapter(
    private val viewModel: HomeViewModel
) : ListAdapter<ScreenEvent, ScreenEventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemScreenEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemScreenEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(event: ScreenEvent) {
            binding.tvScreenOnTime.text = viewModel.formatTime(event.screenOnTime)

            if (event.screenOffTime != null) {
                binding.tvScreenOffTime.text = viewModel.formatTime(event.screenOffTime)
                binding.tvDuration.text = viewModel.formatDuration(event.duration ?: 0)
            } else {
                binding.tvScreenOffTime.text = "使用中..."
                binding.tvDuration.text = "---"
            }
        }
    }

    class EventDiffCallback : DiffUtil.ItemCallback<ScreenEvent>() {
        override fun areItemsTheSame(oldItem: ScreenEvent, newItem: ScreenEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ScreenEvent, newItem: ScreenEvent): Boolean {
            return oldItem == newItem
        }
    }
}

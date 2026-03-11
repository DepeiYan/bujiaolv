package com.screentracker.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.screentracker.databinding.ActivityAnxietyLevelsBinding
import com.screentracker.databinding.ItemAnxietyLevelCardBinding
import com.screentracker.ui.home.AnxietyLevel

/**
 * 焦虑等级说明页面
 * 使用真实的 AnxietyLevel 枚举数据
 */
class AnxietyLevelsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAnxietyLevelsBinding

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AnxietyLevelsActivity::class.java)
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnxietyLevelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerLevels.layoutManager = LinearLayoutManager(this)
        // 使用 AnxietyLevel 枚举的真实数据
        val levels = AnxietyLevel.values().toList()
        binding.recyclerLevels.adapter = AnxietyLevelAdapter(levels)
    }

    // 适配器
    inner class AnxietyLevelAdapter(
        private val items: List<AnxietyLevel>
    ) : RecyclerView.Adapter<AnxietyLevelAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemAnxietyLevelCardBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemAnxietyLevelCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val level = items[position]
            holder.binding.apply {
                tvEmoji.text = level.emoji
                tvLevel.text = "LV.${level.level}"
                tvName.text = level.nameCn
                tvRange.text = "${level.minCount}-${if (level.maxCount == Int.MAX_VALUE) "+" else level.maxCount}次"
                tvDescription.text = level.description

                // 解析颜色
                val startColor = Color.parseColor(level.gradientStart)
                val endColor = Color.parseColor(level.gradientEnd)
                val textColor = Color.parseColor(level.colorHex)

                // 设置文字颜色
                tvLevel.setTextColor(textColor)
                tvName.setTextColor(textColor)
                tvRange.setTextColor(textColor)

                // 设置渐变背景
                val gradientDrawable = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(startColor, endColor)
                )
                gradientDrawable.cornerRadius = 16f * root.resources.displayMetrics.density
                layoutContent.background = gradientDrawable
            }
        }

        override fun getItemCount() = items.size
    }
}
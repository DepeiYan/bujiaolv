package com.screentracker.ui.dialog

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.screentracker.R
import com.screentracker.databinding.DialogAnxietyLevelsBinding
import com.screentracker.databinding.ItemAnxietyLevelBinding

data class AnxietyLevelInfo(
    val level: Int,
    val emoji: String,
    val name: String,
    val range: String,
    val description: String,
    val colorRes: Int
)

class AnxietyLevelDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAnxietyLevelsBinding? = null
    private val binding get() = _binding!!
    private var initialLevel: Int = 1

    private val levels = listOf(
        AnxietyLevelInfo(0, "📱", "无数据", "0次", "暂无屏幕使用记录", R.color.anxiety_level_0),
        AnxietyLevelInfo(1, "🧘", "人间清醒", "1-40次", "手机只是工具，你才是主人。拥有极强的专注力，生活充实，几乎不存在数字焦虑。", R.color.anxiety_level_1),
        AnxietyLevelInfo(2, "😊", "轻度依赖", "41-70次", "偶尔查看消息，但还能控制。适度的手机使用，不影响正常生活。", R.color.anxiety_level_2),
        AnxietyLevelInfo(3, "😰", "中度焦虑", "71-100次", "频繁解锁，开始有点焦虑了。建议适当减少手机使用时间，培养其他兴趣爱好。", R.color.anxiety_level_3),
        AnxietyLevelInfo(4, "😫", "重度依赖", "101-150次", "手机不离手，焦虑感明显。需要引起重视，尝试设定使用限制。", R.color.anxiety_level_4),
        AnxietyLevelInfo(5, "😵", "数字囚徒", "150+次", "被手机绑架，急需改变。建议寻求专业帮助或使用手机管控应用。", R.color.anxiety_level_5)
    )

    companion object {
        fun newInstance(currentLevel: Int): AnxietyLevelDialog {
            return AnxietyLevelDialog().apply {
                initialLevel = currentLevel
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAnxietyLevelsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewPager()
        setupClickOutside()
        animateEntrance()
    }

    private fun setupViewPager() {
        val adapter = AnxietyLevelAdapter(levels)
        binding.viewpagerLevels.adapter = adapter
        binding.viewpagerLevels.setCurrentItem(initialLevel, false)

        // 设置指示器
        TabLayoutMediator(binding.tabIndicator, binding.viewpagerLevels) { _, _ ->
        }.attach()

        // 页面变化时更新指示器
        binding.viewpagerLevels.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateTabIndicator(position)
            }
        })

        // 初始化指示器
        updateTabIndicator(initialLevel)
    }

    private fun updateTabIndicator(position: Int) {
        for (i in 0 until binding.tabIndicator.tabCount) {
            val tab = binding.tabIndicator.getTabAt(i)
            tab?.view?.isSelected = i == position
        }
    }

    private fun setupClickOutside() {
        // 点击外部关闭
        binding.viewOutside.setOnClickListener {
            dismiss()
        }

        // 点击卡片不关闭
        binding.cardContent.setOnClickListener {
            // 不执行任何操作，只是拦截点击事件
        }
    }

    private fun animateEntrance() {
        binding.cardContent.translationY = 500f
        binding.cardContent.alpha = 0f

        val translateAnimator = ObjectAnimator.ofFloat(binding.cardContent, "translationY", 500f, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(binding.cardContent, "alpha", 0f, 1f)

        AnimatorSet().apply {
            playTogether(translateAnimator, alphaAnimator)
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Adapter
    inner class AnxietyLevelAdapter(
        private val items: List<AnxietyLevelInfo>
    ) : RecyclerView.Adapter<AnxietyLevelAdapter.ViewHolder>() {

        inner class ViewHolder(val binding: ItemAnxietyLevelBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemAnxietyLevelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.binding.apply {
                tvEmoji.text = item.emoji
                tvLevel.text = "LV.${item.level}"
                tvName.text = item.name
                tvRange.text = item.range
                tvDescription.text = item.description

                // 设置颜色
                val color = root.context.getColor(item.colorRes)
                tvLevel.setTextColor(color)
                tvName.setTextColor(color)
            }
        }

        override fun getItemCount() = items.size
    }
}
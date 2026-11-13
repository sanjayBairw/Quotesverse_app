package com.snj.quoteverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.snj.quoteverse.databinding.ItemCategoryChipBinding
import com.snj.quoteverse.utils.CategoryItem

data class CategoryUiModel(
    val item: CategoryItem,
    val isSelected: Boolean,
    val count: Int? = null
)

/**
 * Horizontal category selector adapter used on Home and Explore screens.
 */
class CategoryAdapter(
    private val onCategorySelected: (CategoryItem) -> Unit
) : ListAdapter<CategoryUiModel, CategoryAdapter.CategoryViewHolder>(CategoryDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CategoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CategoryViewHolder(
        private val binding: ItemCategoryChipBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: CategoryUiModel) {
            val context = binding.root.context
            binding.ivCategoryIcon.setImageResource(model.item.iconRes)
            binding.tvCategoryName.text = context.getString(model.item.labelRes)

            if (model.count != null) {
                binding.tvCategoryCount.visibility = View.VISIBLE
                binding.tvCategoryCount.text = model.count.toString()
            } else {
                binding.tvCategoryCount.visibility = View.GONE
            }

            if (model.isSelected) {
                val bgColor = MaterialColors.getColor(
                    binding.root,
                    androidx.appcompat.R.attr.colorPrimary
                )
                val fgColor = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnPrimary
                )
                binding.categoryChipCard.setCardBackgroundColor(bgColor)
                binding.categoryChipCard.strokeWidth = 0
                binding.tvCategoryName.setTextColor(fgColor)
                binding.tvCategoryCount.setTextColor(fgColor)
                binding.ivCategoryIcon.setColorFilter(fgColor)
            } else {
                val bgColor = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorSurface
                )
                val strokeColor = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOutlineVariant
                )
                val textColor = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnSurface
                )
                val mutedColor = MaterialColors.getColor(
                    binding.root,
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
                val iconColor = MaterialColors.getColor(
                    binding.root,
                    androidx.appcompat.R.attr.colorPrimary
                )
                binding.categoryChipCard.setCardBackgroundColor(bgColor)
                binding.categoryChipCard.strokeColor = strokeColor
                binding.categoryChipCard.strokeWidth = context.resources.displayMetrics.density.toInt().coerceAtLeast(1)
                binding.tvCategoryName.setTextColor(textColor)
                binding.tvCategoryCount.setTextColor(mutedColor)
                binding.ivCategoryIcon.setColorFilter(iconColor)
            }

            binding.categoryChipCard.setOnClickListener {
                onCategorySelected(model.item)
            }
        }
    }

    private object CategoryDiffCallback : DiffUtil.ItemCallback<CategoryUiModel>() {
        override fun areItemsTheSame(oldItem: CategoryUiModel, newItem: CategoryUiModel): Boolean {
            return oldItem.item.key == newItem.item.key
        }

        override fun areContentsTheSame(oldItem: CategoryUiModel, newItem: CategoryUiModel): Boolean {
            return oldItem == newItem
        }
    }
}


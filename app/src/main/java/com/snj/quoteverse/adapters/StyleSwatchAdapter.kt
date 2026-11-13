package com.snj.quoteverse.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.quoteverse.R
import com.snj.quoteverse.databinding.ItemStyleSwatchBinding
import com.snj.quoteverse.utils.GradientStyle

data class StyleSwatchUiModel(
    val style: GradientStyle,
    val isSelected: Boolean
)

/**
 * RecyclerView adapter displaying gradient atmosphere swatches on the Quote Details screen.
 */
class StyleSwatchAdapter(
    private val onStyleSelected: (GradientStyle) -> Unit
) : ListAdapter<StyleSwatchUiModel, StyleSwatchAdapter.SwatchViewHolder>(SwatchDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SwatchViewHolder {
        val binding = ItemStyleSwatchBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SwatchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SwatchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SwatchViewHolder(
        private val binding: ItemStyleSwatchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(model: StyleSwatchUiModel) {
            val context = binding.root.context
            binding.viewSwatchFill.setBackgroundResource(model.style.backgroundRes)
            binding.viewSwatchSelectedRing.visibility = if (model.isSelected) View.VISIBLE else View.GONE
            binding.swatchCard.contentDescription = context.getString(
                R.string.cd_style_swatch,
                context.getString(model.style.nameRes)
            )
            binding.swatchCard.setOnClickListener { onStyleSelected(model.style) }
        }
    }

    private object SwatchDiffCallback : DiffUtil.ItemCallback<StyleSwatchUiModel>() {
        override fun areItemsTheSame(
            oldItem: StyleSwatchUiModel,
            newItem: StyleSwatchUiModel
        ): Boolean = oldItem.style.id == newItem.style.id

        override fun areContentsTheSame(
            oldItem: StyleSwatchUiModel,
            newItem: StyleSwatchUiModel
        ): Boolean = oldItem == newItem
    }
}


package com.snj.quoteverse.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.snj.quoteverse.R
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.databinding.ItemQuoteCardBinding
import com.snj.quoteverse.utils.CategoryProvider
import java.util.Locale

/**
 * RecyclerView ListAdapter displaying quote cards with DiffUtil diffing and inline actions.
 */
class QuoteAdapter(
    private val onQuoteClick: (QuoteEntity) -> Unit,
    private val onFavoriteClick: (QuoteEntity) -> Unit,
    private val onCopyClick: (QuoteEntity) -> Unit,
    private val onShareClick: (QuoteEntity) -> Unit,
    private val fontProvider: (() -> com.snj.quoteverse.utils.FontOption)? = null
) : ListAdapter<QuoteEntity, QuoteAdapter.QuoteViewHolder>(QuoteDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuoteViewHolder {
        val binding = ItemQuoteCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuoteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuoteViewHolder(
        private val binding: ItemQuoteCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(quote: QuoteEntity) {
            val context = binding.root.context
            val categoryItem = CategoryProvider.findByKey(quote.category)
            binding.tvCategoryBadge.text = context.getString(categoryItem.labelRes).uppercase(Locale.getDefault())
            binding.tvQuoteText.text = quote.quoteText
            fontProvider?.invoke()?.let { font ->
                binding.tvQuoteText.typeface = font.typeface
            }
            binding.tvAuthor.text = context.getString(R.string.author_prefix, quote.author)

            val favoriteIcon = if (quote.isFavorite) {
                R.drawable.ic_favorite_filled
            } else {
                R.drawable.ic_favorite_outline
            }
            binding.btnFavorite.setImageResource(favoriteIcon)

            val tintColor = if (quote.isFavorite) {
                ContextCompat.getColor(context, R.color.qv_favorite_active)
            } else {
                MaterialColors.getColor(
                    binding.btnFavorite,
                    com.google.android.material.R.attr.colorOnSurfaceVariant
                )
            }
            binding.btnFavorite.setColorFilter(tintColor)

            binding.quoteCardRoot.setOnClickListener { onQuoteClick(quote) }
            binding.btnFavorite.setOnClickListener { view ->
                view.animate()
                    .scaleX(1.22f)
                    .scaleY(1.22f)
                    .setDuration(110)
                    .withEndAction {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(110).start()
                    }
                    .start()
                onFavoriteClick(quote)
            }
            binding.btnCopy.setOnClickListener { onCopyClick(quote) }
            binding.btnShare.setOnClickListener { onShareClick(quote) }
        }
    }

    private object QuoteDiffCallback : DiffUtil.ItemCallback<QuoteEntity>() {
        override fun areItemsTheSame(oldItem: QuoteEntity, newItem: QuoteEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: QuoteEntity, newItem: QuoteEntity): Boolean {
            return oldItem == newItem
        }
    }
}


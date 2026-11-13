package com.snj.quoteverse.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.snj.quoteverse.R
import com.snj.quoteverse.data.entity.QuoteEntity
import com.snj.quoteverse.databinding.ItemRecentQuoteBinding
import com.snj.quoteverse.utils.CategoryProvider
import com.snj.quoteverse.utils.GradientStyleProvider
import java.util.Locale

/**
 * Horizontal RecyclerView adapter for recently viewed quotes on the Home screen.
 */
class RecentQuoteAdapter(
    private val onQuoteClick: (QuoteEntity) -> Unit,
    private val onFavoriteClick: (QuoteEntity) -> Unit,
    private val fontProvider: (() -> com.snj.quoteverse.utils.FontOption)? = null
) : ListAdapter<QuoteEntity, RecentQuoteAdapter.RecentViewHolder>(RecentDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentViewHolder {
        val binding = ItemRecentQuoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RecentViewHolder(
        private val binding: ItemRecentQuoteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(quote: QuoteEntity) {
            val context = binding.root.context
            val styleIndex = (quote.id % GradientStyleProvider.styles.size).toInt()
            val gradient = GradientStyleProvider.getById(styleIndex)
            binding.recentGradientContainer.setBackgroundResource(gradient.backgroundRes)

            val categoryItem = CategoryProvider.findByKey(quote.category)
            binding.tvRecentCategory.text = context.getString(categoryItem.labelRes).uppercase(Locale.getDefault())
            binding.tvRecentQuote.text = quote.quoteText
            fontProvider?.invoke()?.let { font ->
                binding.tvRecentQuote.typeface = font.typeface
            }
            binding.tvRecentAuthor.text = context.getString(R.string.author_prefix, quote.author)

            binding.btnRecentFavorite.setImageResource(
                if (quote.isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline
            )
            val tintColor = if (quote.isFavorite) {
                ContextCompat.getColor(context, R.color.qv_favorite_active)
            } else {
                ContextCompat.getColor(context, android.R.color.white)
            }
            binding.btnRecentFavorite.setColorFilter(tintColor)

            binding.recentCardRoot.setOnClickListener { onQuoteClick(quote) }
            binding.btnRecentFavorite.setOnClickListener { onFavoriteClick(quote) }
        }
    }

    private object RecentDiffCallback : DiffUtil.ItemCallback<QuoteEntity>() {
        override fun areItemsTheSame(oldItem: QuoteEntity, newItem: QuoteEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: QuoteEntity, newItem: QuoteEntity): Boolean {
            return oldItem == newItem
        }
    }
}


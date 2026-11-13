package com.snj.quoteverse.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.snj.quoteverse.data.repository.QuoteRepository
import com.snj.quoteverse.utils.QuotePreferences

/**
 * Factory creating [QuoteViewModel] instances with repository and preferences dependencies.
 */
class QuoteViewModelFactory(
    private val repository: QuoteRepository,
    private val preferences: QuotePreferences,
    private val networkMonitor: com.snj.quoteverse.utils.NetworkMonitor? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuoteViewModel::class.java)) {
            return QuoteViewModel(repository, preferences, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}


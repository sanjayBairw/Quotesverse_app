package com.snj.quoteverse.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.snj.quoteverse.R
import com.snj.quoteverse.data.entity.QuoteEntity

/**
 * Shared UI helpers for window insets, Snackbars, and quote clipboard/share actions.
 */
fun View.applyTopSystemBarPadding() {
    val initialTop = paddingTop
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val topInset = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        view.updatePadding(top = initialTop + topInset)
        insets
    }
}

fun View.applyBottomSystemBarMargin() {
    val initialBottom = (layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin ?: 0
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bottomInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = initialBottom + bottomInset
        }
        insets
    }
}

fun Fragment.showQuoteSnackbar(
    @StringRes messageRes: Int,
    actionTextRes: Int? = null,
    onAction: (() -> Unit)? = null
) {
    showQuoteSnackbar(getString(messageRes), actionTextRes, onAction)
}

fun Fragment.showQuoteSnackbar(
    message: String,
    actionTextRes: Int? = null,
    onAction: (() -> Unit)? = null
) {
    val rootView = view ?: return
    val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT)
    val bottomNav = activity?.findViewById<View>(R.id.bottomNavigation)
    if (bottomNav != null && bottomNav.visibility == View.VISIBLE) {
        snackbar.anchorView = bottomNav
    }
    if (actionTextRes != null && onAction != null) {
        snackbar.setAction(actionTextRes) { onAction() }
    }
    snackbar.show()
}

fun Fragment.copyQuoteWithFeedback(context: Context, quote: QuoteEntity) {
    val copied = QuoteActionUtils.copyQuote(context, quote)
    showQuoteSnackbar(if (copied) R.string.snackbar_quote_copied else R.string.snackbar_copy_failed)
}


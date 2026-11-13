package com.snj.quoteverse.utils

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.snj.quoteverse.R

/**
 * Metadata describing a quote category for UI filters and browse cards.
 */
data class CategoryItem(
    val key: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
    val gradientIndex: Int
)

/**
 * Provides the supported QuoteVerse categories with localized labels and vector icons.
 */
object CategoryProvider {

    const val KEY_ALL = "All"

    val allCategory = CategoryItem(
        key = KEY_ALL,
        labelRes = R.string.category_all,
        iconRes = R.drawable.ic_sparkle,
        gradientIndex = 0
    )

    val categories: List<CategoryItem> = listOf(
        CategoryItem("Motivation", R.string.category_motivation, R.drawable.ic_cat_motivation, 0),
        CategoryItem("Success", R.string.category_success, R.drawable.ic_cat_success, 1),
        CategoryItem("Life", R.string.category_life, R.drawable.ic_cat_life, 2),
        CategoryItem("Love", R.string.category_love, R.drawable.ic_cat_love, 3),
        CategoryItem("Happiness", R.string.category_happiness, R.drawable.ic_cat_happiness, 4),
        CategoryItem("Wisdom", R.string.category_wisdom, R.drawable.ic_cat_wisdom, 5),
        CategoryItem("Friendship", R.string.category_friendship, R.drawable.ic_cat_friendship, 1),
        CategoryItem("Attitude", R.string.category_attitude, R.drawable.ic_cat_attitude, 0),
        CategoryItem("Study", R.string.category_study, R.drawable.ic_cat_study, 4),
        CategoryItem("Inspirational", R.string.category_inspirational, R.drawable.ic_cat_inspirational, 2)
    )

    val filterCategories: List<CategoryItem> = listOf(allCategory) + categories

    fun findByKey(key: String?): CategoryItem {
        return filterCategories.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: allCategory
    }
}


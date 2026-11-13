package com.snj.quoteverse

import android.content.Intent
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.HorizontalScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.snj.quoteverse.databinding.ActivityMainBinding

/**
 * Single-activity host managing BottomNavigationView, Navigation Component destinations,
 * directional horizontal slide animations, and global swipe navigation across screens.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var gestureDetector: GestureDetector

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var isDisallowedForTouch = false
    private var isVerticalScrollInProgress = false
    private var hasTriggeredSwipe = false
    private var didCancelChildTouches = false

    private val mainTabIds = listOf(
        R.id.homeFragment,
        R.id.exploreFragment,
        R.id.favoritesFragment,
        R.id.settingsFragment
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        navController = navHostFragment.navController

        setupBottomNavigation()
        setupGestureDetector()

        if (savedInstanceState == null) {
            handleDeepLinkQuote(intent)
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            navigateToTab(item.itemId)
            true
        }

        binding.bottomNavigation.setOnItemReselectedListener {
            // Optional: Reselecting the active tab maintains state
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNavigation.visibility = if (destination.id == R.id.quoteDetailsFragment) {
                View.GONE
            } else {
                View.VISIBLE
            }

            val menu = binding.bottomNavigation.menu
            for (i in 0 until menu.size()) {
                val item = menu.getItem(i)
                if (item.itemId == destination.id) {
                    item.isChecked = true
                    break
                }
            }
        }
    }

    private fun setupGestureDetector() {
        val density = resources.displayMetrics.density
        val minFlingDistance = 45f * density
        val minFlingVelocity = 350f * density

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (isDisallowedForTouch || isVerticalScrollInProgress || hasTriggeredSwipe) {
                    return false
                }
                val dx = e2.rawX - (e1?.rawX ?: return false)
                val dy = e2.rawY - (e1?.rawY ?: return false)

                if (Math.abs(dx) > Math.abs(dy) * 1.35f &&
                    Math.abs(dx) > minFlingDistance &&
                    Math.abs(velocityX) > minFlingVelocity
                ) {
                    hasTriggeredSwipe = true
                    if (dx < 0) {
                        navigateToNextTab()
                    } else {
                        navigateToPreviousTab()
                    }
                    return true
                }
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)

        val density = resources.displayMetrics.density
        val minDragDistance = 75f * density
        val minVerticalLockDistance = 16f * density

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = ev.rawX
                touchDownY = ev.rawY
                isDisallowedForTouch = isTouchInsideHorizontalScrollable(binding.root, touchDownX, touchDownY)
                isVerticalScrollInProgress = false
                hasTriggeredSwipe = false
                didCancelChildTouches = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isDisallowedForTouch && !hasTriggeredSwipe) {
                    val dx = ev.rawX - touchDownX
                    val dy = ev.rawY - touchDownY

                    if (!isVerticalScrollInProgress) {
                        if (Math.abs(dy) > Math.abs(dx) * 1.5f && Math.abs(dy) > minVerticalLockDistance) {
                            isVerticalScrollInProgress = true
                        } else if (Math.abs(dx) > Math.abs(dy) * 1.5f && Math.abs(dx) > minDragDistance) {
                            hasTriggeredSwipe = true
                            if (dx < 0) {
                                navigateToNextTab()
                            } else {
                                navigateToPreviousTab()
                            }
                        }
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDisallowedForTouch = false
                isVerticalScrollInProgress = false
                hasTriggeredSwipe = false
                didCancelChildTouches = false
            }
        }

        if (hasTriggeredSwipe && !didCancelChildTouches) {
            didCancelChildTouches = true
            val cancelEvent = MotionEvent.obtain(ev).apply { action = MotionEvent.ACTION_CANCEL }
            super.dispatchTouchEvent(cancelEvent)
            cancelEvent.recycle()
            return true
        }

        return super.dispatchTouchEvent(ev)
    }

    fun navigateToTab(targetTabId: Int) {
        val currentDestinationId = navController.currentDestination?.id ?: R.id.homeFragment
        if (currentDestinationId == targetTabId) return

        val currentIndex = mainTabIds.indexOf(currentDestinationId)
        val targetIndex = mainTabIds.indexOf(targetTabId)

        val isMovingForward = targetIndex > currentIndex
        val enterAnim = if (isMovingForward) R.anim.slide_in_right else R.anim.slide_in_left
        val exitAnim = if (isMovingForward) R.anim.slide_out_left else R.anim.slide_out_right
        val popEnterAnim = if (isMovingForward) R.anim.slide_in_right else R.anim.slide_in_left
        val popExitAnim = if (isMovingForward) R.anim.slide_out_left else R.anim.slide_out_right

        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(
                navController.graph.findStartDestination().id,
                inclusive = false,
                saveState = true
            )
            .setEnterAnim(enterAnim)
            .setExitAnim(exitAnim)
            .setPopEnterAnim(popEnterAnim)
            .setPopExitAnim(popExitAnim)
            .build()

        try {
            navController.navigate(targetTabId, null, navOptions)
        } catch (_: Exception) {
            binding.bottomNavigation.selectedItemId = targetTabId
        }
    }

    private fun navigateToNextTab() {
        val currentId = navController.currentDestination?.id ?: return
        val currentIndex = mainTabIds.indexOf(currentId)
        if (currentIndex in 0 until mainTabIds.lastIndex) {
            navigateToTab(mainTabIds[currentIndex + 1])
        }
    }

    private fun navigateToPreviousTab() {
        val currentId = navController.currentDestination?.id ?: return
        if (currentId == R.id.quoteDetailsFragment) {
            navController.navigateUp()
            return
        }
        val currentIndex = mainTabIds.indexOf(currentId)
        if (currentIndex > 0) {
            navigateToTab(mainTabIds[currentIndex - 1])
        }
    }

    private fun isTouchInsideHorizontalScrollable(root: View, x: Float, y: Float): Boolean {
        val location = IntArray(2)
        return checkViewIsHorizontalScrollable(root, x, y, location)
    }

    private fun checkViewIsHorizontalScrollable(view: View, x: Float, y: Float, location: IntArray): Boolean {
        if (!view.isShown) return false
        view.getLocationOnScreen(location)
        val left = location[0]
        val top = location[1]
        val right = left + view.width
        val bottom = top + view.height

        if (x < left || x > right || y < top || y > bottom) {
            return false
        }

        if (view is EditText) {
            return true
        }

        if (view is RecyclerView) {
            val lm = view.layoutManager
            if (lm is LinearLayoutManager && lm.orientation == LinearLayoutManager.HORIZONTAL) {
                return true
            }
        } else if (view is HorizontalScrollView) {
            return true
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (checkViewIsHorizontalScrollable(child, x, y, location)) {
                    return true
                }
            }
        }
        return false
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLinkQuote(intent)
    }

    private fun handleDeepLinkQuote(intent: Intent?) {
        val quoteId = intent?.getLongExtra(EXTRA_QUOTE_ID, -1L) ?: -1L
        if (quoteId > 0L) {
            navController.navigate(
                R.id.action_global_quoteDetails,
                bundleOf(ARG_QUOTE_ID to quoteId)
            )
            intent?.removeExtra(EXTRA_QUOTE_ID)
        }
    }

    companion object {
        const val EXTRA_QUOTE_ID = "extra_quote_id"
        const val ARG_QUOTE_ID = "quoteId"
    }
}

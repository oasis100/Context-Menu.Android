package com.yalantis.contextmenu.lib

import android.animation.Animator
import android.animation.AnimatorSet
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.yalantis.contextmenu.lib.extensions.*

open class MenuAdapter(
        private val context: Context,
        private val menuWrapper: LinearLayout,
        private val textWrapper: LinearLayout,
        private val menuObjects: List<MenuObject>,
        private val actionBarSize: Int,
        private val gravity: MenuGravity
) {

    private var onItemClickListener: (view: View) -> Unit = {}
    private var onItemLongClickListener: (view: View) -> Unit = {}
    private var onItemClickListenerCalled: (view: View) -> Unit = {}
    private var onItemLongClickListenerCalled: (view: View) -> Unit = {}

    private var clickedView: View? = null

    private val hideMenuAnimatorSet: AnimatorSet by lazy { setOpenCloseAnimation(true) }
    private val showMenuAnimatorSet: AnimatorSet by lazy { setOpenCloseAnimation(false) }

    private var isMenuOpen = false
    private var isAnimationRun = false

    private var animationDurationMillis = ANIMATION_DURATION_MILLIS

    private val itemClickListener = View.OnClickListener { view ->
        onItemClickListenerCalled = onItemClickListener
        viewClicked(view)
    }

    private val itemLongClickListener = View.OnLongClickListener { view ->
        onItemLongClickListenerCalled = onItemLongClickListener
        viewClicked(view)
        true
    }

    init {
        setViews()
    }

    open fun setOnItemClickListener(listener: (view: View) -> Unit) {
        onItemClickListener = listener
    }

    open fun setOnItemLongClickListener(listener: (view: View) -> Unit) {
        onItemLongClickListener = listener
    }

    open fun setAnimationDuration(durationMillis: Int) {
        animationDurationMillis = durationMillis.toLong()
        showMenuAnimatorSet.duration = animationDurationMillis
        hideMenuAnimatorSet.duration = animationDurationMillis
    }

    open fun menuToggle() {
        if (!isAnimationRun) {
            resetAnimations()
            isAnimationRun = true

            if (isMenuOpen) {
                hideMenuAnimatorSet.start()
            } else {
                showMenuAnimatorSet.start()
            }

            toggleIsMenuOpen()
        }
    }

    open fun getItemCount() = menuObjects.size

    private fun getLastItemPosition() = getItemCount() - 1

    /**
     * Creating views and filling to wrappers
     */
    private fun setViews() {
        menuObjects.forEachIndexed { index, menuObject ->
            context.apply {
                textWrapper.addView(
                        getItemTextView(
                                menuObject,
                                actionBarSize,
                                itemClickListener,
                                itemLongClickListener
                        )
                )
                menuWrapper.addView(
                        getImageWrapper(
                                menuObject,
                                actionBarSize,
                                itemClickListener,
                                itemLongClickListener,
                                index != getLastItemPosition()
                        )
                )
            }
        }
    }

    /**
     * Set starting params to vertical animations
     */
    private fun resetVerticalAnimation(view: View, toTop: Boolean) {
        view.apply {
            if (!isMenuOpen) {
                rotation = ROTATION_ZERO_DEGREES
                rotationY = ROTATION_ZERO_DEGREES
                rotationX = -ROTATION_NINETY_DEGREES
            }

            pivotX = (actionBarSize / 2).toFloat()
            pivotY = (if (!toTop) 0 else actionBarSize).toFloat()
        }
    }

    /**
     * Set starting params to side animations
     */
    private fun resetSideAnimation(view: View) {
        view.apply {
            if (!isMenuOpen) {
                rotation = ROTATION_ZERO_DEGREES
                rotationY = this@MenuAdapter.getRotationY()
                rotationX = ROTATION_ZERO_DEGREES
            }

            pivotX = this@MenuAdapter.getPivotX()
            pivotY = (actionBarSize / 2).toFloat()
        }
    }

    private fun getRotationY() =
            when (gravity) {
                MenuGravity.END -> if (context.isLayoutDirectionRtl()) {
                    ROTATION_NINETY_DEGREES
                } else {
                    -ROTATION_NINETY_DEGREES
                }
                MenuGravity.START -> if (context.isLayoutDirectionRtl()) {
                    -ROTATION_NINETY_DEGREES
                } else {
                    ROTATION_NINETY_DEGREES
                }
            }

    private fun getPivotX() =
            when (gravity) {
                MenuGravity.END -> if (context.isLayoutDirectionRtl()) {
                    ROTATION_ZERO_DEGREES
                } else {
                    actionBarSize.toFloat()
                }
                MenuGravity.START -> if (context.isLayoutDirectionRtl()) {
                    actionBarSize.toFloat()
                } else {
                    ROTATION_ZERO_DEGREES
                }
            }

    /**
     * Set starting params to text animations
     */
    private fun resetTextAnimation(view: View) {
        view.apply {
            alpha = if (!isMenuOpen) ALPHA_INVISIBLE else ALPHA_VISIBLE
            translationX = if (!isMenuOpen) actionBarSize.toFloat() else TRANSLATION_ZERO_VALUE
        }
    }

    /**
     * Set starting params to all animations
     */
    private fun resetAnimations() {
        for (i in 0 until getItemCount()) {
            resetTextAnimation(textWrapper.getChildAt(i))

            if (i == 0) {
                resetSideAnimation(menuWrapper.getChildAt(i))
            } else {
                resetVerticalAnimation(menuWrapper.getChildAt(i), false)
            }
        }
    }

    /**
     * Creates open/close AnimatorSet
     */
    private fun setOpenCloseAnimation(isCloseAnimation: Boolean): AnimatorSet {
        val textAnimations = mutableListOf<Animator>()
        val imageAnimations = mutableListOf<Animator>()

        if (isCloseAnimation) {
            for (i in getLastItemPosition() downTo 0) {
                fillOpenClosingAnimations(true, textAnimations, imageAnimations, i)
            }
        } else {
            for (i in 0 until getItemCount()) {
                fillOpenClosingAnimations(false, textAnimations, imageAnimations, i)
            }
        }

        return AnimatorSet().apply {
            duration = animationDurationMillis
            startDelay = 0

            playTogether(
                    AnimatorSet().apply { playSequentially(textAnimations) },
                    AnimatorSet().apply { playSequentially(imageAnimations) }
            )
            setInterpolator(HesitateInterpolator())
            onAnimationEnd { toggleIsAnimationRun() }
        }
    }

    /**
     * Filling arrays of animations to build Set of Closing / Opening animations
     */
    private fun fillOpenClosingAnimations(
            isCloseAnimation: Boolean,
            textAnimations: MutableList<Animator>,
            imageAnimations: MutableList<Animator>,
            wrapperPosition: Int
    ) {
        textWrapper.getChildAt(wrapperPosition).apply {
            textAnimations.add(AnimatorSet().apply {
                val textAppearance = if (isCloseAnimation) {
                    alphaDisappear()
                } else {
                    alphaAppear()
                }

                val textTranslation = if (isCloseAnimation) {
                    when (gravity) {
                        MenuGravity.END -> translationEnd(getTextEndTranslation())
                        MenuGravity.START -> translationStart(getTextEndTranslation())
                    }
                } else {
                    when (gravity) {
                        MenuGravity.END -> translationStart(getTextEndTranslation())
                        MenuGravity.START -> translationEnd(getTextEndTranslation())
                    }
                }

                playTogether(textAppearance, textTranslation)
            })
        }

        menuWrapper.getChildAt(wrapperPosition).apply {
            imageAnimations.add(
                    if (isCloseAnimation) {
                        if (wrapperPosition == 0) {
                            rotationCloseHorizontal(gravity)
                        } else {
                            rotationCloseVertical()
                        }
                    } else {
                        if (wrapperPosition == 0) {
                            rotationOpenHorizontal(gravity)
                        } else {
                            rotationOpenVertical()
                        }
                    }
            )
        }
    }

    private fun viewClicked(view: View) {
        if (isMenuOpen && !isAnimationRun) {
            clickedView = view

            val childIndex = (view.parent as ViewGroup).indexOfChild(view)
            if (childIndex == -1) {
                return
            }

            toggleIsAnimationRun()
            buildChosenAnimation(childIndex)
            toggleIsMenuOpen()
        }
    }

    private fun buildChosenAnimation(childIndex: Int) {
        val fadeOutTextTopAnimatorList = mutableListOf<Animator>()
        val closeToBottomImageAnimatorList = mutableListOf<Animator>()

        for (i in 0 until childIndex) {
            val menuWrapperChild = menuWrapper.getChildAt(i)
            resetVerticalAnimation(menuWrapperChild, true)
            closeToBottomImageAnimatorList.add(menuWrapperChild.rotationCloseVertical())
            fadeOutTextTopAnimatorList.add(
                    textWrapper.getChildAt(i).fadeOutSet(getTextEndTranslation(), gravity)
            )
        }

        val fadeOutTextBottomAnimatorList = mutableListOf<Animator>()
        val closeToTopAnimatorObjects = mutableListOf<Animator>()

        for (i in getLastItemPosition() downTo childIndex + 1) {
            val menuWrapperChild = menuWrapper.getChildAt(i)
            resetVerticalAnimation(menuWrapperChild, false)
            closeToTopAnimatorObjects.add(menuWrapperChild.rotationCloseVertical())
            fadeOutTextBottomAnimatorList.add(
                    textWrapper.getChildAt(i).fadeOutSet(getTextEndTranslation(), gravity)
            )
        }

        resetSideAnimation(menuWrapper.getChildAt(childIndex))

        val closeToBottom = AnimatorSet()
        closeToBottom.playSequentially(closeToBottomImageAnimatorList)
        val fadeOutTop = AnimatorSet()
        fadeOutTop.playSequentially(fadeOutTextTopAnimatorList)

        val closeToTop = AnimatorSet()
        closeToTop.playSequentially(closeToTopAnimatorObjects)
        val fadeOutBottom = AnimatorSet()
        fadeOutBottom.playSequentially(fadeOutTextBottomAnimatorList)

        val closeToEnd = menuWrapper.getChildAt(childIndex).rotationCloseHorizontal(gravity)
        closeToEnd.onAnimationEnd {
            toggleIsAnimationRun()

            clickedView?.let { notNullView ->
                onItemClickListenerCalled(notNullView)
                onItemLongClickListenerCalled(notNullView)
            }
        }
        val fadeOutChosenText =
                textWrapper.getChildAt(childIndex).fadeOutSet(getTextEndTranslation(), gravity)

        val imageFullAnimatorSet = AnimatorSet()
        imageFullAnimatorSet.play(closeToBottom).with(closeToTop)
        val textFullAnimatorSet = AnimatorSet()
        textFullAnimatorSet.play(fadeOutTop).with(fadeOutBottom)

        if (closeToBottomImageAnimatorList.size >= closeToTopAnimatorObjects.size) {
            imageFullAnimatorSet.play(closeToBottom).before(closeToEnd)
            textFullAnimatorSet.play(fadeOutTop).before(fadeOutChosenText)
        } else {
            imageFullAnimatorSet.play(closeToTop).before(closeToEnd)
            textFullAnimatorSet.play(fadeOutBottom).before(fadeOutChosenText)
        }

        AnimatorSet().apply {
            duration = animationDurationMillis
            interpolator = HesitateInterpolator()
            playTogether(imageFullAnimatorSet, textFullAnimatorSet)
            start()
        }
    }

    private fun getTextEndTranslation() =
            context.getDimension(R.dimen.text_translation).toFloat()

    private fun toggleIsAnimationRun() {
        isAnimationRun = !isAnimationRun
    }

    private fun toggleIsMenuOpen() {
        isMenuOpen = !isMenuOpen
    }

    companion object {

        const val ANIMATION_DURATION_MILLIS = 100L
    }
}
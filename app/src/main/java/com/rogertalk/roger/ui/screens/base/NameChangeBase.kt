package com.rogertalk.roger.ui.screens.base

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import com.rogertalk.kotlinjubatus.*
import com.rogertalk.roger.R
import com.rogertalk.roger.helper.PhotoSettingUIHelper
import com.rogertalk.roger.utils.android.KeyboardUtils
import com.rogertalk.roger.utils.extensions.materialize
import com.rogertalk.roger.utils.extensions.onEnterKey
import kotlinx.android.synthetic.main.name_query_screen.*
import org.jetbrains.anko.enabled
import kotlin.LazyThreadSafetyMode.NONE

/**
 * Shared Base for Name Change screens (currently used for User's name and Group creation
 */
open class NameChangeBase() : EventAppCompatActivity(logOutIfUnauthorized = true),
        TextWatcher,
        TextView.OnEditorActionListener {

    // Photo related
    val photoUiHelper: PhotoSettingUIHelper by lazy(NONE) { PhotoSettingUIHelper(this) }

    var setPhotoOnce = false

    //
    // OVERRIDE METHODS
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.name_query_screen)
        baseSetupUI()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            setPhotoOnce = true
            photoUiHelper.handleOnActivityResult(requestCode, resultCode, data)
        }
    }

    override fun afterTextChanged(s: Editable?) {
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        s?.let {
            confirmNameButton.enabled = s.isNotBlank()
        }
    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
        if (actionId == EditorInfo.IME_ACTION_GO) {
            KeyboardUtils.hideKeyboard(this)
            confirmNameButton.performClick()
            return true
        }
        return false
    }

    //
    // PUBLIC METHODS
    //

    open fun confirmNamePressed() {
    }

    fun setLoading(isLoading: Boolean) {
        confirmNameButton.setLoadingState(isLoading)
        if (isLoading) {
            progressWheel.makeVisible()
        } else {
            progressWheel.beGone()
        }
    }

    //
    // PRIVATE METHODS
    //

    private fun baseSetupUI() {
        // set material animation for previous versions of Android
        confirmNameButton.materialize(Color.BLACK)

        // Listen for "enter" and action keys
        nameInput.setOnEditorActionListener(this)
        nameInput.onEnterKey { confirmNamePressed() }

        nameInput.addTextChangedListener(this)

        // Scroll view automatically if screen dimensions change
        /* parentScroll.onLayoutChange { view, a, b, c, d, e, f, g, h ->
             parentScroll.smoothScrollBy(0, 100)
         }*/

        // Click listeners
        userPhotoOverlay.setOnClickListener { photoUiHelper.choosePhotoSourcePressed() }
        confirmNameButton.setOnClickListener { confirmNamePressed() }

        // Show description label in an animated way to draw attention
        descriptionLabel.waitFor(LONG_ANIM_DURATION) {
            descriptionLabel.fadeIn(duration = MEDIUM_ANIM_DURATION) {}
        }
    }

}
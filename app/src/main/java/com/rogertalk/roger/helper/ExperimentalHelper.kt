package com.rogertalk.roger.helper

import android.content.DialogInterface
import android.support.design.widget.BottomSheetBehavior
import android.support.design.widget.BottomSheetDialog
import android.view.View
import com.rogertalk.roger.R
import com.rogertalk.roger.repo.PrefRepo
import com.rogertalk.roger.ui.screens.TalkActivity

class ExperimentalHelper(talkActivity: TalkActivity) : DialogInterface.OnShowListener,
        DialogInterface.OnDismissListener {

    private val experimentalSheet: View
    private val bottomSheetDialog: BottomSheetDialog
    private val bottomSheetBehavior: BottomSheetBehavior<View>

    //
    // OVERRIDE METHODS
    //

    init {
        experimentalSheet = talkActivity.layoutInflater.inflate(R.layout.experimental_sheet, null)
        bottomSheetDialog = BottomSheetDialog(talkActivity)
        bottomSheetDialog.setContentView(experimentalSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(experimentalSheet.parent as View)
        bottomSheetDialog.setOnShowListener(this)
        bottomSheetDialog.setOnDismissListener(this)
    }


    override fun onShow(dialogInterface: DialogInterface?) {
    }

    override fun onDismiss(p0: DialogInterface?) {
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    //
    // PUBLIC METHODS
    //

    fun displayExperimentalUI() {
        if (PrefRepo.godMode) {
            bottomSheetDialog.show()
        }
    }

}
package com.rogertalk.roger.ui.chips;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.rogertalk.roger.R;
import com.rogertalk.roger.models.data.AvatarSize;
import com.rogertalk.roger.models.data.DeviceContactInfo;
import com.rogertalk.roger.ui.chips.util.Common;
import com.rogertalk.roger.ui.chips.views.ChipLineChangeListener;
import com.rogertalk.roger.ui.chips.views.ChipsEditText;
import com.rogertalk.roger.ui.chips.views.ChipsVerticalLinearLayout;
import com.rogertalk.roger.ui.screens.ContactsActivity;
import com.rogertalk.roger.utils.image.RoundImageUtils;

import java.util.ArrayList;
import java.util.List;

public class ChipsView extends RelativeLayout implements ChipsEditText.InputConnectionWrapperInterface {

    private static final String TAG = "ChipsView";

    private static final int CHIP_HEIGHT = 33;
    private static final int TEXT_EXTRA_TOP_MARGIN = 4;
    public static final int CHIP_BOTTOM_PADDING = 1;

    // RES --------------------------------------------------

    // ------------------------------------------------------

    private int mChipsColor;
    private int mChipsColorClicked;
    private int mChipsColorErrorClicked;
    private int mChipsBgColor;
    private int mChipsBgColorClicked;
    private int mChipsBgColorErrorClicked;
    private int mChipsTextColor;
    private int mChipsTextColorClicked;
    private int mChipsTextColorErrorClicked;
    private int mChipsPlaceholderResId;
    private int mChipsDeleteResId;

    public ContactsActivity contactsActivity = null;

    private int mLastHint = R.string.search_contact_menu_name;

    // ------------------------------------------------------

    private float mDensity;

    private ChipsListener mChipsListener;

    private ChipsEditText mEditText;
    private ChipsVerticalLinearLayout mRootChipsLayout;

    private ChipLineChangeListener chipLineListener;

    private List<Chip> mChipList = new ArrayList<>();

    private Object mCurrentEditTextSpan;

    private ChipValidator mChipsValidator;

    public ChipsView(Context context) {
        super(context);
        init();
    }

    public ChipsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttr(context, attrs);
        init();
    }

    public ChipsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttr(context, attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ChipsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttr(context, attrs);
        init();
    }

    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.ChipsView,
                0, 0);
        try {
            mChipsColor = a.getColor(R.styleable.ChipsView_cv_color,
                    ContextCompat.getColor(context, R.color.s_accent_grey_1));
            mChipsColorClicked = a.getColor(R.styleable.ChipsView_cv_color_clicked,
                    ContextCompat.getColor(context, R.color.s_blue));
            mChipsColorErrorClicked = a.getColor(R.styleable.ChipsView_cv_color_error_clicked,
                    ContextCompat.getColor(context, R.color.s_blue));

            mChipsBgColor = a.getColor(R.styleable.ChipsView_cv_bg_color,
                    ContextCompat.getColor(context, R.color.s_accent_grey_2));
            mChipsBgColorClicked = a.getColor(R.styleable.ChipsView_cv_bg_color_clicked,
                    ContextCompat.getColor(context, R.color.s_blue));

            mChipsBgColorErrorClicked = a.getColor(R.styleable.ChipsView_cv_bg_color_clicked,
                    ContextCompat.getColor(context, R.color.s_blue));

            mChipsTextColor = a.getColor(R.styleable.ChipsView_cv_text_color,
                    Color.BLACK);
            mChipsTextColorClicked = a.getColor(R.styleable.ChipsView_cv_text_color_clicked,
                    Color.WHITE);
            mChipsTextColorErrorClicked = a.getColor(R.styleable.ChipsView_cv_text_color_clicked,
                    Color.WHITE);

            mChipsPlaceholderResId = a.getResourceId(R.styleable.ChipsView_cv_icon_placeholder,
                    R.drawable.ic_person_24dp);
            mChipsDeleteResId = a.getResourceId(R.styleable.ChipsView_cv_icon_delete,
                    R.drawable.ic_close_24dp);

        } finally {
            a.recycle();
        }
    }

    private void init() {
        mDensity = getResources().getDisplayMetrics().density;

        // Dummy item to prevent AutoCompleteTextView from receiving focus
        LinearLayout linearLayout = new LinearLayout(getContext());
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(0, 0);
        linearLayout.setLayoutParams(params);
        linearLayout.setFocusable(true);
        linearLayout.setFocusableInTouchMode(true);

        addView(linearLayout);

        mEditText = new ChipsEditText(getContext(), this);
        mEditText.setBackgroundColor(Color.argb(0, 0, 0, 0));
        setHint(R.string.search_contact_menu_name);
        mEditText.setHintTextColor(ContextCompat.getColor(getContext(), R.color.s_accent_grey_1));
        mEditText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        mEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        mEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);

        mEditText.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if ((keyEvent.getAction() == KeyEvent.ACTION_DOWN)
                        && (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    if (contactsActivity != null) {
                        contactsActivity.chipsEnterPressed();
                    }
                    return true;
                }
                return false;
            }
        });

        addView(mEditText);

        mRootChipsLayout = new ChipsVerticalLinearLayout(getContext());
        mRootChipsLayout.setOrientation(LinearLayout.VERTICAL);
        mRootChipsLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(mRootChipsLayout);

        initListener();
    }

    private void initListener() {
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditText.requestFocus();
            }
        });

        EditTextListener mEditTextListener = new EditTextListener();
        mEditText.addTextChangedListener(mEditTextListener);
        mEditText.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
            }
        });
    }

    public int getLineCount() {
        return mRootChipsLayout.linesCount();
    }

    public void setHint(int resourceId) {
        mLastHint = resourceId;
        mEditText.setHint(resourceId);
    }

    public void addChip(DeviceContactInfo contact) {
        addChip(contact, false);
        mEditText.setText("");
        mEditText.setHint("");
        addLeadingMarginSpan();
    }

    public void addChip(DeviceContactInfo contact, boolean isIndelible) {
        Chip chip = new Chip(contact, isIndelible);
        mChipList.add(chip);
        if (mChipsListener != null) {
            mChipsListener.onChipAdded(chip);
        }

        onChipsChanged(true);
    }

    public boolean removeChipBy(DeviceContactInfo contact) {
        for (int i = 0; i < mChipList.size(); i++) {
            if (mChipList.get(i).mContact != null && mChipList.get(i).mContact.equals(contact)) {
                mChipList.remove(i);
                onChipsChanged(true);
                return true;
            }
        }
        return false;
    }

    /**
     * rebuild all chips and place them right
     */
    private void onChipsChanged(final boolean moveCursor) {
        ChipsVerticalLinearLayout.TextLineParams textLineParams = mRootChipsLayout.onChipsChanged(mChipList);

        // if null then run another layout pass
        if (textLineParams == null) {
            post(new Runnable() {
                @Override
                public void run() {
                    onChipsChanged(moveCursor);
                }
            });
            return;
        }

        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int) (textLineParams.getRow() * CHIP_HEIGHT * mDensity + TEXT_EXTRA_TOP_MARGIN * mDensity);
        mEditText.setLayoutParams(params);
        addLeadingMarginSpan(textLineParams.getLineMargin());
        if (moveCursor) {
            mEditText.setSelection(mEditText.length());
        }

        if (chipLineListener != null) {
            chipLineListener.linesChanged(getLineCount());
        }

        if (mChipList.isEmpty()) {
            mEditText.setHint(mLastHint);
        }
    }

    private void addLeadingMarginSpan(int margin) {
        Spannable spannable = mEditText.getText();
        if (mCurrentEditTextSpan != null) {
            spannable.removeSpan(mCurrentEditTextSpan);
        }
        mCurrentEditTextSpan = new android.text.style.LeadingMarginSpan.LeadingMarginSpan2.Standard(margin, 0);
        spannable.setSpan(mCurrentEditTextSpan, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mEditText.setText(spannable);
    }

    private void addLeadingMarginSpan() {
        Spannable spannable = mEditText.getText();
        if (mCurrentEditTextSpan != null) {
            spannable.removeSpan(mCurrentEditTextSpan);
        }
        spannable.setSpan(mCurrentEditTextSpan, 0, 0, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

        mEditText.setText(spannable);
    }

    private void onEnterPressed(String text) {
        if (text != null && text.length() > 0) {

            if (Common.INSTANCE.isValidUser(text)) {
                onEmailRecognized(text);
            }
            mEditText.setSelection(0);
        }
    }

    private void onEmailRecognized(String userHandle) {
        // TODO: add found user to the list of chips
    }

    private void onContactRecognized(DeviceContactInfo contact) {
        Chip chip = new Chip(contact);
        mChipList.add(chip);
        if (mChipsListener != null) {
            mChipsListener.onChipAdded(chip);
        }
        post(new Runnable() {
            @Override
            public void run() {
                onChipsChanged(true);
            }
        });
    }

    private void selectOrDeleteLastChip() {
        if (mChipList.size() > 0) {
            onChipInteraction(mChipList.size() - 1);
        }
    }

    private void onChipInteraction(int position) {
        try {
            Chip chip = mChipList.get(position);
            if (chip != null) {
                onChipInteraction(chip);
            }
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "Out of bounds", e);
        }
    }

    private void onChipInteraction(Chip chip) {
        unSelectChipsExcept(chip);
        if (chip.isSelected()) {
            mChipList.remove(chip);
            mEditText.setText("");
            if (mChipsListener != null) {
                mChipsListener.onChipDeleted(chip);
            }
            onChipsChanged(true);
        } else {
            chip.setSelected(true);
            onChipsChanged(false);
        }
    }

    private void unSelectChipsExcept(Chip rootChip) {
        for (Chip chip : mChipList) {
            if (chip != rootChip) {
                chip.setSelected(false);
            }
        }
        onChipsChanged(false);
    }

    @NonNull
    @Override
    public InputConnection getInputConnection(@NonNull InputConnection target) {
        return new KeyInterceptingInputConnection(target);
    }

    public void setChipsListener(ChipsListener chipsListener) {
        this.mChipsListener = chipsListener;
    }

    /**
     * sets the ChipsValidator.
     */
    public void setChipsValidator(ChipValidator mChipsValidator) {
        this.mChipsValidator = mChipsValidator;
    }

    private class EditTextListener implements TextWatcher {

        private boolean mIsPasteTextChange = false;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (count > 1) {
                mIsPasteTextChange = true;
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mIsPasteTextChange) {
                mIsPasteTextChange = false;
                // todo handle copy/paste text here

            } else {
                // no paste text change
                if (s.toString().contains("\n")) {
                    String text = s.toString();
                    text = text.replace("\n", "");
                    while (text.contains("  ")) {
                        text = text.replace("  ", " ");
                    }
                    s.clear();
                    if (text.length() > 1) {
                        onEnterPressed(text);
                    } else {
                        s.append(text);
                    }
                }
            }
            if (mChipsListener != null) {
                mChipsListener.onTextChanged(s);
            }
        }
    }

    private class KeyInterceptingInputConnection extends InputConnectionWrapper {

        public KeyInterceptingInputConnection(InputConnection target) {
            super(target, true);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            return super.commitText(text, newCursorPosition);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (mEditText.length() == 0) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                        selectOrDeleteLastChip();
                        return true;
                    }
                }
            }
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                mEditText.append("\n");
                return true;
            }

            return super.sendKeyEvent(event);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            // magic: in latest Android, deleteSurroundingText(1, 0) will be called for backspace
            if (mEditText.length() == 0 && beforeLength == 1 && afterLength == 0) {
                // backspace
                return sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
                        && sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL));
            }

            return super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

    public void addChipLineListener(ChipLineChangeListener listener) {
        chipLineListener = listener;
    }

    public class Chip implements OnClickListener {

        private final DeviceContactInfo mContact;
        private final boolean mIsIndelible;

        private RelativeLayout mView;
        private RelativeLayout mElemParent;
        private View mIconWrapper;
        private TextView mTextView;

        private ImageView mAvatarView;
        private ImageView mPersonIcon;
        private ImageView mCloseIcon;

        private boolean mIsSelected = false;

        public Chip(DeviceContactInfo contact) {
            this(contact, false);
        }

        public Chip(DeviceContactInfo contact, boolean isIndelible) {
            this.mContact = contact;
            this.mIsIndelible = isIndelible;
        }

        public View getView() {
            if (mView == null) {
                mView = (RelativeLayout) inflate(getContext(), R.layout.chips_view, null);
                mElemParent = (RelativeLayout) mView.findViewById(R.id.elemParent);
                mView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (int) (38 * mDensity)));
                mAvatarView = (ImageView) mView.findViewById(R.id.ri_ch_avatar);
                mIconWrapper = mView.findViewById(R.id.rl_ch_avatar);
                mTextView = (TextView) mView.findViewById(R.id.tv_ch_name);
                mPersonIcon = (ImageView) mView.findViewById(R.id.iv_ch_person);
                mCloseIcon = (ImageView) mView.findViewById(R.id.iv_ch_close);

                // set initial res & attrs
                int mChipsBgRes = R.drawable.chip_background;
                mElemParent.setBackgroundResource(mChipsBgRes);
                mElemParent.post(new Runnable() {
                    @Override
                    public void run() {
                        mElemParent.getBackground().setColorFilter(mChipsBgColor, PorterDuff.Mode.SRC_ATOP);
                    }
                });
                mIconWrapper.setBackgroundResource(R.drawable.circle);
                mTextView.setTextColor(mChipsTextColor);

                // set icon resources
                mPersonIcon.setBackgroundResource(mChipsPlaceholderResId);
                mCloseIcon.setBackgroundResource(mChipsDeleteResId);


                mView.setOnClickListener(this);
                mIconWrapper.setOnClickListener(this);
            }
            updateViews();
            return mView;
        }

        private void updateViews() {
            mTextView.setText(mContact.getDisplayName());
            if (mContact.getPhotoURI() != null) {
                RoundImageUtils.INSTANCE.createRoundImage(getContext(),
                        mAvatarView, mContact.getPhotoURI(), AvatarSize.TOP_CORNER);
            }
            if (isSelected()) {
                if (mChipsValidator != null && !mChipsValidator.isValid(mContact)) {
                    // not valid & show error
                    mElemParent.getBackground().setColorFilter(mChipsBgColorErrorClicked, PorterDuff.Mode.SRC_ATOP);
                    mTextView.setTextColor(mChipsTextColorErrorClicked);
                    mIconWrapper.getBackground().setColorFilter(mChipsColorErrorClicked, PorterDuff.Mode.SRC_ATOP);
                } else {
                    mElemParent.getBackground().setColorFilter(mChipsBgColorClicked, PorterDuff.Mode.SRC_ATOP);
                    mTextView.setTextColor(mChipsTextColorClicked);
                    mIconWrapper.getBackground().setColorFilter(mChipsColorClicked, PorterDuff.Mode.SRC_ATOP);
                }
                mPersonIcon.animate().alpha(0.0f).setDuration(200).start();
                mAvatarView.animate().alpha(0.0f).setDuration(200).start();
                mCloseIcon.animate().alpha(1f).setDuration(200).setStartDelay(100).start();

            } else {
                mElemParent.getBackground().setColorFilter(mChipsBgColor, PorterDuff.Mode.SRC_ATOP);
                mTextView.setTextColor(mChipsTextColor);
                mIconWrapper.getBackground().setColorFilter(mChipsColor, PorterDuff.Mode.SRC_ATOP);

                mPersonIcon.animate().alpha(0.3f).setDuration(200).setStartDelay(100).start();
                mAvatarView.animate().alpha(1f).setDuration(200).setStartDelay(100).start();
                mCloseIcon.animate().alpha(0.0f).setDuration(200).start();
            }
        }

        @Override
        public void onClick(View v) {
            mEditText.clearFocus();
            if (v.getId() == mView.getId()) {
                onChipInteraction(this);
            } else {
                onChipInteraction(this);
            }
        }

        public boolean isSelected() {
            return mIsSelected;
        }

        public void setSelected(boolean isSelected) {
            if (mIsIndelible) {
                return;
            }
            this.mIsSelected = isSelected;
        }

        public DeviceContactInfo getContact() {
            return mContact;
        }

        @Override
        public boolean equals(Object o) {
            if (mContact != null && o instanceof DeviceContactInfo) {
                return mContact.equals(o);
            }
            return super.equals(o);
        }
    }

    public interface ChipsListener {
        void onChipAdded(Chip chip);

        void onChipDeleted(Chip chip);

        void onTextChanged(CharSequence text);
    }

    public static abstract class ChipValidator {
        public abstract boolean isValid(DeviceContactInfo contact);
    }
}

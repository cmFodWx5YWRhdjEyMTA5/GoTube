package com.playtube.player.gui.player.controller;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.dueeeke.videoplayer.player.IjkVideoView;
import com.dueeeke.videoplayer.util.L;
import com.dueeeke.videoplayer.util.WindowUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import com.playtube.player.business.Utils;
import com.tube.playtube.R;

/**
 * Created by liyanju on 2018/6/8.
 */

public class DefinitionController extends StandardVideoController {
    protected TextView multiRate;
    //    private PopupMenu mPopupMenu;
    private PopupWindow mPopupWindow;
    private List<String> mRateStr;
    private List<TextView> mRateItems;
    private LinearLayout mPopLayout;


    public DefinitionController(@NonNull Context context) {
        this(context, null);
    }

    public DefinitionController(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DefinitionController(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void initView() {
        super.initView();
        multiRate = controllerView.findViewById(R.id.tv_multi_rate);
        multiRate.setOnClickListener(this);
        mPopupWindow = new PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mPopLayout = (LinearLayout) LayoutInflater.from(getContext()).inflate(R.layout.layout_rate_pop, this, false);
        mPopupWindow.setContentView(mPopLayout);
        mPopupWindow.setBackgroundDrawable(new ColorDrawable(0xffffffff));
        mPopupWindow.setOutsideTouchable(true);
        mPopupWindow.setClippingEnabled(false);
    }

    @Override
    public void setPlayerState(int playerState) {
        super.setPlayerState(playerState);
        switch (playerState) {
            case IjkVideoView.PLAYER_NORMAL:
                multiRate.setVisibility(GONE);
                Utils.transparence(WindowUtil.scanForActivity(getContext()));
                break;
            case IjkVideoView.PLAYER_FULL_SCREEN:
                multiRate.setVisibility(VISIBLE);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        int i = v.getId();
        if (i == R.id.tv_multi_rate) {
            showRateMenu();
        }
    }

    @Override
    public void hide() {
        super.hide();
        if (mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
    }

    private void showRateMenu() {
        mPopLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        mPopupWindow.showAsDropDown(multiRate, -((mPopLayout.getMeasuredWidth() - multiRate.getMeasuredWidth()) / 2),
                -(mPopLayout.getMeasuredHeight() + multiRate.getMeasuredHeight() + WindowUtil.dp2px(getContext(), 10)));
    }

    private int currentIndex;

    @Override
    protected int setProgress() {
        if (multiRate != null && TextUtils.isEmpty(multiRate.getText())) {
            L.d("multiRate");
            LinkedHashMap<String, String> multiRateData = ((DefinitionMediaPlayerControl) mediaPlayer).getDefinitionData();
            if (multiRateData == null) return super.setProgress();
            mRateStr = new ArrayList<>();
            mRateItems = new ArrayList<>();
            int index = 0;
            ListIterator<Map.Entry<String, String>> iterator = new ArrayList<>(multiRateData.entrySet()).listIterator(multiRateData.size());
            while (iterator.hasPrevious()) {//反向遍历
                Map.Entry<String, String> entry = iterator.previous();
                mRateStr.add(entry.getKey());
                TextView rateItem = (TextView) LayoutInflater.from(getContext()).inflate(R.layout.layout_rate_item, null);
                rateItem.setText(entry.getKey());
                rateItem.setTag(index);
                rateItem.setOnClickListener(rateOnClickListener);
                mPopLayout.addView(rateItem);
                mRateItems.add(rateItem);
                index++;
            }
            mRateItems.get(index - 1).setTextColor(ContextCompat.getColor(getContext(), R.color.theme_color));
            multiRate.setText(mRateStr.get(index - 1));
            currentIndex = index - 1;
        }
        return super.setProgress();
    }

    private OnClickListener rateOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            int index = (int) v.getTag();
            if (currentIndex == index) return;
            mRateItems.get(currentIndex).setTextColor(Color.BLACK);
            mRateItems.get(index).setTextColor(ContextCompat.getColor(getContext(), R.color.theme_color));
            multiRate.setText(mRateStr.get(index));
            ((DefinitionMediaPlayerControl) mediaPlayer).switchDefinition(mRateStr.get(index));
            mPopupWindow.dismiss();
            hide();
            currentIndex = index;
        }
    };
}

package com.android.systemui.statusbar.toggles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;

import com.android.systemui.R;

public class RebootToggle extends BaseToggle {

    @Override
    protected void init(Context c, int style) {
        super.init(c, style);
        setIcon(R.drawable.ic_qs_reboot);
        setLabel(R.string.quick_settings_reboot);

    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(Intent.ACTION_REBOOTMENU);
        mContext.sendBroadcast(intent);

        collapseStatusBar();
        dismissKeyguard();
    }

    @Override
    public boolean onLongClick(View v) {
        collapseStatusBar();
        dismissKeyguard();
        Intent intent=new Intent(Intent.ACTION_POWERMENU_REBOOT);
        mContext.sendBroadcast(intent);

        return super.onLongClick(v);
    }

}

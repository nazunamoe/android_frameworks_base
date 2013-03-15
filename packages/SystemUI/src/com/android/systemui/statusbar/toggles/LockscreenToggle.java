
package com.android.systemui.statusbar.toggles;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.View;

import com.android.systemui.R;

public class LockscreenToggle extends StatefulToggle {

    private KeyguardLock mLock = null;

    private boolean mLockState = false;

    @Override
    protected void init(Context c, int style) {
        super.init(c, style);

        KeyguardManager keyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mLock = keyguardManager.newKeyguardLock("ToggleLockscreen");

    }

    @Override
    public boolean onLongClick(View v) {
        startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS));
        return super.onLongClick(v);
    }

    @Override
    protected void updateView() {
        setEnabledState(mLockState);
        setLabel(mLockState ? R.string.quick_settings_ls_on_label
                : R.string.quick_settings_ls_off_label);
        setIcon(mLockState ? R.drawable.ic_qs_ls_on : R.drawable.ic_qs_ls_off);
        super.updateView();
    }

    @Override
    protected void doEnable() {
        mLock.reenableKeyguard();
    }

    @Override
    protected void doDisable() {
        mLock.disableKeyguard();
    }
}

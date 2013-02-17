
package com.android.systemui.statusbar.toggles;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

import com.android.systemui.R;

public class QuietHoursToggle extends StatefulToggle {

    SettingsObserver mObserver = null;

    @Override
    protected void init(Context c, int style) {
        super.init(c, style);

        mObserver = new SettingsObserver(mHandler);
        mObserver.observe();
    }

    @Override
    protected void cleanup() {
        if (mObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mObserver);
            mObserver = null;
        }
        super.cleanup();
    }

    @Override
    protected void doEnable() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, QuiethourState ? 0 : 1);
    }

    @Override
    protected void doDisable() {
        Settings.System.putInt(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, QuiethourState ? 1 : 0);
    }

    @Override
    protected void updateView() {
        final boolean enabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.QUIET_HOURS_ENABLED, 0) == 1;
        setEnabledState(enabled);
        setIcon(enabled ? R.drawable.ic_qs_quiet_hours_on : R.drawable.ic_qs_quiet_hours_off);
        setLabel(enabled ? R.string.quick_settings_quiet_hours_on_label
                : R.string.quick_settings_quiet_hours_off_label);
        super.updateView();
    }

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.Global
                    .getUriFor(Settings.System.QUIET_HOURS_ENABLED), 0,
                    this);
        }

        @Override
        public void onChange(boolean selfChange) {
            scheduleViewUpdate();
        }
    }

}


package com.android.systemui.statusbar.toggles;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;

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
        try {
            Process proc = Runtime.getRuntime()
                .exec(new String[]{ "su", "-c", "reboot" });
            proc.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        collapseStatusBar();
        dismissKeyguard();
    }

    @Override
    public boolean onLongClick(View v) {
        try {
            Process proc = Runtime.getRuntime()
                .exec(new String[]{ "su", "-c", "recovery" });
            proc.waitFor();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        collapseStatusBar();
        dismissKeyguard();
        return super.onLongClick(v);
    }

}

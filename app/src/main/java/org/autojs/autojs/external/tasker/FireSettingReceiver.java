package org.autojs.autojs.external.tasker;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;

import org.autojs.autojs.external.ScriptIntents;
import org.autojs.autojs.external.open.RunIntentActivity;

/**
 * Created by Stardust on 2017/3/27.
 */

public class FireSettingReceiver extends PluginSettingReceiver {

    @Override
    protected boolean isBundleValid(@NonNull Bundle bundle) {
        return ScriptIntents.isTaskerBundleValid(bundle);
    }

    @Override
    protected void firePluginSetting(@NonNull Context context, @NonNull Bundle bundle) {
        context.startActivity(new Intent(context, RunIntentActivity.class)
                .putExtras(bundle)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

}

package com.plakoto.android;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.plakoto.game.PlakotoApp;

public class AndroidLauncher extends AndroidApplication {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        AndroidApplicationConfiguration c = new AndroidApplicationConfiguration();
        c.useImmersiveMode = true;
        initialize(new PlakotoApp(), c);
    }
}

package com.magicleap.httpserverangel;

import android.app.Activity;
import android.content.ContentResolver;

public interface HostCallback {
    public void logHttpEvent(String httpEvent);
    public ContentResolver getHostContentResolver();
}

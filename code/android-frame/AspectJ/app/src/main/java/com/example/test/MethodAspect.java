package com.example.test;


import android.util.Log;

/**
 * Copyright (C) 2013, Xiaomi Inc. All rights reserved.
 */

public class MethodAspect {

    private static final String TAG = "ConstructorAspect";

    public void callMethod() {

    }

    public void beforeMethodCall() {
        Log.e(TAG,"before ->");
    }
}

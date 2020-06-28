package com.example.test;


import android.util.Log;

import androidx.annotation.NonNull;

public class Animal {
    private static final String TAG = "Animal";
    public void fly() {
        Log.e(TAG,this.toString()+"#fly");
    }
}

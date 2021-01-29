Android 加载资源图片

总体上是：


```kotlin
resources.getDrawable(R.mipmap.bg_splash)
```

```java
Resource.java
  @Deprecated
    public Drawable getDrawable(@DrawableRes int id) throws NotFoundException {
        final Drawable d = getDrawable(id, null);
        if (d != null && d.canApplyTheme()) {
            Log.w(TAG, "Drawable " + getResourceName(id) + " has unresolved theme "
                    + "attributes! Consider using Resources.getDrawable(int, Theme) or "
                    + "Context.getDrawable(int).", new RuntimeException());
        }
        return d;
    }

 public Drawable getDrawable(@DrawableRes int id, @Nullable Theme theme)
            throws NotFoundException {
        return getDrawableForDensity(id, 0, theme);
    }

 @Nullable
    public Drawable getDrawableForDensity(@DrawableRes int id, int density, @Nullable Theme theme) {
        final TypedValue value = obtainTempTypedValue();
        try {
            final ResourcesImpl impl = mResourcesImpl;
            //获取图片加载density 密度
            impl.getValueForDensity(id, density, value, true);
            return impl.loadDrawable(this, value, id, density, theme);
        } finally {
            releaseTempTypedValue(value);
        }
    }

ResourcesImpl.java
    
    void getValueForDensity(@AnyRes int id, int density, TypedValue outValue,
            boolean resolveRefs) throws NotFoundException {
        //AssetManager 所有资源管理器
        boolean found = mAssets.getResourceValue(id, density, outValue, resolveRefs);
        if (found) {
            return;
        }
        throw new NotFoundException("Resource ID #0x" + Integer.toHexString(id));
    }

```

```java
AssetManager.java
 @UnsupportedAppUsage
    boolean getResourceValue(@AnyRes int resId, int densityDpi, @NonNull TypedValue outValue,
            boolean resolveRefs) {
        Preconditions.checkNotNull(outValue, "outValue");
        synchronized (this) {
            ensureValidLocked();
            //关键代码 native 获取resource 相关信息
            //id path
             
            final int cookie = nativeGetResourceValue(
                    mObject, resId, (short) densityDpi, outValue, resolveRefs);
            if (cookie <= 0) {
                return false;
            }

            // Convert the changing configurations flags populated by native code.
            outValue.changingConfigurations = ActivityInfo.activityInfoConfigNativeToJava(
                    outValue.changingConfigurations);

            if (outValue.type == TypedValue.TYPE_STRING) {
                outValue.string = mApkAssets[cookie - 1].getStringFromPool(outValue.data);
            }
            return true;
        }
    }
// Primitive resource native methods.
    private static native int nativeGetResourceValue(long ptr, @AnyRes int resId, short density,
            @NonNull TypedValue outValue, boolean resolveReferences);
```

```c++
//frameworks/base/core/jni/android_util_AssetManager.cpp

static jint NativeGetResourceValue(JNIEnv* env, jclass /*clazz*/, jlong ptr, jint resid,
                                   jshort density, jobject typed_value,
                                   jboolean resolve_references) {
  ScopedLock<AssetManager2> assetmanager(AssetManagerFromLong(ptr));
  Res_value value;
  ResTable_config selected_config;
  uint32_t flags;
    //获取ApkAssetsCookie
  ApkAssetsCookie cookie =
      assetmanager->GetResource(static_cast<uint32_t>(resid), false /*may_be_bag*/,
                                static_cast<uint16_t>(density), &value, &selected_config, &flags);
  if (cookie == kInvalidCookie) {
    return ApkAssetsCookieToJavaCookie(kInvalidCookie);
  }

  uint32_t ref = static_cast<uint32_t>(resid);
  if (resolve_references) {
    cookie = assetmanager->ResolveReference(cookie, &value, &selected_config, &flags, &ref);
    if (cookie == kInvalidCookie) {
      return ApkAssetsCookieToJavaCookie(kInvalidCookie);
    }
  }
  return CopyValue(env, cookie, value, ref, flags, &selected_config, typed_value);
}
```


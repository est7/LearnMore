## 1：BimapUtils

### 1.1 ：bitmap大小调整

#### 1.1.1居中裁剪

```java
public static Bitmap resizeAndCropCenter(Bitmap bitmap, int size, boolean recycle) {
int w = bitmap.getWidth();
int h = bitmap.getHeight();
if (w == size && h == size) return bitmap;

// scale the image so that the shorter side equals to the target;
// the longer side will be center-cropped.
float scale = (float) size / Math.min(w, h);

Bitmap target = Bitmap.createBitmap(size, size, getConfig(bitmap));
int width = Math.round(scale * bitmap.getWidth());
int height = Math.round(scale * bitmap.getHeight());
Canvas canvas = new Canvas(target);
canvas.translate((size - width) / 2f, (size - height) / 2f);
canvas.scale(scale, scale);
Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
canvas.drawBitmap(bitmap, 0, 0, paint);
if (recycle) bitmap.recycle();
return target;
}
```

#### 1.1.2bitmap缩放

```java
public static Bitmap resizeBitmapByScale(
Bitmap bitmap, float scale, boolean recycle) {
int width = Math.round(bitmap.getWidth() * scale);
int height = Math.round(bitmap.getHeight() * scale);
if (width == bitmap.getWidth()
&& height == bitmap.getHeight()) return bitmap;
Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
Canvas canvas = new Canvas(target);
canvas.scale(scale, scale);
Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
canvas.drawBitmap(bitmap, 0, 0, paint);
if (recycle) bitmap.recycle();
return target;
}
```

#### 1.1.3bitmap按边裁剪

```java
public static Bitmap resizeDownBySideLength(
Bitmap bitmap, int maxLength, boolean recycle) {
int srcWidth = bitmap.getWidth();
int srcHeight = bitmap.getHeight();
float scale = Math.min(
(float) maxLength / srcWidth, (float) maxLength / srcHeight);
if (scale >= 1.0f) return bitmap;
return resizeBitmapByScale(bitmap, scale, recycle);
}
```

## 2：bitmap输出

```java
public static byte[] compressToBytes(Bitmap bitmap, int quality) {
ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
bitmap.compress(CompressFormat.JPEG, quality, baos);
return baos.toByteArray();
}
```

## 3：bitmap压缩

```java
public static byte[] compressToBytes(Bitmap bitmap, int quality) {
ByteArrayOutputStream baos = new ByteArrayOutputStream(65536);
bitmap.compress(CompressFormat.JPEG, quality, baos);
return baos.toByteArray();
}
```

## 4：bitmap旋转

```java
public static Bitmap rotateBitmap(Bitmap source, int rotation, boolean recycle) {
if (rotation == 0) return source;
int w = source.getWidth();
int h = source.getHeight();
Matrix m = new Matrix();
m.postRotate(rotation);
Bitmap bitmap = Bitmap.createBitmap(source, 0, 0, w, h, m, true);
if (recycle) source.recycle();
return bitmap;
}
```

5:bitmap创建视频缩略图

```java
public static Bitmap createVideoThumbnail(String filePath) {
// MediaMetadataRetriever is available on API Level 8
// but is hidden until API Level 10
Class<?> clazz = null;
Object instance = null;
try {
clazz = Class.forName("android.media.MediaMetadataRetriever");
instance = clazz.newInstance();

Method method = clazz.getMethod("setDataSource", String.class);
method.invoke(instance, filePath);

// The method name changes between API Level 9 and 10.
if (Build.VERSION.SDK_INT <= 9) {
return (Bitmap) clazz.getMethod("captureFrame").invoke(instance);
} else {
byte[] data = (byte[]) clazz.getMethod("getEmbeddedPicture").invoke(instance);
if (data != null) {
Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
if (bitmap != null) return bitmap;
}
return (Bitmap) clazz.getMethod("getFrameAtTime").invoke(instance);
}
} catch (IllegalArgumentException ex) {
// Assume this is a corrupt video file
} catch (RuntimeException ex) {
// Assume this is a corrupt video file.
} catch (InstantiationException e) {
Log.e(TAG, "createVideoThumbnail", e);
} catch (InvocationTargetException e) {
Log.e(TAG, "createVideoThumbnail", e);
} catch (ClassNotFoundException e) {
Log.e(TAG, "createVideoThumbnail", e);
} catch (NoSuchMethodException e) {
Log.e(TAG, "createVideoThumbnail", e);
} catch (IllegalAccessException e) {
Log.e(TAG, "createVideoThumbnail", e);
} finally {
try {
if (instance != null) {
clazz.getMethod("release").invoke(instance);
}
} catch (Exception ignored) {
}
}
return null;
}
```

## 5：bitMap基础API

```java
1:getAllocationByteCount

获取Bitmap真实大小
Api19之后加入，就算bitmap内存复用的情况下
同样会返回bitmap的大小

2:getByteCount
通过复用Bitmap来解码图片，
如果被复用的Bitmap的内存比待分配内存的Bitmap大,

/**getByteCount()
表示新解码图片占用内存的大小（并非实际内存大小,实际大小是复用的那个Bitmap的大小），
getAllocationByteCount()
表示被复用Bitmap真实占用的内存大小（即mBuffer的长度）**/

3:recycle
    
/**
Android3.0(API 11之后)
引入了BitmapFactory.Options.inBitmap字段，
设置此字段之后解码方法会尝试复用一张存在的Bitmap。
这意味着Bitmap的内存被复用，避免了内存的回收及申请过程，显然性能表现更佳。
不过，使用这个字段有几点限制：

声明可被复用的Bitmap必须设置inMutable为true；

Android4.4(API 19)之前只有格式为jpg、png，
同等宽高（要求苛刻），
inSampleSize为1的Bitmap才可以复用；

Android4.4(API 19)之前被复用的Bitmap的inPreferredConfig
会覆盖待分配内存的Bitmap设置的inPreferredConfig；

Android4.4(API 19)之后被复用的Bitmap的内存必须

大于需要申请内存的Bitmap的内存；
Android4.4(API 19)之前待加载Bitmap的
Options.inSampleSize必须明确指定为1。
**/    

```

## 6:BitmapFactory

### 6.1Options

```java
1:inSampleSize //采样率 影响图片实际内存
2：inBitmap //开启图片复用
3：inDensity //图片像素密度
4：inTargetDensity//屏幕像素密度值
6：inScaled //图片缩放比例 不会影响图片实际的宽高    inTargetDensity/inDensity    
```

### 6.2：图片内存复用

```java
options.inBitmap = bitmap;
options.inMutable = true
```

### 6.3：图片内存计算

```java
（windth/insampleSize）*(nTargetDenstity/inDensity)*(height/insampleSize)*(nTargetDensity/inDensity)
*一个像素所在内存
```

### 6.4:大图片加载

```java
//inJustDecodeBounds  = true

options.inJustDecodeBounds = true;

isJustDecodeBounds设置为true的话，
那么解码器将会return一个空的bitmap，
并且会给out...之类的属性进行赋值，
允许用户可以查询bitmap信息而不需要加载图片。
这样我们就通过outWidth以及outHeight拿到原始图片的宽高了。
    
    
BitmapFactory.Options options = new BitmapFactory.Options();
options.inJustDecodeBounds = true;
BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background, options);
int imgWidth = options.outWidth; //要加载的图片的宽
int imgHeight = options.outHeight;//要加载的图片的高
Log.i("doris", "原始图片：" + imgWidth + ":" + imgHeight);
int desiredWidth = 50;
int desiredHeight = 50;
int inSampleSize = 1;
if (imgWidth > desiredWidth || imgHeight > desiredHeight) {
int halfWidth = imgWidth / 2;
int halfHeight = imgHeight / 2;

while ((halfWidth / inSampleSize) >= desiredWidth &&
(halfHeight / inSampleSize) >= desiredHeight) {
inSampleSize *= 2;
}
}
options.inJustDecodeBounds = false;
options.inSampleSize= inSampleSize;
BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background, options);
Log.i("doris", "解压后图片：" + options.inSampleSize + options.outWidth + "::" + options.outHeight);    
    
```

### 6.5：长图加载

```java
private static Bitmap decodeStreamInternal(@NonNull InputStream is,
@Nullable Rect outPadding, @Nullable Options opts) {
// ASSERT(is != null);
byte [] tempStorage = null;
if (opts != null) tempStorage = opts.inTempStorage;
if (tempStorage == null) tempStorage = new byte[DECODE_BUFFER_SIZE];
return nativeDecodeStream(is, tempStorage, outPadding, opts,
Options.nativeInBitmap(opts),	
Options.nativeColorSpace(opts));
}
```

### 6.6：图片不解码，只获取类型及数据

```kotlin
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true//设置为
    }
    BitmapFactory.decodeResource(resources, R.id.myimage, options)
    val imageHeight: Int = options.outHeight
    val imageWidth: Int = options.outWidth
    val imageType: String = options.outMimeType//获取图片类型 jpg  png ...
    
```

### 6.7：图片比例缩小展示

```kotlin
    imageView.setImageBitmap(
            decodeSampledBitmapFromResource(resources, R.id.myimage, 100, 100)
    )
    
```



```kotlin
    fun decodeSampledBitmapFromResource(
            res: Resources,
            resId: Int,
            reqWidth: Int,
            reqHeight: Int
    ): Bitmap {
        // First decode with inJustDecodeBounds=true to check dimensions
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeResource(res, resId, this)

            // Calculate inSampleSize
            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)

            // Decode bitmap with inSampleSize set
            inJustDecodeBounds = false

            BitmapFactory.decodeResource(res, resId, this)
        }
    }
    
```



```kotlin
    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        // Raw height and width of image
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {

            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
```

### 6.8:bitmap 内存缓存

```kotlin
    private LruCache<String, Bitmap> memoryCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ...
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
	   //LruCache创建
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                //返回当前bitmap 大小
                return bitmap.getByteCount() / 1024;
            }
        };
        ...
    }

   public void loadBitmap(int resId, ImageView imageView) {
        final String imageKey = String.valueOf(resId);

        final Bitmap bitmap = getBitmapFromMemCache(imageKey);
        if (bitmap != null) {
            mImageView.setImageBitmap(bitmap);
        } else {
            mImageView.setImageResource(R.drawable.image_placeholder);
            BitmapWorkerTask task = new BitmapWorkerTask(mImageView);
            task.execute(resId);
        }
    }

    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return memoryCache.get(key);
    }
    
```

```kotlin
    class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
        ...
        // Decode image in background.
        @Override
        protected Bitmap doInBackground(Integer... params) {
            final Bitmap bitmap = decodeSampledBitmapFromResource(
                    getResources(), params[0], 100, 100));
            addBitmapToMemoryCache(String.valueOf(params[0]), bitmap);
            return bitmap;
        }
        ...
    }
    
```

### 6.9:bitmap 使用磁盘缓存

```kotlin
     private const val DISK_CACHE_SIZE = 1024 * 1024 * 10 // 10MB
    private const val DISK_CACHE_SUBDIR = "thumbnails"
    ...
    private var diskLruCache: DiskLruCache? = null
    private val diskCacheLock = ReentrantLock()
    private val diskCacheLockCondition: Condition = diskCacheLock.newCondition()
    private var diskCacheStarting = true

    override fun onCreate(savedInstanceState: Bundle?) {
        ...
        // Initialize memory cache
        ...
        // Initialize disk cache on background thread
        val cacheDir = getDiskCacheDir(this, DISK_CACHE_SUBDIR)
        InitDiskCacheTask().execute(cacheDir)
        ...
    }

    internal inner class InitDiskCacheTask : AsyncTask<File, Void, Void>() {
        override fun doInBackground(vararg params: File): Void? {
            diskCacheLock.withLock {
                val cacheDir = params[0]
                diskLruCache = DiskLruCache.open(cacheDir, DISK_CACHE_SIZE)
                diskCacheStarting = false // Finished initialization
                diskCacheLockCondition.signalAll() // Wake any waiting threads
            }
            return null
        }
    }

    internal inner class  BitmapWorkerTask : AsyncTask<Int, Unit, Bitmap>() {
        ...

        // Decode image in background.
        override fun doInBackground(vararg params: Int?): Bitmap? {
            val imageKey = params[0].toString()

            // Check disk cache in background thread
            return getBitmapFromDiskCache(imageKey) ?:
                    // Not found in disk cache
                    decodeSampledBitmapFromResource(resources, params[0], 100, 100)
                            ?.also {
                                // Add final bitmap to caches
                                addBitmapToCache(imageKey, it)
                            }
        }
    }

    fun addBitmapToCache(key: String, bitmap: Bitmap) {
        // Add to memory cache as before
        if (getBitmapFromMemCache(key) == null) {
            memoryCache.put(key, bitmap)
        }

        // Also add to disk cache
        synchronized(diskCacheLock) {
            diskLruCache?.apply {
                if (!containsKey(key)) {
                    put(key, bitmap)
                }
            }
        }
    }

    fun getBitmapFromDiskCache(key: String): Bitmap? =
            diskCacheLock.withLock {
                // Wait while disk cache is started from background thread
                while (diskCacheStarting) {
                    try {
                        diskCacheLockCondition.await()
                    } catch (e: InterruptedException) {
                    }

                }
                return diskLruCache?.get(key)
            }

    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
    // but if not mounted, falls back on internal storage.
    fun getDiskCacheDir(context: Context, uniqueName: String): File {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        val cachePath =
                if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState()
                        || !isExternalStorageRemovable()) {
                    context.externalCacheDir.path
                } else {
                    context.cacheDir.path
                }

        return File(cachePath + File.separator + uniqueName)
    }
    
    
```

## 处理配置更改

运行时配置更改（例如屏幕方向更改）会导致 Android 销毁并使用新的配置重新启动正在运行的 Activity（有关此行为的更多信息，请参阅[处理运行时更改](https://developer.android.com/guide/topics/resources/runtime-changes)）。您需要避免重新处理所有图片，以便用户在配置发生更改时能够获得快速、流畅的体验。

幸运的是，您在[使用内存缓存](https://developer.android.com/topic/performance/graphics/cache-bitmap#memory-cache)部分构建了一个实用的位图内存缓存。您可以使用通过调用 `setRetainInstance(true)` 保留的 `Fragment` 将该缓存传递给新的 Activity 实例。重新创建 Activity 后，系统会重新附加这个保留的 `Fragment`，并且您将可以访问现有的缓存对象，从而能够快速获取图片并将其重新填充到 `ImageView` 对象中。

以下是使用 `Fragment` 在配置更改时保留 `LruCache` 对象的示例：

```kotlin
    private LruCache<String, Bitmap> memoryCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ...
        RetainFragment retainFragment =
                RetainFragment.findOrCreateRetainFragment(getFragmentManager());
        memoryCache = retainFragment.retainedCache;
        if (memoryCache == null) {
            memoryCache = new LruCache<String, Bitmap>(cacheSize) {
                ... // Initialize cache here as usual
            }
            retainFragment.retainedCache = memoryCache;
        }
        ...
    }

    class RetainFragment extends Fragment {
        private static final String TAG = "RetainFragment";
        public LruCache<String, Bitmap> retainedCache;

        public RetainFragment() {}

        public static RetainFragment findOrCreateRetainFragment(FragmentManager fm) {
            RetainFragment fragment = (RetainFragment) fm.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = new RetainFragment();
                fm.beginTransaction().add(fragment, TAG).commit();
            }
            return fragment;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }
    }
    
```


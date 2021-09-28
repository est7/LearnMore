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

### 6.5长图加载

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


# Retrofit自定义GsonConverter处理所有请求错误情况

[![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110191940347.webp)]

通常从服务端拿到的JSON数据格式大概如下:



```json
  {
    "code":1,
    "message":"查询成功",
    "detail":{"aa":"123","bb":"123","cc":"123"}
  }
```

因此通常我们会定义一个实体类来解析对应的json:
​



```java
public class Response {
    @SerializedName("code")
    private int code;
    @SerializedName("message")
    private String message;
    @SerializedName("detail")
    private DetailBean detail;
    //省略getter和setter方法...
      
    public static class DetailBean {
        @SerializedName("aa")
        private String aa;
        @SerializedName("bb")
        private String bb;
        @SerializedName("cc")
        private String cc;
        //省略getter和setter方法...
    }
}
```

其中的code字段表示状态,比如以下值可能代表了不同的含义

- code = 1, 表示成功, 不等于1代表错误
- code = -101, 表示token过期
- code = -102, 表示手机号码已经注册
- 等等等

如果我们按照正常的Retrofit+RxJava逻辑来处理,写出来的代码如下所示:



```java
//ApiService.java
public interface ApiService {
    String ENDPOINT = Constants.END_POINT;

    @POST("app/api")
    Observable<Response1> request1(@Body Request1 request);
  
    @POST("app/api")
    Observable<Response2> request2(@Body Request2 request);
    /**
     * Create a new ApiService
     */
    class Factory {
        private Factory() {  }

        public static ApiService createService( ) {
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            builder.readTimeout(10, TimeUnit.SECONDS);
            builder.connectTimeout(9, TimeUnit.SECONDS);
          
            if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
                interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
                builder.addInterceptor(interceptor);
            }

            builder.addInterceptor(new HeaderInterceptor());
            OkHttpClient client = builder.build();
            Retrofit retrofit =
                    new Retrofit.Builder().baseUrl(ApiService.ENDPOINT)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                            .build();
            return retrofit.create(ApiService.class);
        }
    }
}
```

使用的时候:



```java
ApiService mApiService = ApiService.Factory.createService();
mApiService.request1(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response1>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Response1 response) {
                        int code = response.getCode();
                        switch (code) {
                            case 1: //do something
                                break;
                            case -101://do something
                                break;
                            case -102: //do something
                                break;
                            default:
                                break;
                        }
                    }
                });
```

如果对每一个请求都这么做,那不是写死个人吗, 万一哪天这些值变了, 比如从-102 变成了 -105 , 那你不是每个地方全部都得改, 想想就可怕!

![img](https://raw.githubusercontent.com/Androidkobe/upload-image-note-learnmore/master/img/202110191940982.webp)

img.jpg

## 解决办法



```java
Retrofit retrofit =
                    new Retrofit.Builder().baseUrl(ApiService.ENDPOINT)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                            .build();
```

addConverterFactory(GsonConverterFactory.create()) 这句代码是为了用Gson把服务端返回的json数据解析成实体的, 那就从这里入手,可以自己定义一个GsonConverter,扩展一下原来的功能

先分析一下默认的GsonConverter怎么写的, 由三个类组成:

- GsonConverterFactory // GsonConverter 工厂类, 用来创建GsonConverter
- GsonResponseBodyConverter // 处理ResponseBody
- GsonRequestBodyConverter // 处理RequestBody

从名字就很容易看出每个类是干嘛的, GsonResponseBodyConverter这个类肯定是关键, 看一下这个类:



```java
final class GsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final Gson gson;
  private final TypeAdapter<T> adapter;

  GsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter) {
    this.gson = gson;
    this.adapter = adapter;
  }

  @Override public T convert(ResponseBody value) throws IOException {
    JsonReader jsonReader = gson.newJsonReader(value.charStream());
    try {
      return adapter.read(jsonReader);
    } finally {
      value.close();
    }
  }
}
```

你没有看错,就是这么几行代码... 这个convert()方法就是要扩展的地方了,

只需要在原来的逻辑上面添加上处理code ! = 1 的情况, 如果code ! = 1,就抛出异常,

先直接上代码:



```java
//CustomGsonConverterFactory.java
public class CustomGsonConverterFactory extends Converter.Factory {

    private final Gson gson;

    private CustomGsonConverterFactory(Gson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        this.gson = gson;
    }
  
    public static CustomGsonConverterFactory create() {
        return create(new Gson());
    }
  
    public static CustomGsonConverterFactory create(Gson gson) {
        return new CustomGsonConverterFactory(gson);
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new CustomGsonResponseBodyConverter<>(gson, adapter);
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(Type type,Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
        TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
        return new CustomGsonRequestBodyConverter<>(gson, adapter);
    }
}
```



```java
//CustomGsonRequestBodyConverter.java
final class CustomGsonRequestBodyConverter<T> implements Converter<T, RequestBody> {
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private final Gson gson;
    private final TypeAdapter<T> adapter;

    CustomGsonRequestBodyConverter(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override
    public RequestBody convert(T value) throws IOException {
        Buffer buffer = new Buffer();
        Writer writer = new OutputStreamWriter(buffer.outputStream(), UTF_8);
        JsonWriter jsonWriter = gson.newJsonWriter(writer);
        adapter.write(jsonWriter, value);
        jsonWriter.close();
        return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
    }
}
```



```java
//CustomGsonResponseBodyConverter.java
final class CustomGsonResponseBodyConverter<T> implements Converter<ResponseBody, T> {
    private final Gson gson;
    private final TypeAdapter<T> adapter;

    CustomGsonResponseBodyConverter(Gson gson, TypeAdapter<T> adapter) {
        this.gson = gson;
        this.adapter = adapter;
    }

    @Override
    public T convert(ResponseBody value) throws IOException {
        String response = value.string();
        HttpStatus httpStatus = gson.fromJson(response, HttpStatus.class);
        if (httpStatus.isCodeInvalid()) {
            value.close();
            throw new ApiException(httpStatus.getCode(), httpStatus.getMessage());
        }

        MediaType contentType = value.contentType();
        Charset charset = contentType != null ? contentType.charset(UTF_8) : UTF_8;
        InputStream inputStream = new ByteArrayInputStream(response.getBytes());
        Reader reader = new InputStreamReader(inputStream, charset);
        JsonReader jsonReader = gson.newJsonReader(reader);

        try {
            return adapter.read(jsonReader);
        } finally {
            value.close();
        }
    }
}
```

其他两个类和默认的一样的, 只看第三个类CustomGsonResponseBodyConverter

这里自定义了两个类,一个是HttpStatus和ApiException,下面是这两个类:



```java
//HttpStatus.java
public class HttpStatus {
    @SerializedName("code")
    private int mCode;
    @SerializedName("message")
    private String mMessage;

    public int getCode() {
        return mCode;
    }

    public String getMessage() {
        return mMessage;
    }

    /**
     * API是否请求失败
     *
     * @return 失败返回true, 成功返回false
     */
    public boolean isCodeInvalid() {
        return mCode != Constants.WEB_RESP_CODE_SUCCESS;
    }
}
```



```java
//ApiException.java
public class ApiException extends RuntimeException {
    private int mErrorCode;

    public ApiException(int errorCode, String errorMessage) {
        super(errorMessage);
        mErrorCode = errorCode;
    }

    /**
     * 判断是否是token失效
     *
     * @return 失效返回true, 否则返回false;
     */
    public boolean isTokenExpried() {
        return mErrorCode == Constants.TOKEN_EXPRIED;
    }
}
```

很通俗易懂, 解释一下其中关键的几行代码



```java
 String response = value.string(); //把responsebody转为string
// 这里只是为了检测code是否==1,所以只解析HttpStatus中的字段,因为只要code和message就可以了
 HttpStatus httpStatus = gson.fromJson(response, HttpStatus.class); 
 if (httpStatus.isCodeInvalid()) {
     value.close();
    //抛出一个RuntimeException, 这里抛出的异常会到Subscriber的onError()方法中统一处理
     throw new ApiException(httpStatus.getCode(), httpStatus.getMessage());
 }
```

这里有个关于ResponseBody的坑, 如果有人遇到过这个异常的肯定就知道



```java
java.lang.IllegalStateException: closed
            at com.squareup.okhttp.internal.http.HttpConnection$FixedLengthSource.read(HttpConnection.java:455)
            at okio.Buffer.writeAll(Buffer.java:594)
            at okio.RealBufferedSource.readByteArray(RealBufferedSource.java:87)
            at com.squareup.okhttp.ResponseBody.bytes(ResponseBody.java:56)
            at com.squareup.okhttp.ResponseBody.string(ResponseBody.java:82)
```

因为你只能对ResponseBody读取一次 , 如果你调用了response.body().string()两次或者response.body().charStream()两次就会出现这个异常, 先调用string()再调用charStream()也不可以.

所以通常的做法是读取一次之后就保存起来,下次就不从ResponseBody里读取.

## 最后使用方法:

先建立一个BaseSubscriber



```java
//BaseSubscriber.java
public class BaseSubscriber<T> extends Subscriber<T> {
    protected Context mContext;

    public BaseSubscriber(Context context) {
        this.mContext = context;
    }

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(final Throwable e) {
        Log.w("Subscriber onError", e);
        if (e instanceof HttpException) {
            // We had non-2XX http error
            Toast.makeText(mContext, mContext.getString(R.string.server_internal_error), Toast.LENGTH_SHORT).show();
        } else if (e instanceof IOException) {
            // A network or conversion error happened
            Toast.makeText(mContext, mContext.getString(R.string.cannot_connected_server), Toast.LENGTH_SHORT).show();
        } else if (e instanceof ApiException) {
            ApiException exception = (ApiException) e;
            if (exception.isTokenExpried()) {
                //处理token失效对应的逻辑
            } else {
                Toast.makeText(mContext, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } 
    }

    @Override
    public void onNext(T t) {

    }

}
```

请求方式



```java
ApiService mApiService = ApiService.Factory.createService();
mApiService.request1(request)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new BaseSubscriber<Response1>() {
                    @Override
                    public void onCompleted() {
                        super.onCompleted(); 
                    }

                    @Override
                    public void onError(Throwable e) {
                        super.onError(e); //这里就全部交给基类来处理了
                    }

                    @Override
                    public void onNext(Response1 response) {
                          super.onNext(response);
                       
                    }
                });
```
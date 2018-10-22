package cainiao.phoneassistant.common.http;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cainiao.phoneassistant.common.Constant;
import cainiao.phoneassistant.common.util.DensityUtil;
import cainiao.phoneassistant.common.util.DeviceUtils;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * 项目名称：PhoneAide
 * 创建人：LI
 * 创建时间：2017/4/14 14:12
 * 功能描述：拦截器
 */
public class CommonParamsInterceptor implements Interceptor {
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private Gson mGson;

    private Context mContext;

    public CommonParamsInterceptor(Gson mGson, Context mContext) {
        this.mGson = mGson;
        this.mContext = mContext;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        String method = request.method();

        try {

            //公共参数
            HashMap<String, Object> commomParamsMap = new HashMap<>();

            commomParamsMap.put(Constant.IMEI, DeviceUtils.getIMEI(mContext));
            commomParamsMap.put(Constant.MODEL, DeviceUtils.getModel());
            commomParamsMap.put(Constant.LANGUAGE, DeviceUtils.getLanguage());
            commomParamsMap.put(Constant.os, DeviceUtils.getBuildVersionIncremental());
            commomParamsMap.put(Constant.RESOLUTION, DensityUtil.getScreenW(mContext) + "*" + DensityUtil.getScreenH(mContext));
            commomParamsMap.put(Constant.SDK, DeviceUtils.getBuildVersionSDK() + "");
            commomParamsMap.put(Constant.DENSITY_SCALE_FACTOR, mContext.getResources().getDisplayMetrics().density + "");


            if ("GET".equals(method)) {
                HashMap<String, Object> rootMap = new HashMap<>();
                HttpUrl mHttpUrl = request.url();
                Set<String> paramNames = mHttpUrl.queryParameterNames();
                for (String key : paramNames) {
                    if (Constant.PARAM.equals(key)) {
                        String oldParamJson = mHttpUrl.queryParameter(Constant.PARAM);
                        //原始参数转hashmap

                        if (oldParamJson != null) {
                            HashMap<String, Object> p = mGson.fromJson(oldParamJson, HashMap.class);
                            if (p != null) {
                                for (Map.Entry<String, Object> entry : p.entrySet()) {
                                    rootMap.put(entry.getKey(), entry.getValue());
                                }
                            }

                        }
                    } else {
                        rootMap.put(key, mHttpUrl.queryParameter(key));
                    }
                }


                //加上公共参数 hashmap
                rootMap.put("publicParams", commomParamsMap);
                //新的json参数
                String newParamJson = mGson.toJson(rootMap);
                String url = mHttpUrl.toString();
                int index = url.indexOf("?");
                if (index > 0) {
                    url = url.substring(0, index);
                }
                url = url + "?" + Constant.PARAM + "=" + newParamJson;

                //重新构建请求
                request = request.newBuilder().url(url).build();
            } else if ("POST".equals(method)) {
                HashMap<String, Object> rootMap = new HashMap<>();
                RequestBody body = request.body();
                if (body instanceof FormBody) {
                    for (int i = 0; i < ((FormBody) body).size(); i++) {
                        rootMap.put(((FormBody) body).encodedName(i),((FormBody) body).encodedValue(i));
                    }
                } else {
                    Buffer buffer = new Buffer();
                    body.writeTo(buffer);
                    String oldJsonParams = buffer.readUtf8();
                    rootMap = mGson.fromJson(oldJsonParams, HashMap.class); // 原始参数
                    rootMap.put("publicParams", commomParamsMap); // 重新组装
                    String newJsonParams = mGson.toJson(rootMap); // {"page":0,"publicParams":{"imei":'xxxxx',"sdk":14,.....}}


                    request = request.newBuilder().post(RequestBody.create(JSON, newJsonParams)).build();
                }


            }
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }

        return chain.proceed(request);
    }
}

作者：凉城花祭八回梦
链接：https://www.jianshu.com/p/42bd2d7e2f14
來源：简书
简书著作权归作者所有，任何形式的转载都请联系作者获得授权并注明出处。
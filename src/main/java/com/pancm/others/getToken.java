package com.pancm.others;

import com.alibaba.fastjson.JSON;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class getToken {
    public static void main(String[] args) {
        /**
         *
         * postman 脚本
         * 使用pre-request-script.js
         * 请求体：body.json
         * {{time}}：获取环境变量值。json请求中需要加双引号""
         *
         */

        /**
         * java 代码
         */

        String baseUrl = "baseurl";
        String url = baseUrl + "/auth/token/get";
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String ukey = "ukeyStr";
            String usecret = "usecret";
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put("ukey", ukey);
            queryMap.put("appid", "appid");
            queryMap.put("authType", "3");
            queryMap.put("time", (System.currentTimeMillis()) + "");
            String bodyStr = ukey + createLinkString(queryMap, null) + usecret;
            System.out.println(bodyStr);

            String sign = DigestUtils.md5Hex(bodyStr);
            HttpPost method = new HttpPost(url);

            method.addHeader("WR-Client-Id", "12345");
            method.addHeader("WR-ukey", ukey);
            queryMap.put("sign", sign);
            System.out.println(JSON.toJSONString(queryMap));
            StringEntity entity = new StringEntity(JSON.toJSONString(queryMap), ContentType.APPLICATION_JSON);
            method.setEntity(entity);

            HttpResponse response = httpClient.execute(method);
            System.out.println(response.getStatusLine().getStatusCode());
            System.out.println(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String createLinkString(Map<String, String> params, String joinStr) {
        if (params == null || params.isEmpty()) {
            return null;
        }
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        //默认用"&"连接
        StringJoiner joiner = new StringJoiner((joinStr == null) ? "&" : joinStr);
        keys.forEach(key -> {
            try {
                joiner.add(key + "=" + params.get(key));
            } catch (Exception e) {
                return;
            }
        });
        return joiner.toString();
    }
}

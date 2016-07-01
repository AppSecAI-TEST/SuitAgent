/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.yiji.falcon.agent.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 修订记录:
 * guqiu@yiji.com 2016-06-22 17:48 创建
 */

/**
 * @author guqiu@yiji.com
 */
public class HttpUtil {

    private static final Logger log = LoggerFactory.getLogger(HttpUtil.class);
    
    /**
     * 发送json post请求
     * @param url
     * @param data
     * @return
     * @throws IOException
     */
    public static HttpResponse postJSON(String url,String data) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        StringEntity entity = new StringEntity(data, HTTP.UTF_8);
        entity.setContentType("application/json");
        httpPost.setEntity(entity);
        HttpClient client = new DefaultHttpClient();
        return client.execute(httpPost);
    }

    /**
     * 发送json post请求
     * @param url
     * @return
     * @throws IOException
     */
    public static String get(String url) throws IOException {
        String html = "";
        if (url != null && !"".equals(url)) {
            // 创建一个默认的HttpClient
            HttpClient httpclient = new DefaultHttpClient();
            try {
                // 以get方式请求网页
                HttpGet httpget = new HttpGet(url);
                // 执行请求并获取结果
                HttpResponse httpResponse = httpclient.execute(httpget);
                HttpEntity httpEntity = httpResponse.getEntity();

                html = EntityUtils.toString(httpEntity);
            } catch (Exception e) {
                log.error("GET请求{}错误",url,e);
                return "";
            } finally {
                // 关闭连接管理器
                httpclient.getConnectionManager().shutdown();
            }
        }
        return html;
    }

    /**
     * 构造POST提交表单请求，返回响应结果
     * @param params
     * 提交的参数
     * @param address
     * 提交的地址
     * @return
     */
    public static String post(Map<String,String> params,String address){

        if(params == null){
            params = new HashMap<>();
        }
        if(StringUtils.isEmpty(address)){
            log.error("请求地址不能为空");
            return null;
        }

        String param = "";

        for (String key : params.keySet()) {
            if(!StringUtils.isEmpty(key)){
                param += "&" + key + "=" + params.get(key);
            }
        }

        URL url;
        HttpURLConnection conn = null;
        try {
            url = new URL(address);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(30000);
            conn.setRequestMethod("POST");
            conn.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.1; Trident/4.0; .NET CLR 2.0.50727)");
            conn.setUseCaches(false);
            conn.setDoOutput(true);
            conn.connect();
        } catch (Exception e) {
            log.error("打开链接发生异常",e);
        }

        // 写出
        if(conn != null){
            String result = "";
            try(OutputStream write = conn.getOutputStream()){
                write.write(param.getBytes("utf-8"));
                result = convertToString(conn.getInputStream());
            } catch (IOException e) {
                log.error("写出请求发生异常",e);
            }
            conn.disconnect();
            return result;
        }

        return null;
    }

    /**
     * 获取InputStream流中的字符串
     * @param inputStream
     * 装载字符串的输入流
     * @return
     * 获取InputStream流中的字符串
     */
    public static String convertToString(InputStream inputStream){
        InputStreamReader inputStreamReader;
        try {
            inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("",e);
            return null;
        }
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder result = new StringBuilder();
        String line;
        try {
            while((line = bufferedReader.readLine()) != null){
                result.append(line).append("\n");
            }
        } catch (IOException e) {
            log.error("获取输入流中的字符串发生异常",e);
        } finally {
            try{
                inputStreamReader.close();
                inputStream.close();
                bufferedReader.close();
            }catch(IOException e){
                log.error("获取输入流中的字符串发生异常", e);
            }
        }
        return result.toString();
    }

    /**
     * FileInputStream
     * @param inputStream
     * 装载字符串的输入流
     * @return
     * 获取FileInputStream流中的字符串
     */
    public static String convertToString(FileInputStream inputStream){
        InputStreamReader inputStreamReader;
        try {
            inputStreamReader = new InputStreamReader(inputStream,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("",e);
            return null;
        }
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder result = new StringBuilder();
        String line;
        try {
            while((line = bufferedReader.readLine()) != null){
                result.append(line).append("\n");
            }
        } catch (IOException e) {
            log.error("获取输入流中的字符串发生异常", e);
        } finally {
            try{
                inputStreamReader.close();
                inputStream.close();
                bufferedReader.close();
            }catch(IOException e){
                log.error("获取输入流中的字符串发生异常", e);
            }
        }
        return result.toString();
    }

    /**
     * 用于去除字符串里的HTML代码
     * @param text
     * @return string
     */
    public static String removeHtml(String text){
        String regEx_html = "(&lt;)[^(&gt;)]+(&gt;)|<[^>]+>|[(&nbsp;)(\\n)(\\s)]";
        Pattern compile = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
        Matcher matcher = compile.matcher(text);
        return matcher.replaceAll("");
    }

}

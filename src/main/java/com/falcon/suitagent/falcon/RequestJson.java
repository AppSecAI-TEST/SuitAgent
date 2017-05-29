/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.falcon.suitagent.falcon;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/*
 * 修订记录:
 * guqiu@yiji.com 2016-06-22 17:48 创建
 */

/**
 * @author guqiu@yiji.com
 */
public class RequestJson {

    private final static Logger log = LoggerFactory.getLogger(RequestJson.class);

    /**
     * 转换RequestObject为JSONObject
     * @param requestObject
     * @return
     */
    public static JSONObject translatorByRequestObject(FalconReportObject requestObject){
        if(requestObject == null){
            log.warn("转换对象不能为空");
            return null;
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("endpoint",requestObject.getEndpoint());
        jsonObject.put("metric",requestObject.getMetric());
        jsonObject.put("timestamp",requestObject.getTimestamp());
        jsonObject.put("step",requestObject.getStep());
        jsonObject.put("value",requestObject.getValue());
        jsonObject.put("counterType",requestObject.getCounterType());
        jsonObject.put("tags",requestObject.getTags());
        return jsonObject;
    }

    /**
     * 转换RequestObject为JSON字符串
     * @param requestObject
     * @return
     */
    public static String getJsonString(FalconReportObject requestObject){
        JSONObject jsonObject = translatorByRequestObject(requestObject);
        if(jsonObject != null){
            return jsonObject.toString();
        }
        return null;
    }

    /**
     * 转换RequestObject集合为JSON字符串
     * @param requestObjectList
     * @return
     */
    public static String getJsonString(List<FalconReportObject> requestObjectList){
        if(requestObjectList == null){
            log.warn("转换对象不能为空");
            return null;
        }
        JSONArray jsonArray = new JSONArray();
        for (FalconReportObject requestObject : requestObjectList) {
            JSONObject jsonObject = translatorByRequestObject(requestObject);
            if(jsonObject != null){
                jsonArray.put(jsonObject);
            }
        }
        return jsonArray.toString();
    }

}

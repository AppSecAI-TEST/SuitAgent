/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.falcon.suitagent.plugins.plugin.mysql;
/*
 * 修订记录:
 * guqiu@yiji.com 2016-07-19 15:39 创建
 */

import com.falcon.suitagent.falcon.CounterType;
import com.falcon.suitagent.falcon.FalconReportObject;
import com.falcon.suitagent.plugins.JDBCPlugin;
import com.falcon.suitagent.plugins.metrics.MetricsCommon;
import com.falcon.suitagent.util.Maths;
import org.apache.commons.lang.math.NumberUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 监控值收集
 * @author guqiu@yiji.com
 */
class Metrics {

    private JDBCPlugin plugin;
    private Connection connection;
    /**
     * 需要采集相对变化量的指标
     */
    private static List<String> RALATIVE_METRICS =
            Arrays.asList(
                    "Slow_queries",
                    "Com_delete",
                    "Com_update",
                    "Com_insert",
                    "Com_commit",
                    "Com_rollback",
                    "Threads_connected",
                    "Created_tmp_tables"
            );
    /**
     * 相对变化量数据记录
     */
    private static ConcurrentHashMap<String,Number> metricsHistoryValueForRelative = new ConcurrentHashMap<>();

    Metrics(JDBCPlugin plugin,Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    /**
     * 获取监控值
     * @return
     */
    Collection<FalconReportObject> getReports() throws SQLException, ClassNotFoundException {
        Set<FalconReportObject> reportObjectSet = new HashSet<>();

        reportObjectSet.addAll(getGlobalStatus());
        reportObjectSet.addAll(getGlobalVariables());
//        reportObjectSet.addAll(getInnodbStatus());
        reportObjectSet.addAll(getSalveStatus());

        //相对变化量的指标
        Set<FalconReportObject> counterObj = new HashSet<>();
        reportObjectSet.forEach(falconReportObject -> {
            String metrics = falconReportObject.getMetric();
            if (RALATIVE_METRICS.contains(metrics)){
                Number previousValue = metricsHistoryValueForRelative.get(metrics);
                if (previousValue == null){
                    previousValue = NumberUtils.createNumber(falconReportObject.getValue());
                    //保存此次的值
                    metricsHistoryValueForRelative.put(metrics,previousValue);
                }else {
                    FalconReportObject reportObject = falconReportObject.clone();
                    reportObject.setMetric(metrics + "_Relative");
                    //添加本次与上一次监控值的相对值
                    reportObject.setValue(String.valueOf(Maths.sub(NumberUtils.createDouble(falconReportObject.getValue()),previousValue.doubleValue())));
                    //保存此次监控值为历史值
                    metricsHistoryValueForRelative.put(metrics,NumberUtils.createDouble(falconReportObject.getValue()));
                    counterObj.add(reportObject);
                }


            }
        });

        reportObjectSet.addAll(counterObj);
        return reportObjectSet;
    }

    private Collection<? extends FalconReportObject> getSalveStatus() throws SQLException, ClassNotFoundException {
        Set<FalconReportObject> reportObjectSet = new HashSet<>();
        String sql = "show slave status";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()){
            String value_Slave_IO_Running = rs.getString("Slave_IO_Running");
            String value_Slave_SQL_Running = rs.getString("Slave_SQL_Running");
            String value_Seconds_Behind_Master = rs.getString("Seconds_Behind_Master");
            String value_Connect_Retry = rs.getString("Connect_Retry");

            FalconReportObject falconReportObject = new FalconReportObject();
            MetricsCommon.setReportCommonValue(falconReportObject,plugin.step());
            falconReportObject.setCounterType(CounterType.GAUGE);
            falconReportObject.setTimestamp(System.currentTimeMillis() / 1000);
            falconReportObject.appendTags(MetricsCommon.getTags(plugin.agentSignName(),plugin,plugin.serverName()));

            //Slave_IO_Running
            falconReportObject.setMetric("Slave_IO_Running");
            if(value_Slave_IO_Running.equals("No") || value_Slave_IO_Running.equals("Connecting")){
                falconReportObject.setValue("0");
            }else{
                falconReportObject.setValue("1");
            }
            reportObjectSet.add(falconReportObject.clone());

            //Slave_SQL_Running
            falconReportObject.setMetric("Slave_SQL_Running");
            falconReportObject.setValue("yes".equals(value_Slave_SQL_Running.toLowerCase()) ? "1" : "0");
            reportObjectSet.add(falconReportObject.clone());

            //Seconds_Behind_Master
            falconReportObject.setMetric("Seconds_Behind_Master");
            falconReportObject.setValue(value_Seconds_Behind_Master == null ? "0" : value_Seconds_Behind_Master);
            reportObjectSet.add(falconReportObject.clone());

            //Connect_Retry
            falconReportObject.setMetric("Connect_Retry");
            falconReportObject.setValue(value_Connect_Retry);
            reportObjectSet.add(falconReportObject.clone());

        }
        return reportObjectSet;
    }

    private Collection<? extends FalconReportObject> getGlobalVariables() throws SQLException, ClassNotFoundException {
        Set<FalconReportObject> reportObjectSet = new HashSet<>();
        String sql = "SHOW /*!50001 GLOBAL */ VARIABLES";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()){
            String metric = rs.getString(1);
            String value = rs.getString(2);
            if (NumberUtils.isNumber(value)){
                //收集值为数字的结果
                FalconReportObject falconReportObject = new FalconReportObject();
                MetricsCommon.setReportCommonValue(falconReportObject,plugin.step());
                falconReportObject.setCounterType(CounterType.GAUGE);
                falconReportObject.setTimestamp(System.currentTimeMillis() / 1000);
                falconReportObject.setMetric(metric);
                falconReportObject.setValue(value);
                falconReportObject.appendTags(MetricsCommon.getTags(plugin.agentSignName(),plugin,plugin.serverName()));
                reportObjectSet.add(falconReportObject);
            }
        }
        rs.close();
        pstmt.close();
        return reportObjectSet;
    }

//    private Collection<? extends FalconReportObject> getInnodbStatus() throws SQLException{
//        Set<FalconReportObject> reportObjectSet = new HashSet<>();
//        return reportObjectSet;
//    }

    private Collection<? extends FalconReportObject> getGlobalStatus() throws SQLException, ClassNotFoundException {
        Set<FalconReportObject> reportObjectSet = new HashSet<>();
        String sql = "SHOW /*!50001 GLOBAL */ STATUS";
        PreparedStatement pstmt = connection.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while (rs.next()){
            String value = rs.getString(2);
            if (NumberUtils.isNumber(value)){
                String metric = rs.getString(1);
                FalconReportObject falconReportObject = new FalconReportObject();
                MetricsCommon.setReportCommonValue(falconReportObject,plugin.step());
                falconReportObject.setCounterType(CounterType.GAUGE);
                falconReportObject.setTimestamp(System.currentTimeMillis() / 1000);
                falconReportObject.setMetric(metric);
                falconReportObject.setValue(value);
                falconReportObject.appendTags(MetricsCommon.getTags(plugin.agentSignName(),plugin,plugin.serverName()));
                reportObjectSet.add(falconReportObject);
            }
        }
        rs.close();
        pstmt.close();
        return reportObjectSet;
    }

}

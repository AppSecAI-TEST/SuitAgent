/*
 * www.yiji.com Inc.
 * Copyright (c) 2016 All Rights Reserved
 */
package com.falcon.suitagent.vo.jdbc;
/*
 * 修订记录:
 * guqiu@yiji.com 2016-07-19 14:52 创建
 */

import lombok.Data;

/**
 * @author guqiu@yiji.com
 */
@Data
public class JDBCConnectionInfo {
    private String url;
    private String username;
    private String password;

    public JDBCConnectionInfo(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }
}

package com.project.pms.service.impl;

import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 *  IP地址归属地查询服务实现类
 */
@Service
public class IpLocationService {

    @Autowired
    @Qualifier("ipv4Searcher")
    private Searcher ipv4Searcher;

    @Autowired
    @Qualifier("ipv6Searcher")
    private Searcher ipv6Searcher;

    /**
     * 获取IP归属地（原始格式）
     */
    public String getLocation(String ip) {
        try {
            if (ip == null || ip.isEmpty()) {
                return "未知";
            }

            if (ip.contains(":")) {
                // IPv6地址
                return ipv6Searcher.search(ip);
            } else {
                // IPv4地址
                return ipv4Searcher.search(ip);
            }
        } catch (Exception e) {
            return "未知";
        }
    }

    /**
     * 获取格式化后的IP归属地（省+市）
     */
    public String getFormattedLocation(String ip) {
        String region = getLocation(ip);
        return parseRegion(region);
    }

    /**
     * 解析省/市
     */
    public String parseRegion(String region) {
        if (region == null || "未知".equals(region)) {
            return region;
        }

        String[] parts = region.split("\\|");
        if (parts.length >= 4) {
            String country = parts[0];   // 国家
            String province = parts[2];   // 省份
            String city = parts[3];       // 城市

            // 如果城市存在且不是"0"，返回"省份+城市"
            if (!"0".equals(city) && !city.isEmpty()) {
                return province + city;
            }
            // 否则只返回省份
            return province;
        }
        return region;
    }

    /**
     * 判断是否为内网IP
     */
    public boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        // IPv4内网地址段
        return ip.startsWith("10.") ||
                ip.startsWith("172.16.") ||
                ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") ||
                ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") ||
                ip.startsWith("172.25.") ||
                ip.startsWith("172.26.") ||
                ip.startsWith("172.27.") ||
                ip.startsWith("172.28.") ||
                ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") ||
                ip.startsWith("172.31.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("127.") ||
                ip.equals("localhost") ||
                ip.equals("0:0:0:0:0:0:0:1");  // IPv6本地地址
    }
}
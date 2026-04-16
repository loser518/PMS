package com.project.pms.controller;

import com.project.pms.entity.vo.Result;
import com.project.pms.service.impl.IpLocationService;
import com.project.pms.utils.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @className: IPController
 * @description: IP控制器
 * @author: loser
 * @createTime: 2026/3/19 13:44
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ip")
public class IPController {

    private final IpLocationService ipLocationService;

    @GetMapping("/my-ip")
    public Result getMyLocation(HttpServletRequest request) {
        try {
            // 1. 获取真实IP
            String ip = IpUtil.getIpAddr(request);
            log.info("获取到客户端IP: {}", ip);

            // 2. 检查是否为内网IP
            if (ipLocationService.isInternalIp(ip)) {
                return Result.success("内网IP，无法定位", ip);
            }

            // 3. 获取格式化后的归属地
            String location = ipLocationService.getFormattedLocation(ip);

            // 4. 返回结果
            return Result.success(location, ip);

        } catch (Exception e) {
            log.error("IP解析失败", e);
            return Result.error("IP解析失败");
        }
    }

    /**
     * 批量查询IP
     */
    @GetMapping("/batch")
    public Result batchQuery(@RequestParam List<String> ips) {
        Map<String, String> result = new HashMap<>();
        for (String ip : ips) {
            result.put(ip, ipLocationService.getFormattedLocation(ip));
        }
        return Result.success(result,"");
    }
}


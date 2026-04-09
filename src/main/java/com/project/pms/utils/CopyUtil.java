package com.project.pms.utils;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @className: CopyUtil
 * @description: 封装对象复制工具类
 * @author: loser
 * @createTime: 2026/1/31 21:10
 */
public class CopyUtil {
    /**
     * 复制对象列表
     *
     * @param sList  源对象列表
     * @param classZ 目标类类型
     * @return 复制后的新对象列表
     */
    public static <T, S> List<T> copyList(List<S> sList, Class<T> classZ) {
        List<T> list = new ArrayList<>();
        for (S s : sList) {
            T t = copy(s, classZ);
            if (t != null) {
                list.add(t);
            }
        }
        return list;
    }

    /**
     * 复制单个对象
     *
     * @param s      源对象
     * @param classZ 目标类类型
     * @return 复制后的新对象
     */
    public static <T, S> T copy(S s, Class<T> classZ) {
        if (s == null) {
            return null;
        }

        try {
            T t = classZ.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(s, t);
            return t;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}


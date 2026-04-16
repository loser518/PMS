package com.project.pms.entity.vo;


import com.project.pms.enums.ResponseCodeEnum;
import lombok.Data;

/**
 * @className: Result
 * @description:  封装返回结果
 * @author: loser
 * @createTime: 2026/1/28 19:07
 */
@Data
public class Result<T> {
    private Integer code; // 状态码：1 表示成功，0 表示失败
    private String status;
    private String msg; // 返回的消息
    private T data; // 返回的数据
//    private Map<String, Object> extraData = new HashMap<>(); // 额外的动态数据

    protected static final String STATUS_SUCCESS = "success";
    protected static final String STATUS_ERROR = "error";

    // 成功时的静态方法，携带数据
    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.code = ResponseCodeEnum.CODE_200.getCode();
        result.status = STATUS_SUCCESS;
        result.msg = msg;
        result.data = data;
        return result;
    }

    // 成功时的静态方法，不携带数据
    public static <T> Result<T> success(String msg) {
        Result<T> result = new Result<>();
        result.status = STATUS_SUCCESS;
        result.code = ResponseCodeEnum.CODE_200.getCode();
        result.msg = msg;
        return result;
    }

    // 失败时的静态方法，携带错误信息
    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.status = STATUS_ERROR;
        result.code = ResponseCodeEnum.CODE_200.getCode();
        result.msg = msg;
        return result;
    }

    // 失败时的静态方法，携带错误信息和状态码
    public static <T> Result<T> error(Integer code, String msg) {
        Result<T> result = new Result<>();
        result.status = STATUS_ERROR;
        result.code = code;
        result.msg = msg;
        return result;
    }

    // 添加额外的动态数据
//    public Result<T> addExtraData(String key, Object value) {
//        this.extraData.put(key, value);
//        return this;
//    }
}

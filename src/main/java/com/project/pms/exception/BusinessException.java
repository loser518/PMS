package com.project.pms.exception;

import com.project.pms.enums.ResponseCodeEnum;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;

/**
 * @className: BusinessException
 * @description: 业务异常
 * @author: loser
 * @createTime: 2026/1/31 21:16
 */
@RequiredArgsConstructor
public class BusinessException extends Exception {

    private ResponseCodeEnum responseCode;

    private Integer code;

    private String message;

    public BusinessException(String message, Throwable e) {
        super(message, e);
        this.message = message;
    }

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(Throwable e) {
        super(e);
    }

    public BusinessException(ResponseCodeEnum responseCode) {
        super(responseCode.getMsg());
        this.responseCode = responseCode;
        this.code = responseCode.getCode();
        this.message = responseCode.getMsg();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public ResponseCodeEnum getResponseCode() {
        return responseCode;
    }

    @Override
    public String getMessage() {
        return message;
    }


    public Integer getCode() {
        return code;
    }

    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}


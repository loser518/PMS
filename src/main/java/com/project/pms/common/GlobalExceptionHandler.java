package com.project.pms.common;

import com.project.pms.entity.vo.Result;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.UndeclaredThrowableException;
import java.net.BindException;

/**
 * @className: GlobalExceptionHandler
 * @description:  全局异常处理
 * @author: loser
 * @createTime: 2026/1/31 21:14
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    protected static final String STATUS_SUCCESS = "success";
    protected static final String STATUS_ERROR = "error";

    /**
     * 全局异常处理
     * @param e
     * @param request
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    Object handleException(Exception e, HttpServletRequest request) {
        logger.error("请求错误：{}， 请求地址：{}", e, request.getRequestURI());
        Result ajaxResponse = new Result();

        // 404
        if (e instanceof NoHandlerFoundException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_404.getCode());
            ajaxResponse.setMsg(ResponseCodeEnum.CODE_404.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof MethodArgumentNotValidException) {
            // 参数验证失败
            MethodArgumentNotValidException validException = (MethodArgumentNotValidException) e;
            String errorMessage = validException.getBindingResult().getFieldError() != null 
                ? validException.getBindingResult().getFieldError().getDefaultMessage()
                : "参数验证失败";
            ajaxResponse.setCode(ResponseCodeEnum.CODE_600.getCode());
            ajaxResponse.setMsg(errorMessage);
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof BindException) {
            // 参数绑定异常
            ajaxResponse.setCode(ResponseCodeEnum.CODE_600.getCode());
            ajaxResponse.setMsg(ResponseCodeEnum.CODE_600.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof DuplicateKeyException) {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_601.getCode());
            ajaxResponse.setMsg(ResponseCodeEnum.CODE_601.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else if (e instanceof UndeclaredThrowableException) {
            Throwable cause = ((UndeclaredThrowableException) e).getUndeclaredThrowable();
            if (cause instanceof BusinessException) {
                BusinessException be = (BusinessException) cause;
                ajaxResponse.setCode(be.getCode());
                ajaxResponse.setMsg(be.getMessage());
                ajaxResponse.setStatus(STATUS_ERROR);
            } else {
                ajaxResponse.setCode(ResponseCodeEnum.CODE_500.getCode());
                ajaxResponse.setMsg(ResponseCodeEnum.CODE_500.getMsg());
                ajaxResponse.setStatus(STATUS_ERROR);
            }
        } else if (e instanceof BusinessException) {
            BusinessException be = (BusinessException) e;
            ajaxResponse.setCode(be.getCode());
            ajaxResponse.setMsg(be.getMessage());
            ajaxResponse.setStatus(STATUS_ERROR);
        } else {
            ajaxResponse.setCode(ResponseCodeEnum.CODE_500.getCode());
            ajaxResponse.setMsg(ResponseCodeEnum.CODE_500.getMsg());
            ajaxResponse.setStatus(STATUS_ERROR);
        }
        return ajaxResponse;
    }

}

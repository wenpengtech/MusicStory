package com.cn.dto;

/**
 * restful 风格 错误编码
 */
public enum RestCode {
    SUCCESS(200, "操作成功"),
    USER_ERR(333,"用户名或密码错误"),
    USER_DISABLE(334,"用户被禁用"),
    UNAUTHZ(301 ,"未授权，拒绝访问"),
    PARAM_ERROR(400, "参数不合法"),
    UNAUTHEN(401, "未登录"),
    NOT_FOUND(404 ,"路径错误"),
    HEAD_ERROR(415, "头部不匹配"),
    METHOD_ERROR(405 ,"不支持该请求方式"),
    SERVER_ERROR(500, "服务异常"),
    FILE_UPLOAD_ERR(611,"文件上传失败");

    public int code;
    public String msg;

    RestCode(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}

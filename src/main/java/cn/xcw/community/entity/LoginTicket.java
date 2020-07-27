package cn.xcw.community.entity;

import lombok.Data;

import java.util.Date;

/**
 * @class: LoginTicket
 * @author: 邢成伟
 * @description: TODO
 **/
@Data
public class LoginTicket {

    private int id;
    private int userId;
    private String ticket;//登录凭证
    private int status;
    private Date expired;
}

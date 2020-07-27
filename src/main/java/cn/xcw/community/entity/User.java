package cn.xcw.community.entity;

import lombok.Data;

import java.util.Date;

/**
 * @class: User
 * @author: 邢成伟
 * @description: TODO
 **/
@Data
public class User {

    private int id;
    private String username;
    private String password;
    private String salt;
    private String email;
    private int type;
    private int status;
    private String activationCode;
    private String headerUrl;
    private Date createTime;
}

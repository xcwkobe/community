package cn.xcw.community.service;

import cn.xcw.community.entity.LoginTicket;
import cn.xcw.community.entity.User;
import cn.xcw.community.mapper.UserMapper;
import cn.xcw.community.util.CommunityConstant;
import cn.xcw.community.util.CommunityUtil;
import cn.xcw.community.util.MailClient;
import cn.xcw.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * @class: UserService
 * @author: 邢成伟
 * @description: TODO
 **/
@Service
public class UserService implements CommunityConstant{

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private RedisTemplate redisTemplate;

    //主机加端口
    @Value("${community.path.domain}")
    private String domain;

    //项目名
    @Value("${server.servlet.context-path}")
    private String contextPath;

    /**
     * 注册业务的实现
     * @param user
     * @return 返回的键值对数据可能转换为json
     */
    public Map<String,Object> register(User user){
        Map<String,Object> map=new HashMap<>();
        //空值处理,出现错误直接返回map，map其实返回的都是错误信息
        if (user==null)
            throw new IllegalArgumentException("参数不能为空");
        if(StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","用户名不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }
        //一般注册的时候用户名和邮箱地址不能重复，密码可以
        User u = userMapper.selectByName(user.getUsername());
        if (u!=null){
            map.put("usernameMsg","该账号已存在");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if(u!=null){
            map.put("emailMsg","该邮箱已被注册");
            return map;
        }

        //注册用户
        //在密码上加上随机的盐值
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        //密码+盐值->md5 数据库里面的密码
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        //
        user.setType(0);
        //状态为0就是未激活，1是激活
        user.setStatus(0);
        //随机生成的激活码
        user.setActivationCode(CommunityUtil.generateUUID());
        //随机生成一个头像，地址就是牛客的头像库
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //激活邮件,注册会发送一个激活邮件给用户，
        // 然后点击激活链接到loginController的activation处理
        Context context=new Context();
        context.setVariable("email",user.getEmail());
        String url=domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);
        //创造一个thymeleaf模板
        String content=templateEngine.process("/mail/activation",context);
        //发送的是一个html
        mailClient.sendMail(user.getEmail(),"激活账号",content);
        return map;
    }

    /**
     * 处理激活用户的业务
     * @param userId
     * @param code
     * @return
     */
    public int activation(int userId, String code) {
        User user = userMapper.selectById(userId);
        //如果已经被激活
        if (user.getStatus()==1){
            return ACTIVIATION_REPEATE;
        }else if (user.getActivationCode().equals(code)){
            //激活用户
            userMapper.updateStatus(userId,1);
            return ACTIVATION_SUCCESS;
        }else{
            return ACTIVIATION_FAILURE;
        }
    }

    /**
     *登录 生成登录凭证
     * @param username
     * @param password
     * @param expiredSeconds
     * @return
     */
    public Map<String,Object> login(String username, String password, long expiredSeconds) {
        Map<String,Object> map=new HashMap<>();
        //空值处理
        if(StringUtils.isBlank(username)){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        //验证账号
        //用户名不能不存在
        //状态必须是激活status=1
        //密码必须正确
        User user = userMapper.selectByName(username);
        if(user==null){
            map.put("usernameMsg","该账号不存在");
            return map;
        }
        if(user.getStatus()==0){
            map.put("usernameMsg","该账号尚未激活！");
            return map;
        }
        password=CommunityUtil.md5(password+user.getSalt());
        if(!user.getPassword().equals(password)){
            map.put("passwordMsg","密码错误");
            return map;
        }

        //生成登录凭证
        LoginTicket loginTicket=new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());//随机生成登录凭证
        //登录状态是0，退出是1
        loginTicket.setStatus(0);
        //设置凭证过期时间
        loginTicket.setExpired(new Date(System.currentTimeMillis()+expiredSeconds*1000));
        //本来是把登录凭证存在数据库中，现在改为存在redis中
        //loginTicketMapper.insertLoginTicket(loginTicket);

        //凭证存redis中，
        String redisKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);
        //带回ticket，放到cookie给页面
        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    /**
     * 用户退出业务
     * @param ticket
     */
    public void logOut(String ticket) {
        //通过ticket找到redis中的凭证，更改状态为1,不用删除
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket =(LoginTicket) redisTemplate.opsForValue().get(redisKey);
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }

    /**
     *通过ticket取出存在redis中的登录凭证
     * @param ticket
     * @return
     */
    public LoginTicket findLoginTicket(String ticket) {
        String redisKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket)redisTemplate.opsForValue().get(redisKey);
    }

    public User findUserById(int userId) {
        return userMapper.selectById(userId);
    }

    public void updateHeader(int id, String url) {
        userMapper.updateHeader(id,url);
    }

    /**
     * 更改密码
     * @param userId
     * @param oldPassword
     * @param newPassWord
     * @return
     */
    public Map<String, Object> updatePassword(int userId, String oldPassword, String newPassWord) {
        //先查看userId对不对，再看旧密码对不对
        Map<String,Object> map=new HashMap<>();
        User user = userMapper.selectById(userId);
        if(user==null){
            map.put("usernameMsg","账号不存在");
            return map;
        }
        oldPassword=CommunityUtil.md5(oldPassword+user.getSalt());
        if(!user.getPassword().equals(oldPassword)){
            map.put("oldPasswordMsg","旧密码错误");
            return map;
        }
        //上面两个都对，就修改密码
        newPassWord=CommunityUtil.md5(newPassWord+user.getSalt());
        userMapper.updatePassword(userId,newPassWord);
        map.put("updateSuccess","修改密码成功");
        return map;
    }

    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }
}

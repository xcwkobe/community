package cn.xcw.community.controller;

import cn.xcw.community.entity.User;
import cn.xcw.community.service.UserService;
import cn.xcw.community.util.CommunityConstant;
import cn.xcw.community.util.CommunityUtil;
import cn.xcw.community.util.RedisKeyUtil;
import com.google.code.kaptcha.Producer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @class: LoginController
 * @author: 邢成伟
 * @description: TODO
 **/
@Controller
public class LoginController implements CommunityConstant{

    //打印日志
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("/community")
    private String contextPath;

    //跳转到注册页面
    @GetMapping("/register")
    public String toRegisterPage(){
        return "site/register";
    }

    //注册用户
    @PostMapping("/register")
    public String register(User user, Model model){
        Map<String, Object> map = userService.register(user);
        //map为空，注册成功
        if (map==null || map.isEmpty()){
            model.addAttribute("msg","注册成功，我们已将向您发送了一封激活邮件，请尽快激活");
            model.addAttribute("target","/index");
            return "/site/operate-result";
        }else {//注册失败，返回错误信息
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "/site/register";
        }
        //return "";
    }

    //注册邮箱点击链接激活
    //http://localhost:8080/community/activation/101/code
    @GetMapping("/activation/{userId}/{code}")
    public String activation(Model model, @PathVariable("userId") int userId,@PathVariable("code") String code){
        int result = userService.activation(userId, code);
        if(result==ACTIVATION_SUCCESS){
            //激活成功到登录页面，其他都返回index
            model.addAttribute("msg","激活成功，你的账号已经可以正常使用");
            model.addAttribute("target","/login");
        }else if (result==ACTIVIATION_REPEATE){
            model.addAttribute("msg","该账号已经被激活过了");
            model.addAttribute("target","/index");
        }else{
            model.addAttribute("msg","激活码不正确");
            model.addAttribute("target","/index");
        }
        return "/site/operate-result";
    }

    //跳转登陆页面
    @GetMapping("/login")
    public String toLoginPage(){
        return "/site/login";
    }

    //用户登陆
    //code是验证码
//    @CookieValue：根据cookie的name获取Cookie中的value
    @PostMapping("/login")
    public String login(String username, String password, String code, boolean rememberme,
                        Model model, HttpSession session, HttpServletResponse response,
                        @CookieValue("kaptchaOwner")String kaptchaOwner){
        //登录前先检查验证码对不对
        //从redis获得验证码
        String kaptcha=null;
        if(StringUtils.isNotBlank(kaptchaOwner)){
            //通过cookie的value获得验证码的key，再获得value验证码
            String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha= (String)redisTemplate.opsForValue().get(redisKey);
        }
        if (StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)) {
            model.addAttribute("codeMsg", "验证码不正确!");
            return "/site/login";
        }

        //检查账号密码
        //勾选记住我，过期时间要长一点
        int expiredSeconds = rememberme ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        //如果登录结果中有ticket，说明在redis中生成了登录凭证,并且账号密码没问题，登录成功
        if (map.containsKey("ticket")){
            //
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            //访问哪些路径会带有这个cookie
            cookie.setPath(contextPath);
            //cookie存活时长
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }

    }

    //用kaptcha生成验证码
    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response){
        //生成验证码
        String text=kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        //设置这个cookie就是为了获取redis保存的验证码信息
        String kaptchaOwner= CommunityUtil.generateUUID();
        //cookie的name和value，value用uuid来代替
        Cookie cookie=new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        //将验证码存入redis，存60秒
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);

        //将图片输出给浏览器
        response.setContentType("image/png");
        try{
            OutputStream os = response.getOutputStream();
            ImageIO.write(image,"png",os);
        }catch (IOException e){
            logger.error("响应验证码失败:" + e.getMessage());
        }
    }

    //用户退出
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logOut(ticket);
        //SecurityContextHolder.clearContext();
        return "redirect:/login";
    }


}

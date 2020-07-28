package cn.xcw.community.controller;

import cn.xcw.community.annotation.LoginRequired;
import cn.xcw.community.entity.User;
import cn.xcw.community.service.FollowService;
import cn.xcw.community.service.LikeService;
import cn.xcw.community.service.UserService;
import cn.xcw.community.util.CommunityConstant;
import cn.xcw.community.util.CommunityUtil;
import cn.xcw.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * @class: UserController
 * @author: 邢成伟
 * @description: TODO
 **/

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;//http://localhost:8080

    @Value("${server.servlet.context-path}")
    private String contextPath;///community

//    @Value("${qiniu.key.access}")
//    private String accessKey;
//
//    @Value("${qiniu.key.secret}")
//    private String secretKey;
//
//    @Value("${qiniu.bucket.header.name}")
//    private String headerBucketName;
//
//    @Value("${qiniu.bucket.header.url}")
//    private String headerBucketUrl;

    @Autowired
    private HostHolder hostHolder;

    @LoginRequired
    @GetMapping("/setting")
    public String toSettingPage(){
        return "/site/setting";
    }

    /**
     * 将头像上传到七牛云，提前生成凭证传递过去
     * @param model
     * @return
     */
//    @LoginRequired
//    @GetMapping("/setting")
//    public String toSettingPage(Model model){
//        //上传文件名称
//        String fileName= CommunityUtil.generateUUID();
//        //设置响应信息
//        StringMap policy=new StringMap();
//        policy.put("returnBody",CommunityUtil.getJsonString(0));
//        //生成上传凭证
//        Auth auth=Auth.create(accessKey,secretKey);
//        String uploadToken=auth.uploadToken(headerBucketName,fileName,3600,policy);
//        model.addAttribute("uploadToken",uploadToken);
//        model.addAttribute("fileName",fileName);
//        return "/site/setting";
//    }

    /**
     * 成功上传图片到七牛云后会返回文件名，再把头像地址保存在数据库中
     * @param fileName
     * @return
     */
//    @PostMapping("/header/url")
//    @ResponseBody
//    public String updateHeaderUrl(String fileName){
//        if(!StringUtils.isBlank(fileName)){
//            return CommunityUtil.getJsonString(1,"文件名不能为空");
//        }
//        String url=headerBucketUrl+"/"+fileName;
//        userService.updateHeader(hostHolder.getUser().getId(),url);
//        return CommunityUtil.getJsonString(0);
//    }

    /**
     * 上传到本地服务器
     * @param headerImage
     * @param model
     * @return
     */
    @LoginRequired
    @PostMapping("/upload")
    public String upLoadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }
        //获得原始的图片名称
        String fileName = headerImage.getOriginalFilename();
        //图片后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)) {
            model.addAttribute("error", "文件的格式不正确");
            return "/site/setting";
        }
        //生成随机的文件名
        fileName = CommunityUtil.generateUUID() + suffix;
        //确定文件存放的位置
        File dest = new File(uploadPath + "/" + fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败" + e.getMessage());
            throw new RuntimeException("上传文件失败", e);
        }

        //上传成功之后需要更新当前用户的头像路径（web访问路径）
        User user = hostHolder.getUser();
        //http://localhost:8080/community/user/header/xxx.png
        String headUrl = domain + contextPath + "/user/header/" + fileName;
        userService.updateHeader(user.getId(), headUrl);
        return "redirect:/index";
    }

    /**
     * 读取头像地址
     * @param fileName
     * @param response
     */
    @LoginRequired
    @GetMapping("/header/{fileName}")
    public  void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        //服务器的存放路径
        fileName = uploadPath + "/" + fileName;
        //文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        //响应图片 通过io流将本地的图片输出给浏览器
        response.setContentType("image/" + suffix);

        try (
                FileInputStream fis = new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ) {
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1) {
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败：" + e.getMessage());
        }
    }

    /**
     * 更改密码
     * @param model
     * @param oldPassword
     * @param newPassWord
     * @param confirmPassword
     * @return
     */
    @LoginRequired
    @PostMapping("/updatePassword")
    public String updatePassword(Model model,String oldPassword,String newPassWord,String confirmPassword){
        if(StringUtils.isBlank(oldPassword)){
            model.addAttribute("oldPasswordMsg", "旧密码不能为空");
            return "/site/setting";
        }
        if(StringUtils.isBlank(newPassWord)||StringUtils.isBlank(confirmPassword)){
            model.addAttribute("passwordMsg","密码不能为空");
            return "/site/setting";
        }
        if(!newPassWord.equals(confirmPassword)){
            model.addAttribute("passwordMsg","两次密码不一致，请重新输入");
            return "/site/setting";
        }
        Map<String, Object> map = userService.updatePassword(hostHolder.getUser().getId(), oldPassword, newPassWord);
        //修改成功
        if(map.containsKey("updateSuccess")){
            System.out.println("updateSuccess");
            return "/site/login";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            return "/site/setting";
        }
    }

    /**
     * 到个人主页，不一定是自己的主页
     * @param userId
     * @param model
     * @return
     */
    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user=userService.findUserById(userId);
        if(user==null){
            throw new RuntimeException("该用户不存在");
        }
        model.addAttribute("user",user);
        //点赞数量
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount",likeCount);
        //关注别人的数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount",followeeCount);

        //粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount",followerCount);
        //是否已关注
        boolean hasFollowed=false;
        if(hostHolder.getUser()!=null){
            hasFollowed=followService.hasFollowed(hostHolder.getUser().getId(),ENTITY_TYPE_USER,userId);
        }
        model.addAttribute("hasFollowed",hasFollowed);
        return "/site/profile";
    }



}

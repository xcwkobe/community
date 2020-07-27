package cn.xcw.community;

import cn.xcw.community.util.CommunityUtil;
import cn.xcw.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CommunityApplication.class)
public class CommunityApplicationTests {

	@Autowired
	private MailClient mailClient;

	@Test
	public void test() {
		//mailClient.sendMail("603124891@qq.com","主题","world");

	}

}


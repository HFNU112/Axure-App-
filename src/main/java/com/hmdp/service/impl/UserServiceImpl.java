package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import com.hmdp.utils.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //发送手机验证码
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //校验手机号 -- 正则表达式
        if (RegexUtils.isPhoneInvalid(phone)){
            //如果手机号不符合，就返回错误信息
            return Result.fail("手机号格式有误, 请重新输入!");
        }
        //符合，生成验证码  hutool-all工具包
        String code = RandomUtil.randomNumbers(6);

        //验证码保存到redis中 验证码code选择 String类型
        //设置 有效期 set key value ex
        //session.setAttribute("code", code);
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //发送验证码  -- 调用第三方技术的短信服务（参考阿里云视频点播的案例） 模拟发送短信成功  控制台打印日志模拟由短信服务上发送的验证码
        log.debug("短信验证码发送成功，【" + code + "】");

        return Result.ok();
    }

    //用户登录
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式有误, 请重新输入!");
        }
        // 从redis获取验证码 校验验证码
        //Object codeCache= session.getAttribute("code");
        String codeCache = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if (codeCache == null || !codeCache.toString().equals(code)){
            //验证码不一致，报错
            return Result.fail("验证码错误");
        }
        //一致，根据手机号查询用户user  select * from tb_user where phone = ?
        User user = query().eq("phone", phone).one();

        //判断用户是否存在
        if (user == null) {
            //不存在，创建新用户。根据手机号创建用户
            user = createUserWithPhone(phone);
        }
        //若存在User对象，将User对象保存到session中
        // 方法1：手动new UserDto对象封装id、nickName、icon属性
        // 方法2：将user对象转为UserDto,BeanUtil.copyProperties(user, UserDTO.class避免传递敏感信息
        //存在用户保存到redis
        //生成token
        String token = UUID.randomUUID().toString(true);
        //将User对象转换为hash存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //UserDTO对象属性保证String类型
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(false)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        //存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, userMap);
        //设置token的有效期
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, LOGIN_USER_TTL, TimeUnit.MINUTES);

        //返回token
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(SystemConstants.USER_NICK_NAME_PREFIX +RandomUtil.randomString(6));
        //保存用户
        save(user);
        return user;
    }


}

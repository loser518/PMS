package com.project.pms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.project.pms.entity.constants.Constants;
import com.project.pms.entity.dto.UserDto;
import com.project.pms.entity.po.StudentProfile;
import com.project.pms.entity.po.TeacherProfile;
import com.project.pms.entity.po.User;
import com.project.pms.entity.vo.Result;
import com.project.pms.entity.vo.UserInfoVO;
import com.project.pms.enums.ResponseCodeEnum;
import com.project.pms.enums.user.UserGenderEnum;
import com.project.pms.enums.user.UserRoleEnum;
import com.project.pms.enums.user.UserStatusEnum;
import com.project.pms.exception.BusinessException;
import com.project.pms.im.IMServer;
import com.project.pms.mapper.StudentProfileMapper;
import com.project.pms.mapper.TeacherProfileMapper;
import com.project.pms.mapper.UserMapper;
import com.project.pms.security.CurrentUser;
import com.project.pms.security.UserDetail;
import com.project.pms.service.UserAccountService;
import com.project.pms.utils.DateUtil;
import com.project.pms.utils.JwtUtil;
import com.project.pms.utils.RedisUtil;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @className: UserAccountServiceImpl
 * @description: 账号服务实现类
 * @author: loser
 * @createTime: 2026/1/31 21:11
 */
@Service
@RequiredArgsConstructor
public class UserAccountServiceImpl implements UserAccountService {

    private static final Logger logger = LoggerFactory.getLogger(UserAccountServiceImpl.class);

    private final UserMapper userMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final TeacherProfileMapper teacherProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationProvider authenticationProvider;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final CurrentUser currentUser;

    /**
     * 注册
     *
     * @param username
     * @param role
     * @param password
     * @throws BusinessException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String username, Integer role, String password) throws BusinessException {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(queryWrapper);
        if (null != user) {
            throw new BusinessException("用户已存在");
        }

        LambdaQueryWrapper<User> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.orderByDesc(User::getId).last("limit 1");    // 降序选第一个
        User lastUser = userMapper.selectOne(queryWrapper1);
        int newUserId;
        if (lastUser == null) {
            newUserId = 1;
        } else {
            newUserId = lastUser.getId() + 1;
        }
        String encodedPassword = passwordEncoder.encode(password);  // 密文存储
        Date now = new Date();
        User newUser = new User(newUserId,
                username,
                encodedPassword,
                role == 0 ? Constants.NICKNAME_PREFIX_STUDENT + newUserId : Constants.NICKNAME_PREFIX_TEACHER + newUserId,
                Constants.AVATAR_URL,
                Constants.BG_URL,
                UserGenderEnum.UNKNOWN.getCode(),
                null,
                UserStatusEnum.ENABLE.getStatus(),
                role,
                null,
                null,
                now,
                now);
        userMapper.insert(newUser);
        if (role == 1) {
            TeacherProfile teacherProfile = new TeacherProfile();
            teacherProfile.setTid(newUserId);
            teacherProfile.setCurrentStudentCount(Constants.CURRENT_STUDENT_COUNT);
            teacherProfile.setMaxStudentCount(Constants.MAX_STUDENT_COUNT);
            teacherProfile.setCreateTime(now);
            teacherProfile.setUpdateTime(now);
            teacherProfileMapper.insert(teacherProfile);
//            List<UserInfoVO> userInfoVOList = redisUtil.getAllList(Constants.REDIS_KEY_USER_LIST, UserInfoVO.class);
//            userInfoVOList.add(UserInfoVO.forTeacher(CopyUtil.copy(newUser, UserDto.class), teacherProfile));
//            redisUtil.delValue(Constants.REDIS_KEY_USER_LIST);
//            redisUtil.setAllList(Constants.REDIS_KEY_USER_LIST, userInfoVOList);
        }
        if (role == 0) {
            StudentProfile studentProfile = new StudentProfile();
            studentProfile.setSid(newUserId);
            studentProfile.setEnrollmentYear(DateUtil.getYear(now));
            studentProfile.setCreateTime(now);
            studentProfile.setUpdateTime(now);
            studentProfileMapper.insert(studentProfile);
//            List<UserInfoVO> userInfoVOList = redisUtil.getAllList(Constants.REDIS_KEY_USER_LIST, UserInfoVO.class);
//            userInfoVOList.add(UserInfoVO.forStudent(CopyUtil.copy(newUser, UserDto.class), studentProfile));
//            redisUtil.delValue(Constants.REDIS_KEY_USER_LIST);
//            redisUtil.setAllList(Constants.REDIS_KEY_USER_LIST, userInfoVOList);
        }


//        msgUnreadMapper.insert(new MsgUnread(new_user.getId(),0,0,0,0,0,0));
//        favoriteMapper.insert(new Favorite(null, new_user.getId(), 1, 1, null, "默认收藏夹", "", 0, null));
//        esUtil.addUser(new_user);
    }

    /**
     * 登录
     *
     * @param username
     * @param password
     * @return
     * @throws BusinessException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result login(String username, String password) throws BusinessException {
        Result result = new Result();
        //验证是否能正常登录
        //将用户名和密码封装成一个类，这个类不会存明文了，将是加密后的字符串
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(username, password);

        // 用户名或密码错误会抛出异常
        Authentication authenticate;
        try {
            authenticate = authenticationProvider.authenticate(authenticationToken);
        } catch (Exception e) {
            result.setCode(ResponseCodeEnum.CODE_403.getCode());
            result.setMsg("用户名或密码错误");
            result.setStatus("error");
            return result;
        }
        //将用户取出来
        UserDetail loginUser = (UserDetail) authenticate.getPrincipal();
        User user = loginUser.getUser();

        redisUtil.setExObjectValue(Constants.REDIS_KEY_USER + user.getId(), user);

        // 检查账号状态，1 表示封禁中，不允许登录
        if (user.getStatus() == 1) {
            throw new BusinessException(ResponseCodeEnum.CODE_403.getCode(), "账号异常，封禁中");
        }
        String token = jwtUtil.createToken(user.getId().toString(), Objects.requireNonNull(UserRoleEnum.getEnum(user.getRole())).name());
        try {
            // 把完整的用户信息存入redis，时间跟token一样，注意单位
            // 这里缓存的user信息建议只供读取uid用，其中的状态等非静态数据可能不准，所以 redis另外存值
            redisUtil.setExObjectValue(Constants.REDIS_KEY_SECURITY + Objects.requireNonNull(UserRoleEnum.getEnum(user.getRole())).name() + ":" + user.getId(), user, Constants.JWT_TTL, TimeUnit.SECONDS);
            // 将该用户放到redis中在线集合
        } catch (Exception e) {
            throw e;
        }
        UserDto loginUserDto = new UserDto();
        loginUserDto.setId(user.getId());
        loginUserDto.setUsername(user.getUsername());
        loginUserDto.setAvatar(user.getAvatar());
        loginUserDto.setBackground(user.getBackground());
        loginUserDto.setGender(user.getGender());
        loginUserDto.setDescription(user.getDescription());
        loginUserDto.setStatus(user.getStatus());
        loginUserDto.setNickname(user.getNickname());
        loginUserDto.setRole(user.getRole());
        loginUserDto.setPhone(user.getPhone());
        loginUserDto.setEmail(user.getEmail());
        loginUserDto.setCreateTime(user.getCreateTime());
        loginUserDto.setUpdateTime(user.getUpdateTime());

        UserInfoVO userInfoVO = loginUserDto.getRole() == 0 ?
                UserInfoVO.forStudent(loginUserDto, studentProfileMapper.selectOne(new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getSid, loginUserDto.getId()))) :
                UserInfoVO.forTeacher(loginUserDto, teacherProfileMapper.selectOne(new LambdaQueryWrapper<TeacherProfile>().eq(TeacherProfile::getTid, loginUserDto.getId())));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("token", token);
        userInfo.put("userInfo", userInfoVO);
        return Result.success(userInfo, "登录成功");
    }

    @Override
    public User selectByUid(Integer uid) {
        return userMapper.selectById(uid);
    }

    /**
     * 登出（修复：需先获取用户 role，而非把 id 误传给 UserRoleEnum.getEnum）
     * 同时主动关闭该用户的所有 Netty WebSocket 连接，确保即时下线
     */
    @Override
    public void logout() {
        Integer id = currentUser.getUserId();
        // 先从 Redis 缓存取 User，取不到再查库，从而拿到正确的 role
        User user = redisUtil.getObject(Constants.REDIS_KEY_USER + id, User.class);
        if (user == null) {
            user = userMapper.selectById(id);
        }
        Integer role = (user != null) ? user.getRole() : null;
        String roleName = (role != null && UserRoleEnum.getEnum(role) != null)
                ? UserRoleEnum.getEnum(role).name() : "UNKNOWN";

        redisUtil.removeCache(Constants.REDIS_KEY_USER + id);
        redisUtil.removeCache(Constants.REDIS_KEY_TOKEN + roleName + ":" + id);
        redisUtil.removeCache(Constants.REDIS_KEY_SECURITY + roleName + ":" + id);

        // 主动断开该用户所有 WebSocket 连接，触发 channelInactive 回调从 USER_CHANNEL 中移除
        Set<Channel> channels = IMServer.USER_CHANNEL.remove(id);
        if (channels != null) {
            for (Channel channel : channels) {
                channel.close();
            }
        }
    }

    /**
     * 修改密码
     *
     * @param pwd
     * @param npwd
     * @return
     * @throws BusinessException
     */
    @Override
    public Result updatePassword(String pwd, String npwd) throws BusinessException {
        if (npwd == null || npwd.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.CODE_403.getCode(), "密码不能为空！");
        }
        // 取出当前登录的用户
        UsernamePasswordAuthenticationToken authenticationToken1 =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        UserDetail userDetail = (UserDetail) authenticationToken1.getPrincipal();
        User user = userDetail.getUser();
        // 验证旧密码
        UsernamePasswordAuthenticationToken authenticationToken2 =
                new UsernamePasswordAuthenticationToken(user.getUsername(), pwd);
        try {
            authenticationProvider.authenticate(authenticationToken2);
        } catch (Exception e) {
            return Result.error(ResponseCodeEnum.CODE_403.getCode(), "旧密码错误");
        }

        if (Objects.equals(pwd, npwd)) {
            return Result.error(ResponseCodeEnum.CODE_500.getCode(), "新密码不能与旧密码相同");
        }

        String encodedPassword = passwordEncoder.encode(npwd);  // 密文存储

        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, user.getId()).set(User::getPassword, encodedPassword);
        userMapper.update(null, updateWrapper);
        logout();
        return Result.success("修改密码成功");
    }
}


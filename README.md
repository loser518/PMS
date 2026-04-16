# PMS Frontend

基于当前工作区 `PMS` Spring Boot 后端编写的 Vue3 + JavaScript 前端。

## 技术栈

- Vue 3
- Vue Router 4
- Pinia
- Axios
- Vite

## 已覆盖的后端接口

- `/user/checkCode`
- `/user/login`
- `/user/register`
- `/user/logout`
- `/user/updatePassword`
- `/userInfo`
- `/userInfo/{id}`
- `/userInfo/update`
- `/userInfo/updateAvatar`
- `/projectInfo`
- `/projectInfo/update`
- `/projectProgress`
- `/projectProgress/update`
- `/announcement`
- `/announcement/update`
- `/category`
- `/category/update`
- `/comment/tree/{tid}`
- `/comment/add`
- `/comment/{id}`

## 页面

- 登录/注册
- 概览首页
- 用户管理
- 课题申报管理
- 项目进度管理
- 公告中心
- 公告分类
- 个人中心

## 启动

```bash
cd pms-frontend
npm install
npm run dev
```

开发服务器默认端口为 `5173`，并通过 Vite 代理把 `/api` 转发到 `http://localhost:9090`。

## 说明

1. 认证头使用 `Authorization: Bearer <token>`，与后端 `JwtAuthenticationTokenFilter` 对齐。
2. 项目进度页面当前直接编辑 `fileUrl` / `fileName`，因为后端没有提供独立的进度附件上传接口。
3. 公告评论区当前按公告 `id` 作为 `tid` 进行评论树展示。
4. 用户管理与公告分类页面默认只对教师/管理员开放，和前端路由权限保持一致。

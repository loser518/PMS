# PMS 前端 Vercel 部署指南

## 部署步骤

### 方法一：通过 Vercel 官网部署（推荐）

#### 1. 准备工作
- 确保代码已推送到 GitHub 仓库 ✅（已完成）
- 拥有 Vercel 账号（可以使用 GitHub 账号登录）

#### 2. 部署步骤

1. **登录 Vercel**
   - 访问 https://vercel.com
   - 点击 "Sign Up" 或 "Log In"
   - 选择 "Continue with GitHub"

2. **导入项目**
   - 点击 "Add New..." → "Project"
   - 找到并选择 `PMS` 仓库
   - 点击 "Import"

3. **配置项目**
   - **Project Name**: `pms`（或自定义）
   - **Framework Preset**: 选择 `Vite`
   - **Root Directory**: 保持默认 `./`（因为 vercel.json 已配置）
   - **Build Command**: `cd frontend && npm install && npm run build`
   - **Output Directory**: `frontend/dist`
   - **Install Command**: `cd frontend && npm install`

4. **配置环境变量（重要）**
   点击 "Environment Variables"，添加：
   ```
   VITE_API_BASE_URL = https://your-backend-url.com
   ```
   ⚠️ 注意：将 `https://your-backend-url.com` 替换为你的后端实际地址

5. **部署**
   - 点击 "Deploy"
   - 等待部署完成（通常1-2分钟）
   - 部署成功后会获得一个 `https://pms-xxx.vercel.app` 的域名

---

### 方法二：通过 Vercel CLI 部署

#### 1. 安装 Vercel CLI
```bash
npm install -g vercel
```

#### 2. 登录 Vercel
```bash
vercel login
```

#### 3. 部署项目
在项目根目录（PMS文件夹）执行：
```bash
vercel
```

首次部署会提示配置：
- Set up and deploy? **Y**
- Which scope? 选择你的账号
- Link to existing project? **N**
- What's your project's name? **pms**
- In which directory is your code located? **./**

#### 4. 配置环境变量
```bash
vercel env add VITE_API_BASE_URL production
```
输入你的后端API地址，例如：`https://your-backend.com`

#### 5. 生产环境部署
```bash
vercel --prod
```

---

## 重要配置说明

### 1. 后端 API 地址配置

由于前端使用 `baseURL: "/api"`，你需要在 Vercel 上配置重写规则。

**方式一：修改 vercel.json（推荐）**

在 `vercel.json` 中添加重写规则：
```json
{
  "buildCommand": "cd frontend && npm install && npm run build",
  "outputDirectory": "frontend/dist",
  "installCommand": "cd frontend && npm install",
  "framework": "vite",
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": "https://your-backend-url.com/api/$1"
    }
  ]
}
```
⚠️ 将 `https://your-backend-url.com` 替换为你的后端实际地址

**方式二：使用环境变量**

在 `frontend/src/api/http.js` 中修改：
```javascript
const http = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
    timeout: 15000
});
```

### 2. vercel.json 配置

项目根目录的 `vercel.json` 已配置好构建命令，无需修改。

---

## 后端部署说明

Vercel 只能部署前端，后端需要单独部署。

### 后端部署选项：

1. **Railway** (推荐)
   - https://railway.app
   - 支持 Spring Boot + MySQL
   - 免费额度充足

2. **Render**
   - https://render.com
   - 支持 Java 应用

3. **阿里云/腾讯云服务器**
   - 购买云服务器
   - 部署 Java 应用和 MySQL

4. **本地运行（仅开发测试）**
   - 使用内网穿透工具（如 ngrok）
   - 将后端暴露到公网

---

## 部署后检查清单

- [ ] 前端可以正常访问
- [ ] 登录功能正常
- [ ] API 请求可以到达后端
- [ ] 静态资源加载正常
- [ ] 路由跳转正常（刷新页面不404）

---

## 常见问题

### Q1: 部署后页面空白
**A:** 检查浏览器控制台错误，通常是：
- API 地址配置错误
- 环境变量未配置
- 构建失败

### Q2: API 请求 404
**A:** 检查：
- `vercel.json` 中的 rewrites 配置
- 后端服务是否正常运行
- 后端地址是否正确

### Q3: 刷新页面 404
**A:** 在 `vercel.json` 中添加：
```json
"rewrites": [
  { "source": "/(.*)", "destination": "/index.html" }
]
```

### Q4: 跨域问题
**A:** 确保后端配置了 CORS，允许 Vercel 域名访问。

---

## 持续部署

配置完成后，每次推送到 GitHub 的 `main` 分支，Vercel 都会自动部署新版本。

---

## 需要帮助？

如果遇到部署问题，可以：
1. 查看 Vercel 部署日志
2. 检查浏览器控制台错误
3. 确认后端服务正常运行
4. 验证环境变量配置正确

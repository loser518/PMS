# PMS - 高校课题管理系统

基于 Spring Boot 3 + Vue 3 的高校课题管理系统，支持学生、教师、管理员三种角色。

## 项目结构

```
PMS/
├── backend/                 # Spring Boot 后端
│   ├── src/main/java/
│   ├── src/main/resources/
│   ├── pom.xml
│   └── ...
├── frontend/               # Vue 3 前端
│   ├── src/
│   ├── package.json
│   ├── vite.config.js
│   └── ...
└── README.md
```

## 技术栈

### 后端
- Spring Boot 3.2.4
- MyBatis-Plus
- MySQL
- JWT 认证
- POI Excel 导入导出

### 前端
- Vue 3 + Composition API
- Vite 5
- Element Plus
- Pinia 状态管理
- Vue Router

## 快速启动

### 后端
```bash
cd backend
mvn spring-boot:run
# 启动端口：9090
```

### 前端
```bash
cd frontend
npm install
npm run dev
# 启动端口：5173
```

## 部署

### 前端部署（Vercel）
```bash
cd frontend
npm install
npm run build
# 将 dist 目录部署到 Vercel
```

### 后端部署
```bash
cd backend
mvn package -DskipTests
java -jar target/pms-0.0.1-SNAPSHOT.jar
```

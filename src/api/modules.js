import http from "./http";

// 认证相关 API
export const authApi = {
  getCaptcha() {
    return http.get("/user/checkCode");
  },
  login(params) {
    return http.post("/user/login", null, { params });
  },
  register(params) {
    return http.post("/user/register", null, { params });
  },
  logout() {
    return http.get("/user/logout");
  },
  updatePassword(params) {
    return http.post("/user/updatePassword", null, { params });
  }
};

// 用户信息 API
export const userApi = {
  list(params) {
    return http.get("/userInfo", { params });
  },
  detail(id) {
    return http.get(`/userInfo/${id}`);
  },
  update(data) {
    return http.post("/userInfo/update", data);
  },
  remove(ids) {
    return http.delete("/userInfo", { data: ids });
  },
  uploadAvatar(file) {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/userInfo/updateAvatar", formData);
  },
  search(keyword) {
    return http.get("/userInfo/search", { params: { keyword } });
  },
  /** 导出用户Excel，query 为筛选参数（与列表同） */
  exportExcel(query = {}) {
    return http.download("/userInfo/export", query, "用户列表.xlsx");
  },
  /** 导入用户Excel */
  importExcel(file) {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/userInfo/import", formData);
  }
};

// 课题信息 API
export const projectApi = {
  list(params) {
    return http.get("/projectInfo", { params });
  },
  create(data) {
    return http.post("/projectInfo", data);
  },
  update(data) {
    return http.post("/projectInfo/update", data);
  },
  remove(ids) {
    return http.delete("/projectInfo", { data: ids });
  },
  /** 导出课题Excel，query 为筛选参数 */
  exportExcel(query = {}) {
    return http.download("/projectInfo/export", query, "课题列表.xlsx");
  },
  /** 导入课题Excel */
  importExcel(file) {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/projectInfo/import", formData);
  }
};

// 教师 API（用于课题申报时选择指导老师）
export const teacherApi = {
  options() {
    return http.get("/userInfo/teachers");
  }
};

// 项目进度 API
export const progressApi = {
  list(params) {
    return http.get("/projectProgress", { params });
  },
  create(data) {
    return http.post("/projectProgress", data);
  },
  update(data) {
    return http.post("/projectProgress/update", data);
  },
  remove(ids) {
    return http.delete("/projectProgress", { data: ids });
  },
  uploadFile(file) {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/projectProgress/upload", formData);
  },
  download(objectName, fileName, fileUrl) {
    // 支持传入 objectName 或 fileUrl（后端会自动从 fileUrl 解析 objectName）
    const params = fileUrl ? { fileUrl, fileName } : { objectName, fileName };
    return http.download("/projectProgress/download", params, fileName);
  }
};

// 公告 API
export const announcementApi = {
  list(params) {
    return http.get("/announcement", { params });
  },
  create(data) {
    return http.post("/announcement", data);
  },
  update(data) {
    return http.post("/announcement/update", data);
  },
  remove(ids) {
    return http.delete("/announcement", { data: ids });
  },
  view(id) {
    return http.post(`/announcement/view/${id}`);
  }
};

// 分类 API
export const categoryApi = {
  list(params) {
    return http.get("/category", { params });
  },
  create(data) {
    return http.post("/category", data);
  },
  update(data) {
    return http.post("/category/update", data);
  },
  remove(ids) {
    return http.delete("/category", { data: ids });
  }
};

// 评论 API
export const commentApi = {
  tree(tid) {
    return http.get(`/comment/tree/${tid}`);
  },
  create(data) {
    return http.post("/comment/add", data);
  },
  remove(id) {
    return http.post(`/comment/${id}`);
  }
};

// 通知 API
export const notificationApi = {
  list(pageNo = 1, pageSize = 20) {
    return http.get("/notification", { params: { pageNo, pageSize } });
  },
  countUnread() {
    return http.get("/notification/unread");
  },
  markRead(id) {
    return http.post(`/notification/read/${id}`);
  },
  markAllRead() {
    return http.post("/notification/read/all");
  },
  clearAll() {
    return http.delete("/notification/clear");
  }
};

// 好友 API
export const friendApi = {
  list() {
    return http.get("/friend/list");
  },
  apply(toUid, message) {
    return http.post("/friend/apply", null, { params: { toUid, message } });
  },
  accept(id) {
    return http.post(`/friend/accept/${id}`);
  },
  reject(id) {
    return http.post(`/friend/reject/${id}`);
  },
  requests() {
    return http.get("/friend/requests");
  },
  remove(friendId) {
    return http.delete(`/friend/${friendId}`);
  }
};

// 聊天会话 API
export const chatApi = {
  /** 创建/恢复聊天会话（传入对方 uid） */
  createChat(uid) { return http.get(`/msg/chat/create/${uid}`); },
  /** 获取最近聊天列表（带用户信息和最近消息），offset=已加载条数 */
  recentList(offset = 0) { return http.get("/msg/chat/recent-list", { params: { offset } }); },
  /** 删除聊天会话 */
  deleteChat(uid) { return http.delete(`/msg/chat/delete/${uid}`); },
  /** 进入聊天窗口：标记在线 + 清除未读 */
  online(fromUid) { return http.get("/msg/chat/online", { params: { from: fromUid } }); },
  /** 离开聊天窗口：清除在线状态（不需要鉴权，页面卸载时调用） */
  outline(fromUid, toUid) { return http.get("/msg/chat/outline", { params: { from: fromUid, to: toUid } }); },
};

// 聊天消息详情 API
export const chatDetailedApi = {
  getMore(uid, offset = 0) { return http.get("/msg/chat-detailed/get-more", { params: { uid, offset } }); },
  deleteMsg(id) { return http.delete("/msg/chat-detailed/delete", { params: { id } }); },
  /** 上传聊天文件/图片，返回文件访问 URL */
  uploadFile(file) {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/msg/chat-detailed/upload", formData);
  }
};

// 课题类型 API
export const projectTypeApi = {
  list(params) {
    return http.get("/projectType", { params });
  },
  enabled() {
    return http.get("/projectType/enabled");
  },
  create(data) {
    return http.post("/projectType", data);
  },
  update(data) {
    return http.post("/projectType/update", data);
  },
  remove(ids) {
    return http.delete("/projectType", { data: ids });
  }
};

// AI 辅助写作 API
export const aiApi = {
  generateDescription(title, type) {
    return http.post("/ai/generate/description", null, { params: { title, type } });
  },
  polish(content) {
    return http.post("/ai/polish", null, { params: { content } });
  },
  expand(keywords) {
    return http.post("/ai/expand", null, { params: { keywords } });
  }
};

import axios from "axios";
import {ElNotification} from "element-plus";

const http = axios.create({
    baseURL: "/api",
    timeout: 15000
});

http.interceptors.request.use((config) => {
    const token = localStorage.getItem("pms_token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

http.interceptors.response.use(
    (response) => {
        // blob 类型直接返回完整 response，让调用方处理
        if (response.config.responseType === 'blob') {
            return response;
        }
        return response.data;
    },
    (error) => {
        // 下载接口失败时直接返回错误，不弹全局通知（让调用方处理）
        if (error?.config?.responseType === 'blob') {
            return Promise.reject(error);
        }
        const message =
            error?.response?.data?.msg ||
            error?.response?.headers?.message ||
            error?.message ||
            "请求失败";

        ElNotification({title: "失败", message, type: "error"})

        if (error?.response?.status === 403) {
            localStorage.removeItem("pms_token");
            localStorage.removeItem("pms_user");
            if (window.location.pathname !== "/login") {
                window.location.href = "/login";
            }
        }

        return Promise.reject(new Error(message));
    }
);

/**
 * 下载文件（返回 blob）
 */
http.download = (url, params, fileName) => {
    return http.get(url, {
        params,
        responseType: 'blob'
    }).then(response => {
        // responseType 为 blob 时，axios 返回完整 response 对象
        const blob = response.data;
        // 从 Content-Disposition 获取文件名，如果没有则使用传入的 fileName
        const disposition = response.headers?.['content-disposition'];
        let name = fileName;
        if (disposition) {
            const match = disposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
            if (match && match[1]) {
                // 处理编码的文件名
                name = match[1].replace(/['"]/g, '');
                try {
                    name = decodeURIComponent(name);
                } catch (e) {}
            }
        }
        // 创建下载链接
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = name || 'attachment';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);
    });
};

export default http;

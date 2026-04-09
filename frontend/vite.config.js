import {defineConfig} from "vite";
import vue from "@vitejs/plugin-vue";
import AutoImport from "unplugin-auto-import/vite";
import {ElementPlusResolver} from "unplugin-vue-components/resolvers";
import Components from "unplugin-vue-components/vite";

export default defineConfig({
    plugins: [
        vue(),
        AutoImport({
            resolvers: [ElementPlusResolver()],
        }),
        Components({
            resolvers: [ElementPlusResolver()],
        }),
    ],
    // Vercel 部署时设为 /，本地开发代理到 localhost:9090
    base: process.env.VERCEL ? "/" : "/",
    server: {
        port: 5173,
        host: "0.0.0.0",
        proxy: {
            "/api": {
                target: "http://localhost:9090",
                changeOrigin: true
            }
        }
    },
    define: {
        // 暴露环境变量给前端
        __API_BASE_URL__: JSON.stringify(
            process.env.VITE_API_BASE_URL || "http://localhost:9090"
        ),
    }
});

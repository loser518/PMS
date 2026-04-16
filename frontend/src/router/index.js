import {createRouter, createWebHistory} from "vue-router";
import {useAuthStore} from "../stores/auth";

const routes = [
    {
        path: "/login",
        name: "login",
        component: () => import("../views/LoginView.vue"),
        meta: {public: true}
    },
    {
        path: "/",
        component: () => import("../components/AppShell.vue"),
        children: [
            {path: "", redirect: "/dashboard"},
            {path: "dashboard", name: "dashboard", component: () => import("../views/DashboardView.vue")},
            {path: "users", name: "students", component: () => import("../views/UsersView.vue"), meta: {roles: [1, 2]}},
            {path: "projects", name: "projects", component: () => import("../views/ProjectsView.vue")},
            {
                path: "project-comments",
                name: "project-comments",
                component: () => import("../views/ProjectCommentsView.vue")
            },
            {path: "progress", name: "progress", component: () => import("../views/ProgressView.vue")},
            {path: "project-types", name: "project-types", component: () => import("../views/ProjectTypeView.vue")},
            {path: "announcements", name: "announcements", component: () => import("../views/AnnouncementsView.vue")},
            {
                path: "categories",
                name: "categories",
                component: () => import("../views/CategoriesView.vue"),
                meta: {roles: [2]}
            },
            {path: "chat", name: "chat", component: () => import("../views/ChatView.vue")},
            {path: "topic-market", name: "topic-market", component: () => import("../views/TopicMarketView.vue")},
            {path: "profile", name: "profile", component: () => import("../views/ProfileView.vue")},
            {path: "ai-service", name: "ai-service", component: () => import("../views/AiServiceView.vue")}
        ]
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

router.beforeEach((to) => {
    const authStore = useAuthStore();
    if (to.meta.public) {
        if (authStore.isLoggedIn) return "/dashboard";
        return true;
    }
    if (!authStore.isLoggedIn) return "/login";
    if (to.meta.roles && !to.meta.roles.includes(authStore.role)) return "/dashboard";
    return true;
});

export default router;

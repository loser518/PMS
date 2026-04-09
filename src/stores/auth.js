import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { authApi, userApi } from "../api/modules";

export const useAuthStore = defineStore("auth", () => {
  const token = ref(localStorage.getItem("pms_token") || "");
  const userInfo = ref(JSON.parse(localStorage.getItem("pms_user") || "null"));

  const isLoggedIn = computed(() => Boolean(token.value));
  const role = computed(() => userInfo.value?.user?.role ?? null);
  const userId = computed(() => userInfo.value?.user?.id ?? null);

  function persist() {
    if (token.value) {
      localStorage.setItem("pms_token", token.value);
    } else {
      localStorage.removeItem("pms_token");
    }

    if (userInfo.value) {
      localStorage.setItem("pms_user", JSON.stringify(userInfo.value));
    } else {
      localStorage.removeItem("pms_user");
    }
  }

  async function login(payload) {
    const res = await authApi.login(payload);
    if (res.status !== "success") {
      throw new Error(res.msg || "µÇÂ¼Ê§°Ü");
    }
    token.value = res.data.token;
    userInfo.value = res.data.userInfo;
    persist();
  }

  async function logout() {
    try {
      await authApi.logout();
    } finally {
      token.value = "";
      userInfo.value = null;
      persist();
    }
  }

  async function refreshProfile() {
    if (!userId.value) {
      return;
    }
    const res = await userApi.detail(userId.value);
    userInfo.value = res.data;
    persist();
  }

  function updateLocalUser(nextUserInfo) {
    userInfo.value = nextUserInfo;
    persist();
  }

  return {
    token,
    userInfo,
    role,
    userId,
    isLoggedIn,
    login,
    logout,
    refreshProfile,
    updateLocalUser
  };
});

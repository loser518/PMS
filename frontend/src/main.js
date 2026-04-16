// main.js
import {createApp} from 'vue'
import App from './App.vue'
import router from './router'
import {createPinia} from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './styles/base.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import VueCropper from 'vue-cropper';
import 'vue-cropper/dist/index.css'

const app = createApp(App)

app.use(VueCropper)
app.use(ElementPlus, {locale: zhCn})
app.use(createPinia())
app.use(router)

app.use(VueCropper)

app.mount('#app')

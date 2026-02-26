import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import {
  ArrowDown,
  Avatar,
  CaretBottom,
  ChatDotRound,
  CircleCheckFilled,
  CircleCloseFilled,
  Clock,
  Close,
  Collection,
  Connection,
  DataAnalysis,
  DataLine,
  Delete,
  Document,
  Download,
  EditPen,
  Files,
  FolderOpened,
  Histogram,
  House,
  Key,
  Lightning,
  Link,
  List,
  Loading,
  MoreFilled,
  OfficeBuilding,
  Operation,
  Paperclip,
  PieChart,
  Plus,
  Refresh,
  RefreshRight,
  Search,
  SetUp,
  Setting,
  SwitchButton,
  Tickets,
  Timer,
  Tools,
  Top,
  TrendCharts,
  User,
  VideoPause,
  VideoPlay
} from '@element-plus/icons-vue'
import router from './router'
import App from './App.vue'
import './assets/main.css'
import './styles/gemini.scss'

const app = createApp(App)

const iconComponents = {
  ArrowDown,
  Avatar,
  CaretBottom,
  ChatDotRound,
  CircleCheckFilled,
  CircleCloseFilled,
  Clock,
  Close,
  Collection,
  Connection,
  DataAnalysis,
  DataLine,
  Delete,
  Document,
  Download,
  EditPen,
  Files,
  FolderOpened,
  Histogram,
  House,
  Key,
  Lightning,
  Link,
  List,
  Loading,
  MoreFilled,
  OfficeBuilding,
  Operation,
  Paperclip,
  PieChart,
  Plus,
  Refresh,
  RefreshRight,
  Search,
  SetUp,
  Setting,
  SwitchButton,
  Tickets,
  Timer,
  Tools,
  Top,
  TrendCharts,
  User,
  VideoPause,
  VideoPlay
}

for (const [key, component] of Object.entries(iconComponents)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')

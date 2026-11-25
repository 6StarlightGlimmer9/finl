<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Bell, ChevronRight, Heart, LogIn, LogOut, Moon, Play, Search, ShieldCheck, Star, Sun, UserRound, X } from 'lucide-vue-next'
import AnimeDetailView from './views/AnimeDetailView.vue'
import AnimeListView from './views/AnimeListView.vue'
import CommunityView from './views/CommunityView.vue'
import ReviewsView from './views/ReviewsView.vue'
import ProfileView from './views/ProfileView.vue'
import AdminView from './views/AdminView.vue'
import SearchView from './views/SearchView.vue'
import WatchlistView from './views/WatchlistView.vue'
import { apiFetch, apiJson, clearStoredUser, getStoredUser, setStoredUser } from './api'

const currentPage = ref('首页')
const isLoginOpen = ref(false)
const isRegisterMode = ref(false)
const isLoggedIn = ref(false)
const isProfileMenuOpen = ref(false)
const currentUser = ref({ username: '放映室用户', role: 'USER' })
const loginUsername = ref('')
const loginPassword = ref('')
const registerEmail = ref('')
const loginError = ref('')
const loginLoading = ref(false)
const searchText = ref('')
const searchInput = ref(null)
const activeCategory = ref('番剧')
const activeSlide = ref(0)
const homeAnime = ref([])
const theme = ref(localStorage.getItem('screenroom-theme') || 'light')
const toast = ref(null)
let toastTimer = null
const navItems = ['首页', '番剧', '电影', '漫评', '社群', '片单']
const categories = ['番剧', '电影', '奇幻', '战斗', '青春', '恋爱', '治愈', '热血', '科幻', '悬疑', '喜剧', '日常', '音乐', '运动']
const featuredSlides = ref([])
const featured = computed(() => featuredSlides.value[activeSlide.value] || null)
const videos = ref([])
const posters = computed(() => homeAnime.value.slice(0, 5).map((item, index) => ({
  title: item.title,
  label: (item.genres || [])[0] || (index === 0 ? '本季热播' : '高分推荐'),
  image: item.coverUrl,
  id: item.id
})))
const rankAnime = computed(() => [...homeAnime.value].sort((a, b) => Number(b.score) - Number(a.score)).slice(0, 5))

let carouselTimer = null

function navigate(page) {
  if (page === '片单' && !getStoredUser()) {
    window.dispatchEvent(new CustomEvent('screenroom:login-required', { detail: { message: '登录后可管理我的追番片单' } }))
    return
  }
  currentPage.value = page
  window.location.hash = { 首页: '/', 番剧: '/anime', 电影: '/anime?type=movie', 漫评: '/reviews', 社群: '/community', 片单: '/watchlist' }[page] || '/'
}
function navigateCategory(category) {
  activeCategory.value = category
  if (category === '番剧') window.location.hash = '/anime'
  else if (category === '电影') window.location.hash = '/anime?type=movie'
  else window.location.hash = `/anime?category=${encodeURIComponent(category)}`
}
function openDetail(id) { window.location.hash = `/detail?id=${id}` }
function submitSearch() { if (searchText.value.trim()) window.location.hash = `/search?q=${encodeURIComponent(searchText.value.trim())}` }
function selectSlide(index) {
  if (!featuredSlides.value.length) return
  activeSlide.value = index % featuredSlides.value.length
}
function syncPage() {
  const hashPath = window.location.hash.replace('#', '') || '/'
  const [path, query] = hashPath.split('?')
  const params = new URLSearchParams(query || '')
  currentPage.value = path === '/anime' ? (params.get('type') === 'movie' ? '电影' : '番剧')
    : path === '/detail' ? '详情' : path === '/reviews' ? '漫评' : path === '/community' ? '社群'
      : path === '/profile' ? '个人中心' : path === '/admin' ? '管理员后台' : path === '/search' ? '搜索'
        : path === '/watchlist' ? '片单' : '首页'
  if (path === '/anime') activeCategory.value = params.get('category') || (params.get('type') === 'movie' ? '电影' : '番剧')
  if (path === '/') activeCategory.value = '番剧'
  const storedUser = getStoredUser()
  if (currentPage.value === '管理员后台' && storedUser?.role !== 'ADMIN') currentPage.value = '首页'
  if (['个人中心', '片单'].includes(currentPage.value) && !storedUser) currentPage.value = '首页'
}
async function loadHomeData() {
  try {
    const list = await apiJson('/api/anime')
    if (!Array.isArray(list) || !list.length) return
    homeAnime.value = list
    featuredSlides.value = list.slice(0, 3).map((item, index) => ({
      title: item.title,
      desc: item.summary || '把值得看的故事，放映给懂的人。',
      image: item.coverUrl,
      id: item.id,
      score: item.score,
      scoreCount: item.scoreCount,
      genres: item.genres || [],
      label: index === 0 ? 'NOW SCREENING / 真实评分榜首' : index === 1 ? 'HOT ANIME / 同好热议' : 'MOVIE / 即将登场'
    }))
    videos.value = list.slice(0, 9).map(item => ({
      title: item.title,
      meta: `${item.season || '放映室'} · ${Number(item.scoreCount || 0).toLocaleString()} 人真实评分`,
      image: item.coverUrl,
      id: item.id
    }))
  } catch {
    // 网络异常时保留页面内默认推荐内容
  }
}
async function login() {
  if (!loginUsername.value.trim() || !loginPassword.value) { loginError.value = '请输入账号和密码'; return }
  loginLoading.value = true
  loginError.value = ''
  try {
    const result = await apiJson('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username: loginUsername.value.trim(), password: loginPassword.value })
    })
    setStoredUser(result)
    currentUser.value = result
    isLoggedIn.value = true
    isLoginOpen.value = false
  } catch (error) { loginError.value = error.message } finally { loginLoading.value = false }
}
async function register() {
  if (!loginUsername.value.trim() || !loginPassword.value || !registerEmail.value.trim()) { loginError.value = '请完整填写注册信息'; return }
  loginLoading.value = true
  loginError.value = ''
  try {
    const result = await apiJson('/api/auth/register', {
      method: 'POST',
      body: JSON.stringify({ username: loginUsername.value.trim(), email: registerEmail.value.trim(), password: loginPassword.value })
    })
    setStoredUser(result)
    currentUser.value = result
    isLoggedIn.value = true
    isLoginOpen.value = false
  } catch (error) { loginError.value = error.message } finally { loginLoading.value = false }
}
async function logout() {
  try { await apiFetch('/api/auth/logout', { method: 'POST' }) } catch { /* 忽略退出请求异常 */ }
  clearStoredUser()
  isLoggedIn.value = false
  isProfileMenuOpen.value = false
  currentUser.value = { username: '放映室用户', nickname: '放映室用户', role: 'USER' }
  navigate('首页')
}
function handleAuthExpired() {
  isLoggedIn.value = false
  currentUser.value = { username: '放映室用户', nickname: '放映室用户', role: 'USER' }
  if (currentPage.value === '管理员后台' || currentPage.value === '个人中心') navigate('首页')
}
function handleLoginRequired(event) {
  isRegisterMode.value = false
  loginError.value = event.detail?.message || '请先登录后再操作'
  isLoginOpen.value = true
}
function openProfile() { isProfileMenuOpen.value = false; window.location.hash = '/profile' }
function openAdmin() { isProfileMenuOpen.value = false; window.location.hash = '/admin' }
function toggleTheme() {
  theme.value = theme.value === 'dark' ? 'light' : 'dark'
  localStorage.setItem('screenroom-theme', theme.value)
  document.documentElement.dataset.theme = theme.value
}
function handleToast(event) {
  toast.value = { message: event.detail?.message || '', type: event.detail?.type || 'success' }
  window.clearTimeout(toastTimer)
  toastTimer = window.setTimeout(() => { toast.value = null }, 2800)
}
function handleShortcut(event) {
  if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
    event.preventDefault()
    searchInput.value?.focus()
  }
}
onMounted(async () => {
  document.documentElement.dataset.theme = theme.value
  syncPage()
  const storedUser = getStoredUser()
  if (storedUser?.token) {
    currentUser.value = storedUser
    isLoggedIn.value = true
    try {
      const me = await apiJson('/api/auth/me')
      setStoredUser({ ...storedUser, ...me })
      currentUser.value = { ...storedUser, ...me }
      syncPage()
    } catch { /* apiFetch 已处理过期登录 */ }
  }
  loadHomeData()
  window.addEventListener('hashchange', syncPage)
  window.addEventListener('screenroom:auth-expired', handleAuthExpired)
  window.addEventListener('screenroom:login-required', handleLoginRequired)
  window.addEventListener('screenroom:toast', handleToast)
  window.addEventListener('keydown', handleShortcut)
  carouselTimer = window.setInterval(() => selectSlide(activeSlide.value + 1), 6000)
})
onUnmounted(() => {
  window.clearInterval(carouselTimer)
  window.clearTimeout(toastTimer)
  window.removeEventListener('hashchange', syncPage)
  window.removeEventListener('screenroom:auth-expired', handleAuthExpired)
  window.removeEventListener('screenroom:login-required', handleLoginRequired)
  window.removeEventListener('screenroom:toast', handleToast)
  window.removeEventListener('keydown', handleShortcut)
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <button class="brand" @click="navigate('首页')"><span class="brand-mark">✿</span><span>次元<span class="brand-dark">放映室</span></span></button>
      <nav class="main-nav"><button v-for="item in navItems" :key="item" :class="{ active: currentPage === item }" @click="navigate(item)">{{ item }}</button></nav>
      <div class="search"><Search :size="18" /><input ref="searchInput" v-model="searchText" placeholder="搜索番剧、电影、漫评" @keyup.enter="submitSearch" /><kbd>⌘ K</kbd></div>
      <div class="header-actions">
        <button class="icon-button" :title="theme === 'dark' ? '切换浅色主题' : '切换深色主题'" @click="toggleTheme"><Sun v-if="theme === 'dark'" :size="18" /><Moon v-else :size="18" /></button>
        <button class="icon-button" title="通知"><Bell :size="19" /><i></i></button>
        <button v-if="!isLoggedIn" class="login-button" @click="isRegisterMode = false; isLoginOpen = true"><LogIn :size="16" /> 登录</button>
        <div v-else class="avatar-wrap" @mouseenter="isProfileMenuOpen = true" @mouseleave="isProfileMenuOpen = false">
          <button class="avatar" title="个人菜单">{{ (currentUser.nickname || currentUser.username).slice(0, 1) }}</button>
          <div v-if="isProfileMenuOpen" class="profile-menu">
            <div class="menu-user"><b>{{ currentUser.nickname || currentUser.username }}</b><small>{{ currentUser.role === 'ADMIN' ? '管理员 · 全站管理权限' : '放映室用户 · 真实评分成员' }}</small></div>
            <button @click="openProfile"><UserRound :size="15" /> 个人中心 <ChevronRight :size="15" /></button>
            <button v-if="currentUser.role === 'ADMIN'" @click="openAdmin"><ShieldCheck :size="15" /> 管理员后台 <ChevronRight :size="15" /></button>
            <hr />
            <button @click="logout"><LogOut :size="15" /> 退出登录</button>
          </div>
        </div>
      </div>
    </header>

    <main v-if="currentPage === '首页'" class="home-page">
      <div class="channel-bar wrap"><button v-for="category in categories" :key="category" :class="{ active: activeCategory === category }" @click="navigateCategory(category)">{{ category }}</button></div>
      <section class="home-hero wrap">
        <div class="hero-image">
          <template v-if="featured">
            <Transition name="fade" mode="out-in"><img :key="featured.image" :src="featured.image" :alt="featured.title" /></Transition>
            <div class="hero-shade"></div>
            <div class="hero-copy">
              <span class="section-kicker">{{ featured.label }}</span>
              <h1>{{ featured.title }}</h1>
              <div class="hero-badges"><span><Star :size="13" fill="currentColor" /> {{ Number(featured.score || 0).toFixed(1) }} 真实评分</span><span v-if="featured.genres?.length">{{ featured.genres[0] }}</span><span>{{ Number(featured.scoreCount || 0).toLocaleString() }} 人参与</span></div>
              <p>{{ featured.desc }}</p>
              <button class="primary-button" @click="openDetail(featured.id)"><Play :size="16" fill="currentColor" /> 查看详情</button>
              <div class="carousel-dots"><button v-for="(slide, index) in featuredSlides" :key="slide.title" :class="{ active: activeSlide === index }" :aria-label="`切换到 ${slide.title}`" @click.stop="selectSlide(index)"></button></div>
            </div>
          </template>
          <div v-else class="hero-copy"><span class="section-kicker">LOADING</span><h1>正在放映真实片单...</h1></div>
        </div>
        <aside class="hero-side">
          <div class="hero-side-top"><span>本周热门</span><b>TOP 3</b></div>
          <h2>把热爱放映给懂的人看</h2>
          <p>每一次评分都来自真实用户，加入一场认真讨论。</p>
          <div class="hero-mini-rank">
            <button v-for="(item, index) in rankAnime.slice(0, 3)" :key="item.id" @click="openDetail(item.id)">
              <b>{{ String(index + 1).padStart(2, '0') }}</b>
              <img :src="item.coverUrl" :alt="item.title" />
              <span>{{ item.title }}</span>
              <small>{{ Number(item.score).toFixed(1) }}</small>
            </button>
          </div>
          <button class="text-link" @click="navigate('社群')">进入同好社群 <ChevronRight :size="15" /></button>
        </aside>
      </section>
      <section class="content wrap">
        <div class="section-heading"><div><span class="section-kicker">RECOMMENDED</span><h2>为你推荐</h2></div><button class="more-link" @click="navigate('番剧')">查看更多 <ChevronRight :size="16" /></button></div>
        <div class="video-grid"><article v-for="(video, index) in videos" :key="`${video.title}-${index}`" class="video-card reveal-up" :class="{ 'delay-1': index % 3 === 1, 'delay-2': index % 3 === 2 }" @click="openDetail(video.id)"><div class="video-cover"><img :src="video.image" :alt="video.title" /><span class="duration">番剧推荐</span><button class="play-button"><Play :size="17" fill="currentColor" /></button></div><h3>{{ video.title }}</h3><p>{{ video.meta }}</p></article></div>
      </section>
      <section class="home-rank wrap">
        <div class="rank-card"><span class="section-kicker">REAL USER RATING / TOP 5</span><h2>番剧真实评分榜</h2><div v-for="(item, index) in rankAnime" :key="item.id" class="home-rank-row reveal-up" :class="{ 'delay-1': index === 1, 'delay-2': index === 2 }" @click="openDetail(item.id)"><b>{{ String(index + 1).padStart(2, '0') }}</b><img :src="item.coverUrl" :alt="item.title" /><div><strong>{{ item.title }}</strong><small>{{ item.season }} · {{ Number(item.scoreCount).toLocaleString() }} 人评分</small></div><span><Star :size="15" fill="currentColor" />{{ Number(item.score).toFixed(1) }}</span></div></div>
        <aside class="rank-side"><div><span class="section-kicker">SCORE WITH CONVICTION</span><h2>真实评分，<br />看得见的依据。</h2><p>站内分数由 <strong>ratings 表</strong>中的用户评分实时聚合，修改评分后分数与人数同步更新，不展示虚构数据。</p></div><button class="text-link" @click="navigate('番剧')">进入番剧索引 <ChevronRight :size="15" /></button></aside>
      </section>
      <section class="poster-section wrap">
        <div class="section-heading"><div><span class="section-kicker">VISUAL INDEX</span><h2>本周海报墙</h2></div><button class="more-link" @click="navigate('番剧')">进入番剧库 <ChevronRight :size="16" /></button></div>
        <div class="poster-grid"><article v-for="(poster, index) in posters" :key="poster.title" class="poster-card reveal-up" :class="{ 'delay-1': index % 2 === 1 }" @click="openDetail(poster.id)"><img :src="poster.image" :alt="poster.title" /><div><span>{{ poster.label }}</span><h3>{{ poster.title }}</h3></div></article></div>
      </section>
      <section class="topic-strip wrap"><div><span class="section-kicker">WATCHING TOGETHER</span><h2>正在放映的故事，也正在被讨论</h2><p>去详情页查看 PV、评分分布和真实评论。</p></div><div class="topic-actions"><button @click="openDetail(1)"><Star :size="16" /> 评分热榜</button><button @click="navigate('漫评')"><Heart :size="16" /> 精选漫评</button></div></section>
    </main>
    <main v-else>
      <AnimeListView v-if="currentPage === '番剧' || currentPage === '电影'" />
      <AnimeDetailView v-else-if="currentPage === '详情'" />
      <ReviewsView v-else-if="currentPage === '漫评'" />
      <CommunityView v-else-if="currentPage === '社群'" />
      <ProfileView v-else-if="currentPage === '个人中心'" @logout="logout" />
      <AdminView v-else-if="currentPage === '管理员后台'" />
      <SearchView v-else-if="currentPage === '搜索'" />
      <WatchlistView v-else-if="currentPage === '片单'" />
    </main>
    <footer><div class="wrap"><span class="brand-mark">✿</span> 次元放映室 <span>让每一次观看，都遇见同频的人。</span><small>© 2026 DIMENSION SCREENROOM · 所有评分均来自真实用户</small></div></footer>
    <Transition name="toast"><div v-if="toast" class="app-toast" :class="toast.type">{{ toast.message }}</div></Transition>
    <div v-if="isLoginOpen" class="modal-backdrop" @click.self="isLoginOpen = false">
      <div class="login-modal">
        <button class="close-button" @click="isLoginOpen = false"><X :size="18" /></button>
        <span class="section-kicker">{{ isRegisterMode ? 'JOIN SCREENROOM' : 'WELCOME BACK' }}</span>
        <h2>{{ isRegisterMode ? '注册放映室' : '登录放映室' }}</h2>
        <p>{{ isRegisterMode ? '创建账号，加入 ACGN 同好讨论。' : '加入讨论，记录你的每一次心动。' }}</p>
        <label>用户名<input v-model="loginUsername" placeholder="输入用户名" @keyup.enter="isRegisterMode ? register() : login()" /></label>
        <label v-if="isRegisterMode">邮箱<input v-model="registerEmail" type="email" placeholder="输入邮箱" @keyup.enter="register" /></label>
        <label>密码<input v-model="loginPassword" type="password" placeholder="输入密码" @keyup.enter="isRegisterMode ? register() : login()" /></label>
        <p v-if="loginError" class="form-error">{{ loginError }}</p>
        <button class="primary-button full" :disabled="loginLoading" @click="isRegisterMode ? register() : login()">{{ loginLoading ? '处理中...' : (isRegisterMode ? '注册并进入' : '登录并开始') }}</button>
        <button class="switch-auth" @click="isRegisterMode = !isRegisterMode; loginError = ''">{{ isRegisterMode ? '已有账号？返回登录' : '没有账号？立即注册' }}</button>
        <button class="social-login" @click="isLoginOpen = false">先逛一逛</button>
      </div>
    </div>
  </div>
</template>

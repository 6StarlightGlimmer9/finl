<script setup>
import { onMounted, ref } from 'vue'
import { Bell, ChevronRight, Heart, LogIn, LogOut, Play, Search, Star, UserRound, X } from 'lucide-vue-next'
import AnimeDetailView from './views/AnimeDetailView.vue'
import AnimeListView from './views/AnimeListView.vue'
import CommunityView from './views/CommunityView.vue'
import ReviewsView from './views/ReviewsView.vue'
import ProfileView from './views/ProfileView.vue'
import AdminView from './views/AdminView.vue'
import SearchView from './views/SearchView.vue'

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
const activeCategory = ref('番剧')
const activeSlide = ref(0)
const navItems = ['首页', '番剧', '电影', '漫评', '社群']
const categories = ['番剧', '电影', '国创', '综艺', '纪录片', '音乐', '舞蹈', '游戏', '知识', '生活', '影视', '资讯']
const featuredSlides = [
  { title: '胆大党 第二季', desc: '相信不可思议，也相信自己。这个夏天，一起追一场失控却耀眼的青春冒险。', image: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=1400&q=85', id: 1, label: 'SUMMER ANIME / NOW SCREENING' },
  { title: '薰香花朵凛然绽放', desc: '温柔的日常也值得被认真记录，在夏日里遇见彼此的勇气。', image: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=1400&q=85', id: 2, label: 'YOUTH STORY / NEW EPISODE' },
  { title: '链锯人：剧场版', desc: '当热血与命运再次相撞，剧场版的第一束光已经亮起。', image: 'https://images.unsplash.com/photo-1541562232579-512a21360020?auto=format&fit=crop&w=1400&q=85', id: 3, label: 'MOVIE PREMIERE / COMING SOON' }
]
const featured = ref(featuredSlides[0])
const videos = [
  { title: '胆大党 第二季：本季追番指南', meta: '次元编辑部 · 12.8万播放', image: 'https://images.unsplash.com/photo-1541562232579-512a21360020?auto=format&fit=crop&w=800&q=80', id: 1 },
  { title: '薰香花朵凛然绽放，温柔感正在发生', meta: '夏日放映室 · 8.6万播放', image: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80', id: 2 },
  { title: '新番作画观察：一帧一帧看细节', meta: '胶片研究所 · 6.2万播放', image: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=800&q=80', id: 1 },
  { title: '今晚一起看番吗？放映室直播预告', meta: '同好社群 · 4.9万播放', image: 'https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=800&q=80', id: 2 },
  { title: '链锯人剧场版，期待值拉满', meta: '漫改观察员 · 3.7万播放', image: 'https://images.unsplash.com/photo-1545987796-200677ee1011?auto=format&fit=crop&w=800&q=80', id: 1 },
  { title: '从分镜看一部青春群像剧', meta: '动画研究社 · 2.4万播放', image: 'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=800&q=80', id: 2 },
  { title: '本季高分番剧一次看懂', meta: '放映室榜单 · 1.8万播放', image: 'https://images.unsplash.com/photo-1485846234645-a62644f84728?auto=format&fit=crop&w=800&q=80', id: 1 },
  { title: '治愈系作品的夏日气味', meta: '夏日存档 · 1.2万播放', image: 'https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=800&q=80', id: 2 },
  { title: '剧场版入场前必看的三件事', meta: '电影放映厅 · 9800播放', image: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=800&q=80', id: 3 }
]
const posters = [
  { title: '胆大党 第二季', label: '本季热播', image: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=600&q=85', id: 1 },
  { title: '薰香花朵凛然绽放', label: '青春恋爱', image: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=85', id: 2 },
  { title: '链锯人：剧场版', label: '剧场版', image: 'https://images.unsplash.com/photo-1541562232579-512a21360020?auto=format&fit=crop&w=600&q=85', id: 3 },
  { title: 'Summer Pockets', label: '夏日物语', image: 'https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=600&q=85', id: 4 },
  { title: '葬送的芙莉莲', label: '高分推荐', image: 'https://images.unsplash.com/photo-1534447677768-be436bb09401?auto=format&fit=crop&w=600&q=85', id: 6 }
]

function navigate(page) {
  currentPage.value = page
  window.location.hash = { 首页: '/', 番剧: '/anime', 电影: '/anime?type=movie', 漫评: '/reviews', 社群: '/community' }[page]
}
function openDetail(id) { window.location.hash = `/detail?id=${id}` }
function submitSearch() { if (searchText.value.trim()) window.location.hash = `/search?q=${encodeURIComponent(searchText.value.trim())}` }
function selectSlide(index) { activeSlide.value = index; featured.value = featuredSlides[index] }
function syncPage() {
  const hashPath = window.location.hash.replace('#', '') || '/'
  const [path, query] = hashPath.split('?')
  const params = new URLSearchParams(query || '')
  currentPage.value = path === '/anime' ? (params.get('type') === 'movie' ? '电影' : '番剧') : path === '/detail' ? '详情' : path === '/reviews' ? '漫评' : path === '/community' ? '社群' : path === '/profile' ? '个人中心' : path === '/admin' ? '管理员后台' : path === '/search' ? '搜索' : '首页'
  const storedUser = JSON.parse(localStorage.getItem('screenroom-user') || 'null')
  if (currentPage.value === '管理员后台' && storedUser?.role !== 'ADMIN') currentPage.value = '首页'
  if (currentPage.value === '个人中心' && !storedUser) currentPage.value = '首页'
}
async function login() {
  if (!loginUsername.value.trim() || !loginPassword.value) { loginError.value = '请输入账号和密码'; return }
  loginLoading.value = true
  loginError.value = ''
  try {
    const response = await fetch('/api/auth/login', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: loginUsername.value.trim(), password: loginPassword.value }) })
    const result = await response.json()
    if (!response.ok) throw new Error(result.message || '登录失败')
    localStorage.setItem('screenroom-user', JSON.stringify(result))
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
    const response = await fetch('/api/auth/register', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username: loginUsername.value.trim(), email: registerEmail.value.trim(), password: loginPassword.value }) })
    const result = await response.json()
    if (!response.ok) throw new Error(result.message || '注册失败')
    localStorage.setItem('screenroom-user', JSON.stringify(result)); currentUser.value = result; isLoggedIn.value = true; isLoginOpen.value = false
  } catch (error) { loginError.value = error.message } finally { loginLoading.value = false }
}
function logout() { localStorage.removeItem('screenroom-user'); isLoggedIn.value = false; isProfileMenuOpen.value = false; navigate('首页') }
function openProfile() { isProfileMenuOpen.value = false; window.location.hash = '/profile' }
function openAdmin() { isProfileMenuOpen.value = false; window.location.hash = '/admin' }
onMounted(() => { syncPage(); const storedUser = JSON.parse(localStorage.getItem('screenroom-user') || 'null'); currentUser.value = storedUser || currentUser.value; isLoggedIn.value = Boolean(storedUser); window.addEventListener('hashchange', syncPage); window.setInterval(() => selectSlide((activeSlide.value + 1) % featuredSlides.length), 6000) })
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <button class="brand" @click="navigate('首页')"><span class="brand-mark">✿</span><span>次元<span class="brand-dark">放映室</span></span></button>
      <nav class="main-nav"><button v-for="item in navItems" :key="item" :class="{ active: currentPage === item }" @click="navigate(item)">{{ item }}</button></nav>
      <div class="search"><Search :size="18" /><input v-model="searchText" placeholder="搜索番剧、电影、漫评" @keyup.enter="submitSearch" /><kbd>⌘ K</kbd></div>
      <div class="header-actions"><button class="icon-button" title="通知"><Bell :size="19" /></button><button v-if="!isLoggedIn" class="login-button" @click="isRegisterMode = false; isLoginOpen = true"><LogIn :size="16" /> 登录</button><div v-else class="avatar-wrap" @mouseenter="isProfileMenuOpen = true" @mouseleave="isProfileMenuOpen = false"><button class="avatar" title="个人菜单">{{ currentUser.username.slice(0, 1) }}</button><div v-if="isProfileMenuOpen" class="profile-menu"><div class="menu-user"><b>{{ currentUser.username }}</b><small>硬币：128　关注：12</small></div><button @click="openProfile">个人中心 <ChevronRight :size="15" /></button><button>投稿管理 <ChevronRight :size="15" /></button><button v-if="currentUser.role === 'ADMIN'" @click="openAdmin">管理员后台 <ChevronRight :size="15" /></button><hr /><button @click="logout"><LogOut :size="15" /> 退出登录</button></div></div></div>
    </header>

    <main v-if="currentPage === '首页'" class="home-page">
      <section class="home-hero wrap"><div class="hero-image"><img :src="featured.image" :alt="featured.title" /><div class="hero-shade"></div><div class="hero-copy"><span class="section-kicker">{{ featured.label }}</span><h1>{{ featured.title }}</h1><p>{{ featured.desc }}</p><button class="primary-button" @click="openDetail(featured.id)"><Play :size="16" fill="currentColor" /> 查看详情</button><div class="carousel-dots"><button v-for="(slide, index) in featuredSlides" :key="slide.title" :class="{ active: activeSlide === index }" :aria-label="`切换到 ${slide.title}`" @click.stop="selectSlide(index)"></button></div></div></div><aside class="hero-side"><span>本周热门</span><h2>把热爱放映给懂的人看</h2><p>记录每一次观看，加入每一场认真讨论。</p><button class="text-link" @click="navigate('社群')">进入同好社群 <ChevronRight :size="15" /></button></aside></section>
      <section class="content wrap"><div class="section-heading"><div><span class="section-kicker">RECOMMENDED</span><h2>为你推荐</h2></div><button class="more-link" @click="navigate('番剧')">查看更多 <ChevronRight :size="16" /></button></div><div class="video-grid"><article v-for="video in videos" :key="video.title" class="video-card" @click="openDetail(video.id)"><div class="video-cover"><img :src="video.image" :alt="video.title" /><span class="duration">番剧推荐</span><button class="play-button"><Play :size="17" fill="currentColor" /></button></div><h3>{{ video.title }}</h3><p>{{ video.meta }}</p></article></div></section>
      <section class="poster-section wrap"><div class="section-heading"><div><span class="section-kicker">VISUAL INDEX</span><h2>本周海报墙</h2></div><button class="more-link" @click="navigate('番剧')">进入番剧库 <ChevronRight :size="16" /></button></div><div class="poster-grid"><article v-for="poster in posters" :key="poster.title" class="poster-card" @click="openDetail(poster.id)"><img :src="poster.image" :alt="poster.title" /><div><span>{{ poster.label }}</span><h3>{{ poster.title }}</h3></div></article></div></section>
      <section class="topic-strip wrap"><div><span class="section-kicker">WATCHING TOGETHER</span><h2>正在放映的故事，也正在被讨论</h2><p>去详情页查看 PV、评分和真实评论。</p></div><div class="topic-actions"><button @click="openDetail(1)"><Star :size="16" /> 评分热榜</button><button @click="navigate('漫评')"><Heart :size="16" /> 精选漫评</button></div></section>
    </main>
    <main v-else><AnimeListView v-if="currentPage === '番剧' || currentPage === '电影'" /><AnimeDetailView v-else-if="currentPage === '详情'" /><ReviewsView v-else-if="currentPage === '漫评'" /><CommunityView v-else-if="currentPage === '社群'" /><ProfileView v-else-if="currentPage === '个人中心'" @logout="logout" /><AdminView v-else-if="currentPage === '管理员后台'" /><SearchView v-else-if="currentPage === '搜索'" /></main>
    <footer><div class="wrap"><span class="brand-mark">✿</span> 次元放映室 <span>让每一次观看，都遇见同频的人。</span><small>© 2026 DIMENSION SCREENROOM</small></div></footer>
    <div v-if="isLoginOpen" class="modal-backdrop" @click.self="isLoginOpen = false"><div class="login-modal"><button class="close-button" @click="isLoginOpen = false"><X :size="18" /></button><span class="section-kicker">{{ isRegisterMode ? 'JOIN SCREENROOM' : 'WELCOME BACK' }}</span><h2>{{ isRegisterMode ? '注册放映室' : '登录放映室' }}</h2><p>{{ isRegisterMode ? '创建账号，加入 ACGN 同好讨论。' : '加入讨论，记录你的每一次心动。' }}</p><label>用户名<input v-model="loginUsername" placeholder="输入用户名" @keyup.enter="isRegisterMode ? register() : login()" /></label><label v-if="isRegisterMode">邮箱<input v-model="registerEmail" type="email" placeholder="输入邮箱" @keyup.enter="register" /></label><label>密码<input v-model="loginPassword" type="password" placeholder="输入密码" @keyup.enter="isRegisterMode ? register() : login()" /></label><p v-if="loginError" class="form-error">{{ loginError }}</p><button class="primary-button full" :disabled="loginLoading" @click="isRegisterMode ? register() : login()">{{ loginLoading ? '处理中...' : (isRegisterMode ? '注册并进入' : '登录并开始') }}</button><button class="switch-auth" @click="isRegisterMode = !isRegisterMode; loginError = ''">{{ isRegisterMode ? '已有账号？返回登录' : '没有账号？立即注册' }}</button><button class="social-login" @click="isLoginOpen = false">先逛一逛</button></div></div>
  </div>
</template>

<script setup>
import { BarChart3, Film, Image as ImageIcon, MessageCircle, Pencil, Plus, RotateCcw, Save, ShieldCheck, Trash2, Users } from 'lucide-vue-next'
import { onMounted, reactive, ref } from 'vue'
import { apiJson, getStoredUser } from '../api'

const currentUser = ref(getStoredUser())
const activeTab = ref('overview')
const overview = ref({ users: 0, anime: 0, ratings: 0, comments: 0, averageScore: 0 })
const analytics = ref({ categories: [], statuses: [], topAnime: [], ratingHistogram: new Array(10).fill(0), watchStats: [], activity: [], activeUsers: 0 })
const users = ref([])
const comments = ref([])
const anime = ref([])
const notice = ref('')
const noticeError = ref(false)
const saving = ref(false)
const form = reactive({
  id: null,
  title: '',
  originalTitle: '',
  summary: '',
  coverUrl: '',
  pvUrl: '',
  season: '',
  mediaType: 'ANIME',
  status: 'UPCOMING',
  genres: ''
})

function show(message, error = false) {
  notice.value = message
  noticeError.value = error
}
function formatDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 19) : '—'
}
function barWidth(value, max) {
  return `${max > 0 ? Math.round(Number(value) / max * 100) : 0}%`
}
async function loadAll() {
  try {
    const [overviewData, userData, commentData, animeData, analyticsData] = await Promise.all([
      apiJson('/api/admin/overview'),
      apiJson('/api/admin/users'),
      apiJson('/api/admin/comments'),
      apiJson('/api/anime'),
      apiJson('/api/admin/analytics')
    ])
    overview.value = overviewData
    users.value = userData
    comments.value = commentData
    anime.value = animeData
    analytics.value = analyticsData
  } catch (error) {
    show(error.message, true)
  }
}
function resetForm() {
  Object.assign(form, {
    id: null, title: '', originalTitle: '', summary: '', coverUrl: '', pvUrl: '', season: '',
    mediaType: 'ANIME', status: 'UPCOMING', genres: ''
  })
}
function editAnime(item) {
  Object.assign(form, {
    id: item.id,
    title: item.title,
    originalTitle: item.originalTitle || '',
    summary: item.summary || '',
    coverUrl: item.coverUrl || '',
    pvUrl: item.pvUrl || '',
    season: item.season || '',
    mediaType: item.mediaType,
    status: item.status,
    genres: (item.genres || []).join('、')
  })
  activeTab.value = 'anime'
  show(`正在编辑《${item.title}》，修改封面图片 URL 即可更新海报。`)
}
async function saveAnime() {
  if (!form.title.trim()) { show('作品名称不能为空', true); return }
  saving.value = true
  try {
    const body = {
      title: form.title.trim(),
      originalTitle: form.originalTitle.trim(),
      summary: form.summary.trim(),
      coverUrl: form.coverUrl.trim(),
      pvUrl: form.pvUrl.trim(),
      season: form.season.trim(),
      mediaType: form.mediaType,
      status: form.status,
      genres: form.genres.split(/[,，、\s]+/).filter(Boolean)
    }
    const result = await apiJson(form.id ? `/api/admin/anime/${form.id}` : '/api/admin/anime', {
      method: form.id ? 'PUT' : 'POST',
      body: JSON.stringify(body)
    })
    show(form.id ? `《${result.title}》资料已更新` : `《${result.title}》已发布到番剧库`)
    await loadAll()
    resetForm()
  } catch (error) {
    show(error.message, true)
  } finally {
    saving.value = false
  }
}
async function deleteComment(item) {
  try {
    await apiJson(`/api/admin/comments/${item.id}`, { method: 'DELETE' })
    comments.value = comments.value.filter(comment => comment.id !== item.id)
    overview.value.comments = Math.max(0, Number(overview.value.comments) - 1)
    show('评论已删除')
  } catch (error) { show(error.message, true) }
}
async function removeUser(item) {
  if (item.role === 'ADMIN') { show('管理员账号受保护，不能删除', true); return }
  try {
    await apiJson(`/api/admin/users/${item.userId}`, { method: 'DELETE' })
    users.value = users.value.filter(user => user.userId !== item.userId)
    overview.value.users = Math.max(0, Number(overview.value.users) - 1)
    show('用户已删除')
  } catch (error) { show(error.message, true) }
}
onMounted(() => {
  if (!currentUser.value || currentUser.value.role !== 'ADMIN') {
    window.location.hash = '/'
    return
  }
  loadAll()
})
</script>

<template>
  <section class="page-view wrap admin-page">
    <div class="admin-heading"><div><span class="section-kicker">CONTROL ROOM / ADMIN</span><h1>管理员后台</h1><p>当前登录：{{ currentUser.username }} · 仅管理员账号可进入，所有操作均由服务端校验权限。</p></div><span class="admin-badge"><ShieldCheck :size="16" /> 管理员模式</span></div>
    <div class="admin-tabs">
      <button v-for="tab in [{ key: 'overview', label: '数据概览' }, { key: 'analytics', label: '数据洞察' }, { key: 'comments', label: '评论审核' }, { key: 'anime', label: '作品与图片' }, { key: 'users', label: '用户管理' }]" :key="tab.key" class="admin-tab" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">{{ tab.label }}</button>
    </div>

    <div v-if="activeTab === 'overview'" class="reveal-up">
      <div class="admin-stat-grid">
        <div><span>注册用户</span><b>{{ overview.users }}</b><small>来自 users 表</small></div>
        <div><span>收录作品</span><b>{{ overview.anime }}</b><small>番剧与电影总数</small></div>
        <div><span>真实评分</span><b>{{ overview.ratings }}</b><small>来自 ratings 表</small></div>
        <div><span>全站均分</span><b>{{ Number(overview.averageScore || 0).toFixed(1) }}</b><small>按评分记录实时计算</small></div>
      </div>
      <div class="admin-grid" style="margin-top:18px">
        <section class="admin-panel"><div class="subheading"><h2><MessageCircle :size="17" /> 最新评论</h2><button @click="activeTab = 'comments'">前往审核 →</button></div><p v-if="!comments.length" class="empty-admin">暂无评论</p><div v-for="item in comments.slice(0, 5)" :key="item.id" class="admin-comment"><span><b>{{ item.nickname || item.username }}</b><small>《{{ item.animeTitle }}》· {{ item.content }}</small></span><small>{{ formatDate(item.createdAt) }}</small></div></section>
        <section class="admin-panel"><div class="subheading"><h2><BarChart3 :size="17" /> 评分说明</h2></div><p class="admin-guide">站内展示的分数不再使用预设数字。每条评分都对应一条用户记录，同一用户对同一作品只能保留一条评分，修改或撤销后会自动回算均分与人数。</p><button class="primary-button" style="margin-top:14px" @click="activeTab = 'anime'"><Plus :size="15" /> 发布新动漫</button></section>
      </div>
    </div>

    <div v-else-if="activeTab === 'analytics'" class="reveal-up">
      <div class="analytics-grid">
        <section class="admin-panel"><div class="subheading"><h2><BarChart3 :size="17" /> 分类分布</h2><span>共 {{ analytics.categories.length }} 个标签</span></div><div v-for="item in analytics.categories" :key="item.name" class="analytics-row"><span>{{ item.name }}</span><div><i :style="{ width: barWidth(item.count, Math.max(1, ...analytics.categories.map(row => row.count))) }"></i></div><b>{{ item.count }}</b></div></section>
        <section class="admin-panel"><div class="subheading"><h2>评分分布</h2><span>全站 {{ overview.ratings }} 条评分</span></div><div v-for="(count, index) in analytics.ratingHistogram" :key="index" class="analytics-row"><span>{{ index + 1 }} 分</span><div><i :style="{ width: barWidth(count, Math.max(1, ...analytics.ratingHistogram)) }"></i></div><b>{{ count }}</b></div></section>
        <section class="admin-panel"><div class="subheading"><h2>追番状态</h2><span>用户主动标记</span></div><div v-for="item in analytics.watchStats" :key="item.name" class="analytics-row"><span>{{ { WANT: '想看', WATCHING: '在看', WATCHED: '看过' }[item.name] }}</span><div><i :style="{ width: barWidth(item.count, Math.max(1, ...analytics.watchStats.map(row => row.count))) }"></i></div><b>{{ item.count }}</b></div><p class="admin-guide" style="margin-top:14px">参与评分用户 {{ analytics.activeUsers }} 人，追踪真实参与情况。</p></section>
        <section class="admin-panel"><div class="subheading"><h2>近期活跃</h2><span>最近 7 天</span></div><div v-for="item in analytics.activity" :key="item.date" class="analytics-row compact"><span>{{ item.date.slice(5) }}</span><div><i class="pink" :style="{ width: barWidth(item.comments, Math.max(1, ...analytics.activity.map(row => row.comments))) }" :title="`${item.comments} 条评论`"></i><i class="dark" :style="{ width: barWidth(item.ratings, Math.max(1, ...analytics.activity.map(row => row.ratings))) }" :title="`${item.ratings} 条评分`"></i></div><b>{{ item.comments }}/{{ item.ratings }}</b></div></section>
      </div>
      <section class="admin-panel" style="margin-top:18px"><div class="subheading"><h2>高分作品</h2><span>按真实评分排序</span></div><div class="analytics-top"><article v-for="item in analytics.topAnime" :key="item.animeId"><img :src="item.coverUrl" :alt="item.title" /><div><b>{{ item.title }}</b><small>{{ Number(item.score).toFixed(1) }} 分 · {{ item.scoreCount }} 人</small></div></article></div></section>
    </div>

    <div v-else-if="activeTab === 'comments'" class="admin-grid reveal-up">
      <section class="admin-panel"><div class="subheading"><h2><MessageCircle :size="17" /> 全站评论</h2><button @click="loadAll">刷新</button></div><p v-if="!comments.length" class="empty-admin">暂无评论，用户发表后会出现在这里。</p><TransitionGroup name="comment-list" tag="div"><div v-for="item in comments" :key="item.id" class="admin-comment"><span><b>{{ item.nickname || item.username }}</b><small>《{{ item.animeTitle }}》· {{ item.content }}</small></span><span style="display:flex;align-items:center;gap:10px"><small>{{ formatDate(item.createdAt) }}</small><button title="删除评论" @click="deleteComment(item)"><Trash2 :size="15" /></button></span></div></TransitionGroup></section>
      <aside class="admin-panel"><div class="subheading"><h2>审核规则</h2></div><p class="admin-guide">删除评论会同步从数据库移除记录，并立即从详情页与全站搜索中消失。请确认内容违反社区规范后再执行。</p></aside>
    </div>

    <div v-else-if="activeTab === 'anime'" class="admin-grid reveal-up">
      <section class="admin-panel"><div class="subheading"><h2><Pencil :size="17" /> {{ form.id ? '编辑作品' : '发布新动漫' }}</h2><button v-if="form.id" @click="resetForm"><RotateCcw :size="14" /> 取消编辑</button></div>
        <div class="admin-form">
          <label class="full">作品名称<input v-model="form.title" placeholder="例如：胆大党 第三季" /></label>
          <label>原名<input v-model="form.originalTitle" placeholder="选填" /></label>
          <label>季度 / 年份<input v-model="form.season" placeholder="例如：2026 夏" /></label>
          <label>类型<select v-model="form.mediaType"><option value="ANIME">番剧</option><option value="MOVIE">电影</option></select></label>
          <label>状态<select v-model="form.status"><option value="AIRING">连载中</option><option value="FINISHED">已完结</option><option value="UPCOMING">即将上映</option></select></label>
          <label class="full">简介<textarea v-model="form.summary" rows="3" placeholder="一句话介绍这部作品"></textarea></label>
          <label class="full">封面图片 URL<div class="cover-preview"><img v-if="form.coverUrl" :src="form.coverUrl" alt="封面预览" /><span v-else><ImageIcon :size="18" /> 填入图片地址后显示预览</span></div><input v-model="form.coverUrl" placeholder="https://..." /></label>
          <label class="full">预览地址<input v-model="form.pvUrl" placeholder="Bilibili 视频 / 番剧链接或 BV 号，例如：https://www.bilibili.com/bangumi/play/ss102261" /></label>
          <label class="full">标签<input v-model="form.genres" placeholder="使用逗号或顿号分隔，例如：奇幻、战斗、热血" /></label>
          <div class="admin-form-actions"><button class="primary-button" :disabled="saving" @click="saveAnime"><Save :size="15" /> {{ saving ? '保存中...' : (form.id ? '保存修改' : '发布作品') }}</button><button class="ghost-button" @click="resetForm">清空</button></div>
        </div>
      </section>
      <aside class="admin-panel"><div class="subheading"><h2><Film :size="17" /> 作品列表</h2><span>{{ anime.length }} 部</span></div><p v-if="!anime.length" class="empty-admin">暂无作品</p><div class="admin-list"><div v-for="item in anime" :key="item.id" class="admin-list-row"><img :src="item.coverUrl" :alt="item.title" /><div><b>{{ item.title }}</b><small>{{ item.season }} · {{ Number(item.score).toFixed(1) }} 分 / {{ item.scoreCount }} 人</small></div><button title="编辑作品与图片" @click="editAnime(item)"><Pencil :size="15" /></button></div></div></aside>
    </div>

    <div v-else-if="activeTab === 'users'" class="reveal-up">
      <section class="admin-panel"><div class="subheading"><h2><Users :size="17" /> 注册用户</h2><button @click="loadAll">刷新</button></div><div class="user-table"><div class="user-row user-head"><span>用户</span><span>邮箱</span><span>角色</span><span>注册时间</span><span>操作</span></div><div v-for="item in users" :key="item.userId" class="user-row"><span><b class="table-avatar">{{ (item.nickname || item.username)[0] }}</b>{{ item.nickname || item.username }} <small>({{ item.username }})</small></span><span>{{ item.email }}</span><span><i :class="{ admin: item.role === 'ADMIN' }">{{ item.role === 'ADMIN' ? '管理员' : '普通用户' }}</i></span><span>{{ formatDate(item.createdAt) }}</span><button v-if="item.role !== 'ADMIN'" title="删除用户" @click="removeUser(item)"><Trash2 :size="15" /></button><span v-else class="protected">受保护</span></div></div></section>
    </div>

    <p v-if="notice" class="admin-notice" :class="{ error: noticeError }">{{ notice }}</p>
  </section>
</template>

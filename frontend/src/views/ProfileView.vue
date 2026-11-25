<script setup>
import { Bookmark, Calendar, Heart, LogOut, MessageCircle, Settings, Star, ThumbsUp } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { apiJson, getStoredUser } from '../api'

const user = ref(getStoredUser() || { username: '放映室用户', role: 'USER' })
const profile = ref({ ratingCount: 0, commentCount: 0, watchlistCount: 0, ratings: [], comments: [], watchlist: [] })
const notice = ref('')

const registerDate = computed(() => formatDate(profile.value.createdAt))
const joinedDays = computed(() => {
  const time = Date.parse(profile.value.createdAt || '')
  if (Number.isNaN(time)) return ''
  return Math.max(0, Math.floor((Date.now() - time) / 86400000))
})

function formatDate(value) {
  return value ? String(value).slice(0, 10) : '—'
}
function watchLabel(status) {
  return ({ WANT: '想看', WATCHING: '在看', WATCHED: '看过' })[status] || status || '想看'
}
function openDetail(id) { window.location.hash = `/detail?id=${id}` }
function openWatchlist() { window.location.hash = '/watchlist' }
function openAdmin() { window.location.hash = '/admin' }

onMounted(async () => {
  try {
    const result = await apiJson('/api/auth/profile')
    user.value = { ...user.value, ...result }
    profile.value = {
      ...profile.value,
      ...result,
      ratings: Array.isArray(result.ratings) ? result.ratings : [],
      comments: Array.isArray(result.comments) ? result.comments : [],
      watchlist: Array.isArray(result.watchlist) ? result.watchlist : []
    }
  } catch (error) { notice.value = error.message }
})
</script>

<template>
  <section class="page-view wrap profile-page">
    <div class="profile-banner">
      <div class="profile-avatar">{{ (user.nickname || user.username || '我').slice(0, 1) }}</div>
      <div>
        <span class="section-kicker">MY SCREENROOM</span>
        <h1>{{ user.nickname || user.username || '放映室用户' }}</h1>
        <p>{{ user.role === 'ADMIN' ? '管理员 · 维护真实评分与社区内容。' : '在故事里生活，在评分里留下真实看法。' }}</p>
        <span class="profile-joined"><Calendar :size="14" /> 加入时间 {{ registerDate }}</span>
      </div>
      <button class="profile-edit"><Settings :size="16" /> {{ user.role === 'ADMIN' ? '管理员资料' : '编辑资料' }}</button>
    </div>

    <div class="profile-stats">
      <div><b>{{ profile.ratingCount }}</b><span>我的真实评分</span></div>
      <div><b>{{ profile.commentCount }}</b><span>我的评论</span></div>
      <div><b>{{ profile.watchlistCount }}</b><span>片单作品</span></div>
      <div><b>{{ user.role === 'ADMIN' ? 'ADMIN' : 'USER' }}</b><span>账号角色</span></div>
    </div>

    <p v-if="notice" class="form-error">{{ notice }}</p>

    <div class="profile-layout">
      <section class="profile-main">
        <div class="subheading"><h2>我评过的作品</h2><span>评分来自 ratings 表</span></div>
        <div v-if="!profile.ratings.length" class="empty-admin">还没有评分记录，去番剧详情页留下第一份真实评分吧。</div>
        <div v-else class="watching-list">
          <article v-for="item in profile.ratings" :key="`rating-${item.animeId}`" class="watching-item" @click="openDetail(item.animeId)">
            <img :src="item.coverUrl" :alt="item.title" />
            <div>
              <h3>{{ item.title }}</h3>
              <p>{{ formatDate(item.createdAt) }} · {{ item.mediaType === 'MOVIE' ? '电影' : '番剧' }}</p>
              <strong><Star :size="14" fill="currentColor" /> {{ item.score }}/10</strong>
            </div>
            <Heart :size="18" />
          </article>
        </div>

        <div class="subheading"><h2>我的评论</h2><span>评论来自 comments 表</span></div>
        <div v-if="!profile.comments.length" class="empty-admin">还没有发表评论，去作品详情页写下第一条看法吧。</div>
        <div v-else class="profile-comments">
          <article v-for="item in profile.comments" :key="`comment-${item.id}`" class="profile-comment" @click="openDetail(item.animeId)">
            <div class="profile-comment-head"><b>{{ item.animeTitle }}</b><span>{{ formatDate(item.createdAt) }}</span></div>
            <p>{{ item.content }}</p>
            <small><ThumbsUp :size="13" /> {{ item.likes }} 人觉得有用</small>
          </article>
        </div>

        <div class="subheading"><h2>我的片单</h2><span>片单来自 watchlist 表</span></div>
        <div v-if="!profile.watchlist.length" class="empty-admin">片单还是空的，去作品详情页加入想看清单吧。</div>
        <div v-else class="watching-list">
          <article v-for="item in profile.watchlist" :key="`watch-${item.animeId}`" class="watching-item" @click="openDetail(item.animeId)">
            <img :src="item.coverUrl" :alt="item.title" />
            <div>
              <h3>{{ item.title }}</h3>
              <p>收藏于 {{ formatDate(item.updatedAt) }}</p>
              <strong class="watch-status"><Bookmark :size="13" /> {{ watchLabel(item.status) }}</strong>
            </div>
            <Heart :size="18" />
          </article>
        </div>
      </section>

      <aside class="profile-side">
        <h2>个人动态</h2>
        <p><MessageCircle :size="16" /> 你发表了 {{ profile.commentCount }} 条评论</p>
        <p><Star :size="16" /> 你给 {{ profile.ratingCount }} 部作品打过分</p>
        <p><Bookmark :size="16" /> 你的片单收录 {{ profile.watchlistCount }} 部作品</p>
        <p><Calendar :size="16" /> 注册时间 {{ registerDate }}<em v-if="joinedDays"> · 已加入 {{ joinedDays }} 天</em></p>
        <button class="text-link" @click="openWatchlist">管理我的追番片单 →</button>
        <button v-if="user.role === 'ADMIN'" class="text-link" @click="openAdmin">进入管理员后台 →</button>
        <button class="logout-link" @click="$emit('logout')"><LogOut :size="15" /> 退出登录</button>
      </aside>
    </div>
  </section>
</template>

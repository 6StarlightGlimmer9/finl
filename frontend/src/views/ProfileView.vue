<script setup>
import { Bookmark, Calendar, Heart, LogOut, MessageCircle, Settings, Star } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
import { apiJson, getStoredUser } from '../api'

const user = ref(getStoredUser() || { username: '放映室用户', role: 'USER' })
const profile = ref({ ratingCount: 0, commentCount: 0, ratings: [] })
const notice = ref('')

function openDetail(id) { window.location.hash = `/detail?id=${id}` }
function openWatchlist() { window.location.hash = '/watchlist' }
function openAdmin() { window.location.hash = '/admin' }
onMounted(async () => {
  try {
    const result = await apiJson('/api/auth/profile')
    user.value = { ...user.value, ...result }
    profile.value = result
  } catch (error) { notice.value = error.message }
})
</script>

<template>
  <section class="page-view wrap profile-page">
    <div class="profile-banner"><div class="profile-avatar">{{ (user.nickname || user.username || '我').slice(0, 1) }}</div><div><span class="section-kicker">MY SCREENROOM</span><h1>{{ user.nickname || user.username || '放映室用户' }}</h1><p>{{ user.role === 'ADMIN' ? '管理员 · 维护真实评分与社区内容。' : '在故事里生活，在评分里留下真实看法。' }}</p></div><button class="profile-edit"><Settings :size="16" /> {{ user.role === 'ADMIN' ? '管理员资料' : '编辑资料' }}</button></div>
    <div class="profile-stats"><div><b>{{ profile.ratingCount }}</b><span>我的真实评分</span></div><div><b>{{ profile.commentCount }}</b><span>我的评论</span></div><div><b>{{ profile.watchlistCount || 0 }}</b><span>片单作品</span></div><div><b>{{ user.role === 'ADMIN' ? 'ADMIN' : 'USER' }}</b><span>账号角色</span></div></div>
    <p v-if="notice" class="form-error">{{ notice }}</p>
    <div class="profile-layout"><section><div class="subheading"><h2>我评过的作品</h2><span>评分来自 ratings 表</span></div><div v-if="!profile.ratings.length" class="empty-admin">还没有评分记录，去番剧详情页留下第一份真实评分吧。</div><div v-else class="watching-list"><article v-for="item in profile.ratings" :key="item.animeId" class="watching-item" @click="openDetail(item.animeId)"><img :src="item.coverUrl" :alt="item.title" /><div><h3>{{ item.title }}</h3><p>我的评分</p><strong><Star :size="14" fill="currentColor" /> {{ item.score }}/10</strong></div><Heart :size="18" /></article></div></section><aside class="profile-side"><h2>个人动态</h2><p><MessageCircle :size="16" /> 你发表了 {{ profile.commentCount }} 条评论</p><p><Star :size="16" /> 你给 {{ profile.ratingCount }} 部作品打过分</p><p><Bookmark :size="16" /> 你的片单收录 {{ profile.watchlistCount || 0 }} 部作品</p><p><Calendar :size="16" /> 注册时间 {{ profile.createdAt ? profile.createdAt.slice(0, 10) : '—' }}</p><button class="text-link" @click="openWatchlist">管理我的追番片单 →</button><button v-if="user.role === 'ADMIN'" class="text-link" @click="openAdmin">进入管理员后台 →</button><button class="logout-link" @click="$emit('logout')"><LogOut :size="15" /> 退出登录</button></aside></div>
  </section>
</template>

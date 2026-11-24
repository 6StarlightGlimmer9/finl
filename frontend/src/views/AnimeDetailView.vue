<script setup>
import { Heart, Play, Send, Star } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'

const rating = ref(0)
const comment = ref('')
const anime = ref(null)
const comments = ref([])
const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const saved = ref(false)
const user = ref(null)
const fallbackAnime = { id: 1, title: '胆大党 第二季', season: '2026 夏', status: 'AIRING', score: 9.4, scoreCount: 2486, coverUrl: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80', pvUrl: 'https://www.youtube.com/watch?v=dQw4w9WgXcQ', genres: ['奇幻', '战斗', '校园'] }

function currentAnimeId() {
  return new URLSearchParams(window.location.hash.split('?')[1] || '').get('id') || '1'
}

function openPv() {
  if (anime.value?.pvUrl) window.open(anime.value.pvUrl, '_blank')
}

async function loadDetail() {
  try {
    const id = currentAnimeId()
    const [animeResponse, ratingResponse, commentsResponse] = await Promise.all([
      fetch('/api/anime'),
      fetch(`/api/anime/${id}/rating`),
      fetch(`/api/anime/${id}/comments`)
    ])
    if (!animeResponse.ok || !ratingResponse.ok || !commentsResponse.ok) throw new Error('详情加载失败')
    const animeList = await animeResponse.json()
    anime.value = animeList.find(item => String(item.id) === String(id))
    if (!anime.value) throw new Error('番剧不存在')
    const summary = await ratingResponse.json()
    anime.value.score = summary.score
    anime.value.scoreCount = summary.count
    comments.value = await commentsResponse.json()
  } catch (error) {
    anime.value = { ...fallbackAnime, id: Number(currentAnimeId()) || 1 }
    comments.value = []
    loadError.value = ''
  } finally {
    loading.value = false
  }
}

async function submitRating() {
  user.value = JSON.parse(localStorage.getItem('screenroom-user') || 'null')
  if (!user.value) { loadError.value = '请先登录后再评分'; return }
  if (!rating.value || submitting.value) return
  submitting.value = true
  try {
    const response = await fetch(`/api/anime/${anime.value.id}/ratings`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: user.value.userId, score: rating.value * 2 })
    })
    if (!response.ok) throw new Error('评分提交失败')
    const summary = await response.json()
    anime.value.score = summary.score
    anime.value.scoreCount = summary.count
  } catch (error) {
    loadError.value = error.message
  } finally {
    submitting.value = false
  }
}

async function postComment() {
  user.value = JSON.parse(localStorage.getItem('screenroom-user') || 'null')
  if (!user.value) { loadError.value = '请先登录后再发表评论'; return }
  if (!comment.value.trim() || submitting.value) return
  submitting.value = true
  try {
    const response = await fetch(`/api/anime/${anime.value.id}/comments`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: user.value.username, content: comment.value.trim() })
    })
    if (!response.ok) throw new Error('评论提交失败')
    comments.value.unshift(await response.json())
    comment.value = ''
  } catch (error) {
    loadError.value = error.message
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  user.value = JSON.parse(localStorage.getItem('screenroom-user') || 'null')
  loadDetail()
})
</script>

<template>
  <section class="page-view wrap detail-page">
    <p v-if="loading" class="catalog-state">正在打开番剧详情...</p>
    <p v-else-if="loadError && !anime" class="catalog-state error">{{ loadError }}</p>
    <template v-else-if="anime">
      <div class="breadcrumb">番剧库　/　{{ anime.title }}</div>
      <div class="detail-hero">
        <div class="detail-cover"><img :src="anime.coverUrl" :alt="anime.title" /><span>{{ anime.status === 'AIRING' ? '连载中' : anime.status === 'FINISHED' ? '已完结' : '即将上映' }}</span></div>
        <div class="detail-copy">
          <span class="section-kicker">ANIME DETAIL / {{ anime.season }}</span>
          <h1>{{ anime.title }}</h1>
          <p class="original-title">{{ anime.season }}</p>
          <p>超自然与青春期撞在一起，会迸出怎样的火花？继续跟随角色们，踏入这场失控却耀眼的冒险。</p>
          <div class="detail-tags"><span v-for="genre in anime.genres" :key="genre">{{ genre }}</span></div>
          <div class="detail-actions"><button class="primary-button" @click="openPv"><Play :size="16" fill="currentColor" /> 播放 PV</button><button class="ghost-button" :class="{ saved }" @click="saved = !saved"><Heart :size="16" :fill="saved ? 'currentColor' : 'none'" /> {{ saved ? '已收藏' : '收藏' }}</button></div>
        </div>
        <div class="detail-score"><span>社区评分</span><b>{{ anime.score.toFixed(1) }}</b><div><Star v-for="item in 5" :key="item" :size="16" fill="currentColor" /></div><small>{{ anime.scoreCount.toLocaleString() }} 人评分</small></div>
      </div>
      <div class="detail-content">
        <div>
          <div class="video-frame detail-video"><div class="video-overlay"><button class="big-play" @click="openPv"><Play :size="27" fill="currentColor" /></button><span>点击播放 PV 预告</span></div><img :src="anime.coverUrl" :alt="`${anime.title} PV 预告`" /></div>
          <div class="detail-tabs"><button class="active">剧情简介</button><button>演职员表</button><button>相关作品</button></div>
          <p class="synopsis">这是一部值得慢慢观看的作品。每一次相遇、每一场冒险，都在角色的选择里留下痕迹。从今天开始，把它加入你的放映清单吧。</p>
          <div class="detail-comment-box"><h2>看完 PV，留下你的第一印象</h2><div><input v-model="comment" @keyup.enter="postComment" placeholder="写下你的想法，和同好交流吧" /><button :disabled="submitting" @click="postComment"><Send :size="16" /></button></div><p v-if="loadError" class="form-error">{{ loadError }}</p><article v-for="item in comments" :key="item.id" class="detail-comment"><b>{{ item.username }}</b><small>{{ item.createdAt }}</small><p>{{ item.content }}</p></article></div>
        </div>
        <aside class="detail-side"><h3>给这部作品评分</h3><div class="detail-stars"><button v-for="item in 5" :key="item" @click="rating = item" :class="{ active: item <= rating }"><Star :size="24" fill="currentColor" /></button></div><p>{{ rating ? `你给出了 ${rating} 星` : '点击星星进行评分' }}</p><button class="primary-button full" :disabled="submitting || !rating" @click="submitRating">{{ submitting ? '提交中...' : '提交评分' }}</button><hr /><h3>播出信息</h3><p>{{ anime.season }} · {{ anime.status === 'AIRING' ? '连载中' : '已完结' }}</p><p>社区参与评分 {{ anime.scoreCount.toLocaleString() }} 人</p></aside>
      </div>
    </template>
  </section>
</template>

<script setup>
import { Heart, Play, Send, Star, Trash2, X } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { apiJson, getStoredUser, showToast } from '../api'

const rating = ref(0)
const hoverRating = ref(0)
const comment = ref('')
const anime = ref(null)
const comments = ref([])
const loading = ref(true)
const notice = ref('')
const ratingNotice = ref('')
const activeTab = ref('synopsis')
const submitting = ref(false)
const watchStatus = ref('')
const pvOpen = ref(false)
const user = ref(null)
const ratingSummary = ref({ score: 0, count: 0, distribution: new Array(10).fill(0), myRating: null })
function currentAnimeId() {
  return new URLSearchParams(window.location.hash.split('?')[1] || '').get('id') || '1'
}

function statusLabel(status) {
  return status === 'AIRING' ? '连载中' : status === 'FINISHED' ? '已完结' : '即将上映'
}

function formatDate(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

const bilibiliPv = ref(null)

function bilibiliInfo(value) {
  const text = String(value || '').trim()
  if (!text) return null
  const bv = text.match(/BV[0-9A-Za-z]{10}/)
  if (bv) {
    return {
      embed: `https://player.bilibili.com/player.html?bvid=${bv[0]}&page=1&high_quality=1&danmaku=0`,
      web: `https://www.bilibili.com/video/${bv[0]}`
    }
  }
  const av = text.match(/(?:^|\/)av(\d{4,})/i)
  if (av) {
    return {
      embed: `https://player.bilibili.com/player.html?aid=${av[1]}&page=1&high_quality=1&danmaku=0`,
      web: `https://www.bilibili.com/video/av${av[1]}`
    }
  }
  return null
}

async function resolveBilibiliPreview(value) {
  bilibiliPv.value = bilibiliInfo(value)
  const text = String(value || '')
  if (bilibiliPv.value || !/bilibili\.com\/bangumi\/play\/(?:ss|ep)\d+/.test(text)) return
  try {
    bilibiliPv.value = await apiJson(`/api/bilibili/resolve?url=${encodeURIComponent(text)}`)
  } catch {
    bilibiliPv.value = null
  }
}

function requireLogin(message) {
  if (getStoredUser()) return true
  window.dispatchEvent(new CustomEvent('screenroom:login-required', { detail: { message } }))
  return false
}

function selectRating(value) {
  if (!requireLogin('请先登录后再评分')) return
  rating.value = value
}

async function openPv() {
  if (!anime.value) return
  if (!bilibiliPv.value && anime.value.pvUrl) await resolveBilibiliPreview(anime.value.pvUrl)
  pvOpen.value = true
}

const watchOptions = [
  { value: 'WANT', label: '想看' },
  { value: 'WATCHING', label: '在看' },
  { value: 'WATCHED', label: '看过' }
]

async function loadWatchState() {
  if (!getStoredUser() || !anime.value) return
  try {
    const list = await apiJson('/api/watchlist')
    const found = list.find(item => item.animeId === anime.value.id)
    watchStatus.value = found?.status || ''
  } catch { /* 片单状态加载失败不影响详情页 */ }
}

async function saveWatch(status) {
  if (!requireLogin('登录后可加入我的追番片单')) return
  try {
    const updated = await apiJson(`/api/watchlist/${anime.value.id}`, {
      method: 'POST',
      body: JSON.stringify({ status })
    })
    watchStatus.value = updated.status
    showToast(`已标记为${watchOptions.find(item => item.value === status)?.label || status}`)
  } catch (error) {
    showToast(error.message, 'error')
  }
}

async function likeComment(item) {
  if (!requireLogin('请先登录后再点赞评论')) return
  try {
    const result = await apiJson(`/api/comments/${item.id}/like`, { method: 'POST' })
    item.liked = result.liked
    item.likes = result.likes
  } catch (error) {
    showToast(error.message, 'error')
  }
}

const scoreBuckets = computed(() => [5, 4, 3, 2, 1].map(stars => {
  const index = stars * 2 - 2
  const distribution = ratingSummary.value.distribution || []
  return { stars, count: Number(distribution[index] || 0) + Number(distribution[index + 1] || 0) }
}))
const maxBucket = computed(() => Math.max(1, ...scoreBuckets.value.map(item => item.count)))
const displayRating = computed(() => hoverRating.value || rating.value || (ratingSummary.value.myRating ? Math.round(ratingSummary.value.myRating / 2) : 0))
const ratingHint = computed(() => {
  if (hoverRating.value) return `预览 ${hoverRating.value} 星（${hoverRating.value * 2}/10）`
  if (ratingSummary.value.myRating) return `你已评分 ${ratingSummary.value.myRating}/10，可随时修改`
  return '点击星星评分，分数将计入全站真实评分'
})

async function loadDetail() {
  loading.value = true
  notice.value = ''
  ratingNotice.value = ''
  activeTab.value = 'synopsis'
  try {
    const id = currentAnimeId()
    const [detail, summary, commentList] = await Promise.all([
      apiJson(`/api/anime/${id}`),
      apiJson(`/api/anime/${id}/rating`),
      apiJson(`/api/anime/${id}/comments`)
    ])
    anime.value = detail
    await resolveBilibiliPreview(detail.pvUrl)
    ratingSummary.value = summary
    comments.value = commentList
    if (summary.myRating) rating.value = Math.round(summary.myRating / 2)
  } catch (error) {
    anime.value = null
    bilibiliPv.value = null
    comments.value = []
    notice.value = error.message
  } finally {
    loading.value = false
  }
}

async function submitRating() {
  user.value = getStoredUser()
  if (!requireLogin('请先登录后再评分')) return
  if (submitting.value) return
  if (!rating.value) {
    ratingNotice.value = '请先点击星星选择分数，再提交评分'
    showToast(ratingNotice.value, 'error')
    return
  }
  submitting.value = true
  ratingNotice.value = ''
  try {
    ratingSummary.value = await apiJson(`/api/anime/${anime.value.id}/ratings`, {
      method: 'POST',
      body: JSON.stringify({ score: rating.value * 2 })
    })
    ratingNotice.value = `评分已提交：${rating.value * 2}/10，感谢你的真实反馈`
    showToast(ratingNotice.value)
  } catch (error) {
    ratingNotice.value = error.message
    showToast(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function removeRating() {
  if (!requireLogin('请先登录后再操作评分')) return
  user.value = getStoredUser()
  if (submitting.value) return
  submitting.value = true
  ratingNotice.value = ''
  try {
    ratingSummary.value = await apiJson(`/api/anime/${anime.value.id}/ratings`, { method: 'DELETE' })
    rating.value = 0
    ratingNotice.value = '已撤销你的评分'
    showToast(ratingNotice.value)
  } catch (error) {
    ratingNotice.value = error.message
    showToast(error.message, 'error')
  } finally {
    submitting.value = false
  }
}

async function postComment() {
  user.value = getStoredUser()
  if (!requireLogin('请先登录后再发表评论')) return
  if (!comment.value.trim() || submitting.value) return
  submitting.value = true
  try {
    const created = await apiJson(`/api/anime/${anime.value.id}/comments`, {
      method: 'POST',
      body: JSON.stringify({ content: comment.value.trim() })
    })
    comments.value.unshift(created)
    comment.value = ''
    notice.value = '评论已发布'
  } catch (error) {
    notice.value = error.message
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  user.value = getStoredUser()
  await loadDetail()
  loadWatchState()
})
</script>

<template>
  <section class="page-view wrap detail-page">
    <p v-if="loading" class="catalog-state">正在打开番剧详情...</p>
    <template v-else-if="anime">
      <div class="breadcrumb">番剧库　/　{{ anime.title }}</div>
      <div class="detail-hero reveal-up">
        <div class="detail-cover"><img :src="anime.coverUrl" :alt="anime.title" /><span>{{ statusLabel(anime.status) }}</span></div>
        <div class="detail-copy">
          <span class="section-kicker">ANIME DETAIL / {{ anime.season }}</span>
          <h1>{{ anime.title }}</h1>
          <p class="original-title">{{ anime.originalTitle || anime.season }}</p>
          <p>{{ anime.summary || '这部作品还没有填写简介，管理员可在后台补充。' }}</p>
          <div class="detail-tags"><span v-for="genre in anime.genres" :key="genre">{{ genre }}</span></div>
          <div class="detail-actions"><button class="primary-button" @click="openPv"><Play :size="16" fill="currentColor" /> 查看预览</button><div class="watch-quick"><Heart :size="15" /><button v-for="option in watchOptions" :key="option.value" :class="{ active: watchStatus === option.value }" @click="saveWatch(option.value)">{{ option.label }}</button></div></div>
        </div>
        <div class="detail-score score-pop"><span>社区真实评分</span><b>{{ Number(ratingSummary.score).toFixed(1) }}</b><div><Star v-for="item in 5" :key="item" :size="16" fill="currentColor" /></div><small>{{ Number(ratingSummary.count).toLocaleString() }} 人评分</small></div>
      </div>
      <div class="detail-content">
        <div>
          <div class="video-frame detail-video"><div class="video-overlay"><button class="big-play" @click="openPv"><Play :size="27" fill="currentColor" /></button><span>点击查看预览</span></div><img :src="anime.coverUrl" :alt="`${anime.title} 预览`" /></div>
          <div class="detail-tabs">
            <button :class="{ active: activeTab === 'synopsis' }" @click="activeTab = 'synopsis'">剧情简介</button>
            <button :class="{ active: activeTab === 'broadcast' }" @click="activeTab = 'broadcast'">播出信息</button>
            <button :class="{ active: activeTab === 'rating' }" @click="activeTab = 'rating'">评分说明</button>
          </div>
          <div v-if="activeTab === 'synopsis'" class="detail-tab-panel"><p class="synopsis">{{ anime.summary || '这部作品还没有填写简介，管理员可在后台补充。' }}</p></div>
          <div v-else-if="activeTab === 'broadcast'" class="detail-tab-panel broadcast-grid">
            <div><span>播出季度</span><b>{{ anime.season || '—' }}</b></div>
            <div><span>播出状态</span><b>{{ statusLabel(anime.status) }}</b></div>
            <div><span>作品类型</span><b>{{ anime.mediaType === 'MOVIE' ? '电影' : '番剧' }}</b></div>
            <div><span>原作标题</span><b>{{ anime.originalTitle || '—' }}</b></div>
            <div><span>作品标签</span><b>{{ (anime.genres || []).join(' · ') || '—' }}</b></div>
            <div><span>评分人数</span><b>{{ Number(ratingSummary.count).toLocaleString() }} 人</b></div>
            <div class="full"><span>收录时间</span><b>{{ anime.createdAt ? anime.createdAt.slice(0, 10) : '—' }}</b></div>
          </div>
          <div v-else class="detail-tab-panel rating-explain">
            <p>站内分数由用户提交的 1–10 分评分实时计算，<strong>同一用户对同一作品只保留一条评分</strong>，修改后分数与评分人数会同步更新。</p>
            <p>当前综合评分 <strong>{{ Number(ratingSummary.score).toFixed(1) }}/10</strong>，来自 {{ Number(ratingSummary.count).toLocaleString() }} 位真实用户。</p>
            <p v-if="ratingSummary.myRating">你已评分 <strong>{{ ratingSummary.myRating }}/10</strong>，可在右侧修改或撤销。</p>
            <p v-else>还没有评过分？在右侧点击星星，留下你的真实看法。</p>
          </div>
          <section class="admin-panel rating-panel-detail">
            <div class="subheading"><h2>评分分布</h2><span>来自 {{ Number(ratingSummary.count).toLocaleString() }} 条真实评分</span></div>
            <div class="rating-summary">
              <div class="rating-big"><b>{{ Number(ratingSummary.score).toFixed(1) }}</b><small>综合评分 / 10</small></div>
              <div class="rating-bars">
                <div v-for="bucket in scoreBuckets" :key="bucket.stars" class="rating-bar-row"><span>{{ bucket.stars }} 星</span><div class="rating-bar"><i :style="{ width: `${bucket.count / maxBucket * 100}%` }"></i></div><span>{{ bucket.count }}</span></div>
              </div>
            </div>
            <p class="rating-hint">评分由用户提交的 1–10 分记录实时计算，同一用户对同一作品只能保留一条评分。</p>
          </section>
          <div class="detail-comment-box"><h2>看完预览，留下你的第一印象</h2><div class="comment-input"><input v-model="comment" @focus="requireLogin('请先登录后再发表评论')" @keyup.enter="postComment" placeholder="写下你的想法，和同好交流吧" /><button :disabled="submitting" @click="postComment"><Send :size="16" /></button></div><p v-if="notice" class="form-error">{{ notice }}</p><TransitionGroup name="comment-list" tag="div" class="detail-comment-list"><article v-for="item in comments" :key="item.id" class="detail-comment"><span class="comment-avatar alt">{{ (item.nickname || item.username || '漫').slice(0, 1) }}</span><div class="detail-comment-body"><div class="detail-comment-meta"><b>{{ item.nickname || item.username }}</b><small>{{ formatDate(item.createdAt) }}</small></div><p>{{ item.content }}</p></div><button class="detail-comment-likes" :class="{ liked: item.liked }" :title="item.liked ? '取消点赞' : '点赞评论'" @click="likeComment(item)">♥ {{ item.likes }}</button></article></TransitionGroup></div>
        </div>
        <aside class="detail-side">
          <h3>给这部作品评分</h3>
          <div class="detail-stars">
            <button v-for="item in 5" :key="item" class="star-button" :class="{ active: item <= displayRating }" @mouseenter="hoverRating = item" @mouseleave="hoverRating = 0" @click="selectRating(item)"><Star :size="26" fill="currentColor" /></button>
          </div>
          <p>{{ ratingHint }}</p>
          <button class="primary-button full" :disabled="submitting" @click="submitRating">{{ submitting ? '提交中...' : '提交评分' }}</button>
          <p v-if="ratingNotice" class="rating-notice">{{ ratingNotice }}</p>
          <button v-if="ratingSummary.myRating" class="ghost-button full" :disabled="submitting" @click="removeRating"><Trash2 :size="14" /> 撤销我的评分</button>
          <hr />
          <h3>播出信息</h3>
          <p>{{ anime.season }} · {{ statusLabel(anime.status) }}</p>
          <p>社区参与评分 {{ Number(ratingSummary.count).toLocaleString() }} 人</p>
        </aside>
      </div>
    </template>
    <p v-else class="catalog-state error">{{ notice || '作品不存在或已被删除' }}</p>
    <div v-if="pvOpen && anime" class="modal-backdrop" @click.self="pvOpen = false">
      <div class="pv-modal">
        <button class="close-button" @click="pvOpen = false"><X :size="18" /></button>
        <span class="section-kicker">BILIBILI PREVIEW / 16:9</span>
        <h2>{{ anime.title }}</h2>
        <div class="pv-screen">
          <iframe v-if="bilibiliPv" class="pv-iframe" :src="bilibiliPv.embed" scrolling="no" frameborder="0" allow="autoplay; fullscreen" referrerpolicy="no-referrer" allowfullscreen></iframe>
          <template v-else>
            <img :src="anime.coverUrl" :alt="`${anime.title} 预览`" />
            <div class="pv-placeholder"><Play :size="34" fill="currentColor" /><b>暂无 Bilibili 预览</b><span>该作品的 Bilibili 资源暂时不可用</span></div>
          </template>
        </div>
        <a v-if="bilibiliPv" class="pv-open-link" :href="bilibiliPv.web" target="_blank" rel="noopener">在 Bilibili 打开原视频 →</a>
      </div>
    </div>
  </section>
</template>

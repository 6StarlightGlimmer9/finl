<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowUpRight, BookOpen, PenLine, Search, Sparkles, Star, ThumbsUp, X } from 'lucide-vue-next'
import { apiJson, getStoredUser, showToast } from '../api'

const reviews = ref([])
const writers = ref([])
const stats = ref({ total: 0, writers: 0, likes: 0, avgScore: 0 })
const animeOptions = ref([])
const keyword = ref('')
const sortBy = ref('latest')
const expanded = ref([])
const writing = ref(false)
const loading = ref(true)
const notice = ref('')
const draft = ref({ animeId: '', title: '', content: '', score: 9 })

function requireLogin(message) {
  if (getStoredUser()) return true
  window.dispatchEvent(new CustomEvent('screenroom:login-required', { detail: { message } }))
  return false
}
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '' }
function excerpt(text) {
  const content = String(text || '')
  return content.length > 150 ? `${content.slice(0, 150)}…` : content
}
function resetDraft() { draft.value = { animeId: animeOptions.value[0]?.id || '', title: '', content: '', score: 9 } }

const displayReviews = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  let list = reviews.value.filter(review => {
    if (!query) return true
    const haystack = [review.title, review.content, review.animeTitle, review.nickname, review.username, (review.genres || []).join(' ')].join(' ').toLowerCase()
    return haystack.includes(query)
  })
  list = [...list]
  if (sortBy.value === 'hot') list.sort((a, b) => Number(b.likes) - Number(a.likes) || String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
  else if (sortBy.value === 'score') list.sort((a, b) => Number(b.score) - Number(a.score) || String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
  else list.sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
  return list
})
const hotGenres = computed(() => {
  const counts = {}
  reviews.value.forEach(review => (review.genres || []).forEach(name => { counts[name] = (counts[name] || 0) + 1 }))
  return Object.entries(counts).sort((a, b) => b[1] - a[1]).slice(0, 8).map(([name, count]) => ({ name, count }))
})

async function load() {
  loading.value = true
  notice.value = ''
  try {
    const [reviewData, animeList] = await Promise.all([apiJson('/api/reviews?limit=100'), apiJson('/api/anime')])
    reviews.value = reviewData.reviews || []
    stats.value = reviewData.stats || stats.value
    writers.value = reviewData.writers || []
    animeOptions.value = Array.isArray(animeList) ? animeList : []
    resetDraft()
  } catch (error) {
    notice.value = error.message
  } finally {
    loading.value = false
  }
}
function openWriting() {
  if (!requireLogin('登录后可发表漫评')) return
  if (!draft.value.animeId && animeOptions.value.length) draft.value.animeId = animeOptions.value[0].id
  writing.value = true
}
async function publishReview() {
  if (!requireLogin('登录后可发布漫评')) return
  if (!draft.value.animeId || !draft.value.title.trim() || !draft.value.content.trim()) {
    showToast('请填写关联作品、标题和正文', 'error')
    return
  }
  try {
    const created = await apiJson('/api/reviews', {
      method: 'POST',
      body: JSON.stringify({
        animeId: Number(draft.value.animeId),
        title: draft.value.title.trim(),
        content: draft.value.content.trim(),
        score: Number(draft.value.score)
      })
    })
    reviews.value.unshift(created)
    stats.value.total = Number(stats.value.total || 0) + 1
    resetDraft()
    writing.value = false
    showToast('漫评已发布')
  } catch (error) {
    showToast(error.message, 'error')
  }
}
async function toggleLike(review) {
  if (!requireLogin('登录后可点赞漫评')) return
  try {
    const result = await apiJson(`/api/reviews/${review.id}/like`, { method: 'POST' })
    review.liked = result.liked
    review.likes = result.likes
  } catch (error) {
    showToast(error.message, 'error')
  }
}
function toggleExpanded(id) {
  expanded.value = expanded.value.includes(id) ? expanded.value.filter(item => item !== id) : [...expanded.value, id]
}
function openAnime(id) { window.location.hash = `/detail?id=${id}` }

onMounted(load)
</script>

<template>
  <section class="page-view wrap social-page reviews-page">
    <div class="page-intro social-intro">
      <div>
        <span class="section-kicker">EDITORIAL / 我们的主打</span>
        <h1>漫评</h1>
        <p>认真写下观看之后，仍然留在心里的那一点感受。这里不比谁写得快，只比谁写得更诚实。</p>
        <div class="social-stats">
          <span><b>{{ stats.total || 0 }}</b> 篇长评</span>
          <span><b>{{ stats.writers || 0 }}</b> 位作者</span>
          <span><b>{{ stats.likes || 0 }}</b> 次共鸣</span>
          <span><b>{{ Number(stats.avgScore || 0).toFixed(1) }}</b> 平均打分</span>
        </div>
      </div>
      <button class="primary-button" @click="openWriting"><PenLine :size="16" /> 写一篇漫评</button>
    </div>

    <div class="social-banner reviews-banner">
      <div><span class="section-kicker">DIFFERENT FROM BILIBILI</span><h2>把短评沉淀成一篇能被读完的文章</h2><p>关联作品、给出评分、带上标签，让观点有来处，也让同好能顺着一部作品找到你。</p></div>
      <Sparkles :size="52" />
    </div>

    <div class="social-layout">
      <div class="social-main">
        <div class="social-toolbar">
          <label class="social-search"><Search :size="17" /><input v-model="keyword" placeholder="搜索漫评、作品或作者" /></label>
          <select v-model="sortBy"><option value="latest">最新发布</option><option value="hot">最多共鸣</option><option value="score">最高打分</option></select>
        </div>
        <p v-if="loading" class="catalog-state">正在读取漫评...</p>
        <p v-else-if="notice" class="catalog-state error">{{ notice }}</p>
        <p v-else-if="!displayReviews.length" class="catalog-state">没有匹配的漫评，换个关键词试试。</p>
        <article v-for="review in displayReviews" :key="review.id" class="editorial-card">
          <img class="editorial-cover" :src="review.animeCover" :alt="review.animeTitle" @click="openAnime(review.animeId)" />
          <div class="editorial-body">
            <div class="editorial-meta">
              <span class="editorial-avatar">{{ (review.nickname || review.username || '漫').slice(0, 1) }}</span>
              <div><b>{{ review.nickname || review.username }}</b><small>{{ formatDate(review.createdAt) }} · 关联《{{ review.animeTitle }}》</small></div>
              <span class="editorial-score"><Star :size="14" fill="currentColor" /> {{ Number(review.score).toFixed(1) }}</span>
            </div>
            <h2>{{ review.title }}</h2>
            <div class="editorial-tags"><span v-for="genre in review.genres" :key="genre">{{ genre }}</span><span><BookOpen :size="12" /> {{ review.readMinutes }} 分钟阅读</span></div>
            <p class="editorial-text">{{ expanded.includes(review.id) ? review.content : excerpt(review.content) }}</p>
            <div class="editorial-actions">
              <button class="like-button" :class="{ liked: review.liked }" @click="toggleLike(review)"><ThumbsUp :size="14" /> {{ review.likes }}</button>
              <button class="ghost-button" @click="toggleExpanded(review.id)">{{ expanded.includes(review.id) ? '收起' : '阅读全文' }}</button>
              <button class="text-link" @click="openAnime(review.animeId)">查看作品 <ArrowUpRight :size="14" /></button>
            </div>
          </div>
        </article>
      </div>

      <aside class="social-side">
        <section class="side-card"><h3>本周写作榜</h3><p v-if="!writers.length" class="side-empty">还没有作者数据。</p><div v-for="(writer, index) in writers" :key="writer.nickname" class="writer-row"><b>{{ String(index + 1).padStart(2, '0') }}</b><span>{{ writer.nickname }}</span><small>{{ writer.count }} 篇 · {{ writer.likes }} 赞</small></div></section>
        <section class="side-card"><h3>热门标签</h3><div class="tag-cloud"><button v-for="tag in hotGenres" :key="tag.name" @click="keyword = tag.name">{{ tag.name }} <small>{{ tag.count }}</small></button></div></section>
        <section class="side-card side-cta"><BookOpen :size="26" /><h3>写一篇属于你的长评</h3><p>选一部作品，给一个分数，把还没散掉的感受留在这里。</p><button class="primary-button full" @click="openWriting">开始写作</button></section>
      </aside>
    </div>

    <div v-if="writing" class="modal-backdrop" @click.self="writing = false">
      <div class="write-modal social-modal">
        <button class="close-button" @click="writing = false"><X :size="18" /></button>
        <span class="section-kicker">NEW REVIEW</span><h2>写下你的观感</h2>
        <label>关联作品<select v-model.number="draft.animeId"><option v-for="item in animeOptions" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
        <label>你的打分<input v-model.number="draft.score" type="number" min="1" max="10" /></label>
        <label>标题<input v-model="draft.title" placeholder="给这篇漫评起个标题" /></label>
        <label>正文<textarea v-model="draft.content" rows="7" placeholder="这次观看，哪一个瞬间留了下来？"></textarea></label>
        <button class="primary-button full" @click="publishReview">发布漫评</button>
      </div>
    </div>
  </section>
</template>

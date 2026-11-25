<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { Filter, Search, Star } from 'lucide-vue-next'
import { apiJson } from '../api'

const keyword = ref('')
const genre = ref('全部')
const status = ref('全部')
const sortBy = ref('default')
const filterOpen = ref(false)
const mediaType = ref('ANIME')
const shows = ref([])
const loading = ref(true)
const loadError = ref('')
const category = ref('')

const genres = computed(() => ['全部', ...new Set(shows.value.flatMap(item => item.genres || []))])
const categoryTitle = computed(() => {
  if (category.value && !['番剧', '电影'].includes(category.value)) return `${category.value}作品`
  return mediaType.value === 'MOVIE' ? '电影库' : '番剧库'
})
const filteredShows = computed(() => {
  const result = shows.value.filter(show =>
    show.mediaType === mediaType.value &&
    (genre.value === '全部' || (show.genres || []).includes(genre.value)) &&
    (status.value === '全部' || show.status === status.value) &&
    show.title.toLocaleLowerCase().includes(keyword.value.trim().toLocaleLowerCase())
  )
  if (sortBy.value === 'title') return result.sort((a, b) => a.title.localeCompare(b.title, 'zh-CN'))
  if (sortBy.value === 'count') return result.sort((a, b) => Number(b.scoreCount) - Number(a.scoreCount))
  if (sortBy.value === 'latest') return result.sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || '')))
  if (sortBy.value === 'score') return result.sort((a, b) => Number(b.score) - Number(a.score))
  return result
})

function statusLabel(value) {
  return value === 'AIRING' ? '连载中' : value === 'FINISHED' ? '已完结' : '即将上映'
}
function resetFilters() {
  keyword.value = ''
  genre.value = '全部'
  status.value = '全部'
  sortBy.value = 'default'
  filterOpen.value = false
}
function syncMediaType() {
  const params = new URLSearchParams(window.location.hash.split('?')[1] || '')
  const categoryParam = params.get('category') || ''
  category.value = categoryParam
  mediaType.value = params.get('type') === 'movie' || categoryParam === '电影' ? 'MOVIE' : 'ANIME'
  genre.value = categoryParam && !['番剧', '电影'].includes(categoryParam) ? categoryParam : '全部'
}
function openDetail(show) { window.location.hash = `/detail?id=${show.id}` }
async function loadAnime() {
  loading.value = true
  try {
    const result = await apiJson('/api/anime')
    if (!Array.isArray(result)) throw new Error('数据格式错误')
    shows.value = result.map(item => ({
      ...item,
      desc: item.summary || [...(item.genres || []), item.season].filter(Boolean).join(' · ') || '番剧作品',
      image: item.coverUrl
    }))
  } catch (error) {
    shows.value = []
    loadError.value = '片单加载失败：' + error.message
  } finally {
    loading.value = false
  }
}
onMounted(() => { syncMediaType(); loadAnime(); window.addEventListener('hashchange', syncMediaType) })
onUnmounted(() => window.removeEventListener('hashchange', syncMediaType))
</script>

<template>
  <section class="page-view wrap">
    <div class="page-intro"><div><span class="section-kicker">{{ category && !['番剧','电影'].includes(category) ? `CATEGORY / ${category}` : (mediaType === 'MOVIE' ? 'MOVIE INDEX / 2026' : 'ANIME INDEX / REAL RATINGS') }}</span><h1>{{ categoryTitle }}</h1><p>按真实用户评分整理片单，把值得看的故事慢慢逛完。</p></div><div class="index-stat"><b>{{ filteredShows.length || '—' }}</b><span>{{ mediaType === 'MOVIE' ? '部电影已收录' : '部番剧已收录' }}</span></div></div>
    <div class="filter-bar"><div class="page-search"><Search :size="17" /><input v-model="keyword" placeholder="搜索作品名称" /></div><div class="genre-filter"><button v-for="item in genres" :key="item" :class="{ selected: genre === item }" @click="genre = item">{{ item }}</button></div><button class="filter-button" @click="filterOpen = !filterOpen"><Filter :size="16" /> 筛选</button></div>
    <div v-if="filterOpen" class="filter-panel"><label>播出状态<select v-model="status"><option>全部</option><option value="AIRING">连载中</option><option value="FINISHED">已完结</option><option value="UPCOMING">即将上映</option></select></label><label>排序方式<select v-model="sortBy"><option value="default">默认（系列相邻）</option><option value="score">最高评分</option><option value="count">最多评分人数</option><option value="latest">最新收录</option><option value="title">作品名称</option></select></label><button @click="resetFilters">重置筛选</button></div>
    <p v-if="loading" class="catalog-state">正在读取片单...</p>
    <p v-else-if="loadError" class="catalog-state error">{{ loadError }}</p>
    <p v-else-if="!filteredShows.length" class="catalog-state">没有找到匹配的作品。</p>
    <div v-else class="catalog-grid"><article v-for="(show, index) in filteredShows" :key="show.id" class="catalog-card reveal-up" :class="{ 'delay-1': index % 3 === 1, 'delay-2': index % 3 === 2 }"><img :src="show.image" :alt="show.title" /><div class="catalog-copy"><span>{{ statusLabel(show.status) }}</span><h2>{{ show.title }}</h2><div class="catalog-tags"><span v-for="tag in (show.genres || [])" :key="tag">{{ tag }}</span></div><p>{{ show.desc }}</p><div class="catalog-footer"><strong><Star :size="15" fill="currentColor" /> {{ Number(show.score).toFixed(1) }} <small>{{ Number(show.scoreCount).toLocaleString() }}人</small></strong><button class="catalog-detail-button" @click="openDetail(show)">查看详情 →</button></div></div></article></div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Filter, Search, Star } from 'lucide-vue-next'
const keyword = ref('')
const genre = ref('全部')
const status = ref('全部')
const filterOpen = ref(false)
const mediaType = ref('ANIME')
const shows = ref([])
const loading = ref(true)
const loadError = ref('')
const genres = ['全部', '奇幻', '战斗', '青春', '恋爱', '治愈', '动作', '热血', '冒险', '科幻', '悬疑', '喜剧']
const fallbackShows = [
  { id: 1, title: '胆大党 第二季', mediaType: 'ANIME', status: 'AIRING', season: '2026 夏', score: 9.4, scoreCount: 2486, coverUrl: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=800&q=80', genres: ['奇幻', '战斗', '校园', '喜剧'] },
  { id: 2, title: '薰香花朵凛然绽放', mediaType: 'ANIME', status: 'AIRING', season: '2026 夏', score: 9.1, scoreCount: 1802, coverUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=800&q=80', genres: ['青春', '恋爱', '治愈', '日常'] },
  { id: 3, title: '链锯人：剧场版', mediaType: 'MOVIE', status: 'UPCOMING', season: '2026', score: 9.0, scoreCount: 1230, coverUrl: 'https://images.unsplash.com/photo-1541562232579-512a21360020?auto=format&fit=crop&w=800&q=80', genres: ['动作', '战斗', '热血', '冒险'] }
]
async function loadAnime() {
  try {
    const response = await fetch('/api/anime')
    if (!response.ok) throw new Error('请求失败')
    const result = await response.json()
    if (!Array.isArray(result)) throw new Error('数据格式错误')
    shows.value = result.map(item => ({
      ...item,
      desc: [...(item.genres || []), item.season].filter(Boolean).join(' · ') || '番剧作品',
      image: item.coverUrl
    }))
  } catch (error) {
    shows.value = fallbackShows
    loadError.value = ''
  } finally {
    loading.value = false
  }
}
const filteredShows = computed(() => (Array.isArray(shows.value) ? shows.value : []).filter(show => show.mediaType === mediaType.value && (genre.value === '全部' || show.desc.includes(genre.value)) && (status.value === '全部' || show.status === status.value) && show.title.includes(keyword.value)))
function statusLabel(value) { return value === 'AIRING' ? '连载中' : value === 'FINISHED' ? '已完结' : '即将上映' }
function resetFilters() { keyword.value = ''; genre.value = '全部'; status.value = '全部'; filterOpen.value = false }
function syncMediaType() {
  mediaType.value = new URLSearchParams(window.location.hash.split('?')[1] || '').get('type') === 'movie' ? 'MOVIE' : 'ANIME'
}
function openDetail(show) {
  window.location.hash = `/detail?id=${show.id}`
}
onMounted(() => { syncMediaType(); loadAnime(); window.addEventListener('hashchange', syncMediaType) })
</script>
<template>
  <section class="page-view wrap"><div class="page-intro"><div><span class="section-kicker">{{ mediaType === 'MOVIE' ? 'MOVIE INDEX / 2026' : 'ANIME INDEX / 2026' }}</span><h1>{{ mediaType === 'MOVIE' ? '电影库' : '番剧库' }}</h1><p>把值得看的故事，整理成一张可以慢慢逛的片单。</p></div><div class="index-stat"><b>{{ filteredShows.length || '—' }}</b><span>{{ mediaType === 'MOVIE' ? '部电影已收录' : '部番剧已收录' }}</span></div></div><div class="filter-bar"><div class="page-search"><Search :size="17" /><input v-model="keyword" placeholder="搜索作品名称" /></div><div class="genre-filter"><button v-for="item in genres" :key="item" :class="{ selected: genre === item }" @click="genre = item">{{ item }}</button></div><button class="filter-button" @click="filterOpen = !filterOpen"><Filter :size="16" /> 筛选</button></div><div v-if="filterOpen" class="filter-panel"><label>播出状态<select v-model="status"><option>全部</option><option value="AIRING">连载中</option><option value="FINISHED">已完结</option><option value="UPCOMING">即将上映</option></select></label><button @click="resetFilters">重置筛选</button></div><p v-if="loading" class="catalog-state">正在读取片单...</p><p v-else-if="loadError" class="catalog-state error">{{ loadError }}</p><p v-else-if="!filteredShows.length" class="catalog-state">没有找到匹配的作品。</p><div v-else class="catalog-grid"><article v-for="show in filteredShows" :key="show.id" class="catalog-card"><img :src="show.image" :alt="show.title" /><div class="catalog-copy"><span>{{ statusLabel(show.status) }}</span><h2>{{ show.title }}</h2><div class="catalog-tags"><span v-for="tag in (show.genres || [])" :key="tag">{{ tag }}</span></div><p>{{ show.desc }}</p><strong><Star :size="15" fill="currentColor" /> {{ show.score.toFixed(1) }}</strong><button @click="openDetail(show)">查看详情 →</button></div></article></div></section>
</template>

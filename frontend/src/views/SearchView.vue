<script setup>
import { Film, MessageCircle, Search, Star } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
const keyword = ref(new URLSearchParams(window.location.hash.split('?')[1] || '').get('q') || '')
const results = ref({ anime: [], comments: [] })
const loading = ref(false)
const searched = ref(false)
const fallback = [{ id: 1, title: '胆大党 第二季', mediaType: 'ANIME', score: 9.4, season: '2026 夏', coverUrl: 'https://images.unsplash.com/photo-1578632767115-351597cf2477?auto=format&fit=crop&w=500&q=80', genres: ['奇幻', '战斗'] }, { id: 2, title: '薰香花朵凛然绽放', mediaType: 'ANIME', score: 9.1, season: '2026 夏', coverUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=500&q=80', genres: ['青春', '恋爱'] }]
const allResults = computed(() => results.value.anime.length ? results.value.anime : fallback.filter(item => item.title.includes(keyword.value)))
async function search() {
  if (!keyword.value.trim()) return
  window.location.hash = `/search?q=${encodeURIComponent(keyword.value.trim())}`
  loading.value = true; searched.value = true
  try { const response = await fetch(`/api/search?q=${encodeURIComponent(keyword.value.trim())}`); const result = await response.json(); if (response.ok && Array.isArray(result.anime)) results.value = result } catch { }
  loading.value = false
}
function openDetail(id) { window.location.hash = `/detail?id=${id}` }
onMounted(() => { if (keyword.value) search() })
</script>
<template><section class="page-view wrap search-page"><div class="search-heading"><div><span class="section-kicker">SEARCH THE SCREENROOM</span><h1>全站搜索</h1><p>搜索作品、电影和同好评论。</p></div><div class="global-search"><Search :size="18" /><input v-model="keyword" @keyup.enter="search" placeholder="搜索作品、评论内容" /><button @click="search">搜索</button></div></div><p v-if="loading" class="catalog-state">正在搜索“{{ keyword }}”...</p><template v-else-if="searched"><div class="search-count">与“{{ keyword }}”相关的结果</div><section v-if="allResults.length" class="search-block"><h2><Film :size="18" /> 作品 <small>{{ allResults.length }}</small></h2><div class="search-results"><article v-for="item in allResults" :key="item.id" class="search-result" @click="openDetail(item.id)"><img :src="item.coverUrl" :alt="item.title" /><div><span>{{ item.mediaType === 'MOVIE' ? '电影' : '番剧' }} · {{ item.season }}</span><h3>{{ item.title }}</h3><p>{{ (item.genres || []).join(' · ') }}</p><strong><Star :size="14" fill="currentColor" /> {{ Number(item.score).toFixed(1) }}</strong></div></article></div></section><section v-if="results.comments.length" class="search-block"><h2><MessageCircle :size="18" /> 评论 <small>{{ results.comments.length }}</small></h2><article v-for="item in results.comments" :key="item.id" class="comment-search-result"><b>{{ item.username }}</b> 在《{{ item.animeTitle }}》中说：{{ item.content }}</article></section><p v-if="!allResults.length && !results.comments.length" class="catalog-state">没有找到相关内容。</p></template></section></template>

<script setup>
import { Film, MessageCircle, Search, Star } from 'lucide-vue-next'
import { onMounted, onUnmounted, ref } from 'vue'
import { apiJson } from '../api'

const keyword = ref('')
const results = ref({ anime: [], comments: [] })
const loading = ref(false)
const searched = ref(false)
const loadError = ref('')

async function search() {
  if (!keyword.value.trim()) return
  window.location.hash = `/search?q=${encodeURIComponent(keyword.value.trim())}`
  loading.value = true
  searched.value = true
  loadError.value = ''
  try {
    results.value = await apiJson(`/api/search?q=${encodeURIComponent(keyword.value.trim())}`)
  } catch (error) {
    results.value = { anime: [], comments: [] }
    loadError.value = error.message
  } finally {
    loading.value = false
  }
}
function syncKeyword() {
  const next = new URLSearchParams(window.location.hash.split('?')[1] || '').get('q') || ''
  if (next && next !== keyword.value) { keyword.value = next; search() }
}
function openDetail(id) { window.location.hash = `/detail?id=${id}` }
onMounted(() => { keyword.value = new URLSearchParams(window.location.hash.split('?')[1] || '').get('q') || ''; if (keyword.value) search(); window.addEventListener('hashchange', syncKeyword) })
onUnmounted(() => window.removeEventListener('hashchange', syncKeyword))
</script>

<template>
  <section class="page-view wrap search-page">
    <div class="search-heading"><div><span class="section-kicker">SEARCH THE SCREENROOM</span><h1>全站搜索</h1><p>搜索作品、电影和同好评论。</p></div><div class="global-search"><Search :size="18" /><input v-model="keyword" @keyup.enter="search" placeholder="搜索作品、评论内容" /><button @click="search">搜索</button></div></div>
    <p v-if="loading" class="catalog-state">正在搜索“{{ keyword }}”...</p>
    <p v-else-if="loadError" class="catalog-state error">{{ loadError }}</p>
    <template v-else-if="searched">
      <div class="search-count">与“{{ keyword }}”相关的结果</div>
      <section v-if="results.anime.length" class="search-block"><h2><Film :size="18" /> 作品 <small>{{ results.anime.length }}</small></h2><div class="search-results"><article v-for="item in results.anime" :key="item.id" class="search-result reveal-up" @click="openDetail(item.id)"><img :src="item.coverUrl" :alt="item.title" /><div><span>{{ item.mediaType === 'MOVIE' ? '电影' : '番剧' }} · {{ item.season }}</span><h3>{{ item.title }}</h3><p>{{ (item.genres || []).join(' · ') }}</p><strong><Star :size="14" fill="currentColor" /> {{ Number(item.score).toFixed(1) }} · {{ Number(item.scoreCount).toLocaleString() }}人评分</strong></div></article></div></section>
      <section v-if="results.comments.length" class="search-block"><h2><MessageCircle :size="18" /> 评论 <small>{{ results.comments.length }}</small></h2><article v-for="item in results.comments" :key="item.id" class="comment-search-result"><b>{{ item.nickname || item.username }}</b> 在《{{ item.animeTitle }}》中说：{{ item.content }}</article></section>
      <p v-if="!results.anime.length && !results.comments.length" class="catalog-state">没有找到相关内容。</p>
    </template>
  </section>
</template>

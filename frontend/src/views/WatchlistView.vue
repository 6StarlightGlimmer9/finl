<script setup>
import { Bookmark, ChevronRight, Heart, Play, Star, Trash2 } from 'lucide-vue-next'
import { computed, onMounted, ref } from 'vue'
import { apiJson, getStoredUser, showToast } from '../api'

const items = ref([])
const loading = ref(true)
const activeStatus = ref('ALL')
const options = [
  { value: 'ALL', label: '全部片单' },
  { value: 'WANT', label: '想看' },
  { value: 'WATCHING', label: '在看' },
  { value: 'WATCHED', label: '看过' }
]
const statusLabel = { WANT: '想看', WATCHING: '在看', WATCHED: '看过' }

const visibleItems = computed(() => activeStatus.value === 'ALL'
  ? items.value
  : items.value.filter(item => item.status === activeStatus.value))
const counts = computed(() => Object.fromEntries(options.map(option => [
  option.value,
  option.value === 'ALL' ? items.value.length : items.value.filter(item => item.status === option.value).length
])))

function openDetail(id) { window.location.hash = `/detail?id=${id}` }

async function loadItems() {
  loading.value = true
  try {
    items.value = await apiJson('/api/watchlist')
  } catch (error) {
    showToast(error.message, 'error')
  } finally {
    loading.value = false
  }
}

async function changeStatus(item, status) {
  try {
    const updated = await apiJson(`/api/watchlist/${item.animeId}`, {
      method: 'POST',
      body: JSON.stringify({ status })
    })
    const index = items.value.findIndex(row => row.animeId === updated.animeId)
    if (index >= 0) items.value.splice(index, 1, updated)
    showToast(`已将《${updated.title}》标记为${statusLabel[updated.status]}`)
  } catch (error) {
    showToast(error.message, 'error')
  }
}

async function removeItem(item) {
  try {
    await apiJson(`/api/watchlist/${item.animeId}`, { method: 'DELETE' })
    items.value = items.value.filter(row => row.animeId !== item.animeId)
    showToast(`已从片单移除《${item.title}》`)
  } catch (error) {
    showToast(error.message, 'error')
  }
}

onMounted(() => {
  if (!getStoredUser()) { window.location.hash = '/'; return }
  loadItems()
})
</script>

<template>
  <section class="page-view wrap watchlist-page">
    <div class="page-intro">
      <div><span class="section-kicker">MY WATCHLIST / 追番片单</span><h1>我的放映清单</h1><p>把想看、在看、看过的作品整理成自己的观看轨迹。</p></div>
      <div class="index-stat"><b>{{ items.length }}</b><span>部作品已收录</span></div>
    </div>
    <div class="watch-tabs">
      <button v-for="option in options" :key="option.value" :class="{ active: activeStatus === option.value }" @click="activeStatus = option.value">
        {{ option.label }} <small>{{ counts[option.value] }}</small>
      </button>
    </div>
    <p v-if="loading" class="catalog-state">正在整理你的片单...</p>
    <p v-else-if="!visibleItems.length" class="catalog-state">这里还没有作品，去详情页点击“想看 / 在看 / 看过”添加吧。</p>
    <div v-else class="watch-grid">
      <article v-for="item in visibleItems" :key="item.animeId" class="watch-card">
        <div class="watch-cover" @click="openDetail(item.animeId)"><img :src="item.coverUrl" :alt="item.title" /><span>{{ statusLabel[item.status] }}</span><button title="打开详情"><Play :size="18" fill="currentColor" /></button></div>
        <div class="watch-copy">
          <h3 @click="openDetail(item.animeId)">{{ item.title }}</h3>
          <p><Star :size="14" fill="currentColor" /> {{ Number(item.score).toFixed(1) }} · 社区真实评分</p>
          <div class="watch-status-row">
            <button v-for="option in options.slice(1)" :key="option.value" :class="{ active: item.status === option.value }" @click="changeStatus(item, option.value)">{{ option.label }}</button>
          </div>
          <div class="watch-footer"><small>更新于 {{ item.updatedAt ? item.updatedAt.slice(0, 10) : '—' }}</small><button title="移除片单" @click="removeItem(item)"><Trash2 :size="15" /></button></div>
        </div>
      </article>
    </div>
    <div class="watch-guide"><Bookmark :size="18" /><span>小提示：片单与评分相互独立，先看后评更能留下真实感受。</span><ChevronRight :size="16" /></div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { PenLine, Star, ThumbsUp, X } from 'lucide-vue-next'
import { getStoredUser } from '../api'

const reviews = ref([
  { title: '当青春期遇见宇宙的裂缝', author: '月面散步', score: '9.5', likes: 342, liked: false, text: '《胆大党》最迷人的地方，是它从来不把青春的笨拙藏起来。每一次大喊大叫背后，都有角色认真活着的证据。' },
  { title: '温柔不是故事的终点', author: '夏日存档', score: '9.2', likes: 218, liked: false, text: '那些关于离别与相遇的段落，被做成了很轻的风。没有煽情，却在片尾曲响起来时突然想起很多人。' },
  { title: '今年最让人惊喜的群像', author: '胶片研究所', score: '8.9', likes: 156, liked: false, text: '角色之间的化学反应比主线更有生命力，笑点和情绪落点都很准确，推荐和朋友一起看。' }
])
const writing = ref(false)
const draftTitle = ref('')
const draftText = ref('')

function requireLogin(message) {
  if (getStoredUser()) return true
  window.dispatchEvent(new CustomEvent('screenroom:login-required', { detail: { message } }))
  return false
}
function openWriting() {
  if (!requireLogin('登录后可发表漫评')) return
  writing.value = true
}
function toggleLike(review) {
  if (!requireLogin('登录后可点赞漫评')) return
  review.liked = !review.liked
  review.likes += review.liked ? 1 : -1
}
function publishReview() {
  if (!requireLogin('登录后可发布漫评')) return
  if (!draftTitle.value.trim() || !draftText.value.trim()) return
  reviews.value.unshift({ title: draftTitle.value.trim(), author: '我', score: '待评分', likes: 0, liked: false, text: draftText.value.trim() })
  draftTitle.value = ''; draftText.value = ''; writing.value = false
}
</script>
<template>
  <section class="page-view wrap">
    <div class="page-intro"><div><span class="section-kicker">EDITORIAL / COMMUNITY</span><h1>漫评</h1><p>认真写下观看之后，仍然留在心里的那一点感受。</p></div><button class="primary-button" @click="openWriting"><PenLine :size="16" /> 写一篇漫评</button></div>
    <div class="review-layout"><div class="review-feed"><article v-for="review in reviews" :key="review.title" class="review-card"><div class="review-meta"><span class="review-avatar">{{ review.author[0] }}</span><span>{{ review.author }}</span><small>社区投稿 · 刚刚</small><strong v-if="review.score !== '待评分'"><Star :size="14" fill="currentColor" /> {{ review.score }}</strong></div><h2>{{ review.title }}</h2><p>{{ review.text }}</p><button class="like-button" :class="{ liked: review.liked }" @click="toggleLike(review)"><ThumbsUp :size="14" /> {{ review.likes }}</button></article></div><aside class="review-side"><h3>本周写作榜</h3><p>01　月面散步　<span>12 篇</span></p><p>02　夏日存档　<span>9 篇</span></p><p>03　胶片研究所　<span>8 篇</span></p></aside></div>
    <div v-if="writing" class="modal-backdrop" @click.self="writing = false"><div class="write-modal"><button class="close-button" @click="writing = false"><X :size="18" /></button><span class="section-kicker">NEW REVIEW</span><h2>写下你的观感</h2><input v-model="draftTitle" placeholder="给这篇漫评起个标题" /><textarea v-model="draftText" rows="6" placeholder="分享你的观看感受"></textarea><button class="primary-button full" @click="publishReview">发布漫评</button></div></div>
  </section>
</template>

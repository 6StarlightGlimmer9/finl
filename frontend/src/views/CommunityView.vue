<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowUpRight, Calendar, Film, Hash, MessageCircle, PenLine, Plus, Search, Send, Sparkles, ThumbsUp, Users, X } from 'lucide-vue-next'
import { apiJson, getStoredUser, showToast } from '../api'

const data = ref({ topics: [], groups: [], events: [], comments: [], hotAnime: [], stats: {}, writers: [], trendingTags: [] })
const loading = ref(true)
const notice = ref('')
const keyword = ref('')
const colorFilter = ref('all')
const creating = ref(false)
const topicDraft = ref({ title: '', description: '', color: 'pink' })
const activeTopic = ref(null)
const topicPosts = ref([])
const replyText = ref('')
const colorFilters = [
  { value: 'all', label: '全部' },
  { value: 'pink', label: '粉色' },
  { value: 'blue', label: '蓝色' },
  { value: 'black', label: '深色' }
]

const stats = computed(() => data.value.stats || {})
const activeEvent = computed(() => data.value.events?.[0] || null)
const filteredTopics = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return (data.value.topics || []).filter(topic => {
    if (colorFilter.value !== 'all' && topic.color !== colorFilter.value) return false
    if (!query) return true
    return [topic.title, topic.description, topic.tag, topic.author].join(' ').toLowerCase().includes(query)
  })
})

function requireLogin(message) {
  if (getStoredUser()) return true
  window.dispatchEvent(new CustomEvent('screenroom:login-required', { detail: { message } }))
  return false
}
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '' }
function openDetail(id) { window.location.hash = `/detail?id=${id}` }
function goReviews() { window.location.hash = '/reviews' }

async function load() {
  loading.value = true
  notice.value = ''
  try {
    const result = await apiJson('/api/community?limit=16')
    data.value = {
      topics: result.topics || [], groups: result.groups || [], events: result.events || [],
      comments: result.comments || [], hotAnime: result.hotAnime || [],
      stats: result.stats || {}, writers: result.writers || [], trendingTags: result.trendingTags || []
    }
  } catch (error) {
    notice.value = error.message
  } finally {
    loading.value = false
  }
}
function openCreate() {
  if (!requireLogin('登录后可发起话题')) return
  creating.value = true
}
async function createTopic() {
  if (!requireLogin('登录后可发布话题')) return
  if (!topicDraft.value.title.trim()) {
    showToast('请填写话题标题', 'error')
    return
  }
  try {
    const created = await apiJson('/api/community/topics', {
      method: 'POST',
      body: JSON.stringify({
        title: topicDraft.value.title.trim(),
        description: topicDraft.value.description.trim(),
        color: topicDraft.value.color
      })
    })
    data.value.topics.unshift(created)
    data.value.stats.topicCount = Number(data.value.stats.topicCount || 0) + 1
    topicDraft.value = { title: '', description: '', color: 'pink' }
    creating.value = false
    showToast('话题已发布')
  } catch (error) {
    showToast(error.message, 'error')
  }
}
async function openTopic(topic) {
  try {
    const result = await apiJson(`/api/community/topics/${topic.id}`)
    activeTopic.value = result.topic || topic
    topicPosts.value = result.posts || []
    replyText.value = ''
  } catch (error) {
    showToast(error.message, 'error')
  }
}
async function submitReply() {
  if (!requireLogin('登录后可参与话题')) return
  if (!replyText.value.trim() || !activeTopic.value) return
  try {
    const post = await apiJson(`/api/community/topics/${activeTopic.value.id}/posts`, {
      method: 'POST',
      body: JSON.stringify({ content: replyText.value.trim() })
    })
    topicPosts.value.push(post)
    activeTopic.value.postCount = Number(activeTopic.value.postCount || 0) + 1
    const target = data.value.topics.find(item => item.id === activeTopic.value.id)
    if (target) target.postCount = activeTopic.value.postCount
    data.value.stats.postCount = Number(data.value.stats.postCount || 0) + 1
    replyText.value = ''
    showToast('回复已发布')
  } catch (error) {
    showToast(error.message, 'error')
  }
}
async function toggleGroup(group) {
  if (!requireLogin('登录后可加入小组')) return
  try {
    const result = await apiJson(`/api/community/groups/${group.id}/join`, { method: 'POST' })
    group.joined = result.joined
    group.memberCount = result.memberCount
  } catch (error) {
    showToast(error.message, 'error')
  }
}
async function toggleEvent(event) {
  if (!requireLogin('登录后可报名放映会')) return
  try {
    const result = await apiJson(`/api/community/events/${event.id}/signup`, { method: 'POST' })
    event.signedUp = result.signedUp
    event.attendeeCount = result.attendeeCount
  } catch (error) {
    showToast(error.message, 'error')
  }
}
async function toggleCommentLike(item) {
  if (!requireLogin('登录后可点赞评论')) return
  try {
    const result = await apiJson(`/api/comments/${item.id}/like`, { method: 'POST' })
    item.liked = result.liked
    item.likes = result.likes
  } catch (error) {
    showToast(error.message, 'error')
  }
}

onMounted(load)
</script>

<template>
  <section class="page-view wrap social-page community-page">
    <div class="page-intro social-intro">
      <div>
        <span class="section-kicker">COMMUNITY / 我们的主打</span>
        <h1>同好社群</h1>
        <p>话题、小组、放映会与真实评论都在这里继续发生，让看完之后的交流不随着播放器关闭而结束。</p>
        <div class="social-stats">
          <span><b>{{ stats.topicCount || 0 }}</b> 个话题</span>
          <span><b>{{ stats.postCount || 0 }}</b> 条回复</span>
          <span><b>{{ stats.groupCount || 0 }}</b> 个小组</span>
          <span><b>{{ stats.memberCount || 0 }}</b> 次加入</span>
        </div>
      </div>
      <button class="primary-button" @click="openCreate"><Plus :size="16" /> 创建话题</button>
    </div>

    <div class="social-banner community-banner">
      <div><span class="section-kicker">FIND YOUR PEOPLE</span><h2>从一个话题开始，找到愿意认真聊天的人</h2><p>这里的小组、话题和放映会都会真实记录到数据库，登录后的加入、报名与回复不会刷新丢失。</p></div>
      <div class="banner-icons"><Users :size="34" /><PenLine :size="34" /><Sparkles :size="34" /></div>
    </div>

    <div class="social-layout">
      <div class="social-main">
        <div class="social-toolbar">
          <label class="social-search"><Search :size="17" /><input v-model="keyword" placeholder="搜索话题、标签或发起人" /></label>
          <div class="color-filters"><button v-for="item in colorFilters" :key="item.value" :class="{ active: colorFilter === item.value }" @click="colorFilter = item.value">{{ item.label }}</button></div>
        </div>
        <div class="subheading"><h2>话题广场</h2><span>{{ filteredTopics.length }} 个话题</span></div>
        <p v-if="loading" class="catalog-state">正在读取社群...</p>
        <p v-else-if="notice" class="catalog-state error">{{ notice }}</p>
        <p v-else-if="!filteredTopics.length" class="catalog-state">没有匹配的话题，换个筛选条件试试。</p>
        <article v-for="topic in filteredTopics" :key="topic.id" class="topic-card social-topic-card" :class="topic.color" @click="openTopic(topic)">
          <div class="topic-icon"><Hash :size="21" /></div>
          <div class="topic-copy"><h3>{{ topic.title }}</h3><p>{{ topic.description }}</p><small>{{ topic.tag }} · {{ topic.postCount }} 条回复 · {{ topic.participantCount }} 人参与 · {{ topic.author }} 发起</small></div>
          <ArrowUpRight :size="19" />
        </article>

        <div class="subheading" style="margin-top:24px"><h2>全站最新评论</h2><span>来自真实用户，可直接点赞</span></div>
        <div v-if="!data.comments.length" class="empty-admin">还没有评论，去详情页留下第一印象吧。</div>
        <article v-for="item in data.comments" :key="item.id" class="community-comment" @click="openDetail(item.animeId)">
          <span class="community-avatar">{{ (item.nickname || item.username || '漫').slice(0, 1) }}</span>
          <div><div><b>{{ item.nickname || item.username }}</b><small>在《{{ item.animeTitle }}》中说 · {{ formatDate(item.createdAt) }}</small></div><p>{{ item.content }}</p></div>
          <button class="like-button" :class="{ liked: item.liked }" @click.stop="toggleCommentLike(item)"><ThumbsUp :size="13" /> {{ item.likes }}</button>
        </article>
      </div>

      <aside class="social-side">
        <section class="side-card"><h3>活跃创作者</h3><div v-for="(writer, index) in data.writers" :key="writer.nickname" class="writer-row"><b>{{ String(index + 1).padStart(2, '0') }}</b><span>{{ writer.nickname }}</span><small>{{ writer.count }} 篇 · {{ writer.likes }} 赞</small></div><button class="text-link" @click="goReviews">去漫评页 <ArrowUpRight :size="14" /></button></section>
        <section class="side-card"><h3>热门小组</h3><div v-for="group in data.groups" :key="group.id" class="group-row"><div class="group-symbol"><Users :size="18" /></div><div><b>{{ group.name || group.title }}</b><small>{{ group.tag }} · {{ Number(group.memberCount).toLocaleString() }} 人</small></div><button :class="{ joined: group.joined }" @click="toggleGroup(group)">{{ group.joined ? '已加入' : '加入' }}</button></div></section>
        <section v-if="activeEvent" class="side-card event-card"><h3>放映会报名</h3><div class="event-title"><Calendar :size="18" /><b>{{ activeEvent.title }}</b></div><p>{{ activeEvent.description }}</p><small>{{ activeEvent.tag }} · {{ Number(activeEvent.attendeeCount).toLocaleString() }} 人已报名</small><button class="primary-button full" :class="{ joined: activeEvent.signedUp }" @click="toggleEvent(activeEvent)">{{ activeEvent.signedUp ? '取消报名' : '立即报名' }}</button></section>
        <section class="side-card"><h3>热议作品</h3><div v-for="item in data.hotAnime" :key="item.animeId" class="community-hot" @click="openDetail(item.animeId)"><img :src="item.coverUrl" :alt="item.title" /><div><b>{{ item.title }}</b><small><MessageCircle :size="12" /> {{ item.commentCount }} 条讨论</small></div><Film :size="15" /></div></section>
        <section class="side-card"><h3>趋势标签</h3><div class="tag-cloud"><button v-for="tag in data.trendingTags" :key="tag.name" @click="keyword = tag.name">{{ tag.name }} <small>{{ tag.count }}</small></button></div></section>
      </aside>
    </div>

    <div v-if="creating" class="modal-backdrop" @click.self="creating = false">
      <div class="write-modal social-modal">
        <button class="close-button" @click="creating = false"><X :size="18" /></button>
        <span class="section-kicker">NEW TOPIC</span><h2>发起一个话题</h2>
        <label>标题<input v-model="topicDraft.title" placeholder="大家今晚想聊什么？" /></label>
        <label>主题色<select v-model="topicDraft.color"><option value="pink">粉色</option><option value="blue">蓝色</option><option value="black">深色</option></select></label>
        <label>开场白<textarea v-model="topicDraft.description" rows="4" placeholder="先写一句，让大家知道这个话题想聊什么"></textarea></label>
        <button class="primary-button full" @click="createTopic">发布话题</button>
      </div>
    </div>

    <div v-if="activeTopic" class="modal-backdrop" @click.self="activeTopic = null">
      <div class="write-modal topic-modal">
        <button class="close-button" @click="activeTopic = null"><X :size="18" /></button>
        <span class="section-kicker">TOPIC / {{ activeTopic.tag }}</span><h2>{{ activeTopic.title }}</h2>
        <p class="topic-modal-intro">{{ activeTopic.description }}</p>
        <div class="topic-post-list">
          <article v-for="post in topicPosts" :key="post.id" class="topic-post"><span class="community-avatar">{{ (post.author || '漫').slice(0, 1) }}</span><div><b>{{ post.author }}</b><small>{{ formatDate(post.createdAt) }}</small><p>{{ post.content }}</p></div></article>
          <p v-if="!topicPosts.length" class="side-empty">还没有回复，来说第一句吧。</p>
        </div>
        <label class="topic-reply"><textarea v-model="replyText" rows="3" placeholder="写下你的想法..."></textarea><button class="primary-button" @click="submitReply"><Send :size="15" /> 回复</button></label>
      </div>
    </div>
  </section>
</template>

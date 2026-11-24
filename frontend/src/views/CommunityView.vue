<script setup>
import { Calendar, Hash, MessageCircle, Plus, Users, X } from 'lucide-vue-next'
import { ref } from 'vue'
const topics = ref([{ title: '本季新番交换观感', count: '2,431 人参与', color: 'pink' }, { title: '深夜放映：一起看老番', count: '876 人参与', color: 'black' }, { title: '你的年度神作是哪部？', count: '1,205 人参与', color: 'blue' }])
const groups = ref([{ name: '宇宙电波俱乐部', members: '8,420', tag: '科幻 / 奇幻', joined: false }, { name: '少女漫放映室', members: '5,108', tag: '恋爱 / 治愈', joined: false }, { name: '作画观察协会', members: '3,662', tag: '动画研究', joined: false }])
const creating = ref(false)
const topicTitle = ref('')
const eventJoined = ref(false)
function createTopic() { if (!topicTitle.value.trim()) return; topics.value.unshift({ title: topicTitle.value.trim(), count: '1 人参与', color: 'pink' }); topicTitle.value = ''; creating.value = false }
function toggleGroup(group) { group.joined = !group.joined }
</script>
<template>
  <section class="page-view wrap"><div class="page-intro"><div><span class="section-kicker">FIND YOUR PEOPLE</span><h1>同好社群</h1><p>在不同的观看角度里，遇见愿意认真聊天的人。</p></div><button class="primary-button" @click="creating = true"><Plus :size="16" /> 创建话题</button></div><div class="community-grid"><section><div class="subheading"><h2>正在热聊</h2><span>全部话题 →</span></div><article v-for="topic in topics" :key="topic.title" class="topic-card" :class="topic.color"><div class="topic-icon"><Hash :size="21" /></div><div><h3>{{ topic.title }}</h3><p>{{ topic.count }} · 最近 12 分钟有新回复</p></div><MessageCircle :size="19" /></article></section><aside class="group-panel"><h2>热门小组</h2><div v-for="group in groups" :key="group.name" class="group-row"><div class="group-symbol"><Users :size="18" /></div><div><b>{{ group.name }}</b><small>{{ group.tag }} · {{ group.members }} 人</small></div><button @click="toggleGroup(group)">{{ group.joined ? '已加入' : '加入' }}</button></div><button class="calendar-note" :class="{ joined: eventJoined }" @click="eventJoined = !eventJoined"><Calendar :size="18" /><span>今晚 21:00<br><b>{{ eventJoined ? '已报名：胆大党连载放映会' : '《胆大党》连载放映会' }}</b></span></button></aside></div><div v-if="creating" class="modal-backdrop" @click.self="creating = false"><div class="write-modal"><button class="close-button" @click="creating = false"><X :size="18" /></button><span class="section-kicker">NEW TOPIC</span><h2>发起一个话题</h2><input v-model="topicTitle" @keyup.enter="createTopic" placeholder="大家今晚想聊什么？" /><button class="primary-button full" @click="createTopic">发布话题</button></div></div></section>
</template>

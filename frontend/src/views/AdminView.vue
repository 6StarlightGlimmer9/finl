<script setup>
import { BarChart3, Film, MessageCircle, Pencil, Save, ShieldCheck, Trash2, Users } from 'lucide-vue-next'
import { onMounted, ref } from 'vue'
const users = ref([{ id: 1, name: 'screenroom', email: 'screenroom@example.com', role: '管理员', joined: '2026-08-27' }, { id: 2, name: '月面散步', email: 'moon@example.com', role: '普通用户', joined: '2026-08-21' }, { id: 3, name: '夏日存档', email: 'summer@example.com', role: '普通用户', joined: '2026-08-19' }])
const anime = ref([])
const selectedAnime = ref(null)
const tagInput = ref('')
const comments = ref([])
const notice = ref('')
onMounted(async () => {
	const current = JSON.parse(localStorage.getItem('screenroom-user') || 'null')
	if (!current) return
	try {
		const response = await fetch('/api/admin/users', { headers: { 'X-User-Role': current.role } })
		if (!response.ok) return
		users.value = (await response.json()).map(item => ({ ...item, name: item.username, role: item.role === 'ADMIN' ? '管理员' : '普通用户', joined: '已注册' }))
	} catch { }
	try { const response = await fetch('/api/anime'); if (response.ok) anime.value = await response.json() } catch { }
})
async function removeUser(user) {
	const current = JSON.parse(localStorage.getItem('screenroom-user') || 'null')
	try { await fetch(`/api/admin/users/${user.id}`, { method: 'DELETE', headers: { 'X-User-Role': current?.role || 'USER' } }) } catch { }
	users.value = users.value.filter(item => item.id !== user.id)
}
async function saveTags() {
	const current = JSON.parse(localStorage.getItem('screenroom-user') || 'null')
	if (!selectedAnime.value || !tagInput.value.trim()) return
	const genres = tagInput.value.split(/[,，、 ]+/).filter(Boolean)
	try { const response = await fetch(`/api/admin/anime/${selectedAnime.value.id}/genres`, { method: 'PUT', headers: { 'Content-Type': 'application/json', 'X-User-Role': current?.role || 'USER' }, body: JSON.stringify({ genres }) }); if (!response.ok) throw new Error('标签保存失败'); selectedAnime.value.genres = genres; notice.value = '标签已保存' } catch (error) { notice.value = error.message }
}
async function loadComments() {
	try { const response = await fetch('/api/anime/1/comments'); if (response.ok) comments.value = await response.json() } catch { }
}
async function deleteComment(comment) {
	const current = JSON.parse(localStorage.getItem('screenroom-user') || 'null')
	try { await fetch(`/api/admin/comments/${comment.id}`, { method: 'DELETE', headers: { 'X-User-Role': current?.role || 'USER' } }) } catch { }
	comments.value = comments.value.filter(item => item.id !== comment.id); notice.value = '评论已删除'
}
</script>
<template><section class="page-view wrap admin-page"><div class="admin-heading"><div><span class="section-kicker">CONTROL ROOM / ADMIN</span><h1>管理员后台</h1><p>管理用户、作品和社区内容。</p></div><span class="admin-badge"><ShieldCheck :size="16" /> 管理员模式</span></div><div class="admin-stats"><div><Users :size="20" /><span>注册用户</span><b>1,286</b><small>较上周 +8.4%</small></div><div><Film :size="20" /><span>收录作品</span><b>248</b><small>本月新增 16 部</small></div><div><BarChart3 :size="20" /><span>今日评分</span><b>3,842</b><small>较昨日 +12.1%</small></div></div><div class="admin-panel"><div class="subheading"><h2>用户管理</h2><span>最近注册 · 共 1,286 人</span></div><div class="user-table"><div class="user-row user-head"><span>用户</span><span>邮箱</span><span>角色</span><span>注册时间</span><span>操作</span></div><div v-for="user in users" :key="user.id" class="user-row"><span><b class="table-avatar">{{ user.name[0] }}</b>{{ user.name }}</span><span>{{ user.email }}</span><span><i :class="{ admin: user.role === '管理员' }">{{ user.role }}</i></span><span>{{ user.joined }}</span><button v-if="user.role !== '管理员'" @click="removeUser(user)" title="删除用户"><Trash2 :size="15" /></button><span v-else class="protected">受保护</span></div></div></div><div class="admin-tools"><section class="admin-panel"><div class="subheading"><h2><Pencil :size="17" /> 作品标签</h2><span>仅管理员可修改</span></div><select v-model="selectedAnime" @change="tagInput = selectedAnime.genres?.join('、') || ''"><option :value="null">选择番剧或电影</option><option v-for="item in anime" :key="item.id" :value="item">{{ item.title }}</option></select><div class="tag-editor"><input v-model="tagInput" placeholder="例如：奇幻、冒险、治愈" /><button @click="saveTags"><Save :size="15" /> 保存</button></div></section><section class="admin-panel"><div class="subheading"><h2><MessageCircle :size="17" /> 评论审核</h2><button @click="loadComments">刷新</button></div><p v-if="!comments.length" class="empty-admin">点击刷新加载评论</p><div v-for="comment in comments" :key="comment.id" class="admin-comment"><span><b>{{ comment.username }}</b><small>{{ comment.content }}</small></span><button @click="deleteComment(comment)"><Trash2 :size="15" /></button></div></section></div><p v-if="notice" class="admin-notice">{{ notice }}</p></section></template>

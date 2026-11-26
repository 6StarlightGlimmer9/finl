// pages/topic/index.js · 话题详情与回复
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')

Page({
  data: {
    id: '',
    topic: null,
    posts: [],
    reply: '',
    loading: true,
    error: ''
  },

  onLoad(options) {
    const id = options && options.id
    if (!id) { api.toast('缺少话题编号'); wx.navigateBack(); return }
    this.setData({ id: String(id) })
    this.load()
  },

  onPullDownRefresh() {
    this.load().then(function () { wx.stopPullDownRefresh() })
  },

  load() {
    const that = this
    this.setData({ loading: true, error: '' })
    return api.get('/community/topics/' + this.data.id).then(function (res) {
      const data = res || {}
      const posts = util.safeArray(data.posts).map(function (item) {
        return Object.assign({}, item, {
          initial: util.initial(item.author),
          time: util.timeAgo(item.createdAt)
        })
      })
      that.setData({ topic: data.topic || null, posts: posts, loading: false })
      if (data.topic && data.topic.title) wx.setNavigationBarTitle({ title: data.topic.title })
    }).catch(function (err) {
      that.setData({ loading: false, error: err.message || '话题加载失败' })
    })
  },

  onReplyInput(e) { this.setData({ reply: e.detail.value }) },

  submitReply() {
    const that = this
    if (!api.requireLogin('登录后才能参与话题讨论')) return
    const content = String(this.data.reply || '').trim()
    if (!content) { api.toast('写点什么吧'); return }
    api.post('/community/topics/' + this.data.id + '/posts', { content: content }).then(function (res) {
      const item = Object.assign({}, res || {}, {
        content: content,
        initial: util.initial((res || {}).author),
        time: '刚刚'
      })
      that.setData({ posts: that.data.posts.concat([item]), reply: '' })
      getApp().globalData.communityDirty = true
      api.toast('回复已发布')
    }).catch(function (err) { api.toast(err.message) })
  }
})

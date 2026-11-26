// pages/community/index.js · 话题 / 小组 / 放映会 / 热议短评
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')

const TABS = [
  { label: '话题广场', value: 'topics' },
  { label: '同好小组', value: 'groups' },
  { label: '放映会', value: 'events' },
  { label: '最新短评', value: 'comments' }
]
const COLORS = [
  { label: '粉', value: 'pink' },
  { label: '蓝', value: 'blue' },
  { label: '墨', value: 'black' }
]

function emptyForm() { return { title: '', description: '', color: 'pink' } }

Page({
  data: {
    tabs: TABS,
    colors: COLORS,
    tab: 'topics',
    topics: [],
    groups: [],
    events: [],
    comments: [],
    hotAnime: [],
    trendingTags: [],
    writers: [],
    stats: {},
    topicOpen: false,
    form: emptyForm(),
    submitting: false,
    loading: true,
    error: ''
  },

  onLoad() { this.load() },

  onShow() {
    api.syncTabBar(this, 3)
    api.setTabBarVisible(!this.data.topicOpen, this)
    // 仅在话题详情页发布过回复后刷新，避免与 onLoad 重复请求
    if (getApp().globalData.communityDirty) {
      getApp().globalData.communityDirty = false
      this.load()
    }
  },

  onHide() { api.setTabBarVisible(true, this) },

  onPullDownRefresh() {
    this.load().then(function () { wx.stopPullDownRefresh() })
  },

  load() {
    const that = this
    this.setData({ loading: true, error: '' })
    return api.get('/community', { limit: 16 }).then(function (res) {
      const data = res || {}
      that.setData({
        topics: util.safeArray(data.topics),
        groups: util.safeArray(data.groups),
        events: util.safeArray(data.events),
        comments: util.safeArray(data.comments).map(util.decorateComment),
        hotAnime: util.safeArray(data.hotAnime).map(function (item) {
          return Object.assign({}, item, { cover: util.imageUrl(item.coverUrl) })
        }),
        trendingTags: util.safeArray(data.trendingTags),
        writers: util.safeArray(data.writers),
        stats: data.stats || {},
        loading: false
      })
    }).catch(function (err) {
      that.setData({ loading: false, error: err.message || '社群数据加载失败' })
    })
  },

  switchTab(e) { this.setData({ tab: e.currentTarget.dataset.value }) },

  openTopic(e) {
    wx.navigateTo({ url: '/pages/topic/index?id=' + e.currentTarget.dataset.id })
  },

  openHotAnime(e) { api.goDetail(e.currentTarget.dataset.id) },

  joinGroup(e) {
    const that = this
    if (!api.requireLogin('登录后才能加入同好小组')) return
    const id = e.currentTarget.dataset.id
    const index = Number(e.currentTarget.dataset.index)
    api.post('/community/groups/' + id + '/join').then(function (res) {
      const patch = {}
      patch['groups[' + index + '].joined'] = !!res.joined
      patch['groups[' + index + '].memberCount'] = util.safeNumber(res.memberCount)
      that.setData(patch)
      api.toast(res.joined ? '已加入小组' : '已退出小组')
    }).catch(function (err) { api.toast(err.message) })
  },

  signupEvent(e) {
    const that = this
    if (!api.requireLogin('登录后才能报名放映会')) return
    const id = e.currentTarget.dataset.id
    const index = Number(e.currentTarget.dataset.index)
    api.post('/community/events/' + id + '/signup').then(function (res) {
      const patch = {}
      patch['events[' + index + '].signedUp'] = !!res.signedUp
      patch['events[' + index + '].attendeeCount'] = util.safeNumber(res.attendeeCount)
      that.setData(patch)
      api.toast(res.signedUp ? '报名成功' : '已取消报名')
    }).catch(function (err) { api.toast(err.message) })
  },

  likeComment(e) {
    const that = this
    if (!api.requireLogin('登录后才能点赞短评')) return
    const id = e.currentTarget.dataset.id
    const index = Number(e.currentTarget.dataset.index)
    api.post('/comments/' + id + '/like').then(function (res) {
      const patch = {}
      patch['comments[' + index + '].liked'] = !!res.liked
      patch['comments[' + index + '].likes'] = util.safeNumber(res.likes)
      that.setData(patch)
    }).catch(function (err) { api.toast(err.message) })
  },

  openTopicModal() {
    if (!api.requireLogin('登录后才能发起新话题')) return
    this.setData({ topicOpen: true, form: emptyForm() })
    api.setTabBarVisible(false, this)
  },
  closeTopicModal() {
    this.setData({ topicOpen: false })
    api.setTabBarVisible(true, this)
  },
  noop() {},

  onTopicInput(e) {
    const field = e.currentTarget.dataset.field
    const patch = {}
    patch['form.' + field] = e.detail.value
    this.setData(patch)
  },
  onColor(e) {
    const patch = {}
    patch['form.color'] = e.currentTarget.dataset.value
    this.setData(patch)
  },

  submitTopic() {
    const that = this
    const form = this.data.form
    const title = String(form.title || '').trim()
    if (!title) { api.toast('请填写话题标题'); return }
    this.setData({ submitting: true })
    api.post('/community/topics', {
      title: title,
      description: String(form.description || '').trim(),
      color: form.color
    }).then(function () {
      that.setData({ topicOpen: false, submitting: false, tab: 'topics' })
      api.setTabBarVisible(true, that)
      api.toast('话题已发布')
      return that.load()
    }).catch(function (err) {
      that.setData({ submitting: false })
      api.toast(err.message)
    })
  }
})

// pages/index/index.js · 首页
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')

Page({
  data: {
    slides: [],
    featured: [],
    topList: [],
    reviews: [],
    comments: [],
    stats: null,
    loading: true,
    error: '',
    loggedIn: false
  },

  onLoad() {
    this.loadAll()
  },

  onShow() {
    api.syncTabBar(this, 0)
    this.setData({ loggedIn: api.isLoggedIn() })
  },

  onPullDownRefresh() {
    this.loadAll(true).then(function () { wx.stopPullDownRefresh() })
  },

  loadAll(force) {
    const that = this
    this.setData({ loading: true, error: '' })
    const cached = !force && getApp().globalData.animeList
    const animeTask = cached ? Promise.resolve(cached) : api.get('/anime')
    const reviewsTask = api.get('/reviews', { limit: 3 }).catch(function () { return {} })
    const communityTask = api.get('/community', { limit: 5 }).catch(function () { return {} })

    return Promise.all([animeTask, reviewsTask, communityTask]).then(function (results) {
      const rawList = util.safeArray(results[0])
      const list = rawList.map(util.decorateAnime)
      getApp().globalData.animeList = list
      const labels = ['NOW SCREENING / 本季热播', 'HOT ANIME / 同好热议', 'MOVIE PICK / 剧场精选']
      const slides = list.slice(0, 3).map(function (item, index) {
        return Object.assign({}, item, {
          label: labels[index],
          desc: util.compact(item.summary, 60) || '把值得看的故事，放映给懂的人。'
        })
      })
      const topList = list.slice().sort(function (a, b) {
        return util.safeNumber(b.score) - util.safeNumber(a.score)
      }).slice(0, 5).map(function (item, index) {
        return Object.assign({}, item, { rank: index + 1 })
      })
      that.setData({
        slides: slides,
        featured: list.slice(0, 9),
        topList: topList,
        reviews: util.safeArray(results[1].reviews).slice(0, 3).map(util.decorateReview),
        comments: util.safeArray(results[2].comments).slice(0, 3).map(util.decorateComment),
        stats: results[2].stats || null,
        loading: false
      })
    }).catch(function (err) {
      that.setData({ loading: false, error: err.message || '数据加载失败' })
    })
  },

  openDetail(e) {
    api.goDetail(e.currentTarget.dataset.id)
  },

  goSearch() {
    wx.navigateTo({ url: '/pages/search/index' })
  },

  openServer() {
    // 我的页面是 Tab，只能用 switchTab，通过全局标记直接打开连接设置弹层
    getApp().globalData.openServerSettings = true
    wx.switchTab({ url: '/pages/profile/index' })
  },

  goCatalog(e) {
    const type = e.currentTarget.dataset.type || 'ALL'
    const category = e.currentTarget.dataset.category || '全部'
    getApp().globalData.catalogFilter = { type: type, category: category }
    wx.switchTab({ url: '/pages/anime/index' })
  },

  goTab(e) {
    wx.switchTab({ url: e.currentTarget.dataset.url })
  },

  goWatchlist() {
    if (!api.requireLogin('登录后可管理我的追番片单')) return
    wx.navigateTo({ url: '/pages/watchlist/index' })
  },

  goTopic(e) {
    const id = e.currentTarget.dataset.id
    if (id) wx.navigateTo({ url: '/pages/topic/index?id=' + id })
    else wx.switchTab({ url: '/pages/community/index' })
  }
})

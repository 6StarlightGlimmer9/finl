// pages/profile/index.js · 个人中心、我的记录与服务器连接设置
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')
const config = require('../../config.js')

const SECTIONS = [
  { label: '我的评分', value: 'ratings' },
  { label: '我的短评', value: 'comments' },
  { label: '我的片单', value: 'watchlist' }
]

Page({
  data: {
    sections: SECTIONS,
    section: 'ratings',
    loggedIn: false,
    user: null,
    profile: null,
    initial: '放',
    displayName: '放映室访客',
    isAdmin: false,
    loading: false,
    error: '',
    serverOpen: false,
    apiBase: '',
    imgHost: ''
  },

  onShow() {
    api.syncTabBar(this, 4)
    api.setTabBarVisible(!this.data.serverOpen, this)
    const loggedIn = api.isLoggedIn()
    const user = api.getStoredUser()
    this.setData({
      loggedIn: loggedIn,
      user: user,
      isAdmin: api.isAdmin(),
      initial: util.initial(user && (user.nickname || user.username)),
      displayName: user ? (user.nickname || user.username) : '放映室访客'
    })
    if (loggedIn) this.loadProfile()
    else this.setData({ profile: null, error: '' })
    if (getApp().globalData.openServerSettings) {
      getApp().globalData.openServerSettings = false
      this.openServer()
    }
  },

  onHide() { api.setTabBarVisible(true, this) },

  loadProfile() {
    const that = this
    this.setData({ loading: true, error: '' })
    api.get('/auth/profile').then(function (res) {
      const data = res || {}
      that.setData({
        profile: Object.assign({}, data, {
          ratings: util.safeArray(data.ratings).map(util.decorateProfileRating),
          comments: util.safeArray(data.comments).map(util.decorateComment),
          watchlist: util.safeArray(data.watchlist).map(util.decorateWatchlist)
        }),
        loading: false,
        displayName: data.nickname || data.username || that.data.displayName
      })
    }).catch(function (err) {
      if (!api.isLoggedIn()) that.setData({ loggedIn: false, user: null, profile: null })
      that.setData({ loading: false, error: err.message || '个人资料加载失败' })
    })
  },

  switchSection(e) { this.setData({ section: e.currentTarget.dataset.value }) },
  goLogin() { wx.navigateTo({ url: '/pages/login/index' }) },
  goWatchlist() {
    if (!api.requireLogin('登录后可查看我的片单')) return
    wx.navigateTo({ url: '/pages/watchlist/index' })
  },
  goSearch() { wx.navigateTo({ url: '/pages/search/index' }) },
  goTab(e) { wx.switchTab({ url: e.currentTarget.dataset.url }) },
  openDetail(e) { api.goDetail(e.currentTarget.dataset.id) },

  logout() {
    const that = this
    wx.showModal({
      title: '退出登录',
      content: '确定要离开放映室吗？',
      success: function (res) {
        if (!res.confirm) return
        api.logout().then(function () {
          that.setData({ loggedIn: false, user: null, profile: null, isAdmin: false })
          api.toast('已退出登录')
        })
      }
    })
  },

  openServer() {
    this.setData({
      serverOpen: true,
      apiBase: config.getBaseUrl(),
      imgHost: config.getImgHost()
    })
    api.setTabBarVisible(false, this)
  },
  closeServer() {
    this.setData({ serverOpen: false })
    api.setTabBarVisible(true, this)
  },
  noop() {},

  onServerInput(e) {
    const field = e.currentTarget.dataset.field
    const patch = {}
    patch[field] = e.detail.value
    this.setData(patch)
  },

  saveServer() {
    const apiBase = String(this.data.apiBase || '').trim()
    const imgHost = String(this.data.imgHost || '').trim()
    if (!/^https?:\/\//.test(apiBase) || !/^https?:\/\//.test(imgHost)) {
      api.toast('地址需以 http:// 或 https:// 开头'); return
    }
    config.setServer(apiBase, imgHost)
    this.setData({ serverOpen: false })
    api.setTabBarVisible(true, this)
    api.toast('已保存')
  },

  resetServer() {
    config.resetServer()
    this.setData({ apiBase: config.DEFAULT_API_BASE, imgHost: config.DEFAULT_IMG_HOST })
    api.toast('已恢复默认地址')
  }
})

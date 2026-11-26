// pages/watchlist/index.js · 我的追番片单
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')

const FILTERS = [
  { label: '全部', value: 'ALL' },
  { label: '想看', value: 'WANT' },
  { label: '在看', value: 'WATCHING' },
  { label: '看过', value: 'WATCHED' }
]

Page({
  data: {
    filters: FILTERS,
    statusFilter: 'ALL',
    list: [],
    filtered: [],
    loading: true,
    error: '',
    loggedIn: false
  },

  onShow() {
    const loggedIn = api.isLoggedIn()
    this.setData({ loggedIn: loggedIn })
    if (loggedIn) this.load()
    else this.setData({ list: [], filtered: [], loading: false, error: '' })
  },

  onPullDownRefresh() {
    if (!api.isLoggedIn()) { wx.stopPullDownRefresh(); return }
    this.load().then(function () { wx.stopPullDownRefresh() })
  },

  load() {
    const that = this
    this.setData({ loading: true, error: '' })
    return api.get('/watchlist').then(function (raw) {
      const list = util.safeArray(raw).map(util.decorateWatchlist)
      that.setData({ list: list, loading: false }, function () { that.applyFilter() })
    }).catch(function (err) {
      that.setData({ loading: false, error: err.message || '片单加载失败' })
    })
  },

  applyFilter() {
    const value = this.data.statusFilter
    this.setData({
      filtered: this.data.list.filter(function (item) {
        return value === 'ALL' || item.status === value
      })
    })
  },

  onFilter(e) { this.setData({ statusFilter: e.currentTarget.dataset.value }, () => this.applyFilter()) },

  setStatus(e) {
    const that = this
    const animeId = e.currentTarget.dataset.id
    const status = e.currentTarget.dataset.status
    api.post('/watchlist/' + animeId, { status: status }).then(function () {
      api.toast('已标记为「' + util.watchLabel(status) + '」')
      return that.load()
    }).catch(function (err) { api.toast(err.message) })
  },

  remove(e) {
    const that = this
    const animeId = e.currentTarget.dataset.id
    wx.showModal({
      title: '移出片单',
      content: '确定从片单中移除这部作品吗？',
      success: function (res) {
        if (!res.confirm) return
        api.del('/watchlist/' + animeId).then(function () {
          api.toast('已从片单移除')
          return that.load()
        }).catch(function (err) { api.toast(err.message) })
      }
    })
  },

  openDetail(e) { api.goDetail(e.currentTarget.dataset.id) },
  goCatalog() { wx.switchTab({ url: '/pages/anime/index' }) },
  goLogin() { wx.navigateTo({ url: '/pages/login/index' }) }
})

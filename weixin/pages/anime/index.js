// pages/anime/index.js · 番剧 / 剧场版片库
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')
const config = require('../../config.js')

const TYPE_CHIPS = ['全部', '番剧', '剧场版']
const GENRE_CHIPS = ['全部', '奇幻', '战斗', '青春', '恋爱', '治愈', '热血', '科幻', '悬疑', '喜剧', '日常', '音乐', '运动']
const STATUS_CHIPS = [
  { label: '全部状态', value: 'ALL' },
  { label: '连载中', value: 'AIRING' },
  { label: '已完结', value: 'FINISHED' },
  { label: '即将上映', value: 'UPCOMING' }
]
const SORT_CHIPS = [
  { label: '推荐', value: 'recommend' },
  { label: '评分', value: 'score' },
  { label: '人气', value: 'hot' },
  { label: '最新', value: 'new' }
]

function normalizeType(type) {
  if (type === 'ANIME') return '番剧'
  if (type === 'MOVIE') return '剧场版'
  return '全部'
}

Page({
  data: {
    typeChips: TYPE_CHIPS,
    genreChips: GENRE_CHIPS,
    statusChips: STATUS_CHIPS,
    sortChips: SORT_CHIPS,
    type: '全部',
    genre: '全部',
    status: 'ALL',
    sort: 'recommend',
    keyword: '',
    all: [],
    filtered: [],
    list: [],
    visibleCount: config.PAGE_SIZE,
    hasMore: false,
    loading: true,
    error: ''
  },

  onLoad(options) {
    if (options && (options.type || options.category)) {
      this.setData({
        type: normalizeType(options.type),
        genre: options.category || '全部'
      })
    }
    this.loadData()
  },

  onShow() {
    api.syncTabBar(this, 1)
    const filter = getApp().globalData.catalogFilter
    if (filter) {
      getApp().globalData.catalogFilter = null
      this.setData({
        type: normalizeType(filter.type),
        genre: filter.category || '全部',
        visibleCount: config.PAGE_SIZE
      }, () => this.applyFilter())
    }
  },

  onPullDownRefresh() {
    this.loadData(true).then(function () { wx.stopPullDownRefresh() })
  },

  onReachBottom() {
    this.loadMore()
  },

  loadData(force) {
    const that = this
    const cached = !force && getApp().globalData.animeList
    this.setData({ loading: true, error: '' })
    const task = cached && cached.length ? Promise.resolve(cached) : api.get('/anime')
    return task.then(function (raw) {
      const all = util.safeArray(raw).map(util.decorateAnime)
      getApp().globalData.animeList = all
      that.setData({ all: all, loading: false }, function () { that.applyFilter() })
    }).catch(function (err) {
      that.setData({ loading: false, error: err.message || '片库加载失败' })
    })
  },

  applyFilter() {
    const data = this.data
    const keyword = String(data.keyword || '').trim().toLowerCase()
    let items = data.all.filter(function (item) {
      if (data.type === '番剧' && item.mediaType !== 'ANIME') return false
      if (data.type === '剧场版' && item.mediaType !== 'MOVIE') return false
      if (data.genre !== '全部' && util.safeArray(item.genres).indexOf(data.genre) < 0) return false
      if (data.status !== 'ALL' && item.status !== data.status) return false
      if (keyword) {
        const haystack = [item.title, item.originalTitle, item.summary, util.safeArray(item.genres).join(' ')].join(' ').toLowerCase()
        if (haystack.indexOf(keyword) < 0) return false
      }
      return true
    })
    if (data.sort === 'score') {
      items = items.slice().sort(function (a, b) { return util.safeNumber(b.score) - util.safeNumber(a.score) })
    } else if (data.sort === 'hot') {
      items = items.slice().sort(function (a, b) { return util.safeNumber(b.scoreCount) - util.safeNumber(a.scoreCount) })
    } else if (data.sort === 'new') {
      items = items.slice().sort(function (a, b) {
        return (new Date(b.createdAt || 0).getTime() || 0) - (new Date(a.createdAt || 0).getTime() || 0)
      })
    }
    const count = data.visibleCount
    this.setData({
      filtered: items,
      list: items.slice(0, count),
      hasMore: items.length > count
    })
  },

  loadMore() {
    if (!this.data.hasMore) return
    this.setData({ visibleCount: this.data.visibleCount + config.PAGE_SIZE }, () => this.applyFilter())
  },

  onType(e) { this.setData({ type: e.currentTarget.dataset.value, visibleCount: config.PAGE_SIZE }, () => this.applyFilter()) },
  onGenre(e) { this.setData({ genre: e.currentTarget.dataset.value, visibleCount: config.PAGE_SIZE }, () => this.applyFilter()) },
  onStatus(e) { this.setData({ status: e.currentTarget.dataset.value, visibleCount: config.PAGE_SIZE }, () => this.applyFilter()) },
  onSort(e) { this.setData({ sort: e.currentTarget.dataset.value, visibleCount: config.PAGE_SIZE }, () => this.applyFilter()) },
  onKeyword(e) { this.setData({ keyword: e.detail.value, visibleCount: config.PAGE_SIZE }, () => this.applyFilter()) },
  clearKeyword() { this.setData({ keyword: '', visibleCount: config.PAGE_SIZE }, () => this.applyFilter()) },

  goSearch() { wx.navigateTo({ url: '/pages/search/index' }) },
  openDetail(e) { api.goDetail(e.currentTarget.dataset.id) }
})

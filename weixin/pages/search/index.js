// pages/search/index.js · 全站搜索（作品 + 评论）
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')

const HISTORY_KEY = 'screenroom-search-history'
const HOT_WORDS = ['进击的巨人', '你的名字。', '治愈', '奇幻', '电锯人']

function readHistory() {
  try {
    const value = wx.getStorageSync(HISTORY_KEY)
    return Array.isArray(value) ? value : []
  } catch (e) { return [] }
}

Page({
  data: {
    keyword: '',
    hotWords: HOT_WORDS,
    history: [],
    anime: [],
    comments: [],
    searched: false,
    loading: false
  },

  onLoad(options) {
    this.setData({ history: readHistory() })
    if (options && options.q) {
      let word = options.q
      try { word = decodeURIComponent(options.q) } catch (e) { /* 保留原值 */ }
      this.setData({ keyword: word }, () => this.doSearch())
    }
  },

  onInput(e) { this.setData({ keyword: e.detail.value }) },

  doSearch() {
    const that = this
    const keyword = String(this.data.keyword || '').trim()
    if (!keyword) { api.toast('请输入搜索关键词'); return }
    this.setData({ loading: true, searched: true })
    api.get('/search', { q: keyword }).then(function (res) {
      const data = res || {}
      that.setData({
        anime: util.safeArray(data.anime).map(util.decorateAnime),
        comments: util.safeArray(data.comments).map(util.decorateComment),
        loading: false
      })
      that.saveHistory(keyword)
    }).catch(function (err) {
      that.setData({ loading: false })
      api.toast(err.message || '搜索失败')
    })
  },

  saveHistory(keyword) {
    const next = [keyword].concat(readHistory().filter(function (item) { return item !== keyword })).slice(0, 8)
    wx.setStorageSync(HISTORY_KEY, next)
    this.setData({ history: next })
  },

  tapWord(e) {
    this.setData({ keyword: e.currentTarget.dataset.value }, () => this.doSearch())
  },

  clearHistory() {
    wx.removeStorageSync(HISTORY_KEY)
    this.setData({ history: [] })
  },

  clearKeyword() { this.setData({ keyword: '', anime: [], comments: [], searched: false }) },
  openDetail(e) { api.goDetail(e.currentTarget.dataset.id) }
})

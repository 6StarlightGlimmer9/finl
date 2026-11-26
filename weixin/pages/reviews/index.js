// pages/reviews/index.js · 长评流：排序、搜索、发布、共鸣点赞
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')

const SORTS = [
  { label: '最新发布', value: 'latest' },
  { label: '最多共鸣', value: 'hot' },
  { label: '最高打分', value: 'score' }
]

function emptyForm() {
  return { animeId: '', title: '', content: '', score: 8 }
}

Page({
  data: {
    sorts: SORTS,
    sort: 'latest',
    keyword: '',
    reviews: [],
    stats: { total: 0, writers: 0, likes: 0, avgScore: 0 },
    writers: [],
    animeList: [],
    animeTitles: [],
    animeIndex: 0,
    writeOpen: false,
    form: emptyForm(),
    submitting: false,
    loading: true,
    error: ''
  },

  onLoad() { this.load() },

  onShow() {
    api.syncTabBar(this, 2)
    api.setTabBarVisible(!this.data.writeOpen, this)
  },

  onHide() { api.setTabBarVisible(true, this) },

  onPullDownRefresh() {
    this.load().then(function () { wx.stopPullDownRefresh() })
  },

  load() {
    const that = this
    this.setData({ loading: true, error: '' })
    return api.get('/reviews', { sort: this.data.sort, keyword: this.data.keyword, limit: 100 }).then(function (res) {
      const data = res || {}
      that.setData({
        reviews: util.safeArray(data.reviews).map(util.decorateReview),
        stats: data.stats || { total: 0, writers: 0, likes: 0, avgScore: 0 },
        writers: util.safeArray(data.writers),
        loading: false
      })
    }).catch(function (err) {
      that.setData({ loading: false, error: err.message || '漫评加载失败' })
    })
  },

  onSort(e) { this.setData({ sort: e.currentTarget.dataset.value }, () => this.load()) },
  onKeyword(e) { this.setData({ keyword: e.detail.value }) },
  doSearch() { this.load() },

  loadAnime() {
    const that = this
    const cached = getApp().globalData.animeList
    const task = cached && cached.length ? Promise.resolve(cached) : api.get('/anime')
    return task.then(function (list) {
      const decorated = util.safeArray(list).map(util.decorateAnime)
      getApp().globalData.animeList = decorated
      that.setData({
        animeList: decorated,
        animeTitles: decorated.map(function (item) { return item.title })
      })
      return decorated
    })
  },

  openWrite() {
    const that = this
    if (!api.requireLogin('登录后才能写下你的长评')) return
    this.loadAnime().then(function (list) {
      const form = emptyForm()
      if (list.length) form.animeId = list[0].id
      that.setData({ writeOpen: true, form: form, animeIndex: 0 })
      api.setTabBarVisible(false, that)
    }).catch(function (err) { api.toast(err.message || '作品列表加载失败') })
  },

  closeWrite() {
    this.setData({ writeOpen: false })
    api.setTabBarVisible(true, this)
  },
  noop() {},

  onAnimePick(e) {
    const index = Number(e.detail.value)
    const item = this.data.animeList[index]
    if (!item) return
    const patch = { animeIndex: index }
    patch['form.animeId'] = item.id
    this.setData(patch)
  },

  onFormInput(e) {
    const field = e.currentTarget.dataset.field
    const patch = {}
    patch['form.' + field] = e.detail.value
    this.setData(patch)
  },

  onScoreSlide(e) {
    const patch = {}
    patch['form.score'] = Number(e.detail.value)
    this.setData(patch)
  },

  submitWrite() {
    const that = this
    const form = this.data.form
    const title = String(form.title || '').trim()
    const content = String(form.content || '').trim()
    if (!form.animeId) { api.toast('请选择关联作品'); return }
    if (!title || !content) { api.toast('请填写标题与正文'); return }
    this.setData({ submitting: true })
    api.post('/reviews', {
      animeId: form.animeId,
      title: title,
      content: content,
      score: Number(form.score)
    }).then(function () {
      that.setData({ writeOpen: false, submitting: false, sort: 'latest', keyword: '' })
      api.setTabBarVisible(true, that)
      api.toast('长评已发布')
      return that.load()
    }).catch(function (err) {
      that.setData({ submitting: false })
      api.toast(err.message)
    })
  },

  toggleLike(e) {
    const that = this
    if (!api.requireLogin('登录后才能为长评点赞')) return
    const id = e.currentTarget.dataset.id
    const index = Number(e.currentTarget.dataset.index)
    api.post('/reviews/' + id + '/like').then(function (res) {
      const patch = {}
      patch['reviews[' + index + '].liked'] = !!res.liked
      patch['reviews[' + index + '].likes'] = util.safeNumber(res.likes)
      that.setData(patch)
    }).catch(function (err) { api.toast(err.message) })
  },

  openAnime(e) { api.goDetail(e.currentTarget.dataset.id) }
})

// pages/detail/index.js · 作品详情：评分、片单、短评、PV
const api = require('../../utils/api.js')
const util = require('../../utils/util.js')

const EMPTY_RATING = { score: 0, count: 0, distribution: [], myRating: null }

Page({
  data: {
    id: '',
    anime: null,
    rating: EMPTY_RATING,
    bars: [],
    comments: [],
    watchStatus: '',
    commentText: '',
    rateOpen: false,
    rateValue: 8,
    pvLoading: false,
    loading: true,
    error: '',
    loggedIn: false
  },

  onLoad(options) {
    const id = options && options.id
    if (!id) {
      api.toast('缺少作品编号')
      wx.navigateBack()
      return
    }
    this.setData({ id: String(id) })
    this.load()
  },

  onShow() {
    this.setData({ loggedIn: api.isLoggedIn() })
    if (this.data.anime) this.loadWatchlist()
  },

  onPullDownRefresh() {
    this.load().then(function () { wx.stopPullDownRefresh() })
  },

  load() {
    const that = this
    const id = this.data.id
    this.setData({ loading: true, error: '' })
    const tasks = [
      api.get('/anime/' + id),
      api.get('/anime/' + id + '/rating').catch(function () { return EMPTY_RATING }),
      api.get('/anime/' + id + '/comments').catch(function () { return [] })
    ]
    return Promise.all(tasks).then(function (results) {
      const anime = util.decorateAnime(results[0] || {})
      const rating = results[1] || EMPTY_RATING
      that.setData({
        anime: anime,
        rating: rating,
        bars: util.distributionBars(rating.distribution),
        comments: util.safeArray(results[2]).map(util.decorateComment),
        loading: false
      })
      wx.setNavigationBarTitle({ title: anime.title || '作品详情' })
      return that.loadWatchlist()
    }).catch(function (err) {
      that.setData({ loading: false, error: err.message || '作品信息加载失败' })
    })
  },

  loadWatchlist() {
    const that = this
    if (!api.isLoggedIn()) {
      this.setData({ watchStatus: '' })
      return Promise.resolve()
    }
    return api.get('/watchlist').then(function (list) {
      const found = util.safeArray(list).filter(function (item) {
        return String(item.animeId) === String(that.data.id)
      })[0]
      that.setData({ watchStatus: found ? found.status : '' })
    }).catch(function () { /* 片单失败不影响详情展示 */ })
  },

  openRate() {
    if (!api.requireLogin('登录后可为作品打 1–10 分')) return
    this.setData({ rateOpen: true, rateValue: util.safeNumber(this.data.rating.myRating) || 8 })
  },
  closeRate() { this.setData({ rateOpen: false }) },
  onRateSlide(e) { this.setData({ rateValue: Number(e.detail.value) }) },
  noop() {},

  submitRate() {
    const that = this
    const score = Number(this.data.rateValue)
    api.post('/anime/' + this.data.id + '/ratings', { score: score }).then(function (rating) {
      that.updateRating(rating)
      that.setData({ rateOpen: false })
      api.toast('已打 ' + score + ' 分')
    }).catch(function (err) { api.toast(err.message) })
  },

  removeRating() {
    const that = this
    api.del('/anime/' + this.data.id + '/ratings').then(function (rating) {
      that.updateRating(rating)
      that.setData({ rateOpen: false })
      api.toast('已取消评分')
    }).catch(function (err) { api.toast(err.message) })
  },

  updateRating(rating) {
    const value = rating || EMPTY_RATING
    this.setData({ rating: value, bars: util.distributionBars(value.distribution) })
  },

  setWatch(e) {
    const that = this
    if (!api.requireLogin('登录后可管理我的片单')) return
    const status = e.currentTarget.dataset.status
    api.post('/watchlist/' + this.data.id, { status: status }).then(function () {
      that.setData({ watchStatus: status })
      api.toast('片单已更新为「' + util.watchLabel(status) + '」')
    }).catch(function (err) { api.toast(err.message) })
  },

  removeWatch() {
    const that = this
    if (!this.data.watchStatus) return
    wx.showModal({
      title: '移出片单',
      content: '确定把这部作品从片单中移除吗？',
      success: function (res) {
        if (!res.confirm) return
        api.del('/watchlist/' + that.data.id).then(function () {
          that.setData({ watchStatus: '' })
          api.toast('已从片单移除')
        }).catch(function (err) { api.toast(err.message) })
      }
    })
  },

  onCommentInput(e) { this.setData({ commentText: e.detail.value }) },

  submitComment() {
    const that = this
    if (!api.requireLogin('登录后才能发表短评')) return
    const content = String(this.data.commentText || '').trim()
    if (!content) { api.toast('写点什么吧'); return }
    api.post('/anime/' + this.data.id + '/comments', { content: content }).then(function (res) {
      that.setData({ comments: [util.decorateComment(res)].concat(that.data.comments), commentText: '' })
      api.toast('短评已发布')
    }).catch(function (err) { api.toast(err.message) })
  },

  toggleCommentLike(e) {
    const that = this
    if (!api.requireLogin('登录后才能点赞短评')) return
    const commentId = e.currentTarget.dataset.id
    const index = Number(e.currentTarget.dataset.index)
    api.post('/comments/' + commentId + '/like').then(function (res) {
      const patch = {}
      patch['comments[' + index + '].liked'] = !!res.liked
      patch['comments[' + index + '].likes'] = util.safeNumber(res.likes)
      that.setData(patch)
    }).catch(function (err) { api.toast(err.message) })
  },

  openPv() {
    const that = this
    const anime = this.data.anime
    if (!anime || !anime.pvUrl) { api.toast('这部作品暂无预览链接'); return }
    this.setData({ pvLoading: true })
    api.get('/bilibili/resolve', { url: anime.pvUrl }).then(function (res) {
      const target = res && (res.embed || res.web)
      if (!target) throw new Error('未解析到播放地址')
      wx.navigateTo({
        url: '/pages/web/index?url=' + encodeURIComponent(target) + '&title=' + encodeURIComponent('PV · ' + anime.title)
      })
    }).catch(function () {
      wx.setClipboardData({ data: anime.pvUrl })
      api.toast('预览解析失败，已复制链接，可在浏览器打开')
    }).then(function () {
      that.setData({ pvLoading: false })
    })
  },

  goWatchlist() {
    if (!api.requireLogin('登录后可查看我的片单')) return
    wx.navigateTo({ url: '/pages/watchlist/index' })
  }
})

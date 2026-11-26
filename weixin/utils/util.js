// utils/util.js · 数据装饰与格式化（后端 jackson 配置 non_null，null 字段会被省略）
const config = require('../config.js')

function safeArray(value) { return Array.isArray(value) ? value : [] }
function safeNumber(value) { const n = Number(value); return isNaN(n) ? 0 : n }

function imageUrl(path) {
  if (!path) return ''
  const value = String(path)
  if (/^(https?:|wxfile:|data:|cloud:)/.test(value)) return value
  return config.getImgHost() + (value.charAt(0) === '/' ? value : '/' + value)
}

function scoreText(value) { return safeNumber(value).toFixed(1) }

function mediaLabel(type) { return type === 'MOVIE' ? '剧场版' : '番剧' }
function statusLabel(status) {
  if (status === 'AIRING') return '连载中'
  if (status === 'UPCOMING') return '即将上映'
  return '已完结'
}
function watchLabel(status) {
  if (status === 'WANT') return '想看'
  if (status === 'WATCHING') return '在看'
  if (status === 'WATCHED') return '看过'
  return '未加入'
}

function compact(text, max) {
  const value = String(text || '').replace(/\s+/g, ' ').trim()
  const size = max || 80
  return value.length > size ? value.slice(0, size) + '…' : value
}

function timeAgo(iso) {
  if (!iso) return ''
  const time = new Date(iso).getTime()
  if (!time || isNaN(time)) return ''
  const diff = Date.now() - time
  if (diff < 0) return '刚刚'
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 小时前'
  if (diff < 2592000000) return Math.floor(diff / 86400000) + ' 天前'
  const date = new Date(time)
  return (date.getMonth() + 1) + ' 月 ' + date.getDate() + ' 日'
}

function initial(name) {
  const value = String(name || '放').trim()
  return value ? value.charAt(0).toUpperCase() : '放'
}

function decorateAnime(item) {
  const source = item || {}
  const genres = safeArray(source.genres)
  return Object.assign({}, source, {
    cover: imageUrl(source.coverUrl),
    scoreText: scoreText(source.score),
    meta: [mediaLabel(source.mediaType), source.season, statusLabel(source.status)]
      .filter(function (part) { return !!part }).join(' · '),
    statusText: statusLabel(source.status),
    genres: genres,
    topGenres: genres.slice(0, 3)
  })
}

function decorateReview(item) {
  const source = item || {}
  return Object.assign({}, source, {
    cover: imageUrl(source.animeCover),
    scoreText: scoreText(source.score),
    likes: safeNumber(source.likes),
    liked: !!source.liked,
    nickname: source.nickname || source.username || '放映室用户',
    initial: initial(source.nickname || source.username),
    time: timeAgo(source.createdAt),
    excerpt: compact(source.content, 110),
    genres: safeArray(source.genres)
  })
}

function decorateComment(item) {
  const source = item || {}
  return Object.assign({}, source, {
    likes: safeNumber(source.likes),
    liked: !!source.liked,
    nickname: source.nickname || source.username || '放映室用户',
    initial: initial(source.nickname || source.username),
    time: timeAgo(source.createdAt)
  })
}

function decorateWatchlist(item) {
  const source = item || {}
  return Object.assign({}, source, {
    id: source.animeId,
    cover: imageUrl(source.coverUrl),
    scoreText: scoreText(source.score),
    statusLabel: watchLabel(source.status),
    meta: watchLabel(source.status) + (source.updatedAt ? ' · ' + timeAgo(source.updatedAt) : ''),
    topGenres: []
  })
}

function decorateProfileRating(item) {
  const source = item || {}
  return Object.assign({}, source, {
    cover: imageUrl(source.coverUrl),
    scoreText: scoreText(source.score),
    time: timeAgo(source.createdAt)
  })
}

function distributionBars(distribution) {
  const values = safeArray(distribution)
  const max = Math.max.apply(null, values.concat([1]))
  return values.map(function (count, index) {
    return {
      score: index + 1,
      count: safeNumber(count),
      percent: Math.max(3, Math.round(safeNumber(count) / max * 100))
    }
  })
}

module.exports = {
  safeArray: safeArray,
  safeNumber: safeNumber,
  imageUrl: imageUrl,
  scoreText: scoreText,
  mediaLabel: mediaLabel,
  statusLabel: statusLabel,
  watchLabel: watchLabel,
  compact: compact,
  timeAgo: timeAgo,
  initial: initial,
  decorateAnime: decorateAnime,
  decorateReview: decorateReview,
  decorateComment: decorateComment,
  decorateWatchlist: decorateWatchlist,
  decorateProfileRating: decorateProfileRating,
  distributionBars: distributionBars
}

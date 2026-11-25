# 次元放映室

Spring Boot 2.7 + Vue 3 的番剧评测与 ACGN 同好社群平台。

## 一键初始化数据库

MySQL 账号密码均为 `root`，在项目根目录执行：

```powershell
mysql -u root -proot < database/acgn_screenroom.sql
```

`database` 文件夹现在只有一个权威脚本 `database/acgn_screenroom.sql`，文件内按「建库建表 / 旧库升级 / 种子数据 / 评分回算 / 管理查询」分区并附有注释。表结构包含用户、作品、标签、评分、评论、令牌，以及追番片单 `watchlist`、评论点赞 `comment_likes`、长篇漫评 `reviews` / `review_likes`、社群内容 `community_items` / `community_item_members` / `topic_posts`。

新库和旧库都执行同一个文件即可：表结构用 `CREATE TABLE IF NOT EXISTS` 保证不重复创建，种子数据用唯一约束和 `NOT EXISTS` 去重，末尾会按 `ratings` 表真实记录回算分数。文件末尾还附带了常用管理 SQL，默认注释，不会自动执行。

150 部作品的封面均已放在 `frontend/public/covers/`，统一按 `/covers/{作品ID}.jpg|png` 保存，前端直接读取本地静态资源，不再依赖外部图床。已删除《我的英雄学院》系列与《链锯人：剧场版》《男子高中生的日常 剧场版》等错误条目，并将《你的名字。》《火影忍者 / 火影忍者 疾风传》等重复作品合并；《辉夜大小姐想让我告白》已标注第一季 / 第二季，新增 6 部 Bilibili 运动番与《小马宝莉大电影》，分季与剧场版作品保留独立条目，通过 `sort_order` 在番剧库中相邻展示。

详情页“点击查看预览”弹窗只使用 Bilibili 资源，不再读取本地 MP4。后台的预览地址可填写 Bilibili 视频链接、番剧链接（如 `https://www.bilibili.com/bangumi/play/ss102261`）或 BV 号；番剧链接会自动解析为可内嵌的首集视频，解析不到资源时会在预览窗口显示不可用提示。

所有展示分数均由 `ratings` 表中的用户评分记录实时聚合，同一用户对同一作品只保留一条评分，修改或撤销评分后分数与人数同步回算。

## 增强功能

- **追番片单**：详情页可标记「想看 / 在看 / 看过」，`/watchlist` 页面集中管理，状态带更新时间。
- **评论点赞**：登录用户可点赞 / 取消点赞，后端 `comment_likes` 表防重复，评论数同步更新。
- **真实社群动态**：社群页展示全站最新评论与热议作品，不再只是静态占位内容。
- **搜索排序**：番剧库默认按系列顺序展示，分季 / 剧场版作品相邻；也可切换最高评分、最多评分人数、最新收录、作品名称四种排序。
- **数据洞察**：管理员后台新增分类分布、评分分布、追番状态、近 7 天活跃与高分作品看板。
- **明暗主题**：右上角可切换深色 / 浅色主题，偏好保存在浏览器本地。
- **全局轻提示与快捷键**：操作反馈统一使用右上角 Toast，`Ctrl/⌘ + K` 可快速聚焦搜索框。

## 启动后端

```powershell
cd backend
mvn spring-boot:run
```

后端端口为 `8080`，数据库连接默认使用 `root/root`，可通过环境变量 `DB_USERNAME`、`DB_PASSWORD` 覆盖。

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173。前端 `/api` 请求会自动代理到 `http://localhost:8080`。

## 账号与权限

数据库共 48 个演示用户，评论会显示用户昵称；每个账号都有固定的注册时间，个人中心会同步展示该账号的评分、评论与追番片单。全部 48 个账号都参与了评分与评论数据，每部作品均保证至少 11 条评分和 11 条评论。演示库账号密码保存在 `users.password` 字段中，按需求使用明文，不做哈希或加盐；登录、注册、评分、评论与片单功能均按明文密码工作。此配置仅用于本地演示。

- 普通用户演示账号：`screenroom / 123456`
- 管理员账号：`admin / admin123`
- 其他演示用户：`moon / moon123`、`summer / summer123`、`film / film123`、`reviewer / review123`
- 批量演示用户：`sakura`、`kaze`、`yuki`、`aoi`、`rin` 等，密码统一为 `demo123`
- 新增演示用户：`kaito`、`mio`、`ren`、`yui`、`kuro`、`shiro`、`toma`、`nana`，密码统一为 `demo123`；每个新账号都已预置评分、评论和追番片单记录

统一脚本会写入 150 部作品（87 部番剧、63 部电影）、1666 条评分、1758 条评论、29 条追番片单记录，以及 12 篇漫评、6 个话题、5 个小组、1 场放映会和 18 条话题回复；《咒术回战 0》《青春猪头少年不会梦到怀梦美少女》《利兹与青鸟》已从电影列表移至番剧列表，《链锯人：剧场版》《男子高中生的日常 剧场版》已删除，并新增 6 部运动番与《小马宝莉大电影》。奇幻、战斗、青春、恋爱、治愈、热血、科幻、悬疑、喜剧、日常、音乐、运动等分类均保留完整覆盖。分数仍由评分记录实时回算，番剧库按展示顺序排列，同系列 / 分季作品保持相邻。

登录后服务端签发 7 天有效的 Bearer Token，前端自动在请求头携带。管理员接口在服务端校验角色，前端仅做入口展示控制；普通用户即使伪造请求也无法调用后台接口。未登录时点击评分星级、评论输入框或提交按钮会自动弹出登录框。

## 主打页面：漫评与社群

本站与 Bilibili 的差异化重点放在「看完之后」：

- **漫评**：持久化长评、关联作品、作者打分、标签、阅读全文、搜索、最新 / 热门 / 高分排序、写作榜、发布与点赞。数据来自 `reviews` 与 `review_likes`，不再使用前端硬编码假数据。
- **社群**：话题广场、话题详情与回复、兴趣小组加入 / 退出、放映会报名 / 取消、全站最新评论点赞、热议作品、活跃创作者与趋势标签。话题、小组、放映会、成员关系和回复分别落在 `community_items`、`community_item_members`、`topic_posts`，刷新或换账号后状态仍然保留。
- 首页导航给「漫评」「社群」增加“主打”标记，并新增两张专属入口卡片；底部横幅也直接导向这两个页面。

管理员登录后，悬停右上角头像进入“管理员后台”，可以：

- 查看真实注册用户、作品、评分和评论统计
- 审核并删除全站评论
- 修改作品资料、封面图片 URL、PV 地址与标签
- 发布新动漫 / 电影，发布后首页轮播、番剧库与搜索立即可见

## 页面

`/#/` 首页（含漫评 / 社群主打入口）、`/#/anime` 番剧库、`/#/anime?type=movie` 电影库、`/#/detail?id=1` 详情、`/#/reviews` 漫评（长评流 / 发布 / 点赞 / 写作榜）、`/#/community` 社群（话题 / 回复 / 小组 / 放映会 / 评论点赞）、`/#/watchlist` 我的片单、`/#/search?q=关键词` 搜索、`/#/profile` 个人中心、`/#/admin` 管理员后台。

## 主要接口

- 认证：`POST /api/auth/login`、`POST /api/auth/register`、`POST /api/auth/logout`、`GET /api/auth/me`、`GET /api/auth/profile`
- 内容：`GET /api/anime`、`GET /api/anime/{id}`、`GET /api/search`
- 评分：`GET /api/anime/{id}/rating`、`POST /api/anime/{id}/ratings`、`DELETE /api/anime/{id}/ratings`
- 评论：`GET /api/anime/{id}/comments`、`POST /api/anime/{id}/comments`、`POST /api/comments/{id}/like`
- 片单：`GET /api/watchlist`、`POST /api/watchlist/{animeId}`、`DELETE /api/watchlist/{animeId}`
- 漫评：`GET /api/reviews`、`POST /api/reviews`、`POST /api/reviews/{id}/like`
- 社群：`GET /api/community?limit=12`、`GET /api/community/topics/{id}`、`POST /api/community/topics`、`POST /api/community/topics/{id}/posts`、`POST /api/community/groups/{id}/join`、`POST /api/community/events/{id}/signup`
- 管理员：`GET /api/admin/overview`、`GET /api/admin/analytics`、`GET /api/admin/users`、`DELETE /api/admin/users/{id}`、`GET /api/admin/comments`、`DELETE /api/admin/comments/{id}`、`POST /api/admin/anime`、`PUT /api/admin/anime/{id}`、`PUT /api/admin/anime/{id}/genres`

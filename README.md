# 次元放映室

$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.32'
$env:MAVEN_HOME = "$env:USERPROFILE\.tools\apache-maven-3.9.11"
$env:Path = "$env:MAVEN_HOME\bin;$env:Path"

cd F:\毕设\backend
mvn spring-boot:run

端口占用：
Get-NetTCPConnection -LocalPort 8080

Spring Boot + Vue 3 的番剧评测与 ACGN 同好社群平台。

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173。

## 启动后端

数据库统一脚本请执行 [database/acgn_screenroom.sql](database/acgn_screenroom.sql)，它同时支持新库初始化和旧库字段升级：

```powershell
mysql -u root -p < database/acgn_screenroom.sql
```

旧库缺少 `users.role` 时 Spring Boot 会无法启动，注册和管理员页面也会同时失效。启动后端前，在 PowerShell 中设置数据库凭据：

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '你的 MySQL 密码'
```

也可以直接在 `backend/src/main/resources/application.yml` 中修改默认值。

前端页面地址：`/#/` 首页、`/#/anime` 番剧库、`/#/reviews` 漫评、`/#/community` 社群、`/#/detail` 动漫详情。

```powershell
cd backend
mvn spring-boot:run
```

后端接口：`POST /api/auth/login`、`POST /api/auth/register`、`GET /api/search`、`GET /api/anime`、`GET/POST /api/anime/{id}/rating(s)`、`GET/POST /api/anime/{id}/comments`、`GET/DELETE /api/admin/users`。

管理员演示账号：`admin / admin123`。登录后悬停右上角头像，再进入管理员后台。

package com.dimension.screenroom.web;

import com.dimension.screenroom.domain.*;
import com.dimension.screenroom.repository.*;
import com.dimension.screenroom.util.PasswordUtil;
import org.hibernate.Hibernate;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ScreenroomController {
    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;
    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final GenreRepository genreRepository;
    private final AuthTokenRepository authTokenRepository;
    private final WatchlistRepository watchlistRepository;
    private final CommentLikeRepository commentLikeRepository;

    public ScreenroomController(UserRepository userRepository, AnimeRepository animeRepository,
                                RatingRepository ratingRepository, CommentRepository commentRepository,
                                GenreRepository genreRepository, AuthTokenRepository authTokenRepository,
                                WatchlistRepository watchlistRepository, CommentLikeRepository commentLikeRepository) {
        this.userRepository = userRepository;
        this.animeRepository = animeRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.genreRepository = genreRepository;
        this.authTokenRepository = authTokenRepository;
        this.watchlistRepository = watchlistRepository;
        this.commentLikeRepository = commentLikeRepository;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.username, request.username).orElse(null);
        if (user == null || !PasswordUtil.matches(request.password, user.passwordHash)) {
            return ResponseEntity.status(401).body(Map.of("message", "账号或密码错误"));
        }
        return ResponseEntity.ok(issueToken(user));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username)) {
            return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        }
        if (userRepository.existsByEmail(request.email)) {
            return ResponseEntity.badRequest().body(Map.of("message", "邮箱已注册"));
        }
        User user = new User();
        user.username = request.username.trim();
        user.email = request.email.trim();
        user.nickname = request.username.trim();
        user.passwordHash = PasswordUtil.hash(request.password);
        user.role = Role.USER;
        userRepository.save(user);
        return ResponseEntity.ok(issueToken(user));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        AuthToken token = (AuthToken) request.getAttribute("authToken");
        if (token != null) authTokenRepository.delete(token);
        return ResponseEntity.ok(Map.of("message", "已退出登录"));
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(HttpServletRequest request) {
        User user = currentUser(request);
        return userView(user);
    }

    @GetMapping("/auth/profile")
    @Transactional(readOnly = true)
    public Map<String, Object> profile(HttpServletRequest request) {
        User user = currentUser(request);
        Map<String, Object> result = new HashMap<>(userView(user));
        List<Map<String, Object>> ratings = ratingRepository.findProfileRatings(user.id).stream()
                .map(item -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("animeId", item.anime.id);
                    row.put("title", item.anime.title);
                    row.put("coverUrl", item.anime.coverUrl);
                    row.put("score", item.score);
                    row.put("mediaType", item.anime.mediaType.name());
                    row.put("createdAt", item.createdAt == null ? null : item.createdAt.toString());
                    return row;
                }).collect(Collectors.toList());
        List<CommentResponse> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(user.id).stream()
                .map(item -> toCommentResponse(item, user)).collect(Collectors.toList());
        List<WatchlistResponse> watchlist = watchlistRepository.findByUserIdOrderByUpdatedAtDesc(user.id).stream()
                .map(this::toWatchlistResponse).collect(Collectors.toList());
        result.put("ratingCount", ratings.size());
        result.put("commentCount", comments.size());
        result.put("watchlistCount", watchlist.size());
        result.put("ratings", ratings);
        result.put("comments", comments);
        result.put("watchlist", watchlist);
        return result;
    }

    @GetMapping("/watchlist")
    @Transactional(readOnly = true)
    public List<WatchlistResponse> watchlist(HttpServletRequest request) {
        User user = currentUser(request);
        return watchlistRepository.findByUserIdOrderByUpdatedAtDesc(user.id).stream()
                .map(this::toWatchlistResponse).collect(Collectors.toList());
    }

    @PostMapping("/watchlist/{animeId}")
    @Transactional
    public WatchlistResponse saveWatchlist(@PathVariable Long animeId, @Valid @RequestBody WatchlistRequest request,
                                           HttpServletRequest httpRequest) {
        User user = currentUser(httpRequest);
        Anime anime = findAnime(animeId);
        Watchlist item = watchlistRepository.findByUserIdAndAnimeId(user.id, animeId).orElseGet(Watchlist::new);
        if (item.id == null) {
            item.user = user;
            item.anime = anime;
        }
        item.status = WatchStatus.valueOf(request.status.trim().toUpperCase());
        item.updatedAt = Instant.now();
        watchlistRepository.save(item);
        return toWatchlistResponse(item);
    }

    @DeleteMapping("/watchlist/{animeId}")
    @Transactional
    public Map<String, Object> removeWatchlist(@PathVariable Long animeId, HttpServletRequest request) {
        User user = currentUser(request);
        watchlistRepository.deleteByUserIdAndAnimeId(user.id, animeId);
        return Map.of("message", "已从片单移除");
    }

    @GetMapping
    public Map<String, Object> serviceInfo() {
        return Map.of("name", "次元放映室 API", "status", "running",
                "auth", "Bearer Token", "frontend", "http://localhost:5173");
    }

    @GetMapping("/anime")
    @Transactional(readOnly = true)
    public List<AnimeResponse> listAnime() {
        return animeRepository.findAllByOrderByScoreDesc().stream().map(this::toAnimeResponse).collect(Collectors.toList());
    }

    @GetMapping("/anime/{animeId}")
    @Transactional(readOnly = true)
    public AnimeResponse anime(@PathVariable Long animeId) {
        return toAnimeResponse(findAnime(animeId));
    }

    @GetMapping("/anime/{animeId}/rating")
    @Transactional(readOnly = true)
    public RatingSummary rating(@PathVariable Long animeId, HttpServletRequest request) {
        findAnime(animeId);
        return ratingSummary(animeId, optionalUser(request));
    }

    @PostMapping("/anime/{animeId}/ratings")
    @Transactional
    public RatingSummary rate(@PathVariable Long animeId, @Valid @RequestBody RatingRequest request, HttpServletRequest request2) {
        User user = currentUser(request2);
        Anime anime = findAnime(animeId);
        Rating rating = ratingRepository.findByUserIdAndAnimeId(user.id, animeId).orElseGet(Rating::new);
        if (rating.id == null) {
            rating.user = user;
            rating.anime = anime;
            rating.createdAt = Instant.now();
        }
        rating.score = request.score;
        ratingRepository.save(rating);
        recalculateScore(anime);
        return ratingSummary(animeId, user);
    }

    @DeleteMapping("/anime/{animeId}/ratings")
    @Transactional
    public RatingSummary removeRating(@PathVariable Long animeId, HttpServletRequest request) {
        User user = currentUser(request);
        Anime anime = findAnime(animeId);
        ratingRepository.findByUserIdAndAnimeId(user.id, animeId).ifPresent(ratingRepository::delete);
        recalculateScore(anime);
        return ratingSummary(animeId, user);
    }

    @GetMapping("/anime/{animeId}/comments")
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(@PathVariable Long animeId, HttpServletRequest request) {
        User viewer = optionalUser(request);
        return commentRepository.findByAnimeIdOrderByCreatedAtDesc(animeId).stream()
                .map(item -> toCommentResponse(item, viewer)).collect(Collectors.toList());
    }

    @PostMapping("/anime/{animeId}/comments")
    @Transactional
    public CommentResponse comment(@PathVariable Long animeId, @Valid @RequestBody CommentRequest request,
                                   HttpServletRequest httpRequest) {
        User user = currentUser(httpRequest);
        Comment comment = new Comment();
        comment.user = user;
        comment.anime = findAnime(animeId);
        comment.content = request.content.trim();
        comment.likes = 0;
        comment.createdAt = Instant.now();
        commentRepository.save(comment);
        return toCommentResponse(comment, user);
    }

    @PostMapping("/comments/{commentId}/like")
    @Transactional
    public Map<String, Object> toggleCommentLike(@PathVariable Long commentId, HttpServletRequest request) {
        User user = currentUser(request);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("评论不存在"));
        Optional<CommentLike> existing = commentLikeRepository.findByUserIdAndCommentId(user.id, commentId);
        boolean liked;
        if (existing.isPresent()) {
            commentLikeRepository.delete(existing.get());
            comment.likes = Math.max(0, (comment.likes == null ? 0 : comment.likes) - 1);
            liked = false;
        } else {
            CommentLike like = new CommentLike();
            like.user = user;
            like.comment = comment;
            commentLikeRepository.save(like);
            comment.likes = (comment.likes == null ? 0 : comment.likes) + 1;
            liked = true;
        }
        commentRepository.save(comment);
        return Map.of("liked", liked, "likes", comment.likes);
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public Map<String, Object> search(@RequestParam(defaultValue = "") String q, HttpServletRequest request) {
        String keyword = q.trim();
        Map<String, Object> result = new HashMap<>();
        if (keyword.isEmpty()) {
            result.put("anime", List.of());
            result.put("comments", List.of());
            return result;
        }
        User viewer = optionalUser(request);
        result.put("anime", animeRepository.search(keyword).stream().map(this::toAnimeResponse).collect(Collectors.toList()));
        result.put("comments", commentRepository.findByContentContainingIgnoreCase(keyword).stream()
                .map(item -> toCommentResponse(item, viewer)).collect(Collectors.toList()));
        return result;
    }

    @GetMapping("/community")
    @Transactional(readOnly = true)
    public Map<String, Object> community(@RequestParam(defaultValue = "12") int limit) {
        int size = Math.max(1, Math.min(limit, 50));
        List<CommentResponse> latest = commentRepository.findAllByOrderByCreatedAtDesc().stream()
                .limit(size).map(item -> toCommentResponse(item, null)).collect(Collectors.toList());
        Map<Long, Long> counts = commentRepository.findAll().stream()
                .collect(Collectors.groupingBy(item -> item.anime.id, Collectors.counting()));
        List<Map<String, Object>> hotAnime = counts.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(5).map(entry -> animeRepository.findById(entry.getKey()).map(anime -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("animeId", anime.id);
                    row.put("title", anime.title);
                    row.put("coverUrl", anime.coverUrl);
                    row.put("commentCount", entry.getValue());
                    return row;
                }).orElse(null)).filter(Objects::nonNull).collect(Collectors.toList());
        Map<String, Object> result = new HashMap<>();
        result.put("comments", latest);
        result.put("hotAnime", hotAnime);
        return result;
    }

    @GetMapping("/admin/overview")
    @Transactional(readOnly = true)
    public Map<String, Object> adminOverview() {
        Map<String, Object> result = new HashMap<>();
        result.put("users", userRepository.count());
        result.put("anime", animeRepository.count());
        result.put("ratings", ratingRepository.count());
        result.put("comments", commentRepository.count());
        double average = ratingRepository.findAll().stream().mapToInt(item -> item.score).average().orElse(0);
        result.put("averageScore", roundScore(average));
        return result;
    }

    @GetMapping("/admin/analytics")
    @Transactional(readOnly = true)
    public Map<String, Object> adminAnalytics() {
        Map<String, Object> result = new HashMap<>();
        List<Anime> animes = animeRepository.findAll();
        List<Map<String, Object>> categories = genreRepository.findAll().stream().map(genre -> {
            long count = animes.stream().filter(anime -> anime.genres != null && anime.genres.contains(genre)).count();
            Map<String, Object> row = new HashMap<>();
            row.put("name", genre.name);
            row.put("count", count);
            return row;
        }).sorted((left, right) -> Long.compare((Long) right.get("count"), (Long) left.get("count"))).collect(Collectors.toList());
        result.put("categories", categories);

        Map<String, Long> statusGroups = animes.stream().collect(Collectors.groupingBy(item -> item.status.name(), Collectors.counting()));
        List<Map<String, Object>> statuses = statusGroups.entrySet().stream().map(entry -> {
            Map<String, Object> row = new HashMap<>();
            row.put("name", entry.getKey());
            row.put("count", entry.getValue());
            return row;
        }).collect(Collectors.toList());
        result.put("statuses", statuses);

        List<Map<String, Object>> topAnime = animeRepository.findAllByOrderByScoreDesc().stream().limit(6).map(anime -> {
            Map<String, Object> row = new HashMap<>();
            row.put("animeId", anime.id);
            row.put("title", anime.title);
            row.put("coverUrl", anime.coverUrl);
            row.put("score", anime.score.doubleValue());
            row.put("scoreCount", anime.scoreCount);
            return row;
        }).collect(Collectors.toList());
        result.put("topAnime", topAnime);

        int[] histogram = new int[10];
        for (Rating rating : ratingRepository.findAll()) {
            if (rating.score >= 1 && rating.score <= 10) histogram[rating.score - 1]++;
        }
        result.put("ratingHistogram", histogram);
        result.put("activeUsers", commentRepository.findAll().stream().map(item -> item.user.id)
                .collect(Collectors.toSet()).size());

        List<Map<String, Object>> watchStats = Arrays.stream(WatchStatus.values()).map(status -> {
            Map<String, Object> row = new HashMap<>();
            row.put("name", status.name());
            row.put("count", watchlistRepository.findAll().stream().filter(item -> item.status == status).count());
            return row;
        }).collect(Collectors.toList());
        result.put("watchStats", watchStats);

        Instant start = Instant.now().minus(6, ChronoUnit.DAYS);
        List<Map<String, Object>> activity = new ArrayList<>();
        for (int offset = 6; offset >= 0; offset--) {
            String date = Instant.now().minus(offset, ChronoUnit.DAYS).toString().substring(0, 10);
            long commentCount = commentRepository.findAll().stream().filter(item -> !item.createdAt.isBefore(start)).count();
            long ratingCount = ratingRepository.findAll().stream().filter(item -> item.createdAt != null && !item.createdAt.isBefore(start)).count();
            Map<String, Object> row = new HashMap<>();
            row.put("date", date);
            row.put("comments", commentCount);
            row.put("ratings", ratingCount);
            activity.add(row);
        }
        result.put("activity", activity);
        return result;
    }

    @GetMapping("/admin/users")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminUsers() {
        return userRepository.findAll().stream().map(user -> {
            Map<String, Object> row = new HashMap<>(userView(user));
            row.put("email", user.email);
            return row;
        }).collect(Collectors.toList());
    }

    @DeleteMapping("/admin/users/{userId}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (user.role == Role.ADMIN) return ResponseEntity.badRequest().body(Map.of("message", "管理员账号不能删除"));
        authTokenRepository.deleteByUserId(userId);
        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "用户已删除"));
    }

    @GetMapping("/admin/comments")
    @Transactional(readOnly = true)
    public List<CommentResponse> adminComments() {
        return commentRepository.findAllByOrderByCreatedAtDesc().stream().map(item -> toCommentResponse(item, null)).collect(Collectors.toList());
    }

    @DeleteMapping("/admin/comments/{commentId}")
    @Transactional
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        if (!commentRepository.existsById(commentId)) return ResponseEntity.status(404).body(Map.of("message", "评论不存在"));
        commentRepository.deleteById(commentId);
        return ResponseEntity.ok(Map.of("message", "评论已删除"));
    }

    @PostMapping("/admin/anime")
    @Transactional
    public AnimeResponse createAnime(@Valid @RequestBody AnimeRequest request) {
        Anime anime = new Anime();
        applyAnimeRequest(anime, request);
        anime.score = BigDecimal.ZERO;
        anime.scoreCount = 0;
        anime.genres = resolveGenres(request.genres);
        animeRepository.save(anime);
        return toAnimeResponse(anime);
    }

    @PutMapping("/admin/anime/{animeId}")
    @Transactional
    public AnimeResponse updateAnime(@PathVariable Long animeId, @Valid @RequestBody AnimeRequest request) {
        Anime anime = findAnime(animeId);
        applyAnimeRequest(anime, request);
        if (request.genres != null) anime.genres = resolveGenres(request.genres);
        animeRepository.save(anime);
        return toAnimeResponse(anime);
    }

    @PutMapping("/admin/anime/{animeId}/genres")
    @Transactional
    public AnimeResponse updateGenres(@PathVariable Long animeId, @Valid @RequestBody GenreRequest request) {
        Anime anime = findAnime(animeId);
        anime.genres = resolveGenres(request.genres);
        animeRepository.save(anime);
        return toAnimeResponse(anime);
    }

    private AuthResponse issueToken(User user) {
        String rawToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        AuthToken token = new AuthToken();
        token.user = user;
        token.tokenHash = PasswordUtil.hashToken(rawToken);
        token.expiresAt = Instant.now().plus(7, ChronoUnit.DAYS);
        authTokenRepository.save(token);
        return new AuthResponse(rawToken, user.id, user.username, user.nickname, user.role.name(), token.expiresAt.toString());
    }

    private Map<String, Object> userView(User user) {
        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.id);
        result.put("username", user.username);
        result.put("nickname", user.nickname);
        result.put("email", user.email);
        result.put("role", user.role.name());
        result.put("avatarUrl", user.avatarUrl);
        result.put("bio", user.bio);
        if (user.createdAt != null) result.put("createdAt", user.createdAt.toString());
        return result;
    }

    private User currentUser(HttpServletRequest request) {
        Object user = request.getAttribute("currentUser");
        if (!(user instanceof User)) throw new IllegalArgumentException("请先登录");
        return (User) user;
    }

    private User optionalUser(HttpServletRequest request) {
        Object current = request.getAttribute("currentUser");
        if (current instanceof User) return (User) current;
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        String hash = PasswordUtil.hashToken(header.substring(7).trim());
        AuthToken token = authTokenRepository.findByTokenHash(hash).orElse(null);
        if (token == null || token.expiresAt.isBefore(Instant.now())) return null;
        return token.user;
    }

    private Anime findAnime(Long animeId) {
        return animeRepository.findById(animeId).orElseThrow(() -> new IllegalArgumentException("番剧不存在"));
    }

    private void recalculateScore(Anime anime) {
        long count = ratingRepository.countByAnimeId(anime.id);
        double average = ratingRepository.averageScore(anime.id);
        anime.scoreCount = (int) count;
        anime.score = BigDecimal.valueOf(count == 0 ? 0 : roundScore(average));
    }

    private RatingSummary ratingSummary(Long animeId, User user) {
        int[] distribution = new int[10];
        for (Object[] row : ratingRepository.distribution(animeId)) {
            int score = ((Number) row[0]).intValue();
            if (score >= 1 && score <= 10) distribution[score - 1] = ((Number) row[1]).intValue();
        }
        Integer myRating = null;
        if (user != null) {
            myRating = ratingRepository.findByUserIdAndAnimeId(user.id, animeId).map(item -> item.score).orElse(null);
        }
        long count = ratingRepository.countByAnimeId(animeId);
        double average = ratingRepository.averageScore(animeId);
        return new RatingSummary(count == 0 ? 0 : roundScore(average), (int) count, distribution, myRating);
    }

    private void applyAnimeRequest(Anime anime, AnimeRequest request) {
        anime.title = request.title.trim();
        anime.originalTitle = trimToNull(request.originalTitle);
        anime.summary = trimToNull(request.summary);
        anime.coverUrl = trimToNull(request.coverUrl);
        anime.pvUrl = trimToNull(request.pvUrl);
        anime.season = trimToNull(request.season);
        try {
            anime.mediaType = MediaType.valueOf(request.mediaType.trim().toUpperCase());
            anime.status = AnimeStatus.valueOf(request.status.trim().toUpperCase());
        } catch (Exception exception) {
            throw new IllegalArgumentException("作品类型或播出状态不合法");
        }
    }

    private Set<Genre> resolveGenres(List<String> names) {
        Set<Genre> result = new LinkedHashSet<>();
        if (names == null) return result;
        for (String name : names) {
            if (name == null || name.trim().isEmpty()) continue;
            String normalized = name.trim();
            Genre genre = genreRepository.findByName(normalized).orElseGet(() -> {
                Genre created = new Genre();
                created.name = normalized;
                return genreRepository.save(created);
            });
            result.add(genre);
        }
        return result;
    }

    private AnimeResponse toAnimeResponse(Anime item) {
        List<String> genres = item.genres == null ? List.of()
                : item.genres.stream().map(genre -> genre.name).collect(Collectors.toList());
        return new AnimeResponse(item.id, item.title, item.originalTitle, item.summary, item.coverUrl, item.pvUrl,
                item.season, item.mediaType.name(), item.status.name(), item.score.doubleValue(), item.scoreCount,
                item.createdAt == null ? null : item.createdAt.toString(), genres);
    }

    private WatchlistResponse toWatchlistResponse(Watchlist item) {
        return new WatchlistResponse(item.anime.id, item.anime.title, item.anime.coverUrl, item.anime.score.doubleValue(),
                item.status.name(), item.updatedAt == null ? null : item.updatedAt.toString());
    }

    private CommentResponse toCommentResponse(Comment item, User viewer) {
        Hibernate.initialize(item.user);
        Hibernate.initialize(item.anime);
        String nickname = item.user.nickname == null || item.user.nickname.isEmpty() ? item.user.username : item.user.nickname;
        boolean liked = viewer != null && commentLikeRepository.findByUserIdAndCommentId(viewer.id, item.id).isPresent();
        return new CommentResponse(String.valueOf(item.id), item.user.username, nickname, item.user.id, item.anime.id,
                item.anime.title, item.content, item.likes, liked, item.createdAt == null ? null : item.createdAt.toString());
    }

    private double roundScore(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private String trimToNull(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        return value.trim();
    }

    public static class LoginRequest {
        @NotBlank public String username;
        @NotBlank public String password;
    }

    public static class RegisterRequest {
        @NotBlank public String username;
        @NotBlank @Email public String email;
        @NotBlank public String password;
    }

    public static class RatingRequest {
        @NotNull @Min(1) @Max(10) public Integer score;
    }

    public static class CommentRequest {
        @NotBlank public String content;
    }

    public static class WatchlistRequest {
        @NotBlank public String status;
    }

    public static class GenreRequest {
        @NotNull public List<String> genres;
    }

    public static class AnimeRequest {
        @NotBlank public String title;
        public String originalTitle;
        public String summary;
        public String coverUrl;
        public String pvUrl;
        public String season;
        @NotBlank public String mediaType;
        @NotBlank public String status;
        public List<String> genres;
    }

    public static class AuthResponse {
        public final String token;
        public final Long userId;
        public final String username;
        public final String nickname;
        public final String role;
        public final String expiresAt;

        public AuthResponse(String token, Long userId, String username, String nickname, String role, String expiresAt) {
            this.token = token;
            this.userId = userId;
            this.username = username;
            this.nickname = nickname;
            this.role = role;
            this.expiresAt = expiresAt;
        }
    }

    public static class RatingSummary {
        public final double score;
        public final int count;
        public final int[] distribution;
        public final Integer myRating;

        public RatingSummary(double score, int count, int[] distribution, Integer myRating) {
            this.score = score;
            this.count = count;
            this.distribution = distribution;
            this.myRating = myRating;
        }
    }

    public static class AnimeResponse {
        public final Long id;
        public final String title;
        public final String originalTitle;
        public final String summary;
        public final String coverUrl;
        public final String pvUrl;
        public final String season;
        public final String mediaType;
        public final String status;
        public final double score;
        public final int scoreCount;
        public final String createdAt;
        public final List<String> genres;

        public AnimeResponse(Long id, String title, String originalTitle, String summary, String coverUrl, String pvUrl,
                             String season, String mediaType, String status, double score, int scoreCount,
                             String createdAt, List<String> genres) {
            this.id = id;
            this.title = title;
            this.originalTitle = originalTitle;
            this.summary = summary;
            this.coverUrl = coverUrl;
            this.pvUrl = pvUrl;
            this.season = season;
            this.mediaType = mediaType;
            this.status = status;
            this.score = score;
            this.scoreCount = scoreCount;
            this.createdAt = createdAt;
            this.genres = genres;
        }
    }

    public static class CommentResponse {
        public final String id;
        public final String username;
        public final String nickname;
        public final Long userId;
        public final Long animeId;
        public final String animeTitle;
        public final String content;
        public final int likes;
        public final boolean liked;
        public final String createdAt;

        public CommentResponse(String id, String username, String nickname, Long userId, Long animeId, String animeTitle,
                               String content, int likes, boolean liked, String createdAt) {
            this.id = id;
            this.username = username;
            this.nickname = nickname;
            this.userId = userId;
            this.animeId = animeId;
            this.animeTitle = animeTitle;
            this.content = content;
            this.likes = likes;
            this.liked = liked;
            this.createdAt = createdAt;
        }
    }

    public static class WatchlistResponse {
        public final Long animeId;
        public final String title;
        public final String coverUrl;
        public final double score;
        public final String status;
        public final String updatedAt;

        public WatchlistResponse(Long animeId, String title, String coverUrl, double score, String status, String updatedAt) {
            this.animeId = animeId;
            this.title = title;
            this.coverUrl = coverUrl;
            this.score = score;
            this.status = status;
            this.updatedAt = updatedAt;
        }
    }
}

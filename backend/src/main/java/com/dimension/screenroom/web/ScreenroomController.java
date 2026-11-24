package com.dimension.screenroom.web;

import javax.validation.Valid;
import javax.validation.constraints.Email;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.dimension.screenroom.domain.Anime;
import com.dimension.screenroom.domain.Comment;
import com.dimension.screenroom.domain.Rating;
import com.dimension.screenroom.domain.User;
import com.dimension.screenroom.repository.AnimeRepository;
import com.dimension.screenroom.repository.CommentRepository;
import com.dimension.screenroom.repository.RatingRepository;
import com.dimension.screenroom.repository.UserRepository;
import com.dimension.screenroom.repository.GenreRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class ScreenroomController {
    private final UserRepository userRepository;
    private final AnimeRepository animeRepository;
    private final RatingRepository ratingRepository;
    private final CommentRepository commentRepository;
    private final GenreRepository genreRepository;

    public ScreenroomController(UserRepository userRepository, AnimeRepository animeRepository,
                                RatingRepository ratingRepository, CommentRepository commentRepository, GenreRepository genreRepository) {
        this.userRepository = userRepository;
        this.animeRepository = animeRepository;
        this.ratingRepository = ratingRepository;
        this.commentRepository = commentRepository;
        this.genreRepository = genreRepository;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.username, request.username)
                .orElseThrow(() -> new IllegalArgumentException("账号不存在"));
        if (!user.passwordHash.equals(request.password)) {
            return ResponseEntity.status(401).body(Map.of("message", "账号或密码错误"));
        }
        return ResponseEntity.ok(Map.of("token", "demo-token", "userId", user.id, "username", user.username, "role", user.role));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username)) return ResponseEntity.badRequest().body(Map.of("message", "用户名已存在"));
        if (userRepository.existsByEmail(request.email)) return ResponseEntity.badRequest().body(Map.of("message", "邮箱已注册"));
        User user = new User();
        user.username = request.username;
        user.email = request.email;
        user.passwordHash = request.password;
        user.role = "USER";
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("token", "demo-token", "userId", user.id, "username", user.username, "role", user.role));
    }

    @GetMapping
    public Map<String, Object> serviceInfo() {
        return Map.of("name", "次元放映室 API", "status", "running", "frontend", "http://localhost:5173", "endpoints", List.of("/api/anime", "/api/auth/login"));
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> adminUsers(@RequestHeader(value = "X-User-Role", defaultValue = "USER") String role) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).body(Map.of("message", "需要管理员权限"));
        return ResponseEntity.ok(userRepository.findAll().stream().map(user -> Map.of("id", user.id, "username", user.username, "email", user.email, "role", user.role)).collect(Collectors.toList()));
    }

    @DeleteMapping("/admin/users/{userId}")
    public ResponseEntity<?> deleteUser(@RequestHeader(value = "X-User-Role", defaultValue = "USER") String role, @PathVariable Long userId) {
        if (!"ADMIN".equals(role)) return ResponseEntity.status(403).body(Map.of("message", "需要管理员权限"));
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if ("ADMIN".equals(user.role)) return ResponseEntity.badRequest().body(Map.of("message", "管理员账号不能删除"));
        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "用户已删除"));
    }

    @GetMapping("/search")
    @Transactional(readOnly = true)
    public Map<String, Object> search(@RequestParam(defaultValue = "") String q) {
        String keyword = q.trim();
        if (keyword.isEmpty()) return Map.of("anime", List.of(), "comments", List.of());
        List<AnimeResponse> anime = animeRepository.search(keyword).stream()
                .map(item -> new AnimeResponse(item.id, item.title, item.coverUrl, item.pvUrl, item.season, item.mediaType.name(), item.status.name(), item.score.doubleValue(), item.scoreCount,
                        item.genres.stream().map(genre -> genre.name).collect(Collectors.toList())))
                .collect(Collectors.toList());
        List<Map<String, Object>> matchedComments = commentRepository.findByContentContainingIgnoreCase(keyword).stream()
                .map(item -> Map.<String, Object>of("id", item.id, "content", item.content, "username", item.user.username, "animeId", item.anime.id, "animeTitle", item.anime.title))
                .collect(Collectors.toList());
        return Map.of("anime", anime, "comments", matchedComments);
    }

    @DeleteMapping("/admin/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@RequestHeader(value = "X-User-Role", defaultValue = "USER") String role, @PathVariable Long commentId) {
        requireAdmin(role);
        commentRepository.deleteById(commentId);
        return ResponseEntity.ok(Map.of("message", "评论已删除"));
    }

    @PutMapping("/admin/anime/{animeId}/genres")
    @Transactional
    public ResponseEntity<?> updateGenres(@RequestHeader(value = "X-User-Role", defaultValue = "USER") String role, @PathVariable Long animeId, @Valid @RequestBody GenreRequest request) {
        requireAdmin(role);
        Anime anime = animeRepository.findById(animeId).orElseThrow(() -> new IllegalArgumentException("作品不存在"));
        anime.genres.clear();
        anime.genres.addAll(genreRepository.findByNameIn(request.genres));
        animeRepository.save(anime);
        return ResponseEntity.ok(Map.of("message", "作品标签已更新"));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equals(role)) throw new IllegalArgumentException("需要管理员权限");
    }

    @GetMapping("/anime")
    @Transactional(readOnly = true)
    public List<AnimeResponse> listAnime() {
        return animeRepository.findAllByOrderByScoreDesc().stream()
            .map(item -> new AnimeResponse(item.id, item.title, item.coverUrl, item.pvUrl, item.season, item.mediaType.name(), item.status.name(), item.score.doubleValue(), item.scoreCount,
                item.genres.stream().map(genre -> genre.name).collect(Collectors.toList())))
            .collect(Collectors.toList());
    }

    @GetMapping("/anime/{animeId}/rating")
    public RatingSummary rating(@PathVariable Long animeId) {
        Anime anime = animeRepository.findById(animeId).orElseThrow(() -> new IllegalArgumentException("番剧不存在"));
        return new RatingSummary(anime.score.doubleValue(), anime.scoreCount);
    }

    @PostMapping("/anime/{animeId}/ratings")
    @Transactional
    public RatingSummary rate(@PathVariable Long animeId, @Valid @RequestBody RatingRequest request) {
        User user = userRepository.findById(request.userId).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Anime anime = animeRepository.findById(animeId).orElseThrow(() -> new IllegalArgumentException("番剧不存在"));
        Rating rating = ratingRepository.findByUserIdAndAnimeId(user.id, animeId).orElseGet(Rating::new);
        if (rating.id == null) { rating.user = user; rating.anime = anime; anime.scoreCount++; }
        rating.score = request.score;
        ratingRepository.save(rating);
        double sum = ratingRepository.findAll().stream().filter(item -> item.anime.id.equals(animeId)).mapToInt(item -> item.score).sum();
        anime.score = BigDecimal.valueOf(Math.round(sum / anime.scoreCount * 10) / 10.0);
        animeRepository.save(anime);
        return new RatingSummary(anime.score.doubleValue(), anime.scoreCount);
    }

    @GetMapping("/anime/{animeId}/comments")
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(@PathVariable Long animeId) {
        return commentRepository.findByAnimeIdOrderByCreatedAtDesc(animeId).stream()
            .map(item -> new CommentResponse(String.valueOf(item.id), item.user.username, item.content, item.createdAt.toString()))
            .collect(Collectors.toList());
    }

    @PostMapping("/anime/{animeId}/comments")
    public CommentResponse comment(@PathVariable Long animeId, @Valid @RequestBody CommentRequest request) {
        User user = userRepository.findByUsernameOrEmail(request.username, request.username).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        Comment comment = new Comment();
        comment.user = user;
        comment.anime = animeRepository.findById(animeId).orElseThrow(() -> new IllegalArgumentException("番剧不存在"));
        comment.content = request.content;
        commentRepository.save(comment);
        return new CommentResponse(String.valueOf(comment.id), user.username, comment.content, comment.createdAt.toString());
    }

    public static class LoginRequest {
        @NotBlank public String username;
        @NotBlank public String password;
        @Email public String email;
    }
    public static class RegisterRequest {
        @NotBlank public String username;
        @NotBlank @Email public String email;
        @NotBlank public String password;
    }
    public static class GenreRequest {
        @NotNull public List<String> genres;
    }
    public static class RatingRequest {
        @NotNull @Min(1) @Max(10) public Integer score;
        @NotNull public Long userId;
    }
    public static class CommentRequest {
        @NotBlank public String username;
        @NotBlank public String content;
    }
    public static class RatingSummary {
        public final double score;
        public final int count;
        public RatingSummary(double score, int count) { this.score = score; this.count = count; }
    }
    public static class AnimeResponse {
        public final Long id;
        public final String title;
        public final String coverUrl;
        public final String pvUrl;
        public final String season;
        public final String mediaType;
        public final String status;
        public final double score;
        public final int scoreCount;
        public final List<String> genres;
        public AnimeResponse(Long id, String title, String coverUrl, String pvUrl, String season, String mediaType, String status, double score, int scoreCount, List<String> genres) {
            this.id = id;
            this.title = title;
            this.coverUrl = coverUrl;
            this.pvUrl = pvUrl;
            this.season = season;
            this.mediaType = mediaType;
            this.status = status;
            this.score = score;
            this.scoreCount = scoreCount;
            this.genres = genres;
        }
    }
    public static class CommentResponse {
        public final String id;
        public final String username;
        public final String content;
        public final String createdAt;
        public CommentResponse(String id, String username, String content, String createdAt) {
            this.id = id; this.username = username; this.content = content; this.createdAt = createdAt;
        }
    }
}

// PostBlog.java

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.util.Date;

@Entity
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
	// Custom annotation to limit size of variables
	@Target(ElementType.FIELD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MaxLength {
		int value();
	}
	
	@MaxLength(50)
    private String title;
	@MaxLength(250)
    private String content;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}

// PostRepository.java

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
}

// PostService.java

import java.util.List;

public interface PostService {
    List<Post> findAll();
    Post findById(Long id);
    Post save(Post post);
    void deleteById(Long id);
}

// PostServiceImplement.java

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PostServiceImplement implements PostService {

    @Autowired
    private PostRepository myPostRepository;
	private PostRepository myCommentRepository

    @Override
    public List<Post> findAll() {
        return myPostRepository.findAll();
    }

    @Override
    public Post findById(Long id) {
        return myPostRepository.findById(id).orElse(null);
    }

    @Override
    public Post save(Post post) {
        return myPostRepository.save(post);
    }

    @Override
    public void deleteById(Long id) {
        myPostRepository.deleteById(id);
    }
	
	@Override
	public void retrieveAllBlogPostsDesc {
		Pageable wholePage = Pageable.unpaged();
		return myPostRepository.findAll(wholePage);
	}
	
	@Override
	public void retrieveAllBlogPostBySingleUser(string userId) {
		return myPostRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
	}
	
	@Override
	@Transactional(readOnly = true)
	public void retrieveSingleBlogPostWithCommentsAsc {
	      return myPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
    }
		
	@Override
	public void deleteComment(Long commentId) {
        if (myCommentRepository.existsById(commentId)) {
            myCommentRepository.deleteById(commentId);
        } else {
            throw new CommentNotFoundException("Comment with ID " + commentId + " not found.");
        }
    }
}

// Combined Post with Comments 
public interface CombinedPostRepository extends JpaRepository<Post, Long>, CustomPostRepository {
    // Inherits both JpaRepository and custom methods
	@Service
	public class PostService {
		private final PostRepositoryA postRepositoryA;
		private final PostRepositoryB postRepositoryB;

    public PostService(PostRepositoryA postRepositoryA, PostRepositoryB postRepositoryB) {
        this.postRepositoryA = postRepositoryA;
        this.postRepositoryB = postRepositoryB;
    }

    public List<Post> getAllPosts() {
        List<Post> postsA = postRepositoryA.findAll();
        List<Post> postsB = postRepositoryB.findAll();
        List<Post> combinedPosts = new ArrayList<>();
        combinedPosts.addAll(postsA);
        combinedPosts.addAll(postsB);
        return combinedPosts;
    }
  }
}

// PostController.java

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService postService;
   
    @GetMapping
    public List<Post> findAll() {
    return postService.findAll();
}

@GetMapping("/{id}")
public Post findById(@PathVariable Long id) {
    return postService.findById(id);
}

@PostMapping
public Post save(@RequestBody Post post) {
    return postService.save(post);
}

@DeleteMapping("/{id}")
public void deleteById(@PathVariable Long id) {
    postService.deleteById(id);
}

}

// Blog request filter : user can update only their own blog post within 15 minutes of posting (perorm filter per IP Addrress)

@Component
public class blogRequestThrottleFilter implements Filter {
    private int MAX_BLOG_UPDATES_PER_SECOND = 900; // 15min
    private LoadingCache<String, Integer> requestCountsPerIpAddress;

    public requestThrottleFilter(){
      super();
      requestCountsPerIpAddress = Caffeine.newBuilder().
            expireAfterWrite(1, TimeUnit.SECONDS).build(new CacheLoader<String, Integer>() {
        public Integer load(String key) {
            return 0;
        }
    });
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        String clientIpAddress = getClientIP((HttpServletRequest) servletRequest);
        if(isMaximumRequestsPerSecondExceeded(clientIpAddress)){
          httpServletResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
          httpServletResponse.getWriter().write("Too many requests");
          return;
         }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    private boolean isMaximumRequestsPerSecondExceeded(String clientIpAddress){
      Integer requests = 0;
      requests = requestCountsPerIpAddress.get(clientIpAddress);
      if(requests != null){
          if(requests > MAX_BLOG_UPDATES_PER_SECOND) {
            requestCountsPerIpAddress.asMap().remove(clientIpAddress);
            requestCountsPerIpAddress.put(clientIpAddress, requests);
            return true;
        }

      } else {
        requests = 0;
      }
      requests++;
      requestCountsPerIpAddress.put(clientIpAddress, requests);
      return false;
      }

    public String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null){
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0]; 
    }

    @Override
    public void destroy() {

    }
}
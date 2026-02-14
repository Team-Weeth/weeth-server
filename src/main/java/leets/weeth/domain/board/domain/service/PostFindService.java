package leets.weeth.domain.board.domain.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import leets.weeth.domain.board.application.exception.PostNotFoundException;
import leets.weeth.domain.board.domain.entity.Post;
import leets.weeth.domain.board.domain.entity.enums.Category;
import leets.weeth.domain.board.domain.entity.enums.Part;
import leets.weeth.domain.board.domain.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostFindService {

    private final PostRepository postRepository;

    public Post find(Long postId){
        return postRepository.findById(postId)
                .orElseThrow(PostNotFoundException::new);
    }

    public List<Post> find(){
        return postRepository.findAll();
    }

    public List<String> findByPart(Part part) {
        return postRepository.findDistinctStudyNamesByPart(part);
    }

    public Slice<Post> findRecentPosts(Pageable pageable) {
        return postRepository.findRecentPart(pageable);
    }

    public Slice<Post> findRecentEducationPosts(Pageable pageable) {
        return postRepository.findRecentEducation(pageable);
    }

    public Slice<Post> search(String keyword, Pageable pageable) {
        if(keyword == null || keyword.isEmpty()){
            return findRecentPosts(pageable);
        }
        return postRepository.searchPart(keyword.strip(), pageable);
    }

    public Slice<Post> searchEducation(String keyword, Pageable pageable) {
        if(keyword == null || keyword.isEmpty()){
            return findRecentEducationPosts(pageable);
        }
        return postRepository.searchEducation(keyword.strip(), pageable);
    }

    public Slice<Post> findByPartAndOptionalFilters(Part part, Category category, Integer cardinalNumber, String  studyName, Integer week, Pageable pageable) {

        return postRepository.findByPartAndOptionalFilters(
                part, category, cardinalNumber, studyName, week, pageable
        );
    }

    public Slice<Post> findEducationByCardinals(Part part, Collection<Integer> cardinals, Pageable pageable) {
        if (cardinals == null || cardinals.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }
        String partName = (part != null ? part.name() : Part.ALL.name());

        return postRepository.findByCategoryAndCardinalInWithPart(partName, Category.Education, cardinals, pageable);
    }

    public Slice<Post> findEducationByCardinal(Part part, int cardinalNumber, Pageable pageable) {
        String partName = (part != null ? part.name() : Part.ALL.name());

        return postRepository.findByCategoryAndCardinalNumberWithPart(partName, Category.Education, cardinalNumber, pageable);
    }

    public Slice<Post> findByCategory(Part part, Category category, Integer cardinal, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        String partName = (part != null ? part.name() : Part.ALL.name());

        return postRepository.findByCategoryAndOptionalCardinalWithPart(partName, category, cardinal, pageable);
    }
}

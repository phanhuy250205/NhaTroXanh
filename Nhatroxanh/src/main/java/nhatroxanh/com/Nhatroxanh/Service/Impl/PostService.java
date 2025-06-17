package nhatroxanh.com.Nhatroxanh.Service.Impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import nhatroxanh.com.Nhatroxanh.Model.enity.ApprovalStatus;
import nhatroxanh.com.Nhatroxanh.Model.enity.Post;
import nhatroxanh.com.Nhatroxanh.Repository.PostRepository;

@Service
public class PostService {
    @Autowired
    private PostRepository postRepository;

    public List<Post> findTopApprovedActivePostsByViews(int limit) {
        return postRepository.findByStatusTrueAndApprovalStatusOrderByViewDesc(
            ApprovalStatus.APPROVED, PageRequest.of(0, limit)).getContent();
    }
}

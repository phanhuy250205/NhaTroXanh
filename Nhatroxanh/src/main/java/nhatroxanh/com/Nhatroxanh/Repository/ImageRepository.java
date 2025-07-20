package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.entity.Image;
import nhatroxanh.com.Nhatroxanh.Model.entity.Post;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {
    List<Image> findByPost_PostId(Integer postId);

    List<Image> findByPost(Post post);

    void deleteByPost(Post post);
}

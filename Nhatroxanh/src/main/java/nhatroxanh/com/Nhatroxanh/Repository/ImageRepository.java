package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Image;

@Repository
public interface ImageRepository extends JpaRepository<Image, Integer> {
    List<Image> findByPost_PostId(Integer postId);
    
}

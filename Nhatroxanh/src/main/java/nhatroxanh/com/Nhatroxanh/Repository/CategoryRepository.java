package nhatroxanh.com.Nhatroxanh.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import nhatroxanh.com.Nhatroxanh.Model.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
    List<Category> findByNameContainingIgnoreCase(String name);
}

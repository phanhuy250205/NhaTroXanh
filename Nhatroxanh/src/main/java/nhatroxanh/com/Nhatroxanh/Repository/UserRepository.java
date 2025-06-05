package nhatroxanh.com.Nhatroxanh.Repository;

import org.springframework.data.jpa.repository.JpaRepository;

import nhatroxanh.com.Nhatroxanh.Model.enity.Users;

public interface UserRepository extends JpaRepository<Users, Integer> {
}

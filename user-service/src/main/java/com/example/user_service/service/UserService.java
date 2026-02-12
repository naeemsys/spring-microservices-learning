package com.example.user_service.service;

import com.example.user_service.entity.User;
import com.example.user_service.repo.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
  private final UserRepository repo;
  public UserService(UserRepository repo){ this.repo = repo; }

  public User create(User u){ return repo.save(u); }
  public Optional<User> findById(Long id){ return repo.findById(id); }
  public void delete(Long id){ repo.deleteById(id); }
}

package br.com.fabricio.url_shortener_api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(nullable = false)
  private String password;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  protected User() {
  }

  public User(String name, String email, String password, LocalDateTime createdAt) {
    this.name = name;
    this.email = email;
    this.password = password;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public String getPassword() {
    return password;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}

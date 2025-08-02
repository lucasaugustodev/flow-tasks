package com.projectmanagement.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_access")
public class ProjectAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private AccessRole role = AccessRole.MEMBER;

    @Column(name = "granted_at")
    private LocalDateTime grantedAt;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    // Constructors
    public ProjectAccess() {}

    public ProjectAccess(Project project, User user, AccessRole role, User grantedBy) {
        this.project = project;
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
        this.grantedAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        grantedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public AccessRole getRole() { return role; }
    public void setRole(AccessRole role) { this.role = role; }

    public LocalDateTime getGrantedAt() { return grantedAt; }
    public void setGrantedAt(LocalDateTime grantedAt) { this.grantedAt = grantedAt; }

    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getGrantedBy() { return grantedBy; }
    public void setGrantedBy(User grantedBy) { this.grantedBy = grantedBy; }
}



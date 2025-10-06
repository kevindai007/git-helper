package com.kevindai.git.helper.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "mr_info")
public class MrInfoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @NotNull
    @Column(name = "mr_id", nullable = false)
    private Long mrId;

    @Size(max = 512)
    @NotNull
    @Column(name = "mr_title", nullable = false, length = 512)
    private String mrTitle;

    @Size(max = 64)
    @NotNull
    @Column(name = "state", nullable = false, length = 64)
    private String state;

    @Size(max = 256)
    @NotNull
    @Column(name = "target_branch", nullable = false, length = 256)
    private String targetBranch;

    @Size(max = 256)
    @NotNull
    @Column(name = "source_branch", nullable = false, length = 256)
    private String sourceBranch;

    @Size(max = 256)
    @NotNull
    @Column(name = "sha", nullable = false, length = 256)
    private String sha;

    @Size(max = 512)
    @NotNull
    @Column(name = "web_url", nullable = false, length = 512)
    private String webUrl;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "summary_markdown", columnDefinition = "TEXT")
    private String summaryMarkdown;

}
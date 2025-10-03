package com.kevindai.git.helper.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "mr_analysis_detail")
public class MrAnalysisDetailEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "mr_info_id", nullable = false)
    private Long mrInfoId;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "mr_id", nullable = false)
    private Long mrId;

    @Column(name = "severity", length = 16)
    private String severity;

    @Column(name = "category", length = 32)
    private String category;

    @Column(name = "title", length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "file", length = 512)
    private String file;

    @Column(name = "line_type", length = 8)
    private String lineType; // new_line|old_line

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "start_col")
    private Integer startCol;

    @Column(name = "end_col")
    private Integer endCol;

    @Column(name = "evidence", columnDefinition = "TEXT")
    private String evidence;

    @Column(name = "remediation_steps", columnDefinition = "TEXT")
    private String remediationSteps;

    @Column(name = "remediation_diff", columnDefinition = "TEXT")
    private String remediationDiff;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "tags_json", columnDefinition = "TEXT")
    private String tagsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}


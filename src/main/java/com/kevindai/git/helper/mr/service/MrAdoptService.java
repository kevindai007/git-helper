package com.kevindai.git.helper.mr.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

import com.kevindai.git.helper.entity.MrAnalysisDetailEntity;
import com.kevindai.git.helper.entity.MrInfoEntity;
import com.kevindai.git.helper.mr.dto.gitlab.MrVersion;
import com.kevindai.git.helper.repository.MrAnalysisDetailRepository;
import com.kevindai.git.helper.repository.MrInfoEntityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MrAdoptService {

    private final MrAnalysisDetailRepository detailRepository;
    private final MrInfoEntityRepository mrInfoRepository;
    private final GitLabService gitLabService;
    private final AddressableDiffBuilder addressableDiffBuilder;
    private final GitTokenService gitTokenService;
    private final GitLabRequestContext gitLabRequestContext;

    public void adoptRecommendation(long detailId) {
        MrAnalysisDetailEntity detail = detailRepository.findById(detailId)
                .orElseThrow(() -> new IllegalArgumentException("Detail not found: " + detailId));

        MrInfoEntity mrInfo = mrInfoRepository.findById(detail.getMrInfoId())
                .orElseThrow(() -> new IllegalStateException("MR info not found for id: " + detail.getMrInfoId()));

        long projectId = detail.getProjectId();
        int mrId = Math.toIntExact(detail.getMrId());

        // Resolve token by parsing group path from MR URL
        var parsedUrl = gitLabService.parseMrUrl(mrInfo.getWebUrl());
        String token = gitTokenService.resolveTokenForGroup(parsedUrl.getProjectFullPath());
        gitLabRequestContext.setGroupFullPath(parsedUrl.getProjectFullPath());
        gitLabRequestContext.setToken(token);

        List<MrVersion> versions = gitLabService.fetchMrVersions(projectId, mrId);
        if (versions == null || versions.isEmpty()) {
            throw new IllegalStateException("No versions found for MR " + mrId);
        }

        // Pick the latest by created_at
        MrVersion latest = versions.stream().max(Comparator.comparing(v -> parseTimeSafe(v.getCreated_at()))).orElse(versions.getFirst());

        if (!safeEq(latest.getHead_commit_sha(), mrInfo.getSha())) {
            throw new IllegalStateException("MR head SHA mismatch. Expected=" + mrInfo.getSha() + ", latest=" + latest.getHead_commit_sha());
        }

        // fetch diffs (latest head verified above) and build anchor index
        var diffs = gitLabService.fetchMrDiffs(projectId, mrId);
        var annotated = addressableDiffBuilder.buildAnnotatedWithIndex(diffs);
        var index = annotated.getIndex();
        Integer newLine = null;
        Integer oldLine = null;
        String filePathForPosition = detail.getFile();

        // Prefer anchor if available
        if (StringUtils.hasText(detail.getAnchorId())) {
            var ae = index.get(detail.getAnchorId());
            if (ae == null) {
                throw new IllegalStateException("Anchor not found in current diff index: " + detail.getAnchorId());
            }
            if (ae.side == 'N') {
                filePathForPosition = ae.newPath;
                newLine = ae.newLine;
            } else {
                filePathForPosition = ae.oldPath;
                oldLine = ae.oldLine;
            }
        }

        String finalFilePathForPosition = filePathForPosition;
        var matched = diffs.stream()
                .filter(d -> safeEq(finalFilePathForPosition, d.getNew_path()) || safeEq(finalFilePathForPosition, d.getOld_path()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No diff found for file: " + finalFilePathForPosition));

        // If still no line resolved, fall back to stored lineType/startLine
        if (newLine == null && oldLine == null) {
            String lt = detail.getLineType();
            Integer sl = detail.getStartLine();
            if (sl == null || !StringUtils.hasText(lt)) {
                throw new IllegalStateException("Missing anchor and lineType/startLine for detail: " + detailId);
            }
            if ("new_line".equals(lt)) {
                newLine = sl;
            } else if ("old_line".equals(lt)) {
                oldLine = sl;
            } else {
                throw new IllegalStateException("Unsupported lineType: " + lt);
            }
        }

        // Enforce file state compatibility
        if (Boolean.TRUE.equals(matched.isDeleted_file())) {
            newLine = null; // cannot comment on new side for deleted files
            if (oldLine == null) {
                throw new IllegalStateException("Deleted file requires old_line");
            }
        }
        if (Boolean.TRUE.equals(matched.isNew_file())) {
            oldLine = null; // cannot comment on old side for new files
            if (newLine == null) {
                throw new IllegalStateException("New file requires new_line");
            }
        }

        gitLabService.createMrDiscussion(
                projectId,
                mrId,
                latest.getBase_commit_sha(),
                latest.getHead_commit_sha(),
                latest.getStart_commit_sha(),
                filePathForPosition,
                newLine,
                oldLine,
                detail.getRemediationSteps()
        );

        // Mark as adopted on success
        detail.setStatus(1);
        detailRepository.save(detail);

    }

    private static Instant parseTimeSafe(String iso) {
        if (iso == null) {
            return Instant.EPOCH;
        }
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }

    private static boolean safeEq(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    static class Anchor {

        enum Side {NEW, OLD}

        final Side side;
        final String path;
        final int line;

        Anchor(Side s, String p, int l) {
            this.side = s;
            this.path = p;
            this.line = l;
        }

        static Anchor parse(String anchorId) {
            if (anchorId == null) {
                return null;
            }
            String id = anchorId.trim();
            // Accept forms with or without surrounding markers, e.g., <<ANCHOR N:path:line>> or N:path:line
            if (id.startsWith("<<ANCHOR ") && id.endsWith(">>")) {
                id = id.substring("<<ANCHOR ".length(), id.length() - 2);
            }
            String[] parts = id.split(":", 3);
            if (parts.length != 3) {
                return null;
            }
            Side side = switch (parts[0]) {
                case "N" -> Side.NEW;
                case "O" -> Side.OLD;
                default -> null;
            };
            if (side == null) {
                return null;
            }
            int line;
            try {
                line = Integer.parseInt(parts[2]);
            } catch (Exception e) {
                return null;
            }
            return new Anchor(side, parts[1], line);
        }
    }
}

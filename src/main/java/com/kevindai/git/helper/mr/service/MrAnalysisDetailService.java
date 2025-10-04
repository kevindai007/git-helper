package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.entity.MrAnalysisDetailEntity;
import com.kevindai.git.helper.entity.MrInfoEntity;
import com.kevindai.git.helper.mr.dto.llm.Finding;
import com.kevindai.git.helper.mr.dto.llm.LlmAnalysisReport;
import com.kevindai.git.helper.repository.MrAnalysisDetailRepository;
import com.kevindai.git.helper.utils.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MrAnalysisDetailService {

    private final MrAnalysisDetailRepository analysisDetailRepository;

    public void persist(MrInfoEntity mrInfo,
                        LlmAnalysisReport report,
                        Map<String, AddressableDiffBuilder.AnchorEntry> anchorIndex) {
        if (mrInfo == null || report == null || report.getFindings() == null) {
            return;
        }

        try {
            analysisDetailRepository.deleteByMrInfoId(mrInfo.getId());
        } catch (Exception e) {
            log.warn("Failed clearing previous analysis details for mr_info_id={}", mrInfo.getId());
        }

        Instant now = Instant.now();
        for (Finding f : report.getFindings()) {
            MrAnalysisDetailEntity e = new MrAnalysisDetailEntity();
            e.setMrInfoId(mrInfo.getId());
            e.setProjectId(mrInfo.getProjectId());
            e.setMrId(mrInfo.getMrId());
            e.setStatus(0); // default: not adopted
            e.setSeverity(f.getSeverity());
            e.setCategory(f.getCategory());
            e.setTitle(f.getTitle());
            e.setDescription(f.getDescription());

            if (f.getLocation() != null) {
                String anchorId = f.getLocation().getAnchorId();
                String anchorSide = f.getLocation().getAnchorSide();
                e.setAnchorId(anchorId);
                e.setAnchorSide(anchorSide);

                AddressableDiffBuilder.AnchorEntry ae = anchorId != null && anchorIndex != null ? anchorIndex.get(anchorId) : null;
                if (ae != null) {
                    if (ae.side == 'N') {
                        e.setFile(ae.newPath);
                        e.setLineType("new_line");
                        e.setStartLine(ae.newLine);
                    } else {
                        e.setFile(ae.oldPath);
                        e.setLineType("old_line");
                        e.setStartLine(ae.oldLine);
                    }
                } else {
                    e.setFile(f.getLocation().getFile());
                    e.setStartLine(f.getLocation().getStartLine());
                    e.setLineType(f.getLocation().getLineType());
                }
            }

            e.setEvidence(f.getEvidence());
            if (f.getRemediation() != null) {
                e.setRemediationSteps(f.getRemediation().getSteps());
            }
            e.setConfidence(f.getConfidence());
            if (f.getTags() != null) {
                e.setTagsJson(JsonUtils.toJSONString(f.getTags()));
            }
            e.setCreatedAt(now);
            e.setUpdatedAt(now);
            analysisDetailRepository.save(e);
        }
    }
}

package com.kevindai.git.helper.mr.service;

import com.kevindai.git.helper.config.GitConfig;
import com.kevindai.git.helper.entity.GitTokenEntity;
import com.kevindai.git.helper.repository.GitTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class GitTokenService {

    private final GitTokenRepository tokenRepository;
    private final GitConfig gitConfig; // fallback token from config/env

    public String resolveTokenForGroup(String fullGroupPath) {
        // Generate candidates: full path, then parent prefixes
        List<String> candidates = new ArrayList<>();
        if (StringUtils.hasText(fullGroupPath)) {
            String[] parts = fullGroupPath.split("/");
            for (int i = parts.length; i >= 1; i--) {
                String prefix = String.join("/", Arrays.copyOf(parts, i));
                candidates.add(prefix);
            }
        }

        if (!candidates.isEmpty()) {
            List<GitTokenEntity> matches = tokenRepository.findByGroupPathIn(candidates);
            if (!matches.isEmpty()) {
                // pick the longest group_path match
                matches.sort(Comparator.comparingInt(e -> -Optional.ofNullable(e.getGroupPath()).orElse("").length()));
                return matches.getFirst().getToken();
            }
        }

        // fallback to default record
        Optional<GitTokenEntity> def = tokenRepository.findFirstByIsDefaultIsTrue();
        if (def.isPresent()) {
            return def.get().getToken();
        }

        // final fallback to config token
        return gitConfig.getToken();
    }
}


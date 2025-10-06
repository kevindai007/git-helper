package com.kevindai.git.helper.repository;

import com.kevindai.git.helper.entity.GitTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GitTokenRepository extends JpaRepository<GitTokenEntity, Long> {
    Optional<GitTokenEntity> findFirstByIsDefaultIsTrue();

    Optional<GitTokenEntity> findByGroupPath(String groupPath);

    List<GitTokenEntity> findByGroupPathIn(Collection<String> groupPaths);
}


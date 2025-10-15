package com.kopo.hanagreenworld.activity.repository;

import com.kopo.hanagreenworld.activity.domain.ImageHash;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface ImageHashRepository extends JpaRepository<ImageHash, Long> {

    Optional<ImageHash> findByImageHash(String imageHash);

    List<ImageHash> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    Optional<ImageHash> findByMemberIdAndChallengeId(Long memberId, Long challengeId);

    boolean existsByImageHash(String imageHash);

    boolean existsByMemberIdAndImageHash(Long memberId, String imageHash);

    @Query("SELECT COUNT(DISTINCT ih.memberId) FROM ImageHash ih WHERE ih.imageHash = :imageHash")
    long countDistinctMembersByImageHash(@Param("imageHash") String imageHash);

    long countByMemberId(Long memberId);
}

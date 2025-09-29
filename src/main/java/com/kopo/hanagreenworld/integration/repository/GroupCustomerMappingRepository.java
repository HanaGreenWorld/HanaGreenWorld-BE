package com.kopo.hanagreenworld.integration.repository;

import com.kopo.hanagreenworld.integration.domain.GroupCustomerMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupCustomerMappingRepository extends JpaRepository<GroupCustomerMapping, Long> {

    /**
     * 그룹 고객 토큰으로 매핑 정보 조회
     */
    Optional<GroupCustomerMapping> findByGroupCustomerToken(String groupCustomerToken);

    /**
     * 휴대폰 번호로 고객 매핑 조회 (본인인증 시 사용)
     */
    Optional<GroupCustomerMapping> findByPhoneNumber(String phoneNumber);

    /**
     * 이메일로 고객 매핑 조회
     */
    Optional<GroupCustomerMapping> findByEmail(String email);

    /**
     * 휴대폰 번호와 이름으로 고객 매핑 조회 (보안 강화)
     */
    Optional<GroupCustomerMapping> findByPhoneNumberAndName(String phoneNumber, String name);

    /**
     * 특정 그룹사에 계정이 있는 고객 수 조회
     */
    @Query("SELECT COUNT(g) FROM GroupCustomerMapping g WHERE g.hasBankAccount = :hasBankAccount")
    long countByHasBankAccount(@Param("hasBankAccount") Boolean hasBankAccount);

    @Query("SELECT COUNT(g) FROM GroupCustomerMapping g WHERE g.hasCardAccount = :hasCardAccount")
    long countByHasCardAccount(@Param("hasCardAccount") Boolean hasCardAccount);

    @Query("SELECT COUNT(g) FROM GroupCustomerMapping g WHERE g.hasGreenWorldAccount = :hasGreenWorldAccount")
    long countByHasGreenWorldAccount(@Param("hasGreenWorldAccount") Boolean hasGreenWorldAccount);

    /**
     * 모든 그룹사에 계정이 있는 고객 조회
     */
    @Query("SELECT g FROM GroupCustomerMapping g WHERE g.hasBankAccount = true AND g.hasCardAccount = true AND g.hasGreenWorldAccount = true")
    java.util.List<GroupCustomerMapping> findAllGroupCustomers();
}










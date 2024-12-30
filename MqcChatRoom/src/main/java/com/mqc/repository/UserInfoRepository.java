package com.mqc.repository;

import com.mqc.entity.UserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserInfoRepository extends JpaRepository<UserInfo, Integer> {

    UserInfo findByAccount(String account);

    UserInfo findByExtension(String extension);

    List<UserInfo> findAllByIsAdmin(boolean isAdmin);
    @Query(value = "select u from UserInfo u where u.account =:account or :account is null")
    Page<UserInfo> findAllByAccount(String account, Pageable pageable);
}

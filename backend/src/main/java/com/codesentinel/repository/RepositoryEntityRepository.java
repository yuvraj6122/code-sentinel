package com.codesentinel.repository;

import com.codesentinel.model.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositoryEntityRepository extends JpaRepository<RepositoryEntity, Long> {
}

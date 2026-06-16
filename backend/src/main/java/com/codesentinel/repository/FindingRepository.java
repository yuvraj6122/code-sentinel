package com.codesentinel.repository;

import com.codesentinel.model.Finding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FindingRepository extends JpaRepository<Finding, Long> {
}

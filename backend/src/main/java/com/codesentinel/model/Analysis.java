package com.codesentinel.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "analyses")
public class Analysis {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "repository_id", nullable = false)
	private RepositoryEntity repository;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AnalysisStatus status;

	private LocalDateTime startedAt;

	private LocalDateTime completedAt;

	@OneToMany(mappedBy = "analysis", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Finding> findings = new ArrayList<>();

	public Analysis() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RepositoryEntity getRepository() {
		return repository;
	}

	public void setRepository(RepositoryEntity repository) {
		this.repository = repository;
	}

	public AnalysisStatus getStatus() {
		return status;
	}

	public void setStatus(AnalysisStatus status) {
		this.status = status;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(LocalDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}

	public List<Finding> getFindings() {
		return findings;
	}

	public void setFindings(List<Finding> findings) {
		this.findings = findings;
	}
}

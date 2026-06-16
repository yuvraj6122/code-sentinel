package com.codesentinel.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "repositories")
public class RepositoryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String githubUrl;

	@Column(nullable = false)
	private String name;

	private String language;

	private String buildTool;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "repository", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Analysis> analyses = new ArrayList<>();

	public RepositoryEntity() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getGithubUrl() {
		return githubUrl;
	}

	public void setGithubUrl(String githubUrl) {
		this.githubUrl = githubUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getBuildTool() {
		return buildTool;
	}

	public void setBuildTool(String buildTool) {
		this.buildTool = buildTool;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public List<Analysis> getAnalyses() {
		return analyses;
	}

	public void setAnalyses(List<Analysis> analyses) {
		this.analyses = analyses;
	}
}

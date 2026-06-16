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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "analyses")
@Getter
@Setter
@NoArgsConstructor
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
}

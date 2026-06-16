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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repositories")
@Getter
@Setter
@NoArgsConstructor
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
}

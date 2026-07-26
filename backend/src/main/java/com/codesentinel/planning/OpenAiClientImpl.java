package com.codesentinel.planning;

import com.codesentinel.config.PlanningProperties;
import com.codesentinel.exception.PlanningAgentException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * OpenAI chat-completions client built on the JDK {@link HttpClient}. Requests
 * JSON-object responses, applies a per-request timeout, and retries transient
 * failures (timeouts, rate limits, 5xx) with linear backoff. Non-recoverable
 * errors (missing key, 4xx) fail fast with a clear message.
 */
@Slf4j
@Component
public class OpenAiClientImpl implements OpenAiClient {

	private final PlanningProperties properties;
	private final ObjectMapper objectMapper;
	private final HttpClient httpClient;

	public OpenAiClientImpl(PlanningProperties properties, ObjectMapper objectMapper) {
		this.properties = properties;
		this.objectMapper = objectMapper;
		this.httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
				.build();
	}

	@Override
	public String complete(String systemPrompt, String userPrompt) {
		if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			throw new PlanningAgentException(
					"OpenAI API key is not configured (set codesentinel.planning.api-key)");
		}

		HttpRequest request = buildRequest(systemPrompt, userPrompt);
		int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);

		PlanningAgentException lastTransient = null;
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				HttpResponse<String> response =
						httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				int status = response.statusCode();

				if (status >= 200 && status < 300) {
					return extractContent(response.body());
				}
				if (isTransient(status)) {
					lastTransient = new PlanningAgentException(
							"OpenAI request failed with status " + status);
					log.warn(
							"OpenAI attempt {}/{} got transient status {} — retrying",
							attempt,
							maxAttempts,
							status);
				} else {
					throw new PlanningAgentException(
							"OpenAI request failed with status " + status + ": " + summarize(response.body()));
				}
			} catch (IOException ex) {
				lastTransient = new PlanningAgentException("OpenAI request failed: " + ex.getMessage(), ex);
				log.warn("OpenAI attempt {}/{} failed — {}", attempt, maxAttempts, ex.getMessage());
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new PlanningAgentException("OpenAI request was interrupted", ex);
			}

			backoff(attempt, maxAttempts);
		}

		throw lastTransient != null
				? lastTransient
				: new PlanningAgentException("OpenAI request failed after " + maxAttempts + " attempts");
	}

	private HttpRequest buildRequest(String systemPrompt, String userPrompt) {
		Map<String, Object> body = Map.of(
				"model", properties.getModel(),
				"temperature", properties.getTemperature(),
				"response_format", Map.of("type", "json_object"),
				"messages", List.of(
						Map.of("role", "system", "content", systemPrompt),
						Map.of("role", "user", "content", userPrompt)));
		try {
			return HttpRequest.newBuilder()
					.uri(URI.create(properties.getBaseUrl() + "/chat/completions"))
					.timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
					.header("Authorization", "Bearer " + properties.getApiKey())
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
					.build();
		} catch (JacksonException ex) {
			throw new PlanningAgentException("Failed to serialize OpenAI request", ex);
		}
	}

	private String extractContent(String body) {
		try {
			JsonNode content = objectMapper.readTree(body).path("choices").path(0).path("message")
					.path("content");
			if (content.isMissingNode() || content.asString().isBlank()) {
				throw new PlanningAgentException("OpenAI response did not contain any content");
			}
			return content.asString();
		} catch (JacksonException ex) {
			throw new PlanningAgentException("Could not parse the OpenAI response envelope", ex);
		}
	}

	private boolean isTransient(int status) {
		return status == 429 || status >= 500;
	}

	private void backoff(int attempt, int maxAttempts) {
		if (attempt >= maxAttempts) {
			return;
		}
		try {
			Thread.sleep(properties.getRetryBackoffMillis() * attempt);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new PlanningAgentException("OpenAI retry backoff was interrupted", ex);
		}
	}

	private String summarize(String body) {
		if (body == null || body.isBlank()) {
			return "no response body";
		}
		return body.length() > 300 ? body.substring(0, 300) + "…" : body;
	}
}

package it.giannibombelli.wsc2026.testsupport;

import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.json.JavalinJackson3;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

public final class JavalinTestHelper {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private Javalin app;
    private int port;

    public void start(Consumer<JavalinConfig> registrar) {
        app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson3());
            registrar.accept(config);
        });
        app.start(0);
        port = app.port();
    }

    public void stop() {
        if (app != null) {
            app.stop();
            app = null;
        }
    }

    public int port() {
        return port;
    }

    public TestResponse post(String path, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return new TestResponse(response.statusCode(), response.body(), response.headers());
        } catch (Exception e) {
            throw new RuntimeException("POST " + path + " failed", e);
        }
    }

    public TestResponse post(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return new TestResponse(response.statusCode(), response.body(), response.headers());
        } catch (Exception e) {
            throw new RuntimeException("POST " + path + " failed", e);
        }
    }

    public TestResponse get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            return new TestResponse(response.statusCode(), response.body(), response.headers());
        } catch (Exception e) {
            throw new RuntimeException("GET " + path + " failed", e);
        }
    }

    public record TestResponse(int status, String body, HttpHeaders headers) {

        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }
    }
}
package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class CrptApi {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String CREATE_DOCUMENT_URI = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final HttpClient httpClient;
    private final Semaphore requestSemaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        requestSemaphore = new Semaphore(requestLimit);

        httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(requestSemaphore::drainPermits, 1, 1, timeUnit);
    }

    public void createDocument(Document document, String signature) {
        try {
            requestSemaphore.acquire();
            String jsonDocument = objectMapper.writeValueAsString(document);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CREATE_DOCUMENT_URI))
                    .header("Content-Type", "application/json")
                    .header("Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(System.out::println)
                    .join();
        } catch (InterruptedException | JsonProcessingException e) {
            Thread.currentThread().interrupt();
        }
    }


    public record Document(Description description, String doc_id, String doc_status, String doc_type,
                           boolean importRequest, String owner_inn, String participant_inn, String producer_inn,
                           String production_date, String production_type, Product[] products, String reg_date,
                           String reg_number) {
    }

    public record Description(String participantInn) {
    }

    public record Product(String certificate_document, String certificate_document_date,
                          String certificate_document_number, String owner_inn, String producer_inn,
                          String production_date, String tnved_code, String uit_code, String uitu_code) {
    }
}


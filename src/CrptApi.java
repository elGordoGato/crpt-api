import com.google.gson.Gson;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.http.*;
import java.net.URI;
import java.time.Duration;

public class CrptApi {
    private final HttpClient httpClient;
    private final int requestLimit;

    private final TimeUnit timeUnit;
    private final Duration durationLimit;
    private final AtomicInteger currentRequestCount;
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.durationLimit = Duration.ofMillis(timeUnit.toMillis(1));
        this.currentRequestCount = new AtomicInteger(0);
        this.semaphore = new Semaphore(requestLimit);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        new Thread(() -> {
            while (true) {
                try {
                    timeUnit.sleep(1);
                    semaphore.release(requestLimit);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public synchronized void createDocument(String jsonDocument, String signature) throws InterruptedException {
        if (semaphore.tryAcquire(1, timeUnit)) {
            try {
                if (currentRequestCount.incrementAndGet() <= requestLimit) {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                            .header("Content-Type", "application/json")
                            .header("Signature", signature)
                            .POST(HttpRequest.BodyPublishers.ofString(jsonDocument))
                            .build();

                    httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                            .thenApply(HttpResponse::body)
                            .thenAccept(System.out::println)
                            .join();
                }
            } finally {
                currentRequestCount.decrementAndGet();
                semaphore.release();
            }
        } else {
            System.out.println("Request limit exceeded. Please wait.");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        CrptApi crptApi = new CrptApi(TimeUnit.MINUTES, 5);
        for (int i = 0; i < 15; i++) {
            crptApi.createDocument("json", "sig");
        }
    }

    // Внутренние классы, если они вам нужны, могут быть добавлены здесь.
}

// Пример использования:
// CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 5); // 5 запросов в секунду
// crptApi.createDocument(jsonDocument, signature);


/*

public class CrptApi {
    private String urlValue = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final Gson gson = new Gson();
    private final Semaphore semaphore;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        semaphore = new Semaphore(requestLimit, true);

        new Thread(() -> {
            while (true) {
                try {
                    timeUnit.sleep(1);
                    semaphore.release(requestLimit);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    public void createDocument(Document document, String signature) {
        try {
            semaphore.acquire();

            URL url = new URL(urlValue);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Signature", signature);
            connection.setDoOutput(true);

            OutputStream output = connection.getOutputStream();

            String json = gson.toJson(document);
            output.write(json.getBytes(StandardCharsets.UTF_8));

            output.close();

            int responseCode = connection.getResponseCode();

            if (responseCode == HttpsURLConnection.HTTP_OK) {
                System.out.println("Document created successfully");
            } else {
                System.out.println("Document creation failed: " + responseCode);
            }
            connection.disconnect();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public void setUrl(String urlValue){
        this.urlValue = urlValue;
    }


    private static class Document {
        private final Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private Product[] products;
        private String reg_date;
        private String reg_number;

        public Document(Description description, String doc_id, String doc_status, String doc_type,
                        boolean importRequest, String owner_inn, String participant_inn, String producer_inn,
                        String production_date, String production_type, Product[] products,
                        String reg_date, String reg_number) {
            this.description = description;
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }

        // Getters, Setters
    }

    private static class Description {
        private String participantInn;

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }

    }

    private static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        public Product(String certificate_document, String certificate_document_date,
                       String certificate_document_number, String owner_inn,
                       String producer_inn, String production_date,
                       String tnved_code, String uit_code, String uitu_code) {
            this.certificate_document = certificate_document;
            this.certificate_document_date = certificate_document_date;
            this.certificate_document_number = certificate_document_number;
            this.owner_inn = owner_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.tnved_code = tnved_code;
            this.uit_code = uit_code;
            this.uitu_code = uitu_code;
        }
    }
}
*/

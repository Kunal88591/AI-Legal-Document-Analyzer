package com.legalanalyzer.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.publisher.Mono;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.Loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final WebClient webClient;

    @Autowired
    public DocumentController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://nlp-service:5000").build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadDocument(@RequestPart("file") FilePart file) {
        return DataBufferUtils.join(file.content())
            .flatMap(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);
                try {
                    String text = extractTextFromPdf(bytes);
                    return webClient.post()
                        .uri("/analyze")
                        .bodyValue(new TextRequest(text))
                        .retrieve()
                        .bodyToMono(String.class)
                        .map(ResponseEntity::ok)
                        .onErrorResume(e -> Mono.just(
                            ResponseEntity.internalServerError()
                                .body("Analysis failed: " + e.getMessage())
                        ));
                } catch (Exception e) {
                    return Mono.just(
                        ResponseEntity.internalServerError()
                            .body("PDF processing error: " + e.getMessage())
                    );
                }
            });
    }

    private String extractTextFromPdf(byte[] pdfBytes) {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            return new PDFTextStripper().getText(doc);
        } catch (Exception e) {
            throw new RuntimeException("PDF processing failed", e);
        }
    }

    // Helper class for sending text as JSON to NLP service
    public static class TextRequest {
        private String text;
        public TextRequest() {}
        public TextRequest(String text) { this.text = text; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}

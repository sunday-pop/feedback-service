package org.example.feedbackservice.summary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

@Service
@Log
@RequiredArgsConstructor
public class FileReadServiceImpl implements FileReadService {

    @Override
    public List<String> extractTextsFromFiles(List<String> fileUrls) {
        return fileUrls.stream()
                .map(this::extractTextFromTika)
                .toList();
    }

    private String extractTextFromTika(String fileUrl) {
        AutoDetectParser parser = new AutoDetectParser();
        try (InputStream input = new URL(fileUrl).openStream()) {
            BodyContentHandler handler =new BodyContentHandler(-1); // 무제한
            Metadata metadata = new Metadata();
            parser.parse(input, handler, metadata, new ParseContext());
            return handler.toString();
        } catch (Exception e) {
            return "";
        }
    }

}

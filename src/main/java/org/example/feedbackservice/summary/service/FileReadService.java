package org.example.feedbackservice.summary.service;

import java.util.List;

public interface FileReadService {
    List<String> extractTextsFromFiles(List<String> fileUrls);
}


package org.example.feedbackservice.summary.model.dto;

import org.example.feedbackservice.summary.model.entity.SummaryStatus;

public record SummaryStatusResponse(
        SummaryStatus status
) {
}

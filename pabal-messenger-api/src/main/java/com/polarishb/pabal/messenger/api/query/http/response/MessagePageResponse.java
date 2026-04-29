package com.polarishb.pabal.messenger.api.query.http.response;

import java.util.List;

public record MessagePageResponse(
    List<MessageResponse> messages,
    Long nextCursor,
    boolean hasNext
) {}
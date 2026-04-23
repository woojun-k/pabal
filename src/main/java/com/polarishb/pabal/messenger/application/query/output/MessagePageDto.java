package com.polarishb.pabal.messenger.application.query.output;

import java.util.List;

public record MessagePageDto(
    List<MessageDto> messages,
    Long nextCursor,
    boolean hasNext
) {}
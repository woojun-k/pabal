package com.polarishb.pabal.messenger.domain.policy;

import com.polarishb.pabal.messenger.domain.model.vo.OptionalName;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

public final class RoomNameFormatter {

    private static final int MAX_DISPLAYED_MEMBERS = 3;
    private static final int UUID_DISPLAY_TOKEN_LENGTH = 8;

    private RoomNameFormatter() {
    }

    public static String formatGroupRoomName(UUID requesterId, List<UUID> participantIds) {
        Objects.requireNonNull(requesterId, "requesterId must not be null");
        Objects.requireNonNull(participantIds, "participantIds must not be null");

        // TODO: 유저 이름 조회가 가능해지면 application WebClient 어댑터를 통해 표시 이름을 받아오도록 변경한다.
        List<String> memberNames = Stream.concat(Stream.of(requesterId), participantIds.stream())
                .map(userId -> Objects.requireNonNull(userId, "userId must not be null"))
                .distinct()
                .sorted()
                .map(RoomNameFormatter::formatUuidFallback)
                .toList();

        String roomName = formatDisplayedMembers(memberNames);
        return boundToOptionalNameLimit(roomName);
    }

    private static String formatDisplayedMembers(List<String> memberNames) {
        if (memberNames.size() <= MAX_DISPLAYED_MEMBERS) {
            return String.join(", ", memberNames);
        }

        String displayed = String.join(", ", memberNames.subList(0, MAX_DISPLAYED_MEMBERS));
        int remaining = memberNames.size() - MAX_DISPLAYED_MEMBERS;
        return displayed + " 외 " + remaining + "명";
    }

    private static String formatUuidFallback(UUID userId) {
        return userId.toString().substring(0, UUID_DISPLAY_TOKEN_LENGTH);
    }

    private static String boundToOptionalNameLimit(String roomName) {
        if (roomName.length() <= OptionalName.MAX_LENGTH) {
            return roomName;
        }
        return roomName.substring(0, OptionalName.MAX_LENGTH);
    }
}

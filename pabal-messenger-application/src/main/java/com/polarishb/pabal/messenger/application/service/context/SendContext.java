package com.polarishb.pabal.messenger.application.service.context;

import com.polarishb.pabal.messenger.domain.model.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.ChatRoomMember;

public record SendContext(
    ChatRoom chatRoom,
    ChatRoomMember chatRoomMember
) {}
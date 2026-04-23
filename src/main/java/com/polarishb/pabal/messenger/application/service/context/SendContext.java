package com.polarishb.pabal.messenger.application.service.context;

import com.polarishb.pabal.messenger.domain.model.entity.ChatRoom;
import com.polarishb.pabal.messenger.domain.model.entity.ChatRoomMember;

public record SendContext(
    ChatRoom chatRoom,
    ChatRoomMember chatRoomMember
) {}
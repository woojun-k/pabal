package com.polarishb.pabal.messenger.application.service.context;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.PersistedChatRoom;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.PersistedChatRoomMember;

public record ChatRoomReadAccess(
    PersistedChatRoom room,
    PersistedChatRoomMember member
) {}
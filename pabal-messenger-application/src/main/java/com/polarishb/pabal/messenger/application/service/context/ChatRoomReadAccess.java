package com.polarishb.pabal.messenger.application.service.context;

import com.polarishb.pabal.messenger.contract.persistence.chatroom.ChatRoomState;
import com.polarishb.pabal.messenger.contract.persistence.chatroommember.ChatRoomMemberState;

public record ChatRoomReadAccess(
    ChatRoomState room,
    ChatRoomMemberState member
) {}

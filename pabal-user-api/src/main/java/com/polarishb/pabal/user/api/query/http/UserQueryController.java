package com.polarishb.pabal.user.api.query.http;

import com.polarishb.pabal.user.api.query.http.response.UserResponse;
import com.polarishb.pabal.user.api.query.mapper.UserQueryMapper;
import com.polarishb.pabal.user.application.query.handler.GetUserQueryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserQueryController {

    private final UserQueryMapper userQueryMapper;
    private final GetUserQueryHandler getUserQueryHandler;

    @GetMapping("/users/me")
    public UserResponse getMe(Authentication authentication) {
        return userQueryMapper.toUserResponse(
                getUserQueryHandler.handle(
                        userQueryMapper.toGetMeQuery(authentication)
                )
        );
    }

    @GetMapping("/users/{userId}")
    public UserResponse getUser(
            @PathVariable UUID userId,
            Authentication authentication
    ) {
        return userQueryMapper.toUserResponse(
                getUserQueryHandler.handle(
                        userQueryMapper.toGetUserQuery(userId, authentication)
                )
        );
    }
}

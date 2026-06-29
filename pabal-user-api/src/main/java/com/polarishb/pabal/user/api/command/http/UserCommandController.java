package com.polarishb.pabal.user.api.command.http;

import com.polarishb.pabal.user.api.command.http.request.CreateUserRequest;
import com.polarishb.pabal.user.api.command.http.response.CreateUserResponse;
import com.polarishb.pabal.user.api.command.mapper.UserCommandMapper;
import com.polarishb.pabal.user.application.command.handler.CreateUserCommandHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandMapper userCommandMapper;
    private final CreateUserCommandHandler createUserCommandHandler;

    @PostMapping("/users/me")
    public CreateUserResponse createMe(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication
    ) {
        return userCommandMapper.toCreateUserResponse(
                createUserCommandHandler.handle(
                        userCommandMapper.toCreateUserCommand(request, authentication)
                )
        );
    }
}

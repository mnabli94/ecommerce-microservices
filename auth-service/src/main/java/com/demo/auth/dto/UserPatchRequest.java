package com.demo.auth.dto;

import java.time.Instant;

public record UserPatchRequest(
    Boolean locked,
    String lockReason,
    Boolean resetAttempts,
    Instant lockedUntil
) {

}
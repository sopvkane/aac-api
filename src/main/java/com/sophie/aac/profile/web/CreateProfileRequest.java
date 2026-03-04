package com.sophie.aac.profile.web;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

/** Request body for creating a new communicator profile. */
public record CreateProfileRequest(
    @NotBlank(message = "Display name is required")
    @Length(min = 1, max = 50)
    String displayName,

    @Length(max = 50)
    String wakeName
) {}

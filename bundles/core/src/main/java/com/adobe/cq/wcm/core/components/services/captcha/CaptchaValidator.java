package com.adobe.cq.wcm.core.components.services.captcha;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Captcha validator service interface.
 */
public interface CaptchaValidator {

    /**
     * Get the validator for the given resource.
     *
     * @param resource The resource for which to get the validator.
     * @return The validator, or empty if no applicable CaConfig could be found.
     */
    @NotNull
    Optional<? extends CaptchaTokenValidator> getValidator(@NotNull final Resource resource);
}

/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2020 Adobe
 ~
 ~ Licensed under the Apache License, Version 2.0 (the "License");
 ~ you may not use this file except in compliance with the License.
 ~ You may obtain a copy of the License at
 ~
 ~     http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing, software
 ~ distributed under the License is distributed on an "AS IS" BASIS,
 ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ~ See the License for the specific language governing permissions and
 ~ limitations under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package com.adobe.cq.wcm.core.components.services.captcha;

import org.apache.sling.api.resource.Resource;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Recaptcha validator factory.
 */
public interface CaptchaValidatorFactory {

    /**
     * Get the validator for the given resource.
     *
     * @param resource The resource for which to get the validator.
     * @return The validator, or empty if no applicable CaConfig could be found.
     */
    Optional<? extends CaptchaValidator> getValidator(@NotNull final Resource resource);

}

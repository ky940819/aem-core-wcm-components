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
package com.adobe.cq.wcm.core.components.internal.services.captcha;

import com.adobe.cq.wcm.core.components.services.captcha.CaptchaValidator;
import com.adobe.cq.wcm.core.components.services.captcha.CaptchaValidatorFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import java.util.Optional;

/**
 * Recaptcha validator factory service implementation.
 */
@Component(service = CaptchaValidatorFactory.class, immediate = true)
public final class CaptchaValidatorFactoryImpl implements CaptchaValidatorFactory {

    /**
     * Get the validator for the given resource.
     *
     * @param resource The resource for which to get the validator.
     * @return The validator, or empty if no applicable CaConfig could be found.
     */
    @NotNull
    @Override
    public Optional<CaptchaValidator> getValidator(@NotNull final Resource resource) {
        return Optional.ofNullable(resource.adaptTo(ConfigurationBuilder.class))
            .map(cb -> cb.as(CaptchaValidatorCaConfig.class))
            .map(conf -> new ReCaptchaValidatorImpl(conf.secretKey(), conf.verifyURLOverride()));
    }

}

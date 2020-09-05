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

import com.adobe.cq.wcm.core.components.services.captcha.CaptchaTokenValidator;
import com.adobe.cq.wcm.core.components.services.captcha.CaptchaTokenValidatorFactory;
import com.adobe.cq.wcm.core.components.services.captcha.CaptchaValidator;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recaptcha validator factory service implementation.
 */
@Component(service = CaptchaValidator.class, immediate = true, configurationPolicy = ConfigurationPolicy.IGNORE)
public final class CaptchaValidatorImpl implements CaptchaValidator {

    /**
     * Map of bound validator factories where the key is the captcha type and the value if the validator factory.
     */
    private final Map<String, CaptchaTokenValidatorFactory> validatorFactories = new ConcurrentHashMap<>();

    /**
     * Bind a CaptchaTokenValidatorFactory service to this service.
     *
     * @param validatorFactory The captcha token validator factory to bind.
     */
    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addCaptchaTokenValidatorFactory(final CaptchaTokenValidatorFactory validatorFactory) {
        validatorFactory.getServiceTypes().forEach(type -> this.validatorFactories.put(type, validatorFactory));
    }

    /**
     * Unbind a CaptchaTokenValidatorFactory service to this service.
     *
     * @param validatorFactory The captcha token validator factory to be unbound.
     */
    protected void removeCaptchaTokenValidatorFactory(final CaptchaTokenValidatorFactory validatorFactory) {
        this.validatorFactories.values().removeIf(item -> item.equals(validatorFactory));
    }

    /**
     * Get the validator for the given resource.
     *
     * @param resource The resource for which to get the validator.
     * @return The validator, or empty if no applicable CaConfig could be found.
     */
    @NotNull
    @Override
    public Optional<? extends CaptchaTokenValidator> getValidator(@NotNull final Resource resource) {
        return Optional.ofNullable(resource.adaptTo(ConfigurationBuilder.class))
            .map(cb -> cb.as(CaptchaValidatorCaConfig.class))
            .map(conf -> this.validatorFactories.get(conf.type()))
            .flatMap(validatorFactory -> validatorFactory.getValidator(resource));
    }
}

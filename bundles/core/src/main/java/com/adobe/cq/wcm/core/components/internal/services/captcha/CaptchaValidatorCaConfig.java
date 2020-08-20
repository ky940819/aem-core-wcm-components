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

import org.apache.sling.caconfig.annotation.Configuration;
import org.apache.sling.caconfig.annotation.Property;

/**
 * CAPTCHA validator Context-Aware configuration.
 */
@Configuration(label = "reCaptcha Validator", description = "reCaptcha Validator Configuration")
public @interface CaptchaValidatorCaConfig {

    /**
     * The public API key.
     *
     * @return The public api key
     */
    @Property(label = "Site Key", description = "The public API key.")
    String siteKey() default "";

    /**
     * The secret API key.
     *
     * @return The secret key.
     */
    @Property(label = "Secret Key", description = "The private API key.")
    String secretKey() default "";

    /**
     * Get the CAPTCHA type.
     *
     * @return The CAPTCHA type.
     */
    @Property(label = "Captcha Type", description = "The type of Captcha.",
        property = {
            "widgetType=dropdown",
            "dropdownOptions=["
                + "{'value':'recaptcha-v2-invisible','description':'reCAPTCHA V2 Invisible'},"
                + "{'value':'recaptcha-v2-checkbox','description':'reCAPTCHA V2 Checkbox'},"
                + "]"
        })
    String type();

    /**
     * The URL against which to perform validations.
     *
     * @return The URL against which to perform validations.
     */
    @Property(label = "Verify URL", description = "The verify URL.")
    String verifyURLOverride() default "";
}

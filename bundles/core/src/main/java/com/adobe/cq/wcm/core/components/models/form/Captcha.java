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
package com.adobe.cq.wcm.core.components.models.form;

import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

/**
 * Defines the {@code Captcha} Sling Model used for the {@code /apps/core/wcm/components/form/captcha} component.
 *
 * @since com.adobe.cq.wcm.core.components.models.form 14.3.0
 */
@ConsumerType
public interface Captcha extends Field {

    /**
     * Name of the resource property that indicates if a captcha is required.
     *
     * @since com.adobe.cq.wcm.core.components.models.form 14.3.0
     */
    String PN_CAPTCHA_REQUIRED = "captchaRequired";

    /**
     * Get the CAPTCHA site key to use for this form.
     *
     * @return The CAPTCHA site key for this form, or <code>null</code> if captcha is not enabled.
     * @since com.adobe.cq.wcm.core.components.models.form 14.3.0
     */
    @Nullable
    default String getSiteKey() {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the CAPTCHA type.
     *
     * @return The type of CAPTCHA.
     */
    default String getType() {
        throw new UnsupportedOperationException();
    }

}

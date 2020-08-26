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
package com.adobe.cq.wcm.core.components.internal.models.v1.form;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.internal.form.FormConstants;
import com.adobe.cq.wcm.core.components.internal.services.captcha.CaptchaValidatorCaConfig;
import com.adobe.cq.wcm.core.components.models.form.Captcha;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * Captcha component model implementation.
 */
@Model(adaptables = SlingHttpServletRequest.class,
       adapters = {Captcha.class, ComponentExporter.class},
       resourceType = {FormConstants.RT_CORE_FORM_CAPTCHA_V1})
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class CaptchaImpl extends AbstractFieldImpl implements Captcha {

    /**
     * Badge position property name.
     */
    private static final String PN_POSITION = "position";

    /**
     * Component ID prefix.
     */
    private static final String ID_PREFIX = "form-captcha";

    /**
     * Default position.
     */
    @NotNull
    private static final String POSITION_DEFAULT = "inline";

    /**
     * The current request.
     */
    @Self
    private SlingHttpServletRequest request;

    /**
     * Validator Context-Aware configuration.
     */
    @Nullable
    private CaptchaValidatorCaConfig caConfig;

    /**
     * The site key for reCAPTCHA.
     */
    private String siteKey;

    /**
     * Captcha type.
     */
    private String type;

    /**
     * Badge position.
     */
    private String position;

    /**
     * Initialize the module.
     */
    @PostConstruct
    private void initModel() {
        this.caConfig = Optional.ofNullable(this.request.getResource().adaptTo(ConfigurationBuilder.class))
            .map(cb -> cb.as(CaptchaValidatorCaConfig.class))
            .orElse(null);
    }

    @Nullable
    @Override
    public String getSiteKey() {
        if (this.siteKey == null) {
            this.siteKey = Optional.ofNullable(this.caConfig)
                .map(CaptchaValidatorCaConfig::siteKey)
                .filter(StringUtils::isNotEmpty)
                .orElse(null);
        }
        return this.siteKey;
    }

    @Nullable
    @Override
    public String getType() {
        if (this.type == null) {
            this.type = Optional.ofNullable(this.caConfig)
                .map(CaptchaValidatorCaConfig::type)
                .filter(StringUtils::isNotEmpty)
                .orElse(null);
        }
        return this.type;
    }

    @NotNull
    @Override
    public String getPosition() {
        if (this.position == null) {
            this.position = this.request.getResource().getValueMap().get(PN_POSITION, POSITION_DEFAULT);
        }
        return this.position;
    }

    @Override
    protected String getIDPrefix() {
        return ID_PREFIX;
    }

    @Override
    protected String getDefaultName() {
        return "";
    }

    @Override
    protected String getDefaultValue() {
        return null;
    }

    @Override
    protected String getDefaultTitle() {
        return "Captcha";
    }

    @NotNull
    @Override
    public String getExportedType() {
        return request.getResource().getResourceType();
    }
}

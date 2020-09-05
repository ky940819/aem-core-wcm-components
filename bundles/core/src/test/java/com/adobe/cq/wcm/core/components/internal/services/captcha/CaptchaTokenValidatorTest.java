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


import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;
import com.adobe.cq.wcm.core.components.services.captcha.CaptchaValidator;
import com.adobe.cq.wcm.core.components.services.captcha.CaptchaValidatorFactory;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.caconfig.ConfigurationBuilder;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Recaptcha service unit tests.
 */
@ExtendWith({AemContextExtension.class})
public class CaptchaValidatorTest extends RecaptchaLocalServerTestBase {

    private static final String TEST_BASE = "/form/container";
    private static final String CONTAINING_PAGE = "/content/coretest/demo-page";
    private static final String FORM4_PATH = CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container-v2-captcha-required";

    /**
     * The AEM Context.
     */
    public final AemContext context = CoreComponentTestContext.newAemContext();

    /**
     * The recaptcha service.
     */
    private CaptchaValidatorFactory validatorFactory;

    /**
     * The public key.
     */
    private static final String SITE_KEY = "site-key";

    /**
     * Valid secret key.
     */
    private static final String SECRET_KEY = "validSecret";

    /**
     * Resource.
     */
    private Resource currentResource;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.context.load().json(TEST_BASE + CoreComponentTestContext.TEST_CONTENT_JSON, CONTAINING_PAGE);
        this.currentResource = Objects.requireNonNull(context.resourceResolver().getResource(FORM4_PATH));
        this.validatorFactory = this.context.registerInjectActivateService(new CaptchaValidatorFactoryImpl());
    }

    @AfterEach
    void tearDown() throws Exception {
        super.shutDown();
    }

    @Test
    @DisplayName("getValidator() - No Recaptcha Context-Aware Configuration present")
    void getValidator_noCaConfig() {
        assertEquals(Optional.empty(), this.validatorFactory.getValidator(currentResource));
        assertFalse(validatorFactory.getValidator(currentResource).isPresent());
    }

    @Test
    @DisplayName("getValidator() - Recaptcha Context-Aware Configuration present")
    void getValidator() {
        enableCaptchaCAConfig(context, SITE_KEY, SECRET_KEY, "type", httpHost.toURI() + "/recaptcha/api/siteverify");
        assertNotEquals(Optional.empty(), validatorFactory.getValidator(currentResource));
        assertTrue(validatorFactory.getValidator(currentResource).isPresent());
    }

    @Test
    @DisplayName("validate() - valid user response")
    void validate_valid() {
        enableCaptchaCAConfig(context, SITE_KEY, SECRET_KEY, "type", httpHost.toURI() + "/recaptcha/api/siteverify");
        CaptchaValidator validator = validatorFactory.getValidator(currentResource).orElseGet(Assertions::fail);
        assertTrue(validator.validate("validResponse"));
    }

    @Test
    @DisplayName("validate() - invalid user response")
    void validate_invalid() {
        enableCaptchaCAConfig(context, SITE_KEY, SECRET_KEY, "type", httpHost.toURI() + "/recaptcha/api/siteverify");
        CaptchaValidator validator = validatorFactory.getValidator(currentResource).orElseGet(Assertions::fail);
        assertFalse(validator.validate("invalidResponse"));
        assertFalse(validator.validate(""));
        assertFalse(validator.validate((String) null));
    }

    @Test
    @DisplayName("validate() - no secret key")
    void validate_empty_key() {
        enableCaptchaCAConfig(context, SITE_KEY, "", "type", httpHost.toURI() + "/recaptcha/api/siteverify");
        CaptchaValidator validator = validatorFactory.getValidator(currentResource).orElseGet(Assertions::fail);
        assertFalse(validator.validate("validResponse"));
    }

    @Test
    @DisplayName("validate() - bad response from validation endpoint")
    void validate_invalid_verify_url() {
        enableCaptchaCAConfig(context, SITE_KEY, SECRET_KEY, "type",httpHost.toURI() + "/garbage");
        CaptchaValidator validator = validatorFactory.getValidator(currentResource).orElseGet(Assertions::fail);
        assertFalse(validator.validate("validResponse"));
    }

    @Test
    @DisplayName("validate() - IOException sending request to validation endpoint")
    void validate_io_exception() {
        enableCaptchaCAConfig(context, SITE_KEY, SECRET_KEY, "type","nope");
        CaptchaValidator validator = validatorFactory.getValidator(currentResource).orElseGet(Assertions::fail);
        assertFalse(validator.validate("validResponse"));
    }

    /**
     * Helper function to enable recaptcha validator Context-Aware Configuration.
     *
     * @param context The AEM context.
     * @param siteKey The site key.
     * @param secretKey The secret key.
     * @param type The key type.
     * @param verifyUrl The verify URL override.
     */
    public static void enableCaptchaCAConfig(@NotNull final AemContext context,
                                             final String siteKey,
                                             final String secretKey,
                                             final String type,
                                             final String verifyUrl) {
        ConfigurationBuilder builder = mock(ConfigurationBuilder.class);
        CaptchaValidatorCaConfig caConfig = mock(CaptchaValidatorCaConfig.class);
        if (siteKey != null) {
            when(caConfig.siteKey()).thenReturn(siteKey);
        }
        if (secretKey != null) {
            when(caConfig.secretKey()).thenReturn(secretKey);
        }
        if (verifyUrl != null) {
            when(caConfig.verifyURLOverride()).thenReturn(verifyUrl);
        }
        if (type != null) {
            when(caConfig.type()).thenReturn(type);
        }
        when(builder.as(CaptchaValidatorCaConfig.class)).thenReturn(caConfig);
        context.registerAdapter(Resource.class, ConfigurationBuilder.class, builder);
    }

}

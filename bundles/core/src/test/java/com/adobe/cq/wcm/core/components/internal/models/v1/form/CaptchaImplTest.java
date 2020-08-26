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

import com.adobe.cq.wcm.core.components.Utils;
import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;
import com.adobe.cq.wcm.core.components.internal.services.captcha.CaptchaValidatorTest;
import com.adobe.cq.wcm.core.components.models.form.Captcha;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.Resource;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Captcha model implementation tests.
 */
@ExtendWith(AemContextExtension.class)
public class CaptchaImplTest {

    private static final String TEST_BASE = "/form/captcha";
    private static final String CONTENT_ROOT = "/content/captcha";
    private static final String CAPTCHA_1 = CONTENT_ROOT + "/jcr:content/root/container/captcha";

    private static final String SITE_KEY = "SITE_KEY";
    private static final String SECRET_KEY = "SECRET_KEY";
    private static final String TYPE = "TYPE";

    public final AemContext context = CoreComponentTestContext.newAemContext();

    @BeforeEach
    public void setUp() {
        context.load().json(TEST_BASE + CoreComponentTestContext.TEST_CONTENT_JSON, CONTENT_ROOT);
    }

    @Test
    public void testGetSiteKey() {
        CaptchaValidatorTest.enableCaptchaCAConfig(this.context, SITE_KEY, SECRET_KEY, TYPE, "");
        Captcha captcha = getCaptchaUnderTest(CAPTCHA_1);
        assertEquals(SITE_KEY, captcha.getSiteKey());
        assertSame(captcha.getSiteKey(), captcha.getSiteKey());
    }

    @Test
    public void testGetSiteKey_noCAConfig() {
        Captcha captcha = getCaptchaUnderTest(CAPTCHA_1);
        assertNull(captcha.getSiteKey());
    }

    @Test
    public void testGetType() {
        CaptchaValidatorTest.enableCaptchaCAConfig(this.context, SITE_KEY, SECRET_KEY, TYPE, "");
        Captcha captcha = getCaptchaUnderTest(CAPTCHA_1);
        assertEquals(TYPE, captcha.getType());
        assertSame(captcha.getType(), captcha.getType());
    }

    @Test
    public void testGetType_noCAConfig() {
        Captcha captcha = getCaptchaUnderTest(CAPTCHA_1);
        assertNull(captcha.getType());
    }

    @Test
    public void testGetPosition() {
        Captcha captcha = getCaptchaUnderTest(CAPTCHA_1);
        assertEquals("bottom-left", captcha.getPosition());
        assertSame(captcha.getPosition(), captcha.getPosition());
    }

    @Test
    public void testComponentExporter() {
        CaptchaValidatorTest.enableCaptchaCAConfig(this.context, SITE_KEY, SECRET_KEY, TYPE, "");
        Captcha captcha = getCaptchaUnderTest(CAPTCHA_1);
        Utils.testJSONExport(captcha, Utils.getTestExporterJSONPath(TEST_BASE, "captcha"));
    }

    private Captcha getCaptchaUnderTest(@NotNull final String resourcePath) {
        System.out.println(resourcePath);
        Resource resource = Objects.requireNonNull(context.resourceResolver().getResource(resourcePath));
        this.context.currentResource(resource);
        return this.context.request().adaptTo(Captcha.class);
    }

}

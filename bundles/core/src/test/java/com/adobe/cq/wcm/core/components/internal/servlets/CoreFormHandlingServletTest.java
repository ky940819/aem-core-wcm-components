/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2017 Adobe
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
package com.adobe.cq.wcm.core.components.internal.servlets;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.adobe.cq.wcm.core.components.internal.form.FormStructureHelperImpl;
import com.adobe.cq.wcm.core.components.internal.services.captcha.CaptchaValidatorImpl;
import com.day.cq.wcm.foundation.forms.FormStructureHelper;
import org.apache.sling.scripting.api.resource.ScriptingResourceResolverProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;
import com.adobe.cq.wcm.core.components.internal.form.FormConstants;
import com.adobe.cq.wcm.core.components.models.form.Captcha;
import com.adobe.cq.wcm.core.components.services.captcha.CaptchaTokenValidator;
import com.adobe.cq.wcm.core.components.services.captcha.CaptchaTokenValidatorFactory;
import com.adobe.cq.wcm.core.components.testing.Utils;
import com.day.cq.wcm.foundation.forms.FormsHandlingServletHelper;
import com.day.cq.wcm.foundation.forms.FormStructureHelperFactory;
import com.day.cq.wcm.foundation.security.SaferSlingPostValidator;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static com.adobe.cq.wcm.core.components.internal.services.captcha.CaptchaTokenValidatorTest.enableCaptchaCAConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

@ExtendWith({AemContextExtension.class})
public class CoreFormHandlingServletTest {

    private static final String TEST_BASE = "/form/container";
    private static final String CONTAINING_PAGE = "/content/coretest/demo-page";
    private static final String FORM3_PATH = CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container-v2";
    private static final String FORM4_PATH = CONTAINING_PAGE + "/jcr:content/root/responsivegrid/container-v2-captcha-required";

    private static final String RECAPTCHA_TOKEN_PARAMETER = "TOKEN_PARAMETER";

    private static final String[] NAME_WHITELIST = {"param-text", "param-button"};
    private static final Boolean ALLOW_EXPRESSIONS = Boolean.FALSE;
    private static final String SELECTOR = "form";
    private static final String EXTENSION = "html";

    private FormsHandlingServletHelper formsHandlingServletHelper;

    private CoreFormHandlingServlet servlet;

    public final AemContext context = CoreComponentTestContext.newAemContext();

    @BeforeEach
    public void setUp() {
        context.load().json(TEST_BASE + CoreComponentTestContext.TEST_CONTENT_JSON, CONTAINING_PAGE);

        context.registerInjectActivateService(new CaptchaValidatorImpl());
        context.registerService(ScriptingResourceResolverProvider.class, context::resourceResolver);
        FormStructureHelper helper = context.registerInjectActivateService(new FormStructureHelperImpl());
        context.registerService(FormStructureHelperFactory.class, resource -> helper);
        context.registerService(SaferSlingPostValidator.class, mock(SaferSlingPostValidator.class));

        this.servlet = this.context.registerInjectActivateService(new CoreFormHandlingServlet(), new HashMap<String, Object>() {{
            put("name_whitelist", NAME_WHITELIST);
            put("allow_expressions", ALLOW_EXPRESSIONS);
        }});

        this.formsHandlingServletHelper = mock(FormsHandlingServletHelper.class);
        Utils.setInternalState(servlet, "formsHandlingServletHelper", formsHandlingServletHelper);
    }

    @Test
    @DisplayName("Captcha not required")
    public void testDoPost() throws Exception {
        context.currentResource(this.context.resourceResolver().getResource(FORM3_PATH));
        servlet.doPost(context.request(), context.response());

        assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
        verify(formsHandlingServletHelper).doPost(context.request(), context.response());
    }

    @Nested
    class CaptchaRequired {

        CaptchaTokenValidator validator;

        @BeforeEach
        public void setUp() {
            this.validator = spy(new MockTokenValidator());
        }

        @Test
        @DisplayName("Captcha required as per component properties, invalid token provided")
        public void testDoPost_captchaRequired_invalid() throws Exception {
            enableCaptcha();
            context.currentResource(context.resourceResolver().getResource(FORM4_PATH));
            servlet.doPost(context.request(), context.response());

            assertEquals(HttpServletResponse.SC_FORBIDDEN, context.response().getStatus());
            verify(formsHandlingServletHelper, never()).doPost(context.request(), context.response());
            verify(validator).validate(any(HttpServletRequest.class));
        }

        @Test
        @DisplayName("Captcha required as per component properties, valid token provided")
        public void testDoPost_captchaRequired_valid() throws Exception {
            enableCaptcha();
            context.request().setParameterMap(Collections.singletonMap(RECAPTCHA_TOKEN_PARAMETER, "valid_response"));

            context.currentResource(context.resourceResolver().getResource(FORM4_PATH));
            servlet.doPost(context.request(), context.response());

            assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
            verify(formsHandlingServletHelper).doPost(context.request(), context.response());
            verify(validator).validate(eq("valid_response"));
        }

        @Test
        @DisplayName("Captcha required as per policy, no captcha components")
        public void testDoPost_captchaRequired_by_policy_no_captcha_components() throws Exception {
            context.contentPolicyMapping(FormConstants.RT_CORE_FORM_CONTAINER_V2, new HashMap<String, Object>() {{
                put(Captcha.PN_CAPTCHA_REQUIRED, Boolean.TRUE);
            }});

            context.request().setParameterMap(Collections.singletonMap(RECAPTCHA_TOKEN_PARAMETER, "valid_response"));

            context.currentResource(context.resourceResolver().getResource(FORM3_PATH));
            servlet.doPost(context.request(), context.response());

            assertEquals(HttpServletResponse.SC_FORBIDDEN, context.response().getStatus());
            verify(formsHandlingServletHelper, never()).doPost(context.request(), context.response());
            verifyZeroInteractions(validator);
        }

        @Test
        @DisplayName("Captcha required as per policy, valid token provided")
        public void testDoPost_captchaRequired_by_policy_valid() throws Exception {
            enableCaptcha();
            context.contentPolicyMapping(FormConstants.RT_CORE_FORM_CONTAINER_V2, new HashMap<String, Object>() {{
                put(Captcha.PN_CAPTCHA_REQUIRED, Boolean.TRUE);
            }});

            context.request().setParameterMap(Collections.singletonMap(RECAPTCHA_TOKEN_PARAMETER, "valid_response"));

            context.currentResource(context.resourceResolver().getResource(FORM4_PATH));
            servlet.doPost(context.request(), context.response());

            assertEquals(HttpServletResponse.SC_OK, context.response().getStatus());
            verify(formsHandlingServletHelper).doPost(context.request(), context.response());
            verify(validator).validate(eq("valid_response"));
        }

        private void enableCaptcha() {
            CaptchaTokenValidatorFactory captchaTokenValidatorFactory = mock(CaptchaTokenValidatorFactory.class);
            doReturn(Collections.singletonList("CAPTCHA_TYPE")).when(captchaTokenValidatorFactory).getServiceTypes();
            context.registerService(CaptchaTokenValidatorFactory.class, captchaTokenValidatorFactory);
            enableCaptchaCAConfig(context, null, null, "CAPTCHA_TYPE", null);
            doReturn(Optional.of(this.validator)).when(captchaTokenValidatorFactory).getValidator(any());
        }
    }

    @Test
    public void testDoFilter() throws Exception {
        servlet.init(mock(FilterConfig.class));

        FilterChain filterChain = mock(FilterChain.class);
        servlet.doFilter(context.request(), context.response(), filterChain);
        verify(formsHandlingServletHelper).handleFilter(context.request(), context.response(), filterChain, EXTENSION, SELECTOR);
    }

    static final class MockTokenValidator implements CaptchaTokenValidator {

        @Override
        public boolean validate(@Nullable String userResponse) {
            return "valid_response".equals(userResponse);
        }

        @Override
        public boolean validate(@NotNull HttpServletRequest request) {
            return this.validate(request.getParameter(RECAPTCHA_TOKEN_PARAMETER));
        }
    }
}

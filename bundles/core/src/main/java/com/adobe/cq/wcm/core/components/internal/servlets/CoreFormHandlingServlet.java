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

import java.io.IOException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import com.adobe.cq.wcm.core.components.models.form.Captcha;
import com.adobe.cq.wcm.core.components.services.captcha.CaptchaValidatorFactory;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.engine.EngineConstants;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.cq.wcm.core.components.internal.form.FormConstants;
import com.day.cq.wcm.foundation.forms.FormStructureHelperFactory;
import com.day.cq.wcm.foundation.forms.FormsHandlingServletHelper;
import com.day.cq.wcm.foundation.security.SaferSlingPostValidator;

/**
 * This form handling servlet accepts POSTs to a core form container
 * but only if the selector "form" and the extension "html" is used.
 */
@SuppressWarnings("serial")
@Component(
        service = {Servlet.class, Filter.class},
        configurationPid = "com.adobe.cq.wcm.core.components.commons.forms.impl.CoreFormsHandlingServlet",
        property = {
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + FormConstants.RT_CORE_FORM_CONTAINER_V1,
            ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES + "=" + FormConstants.RT_CORE_FORM_CONTAINER_V2,
            ServletResolverConstants.SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
            ServletResolverConstants.SLING_SERVLET_SELECTORS + "=" + CoreFormHandlingServlet.SELECTOR,
            ServletResolverConstants.SLING_SERVLET_EXTENSIONS + "=" + CoreFormHandlingServlet.EXTENSION,
            EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
            Constants.SERVICE_RANKING + ":Integer=610",
        }
)
@Designate(ocd = CoreFormHandlingServlet.Configuration.class)
public final class CoreFormHandlingServlet extends SlingAllMethodsServlet implements Filter {

    /**
     * Core Form Handling Servlet Configuration.
     */
    @ObjectClassDefinition(name = "Core Form Handling Servlet",
        description = "Accepts posting to a form container component and performs validations")
    @interface Configuration {

        /**
         * Get the parameter name whitelist.
         *
         * @return The list of whitelisted parameter names.
         */
        @AttributeDefinition(
            name = "Parameter Name Whitelist",
            description = "List of name expressions that will pass request validation. A validation error will occur " +
                "if any posted parameters are not in the whitelist and not defined on the form.")
        String[] name_whitelist() default {};

        /**
         * Check if expressions should be evaluated in form submissions.
         *
         * @return True if expressions should be evaluated, false if not.
         */
        @AttributeDefinition(name = "Allow Expressions", description = "Evaluate expressions on form submissions.")
        boolean allow_expressions() default true;
    }

    /**
     * Form selector that must be present for the servlet to handle the request.
     */
    static final String SELECTOR = "form";

    /**
     * Extension that must be present for the servlet to handle the request.
     */
    static final String EXTENSION = "html";

    /**
     * The name of the request property containing the users reCaptcha challenge response token.
     */
    static final String RECAPTCHA_TOKEN_PARAMETER = "g-recaptcha-response";

    /**
     * The helper for processing forms.
     */
    private transient FormsHandlingServletHelper formsHandlingServletHelper;

    /**
     * Request validator (Service to check Sling Post requests for unsafe constructs).
     */
    @Reference
    private transient SaferSlingPostValidator validator;

    /**
     * Form structure helper factory service.
     */
    @Reference
    private transient FormStructureHelperFactory formStructureHelperFactory;

    /**
     * Recaptcha validator factory service.
     */
    @Reference
    private transient CaptchaValidatorFactory recaptchaValidatorFactory;

    /**
     * Activate the service.
     *
     * @param configuration The service configuration.
     */
    @Activate
    protected void activate(@NotNull final Configuration configuration) {
        this.formsHandlingServletHelper = new FormsHandlingServletHelper(
            configuration.name_whitelist(),
            validator,
            FormConstants.RT_ALL_CORE_FORM_CONTAINER,
            configuration.allow_expressions(),
            formStructureHelperFactory);
    }

    /**
     * @see org.apache.sling.api.servlets.SlingAllMethodsServlet#doPost(org.apache.sling.api.SlingHttpServletRequest, org.apache.sling.api.SlingHttpServletResponse)
     */
    @Override
    protected void doPost(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response)
            throws ServletException, IOException {
        if (!this.captchaRequired(request) || this.validateCaptcha(request)) {
            formsHandlingServletHelper.doPost(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    /**
     * Check if captcha is required for the current request.
     * A captcha is required if the form container policy requires it,
     * or if the form container contains a button with captcha enabled.
     *
     * @param request The current request.
     * @return True if captcha validation is required, false if not.
     */
    private boolean captchaRequired(@NotNull final SlingHttpServletRequest request) {
        // supplier to check if required by policy
        Supplier<Boolean> captchaRequiredByPolicy = () ->
            Optional.ofNullable(request.getResourceResolver().adaptTo(ContentPolicyManager.class))
                .map(policyManager -> policyManager.getPolicy(request.getResource(), request))
                .map(ContentPolicy::getProperties)
                .map(props -> props.get(Captcha.PN_CAPTCHA_REQUIRED, Boolean.FALSE))
                .orElse(Boolean.FALSE);

        Supplier<Boolean> captchaRequiredByContent = () ->
            StreamSupport.stream(formStructureHelperFactory.getFormStructureHelper(request.getResource())
                .getFormElements(request.getResource()).spliterator(), false)
                .anyMatch(resource -> resource.isResourceType(FormConstants.RT_CORE_FORM_CAPTCHA_V1));

        return captchaRequiredByPolicy.get() || captchaRequiredByContent.get();
    }

    /**
     * Validates the captcha token included in the current request.
     *
     * @param request The current request.
     * @return True if the captcha validates, false if it does not.
     */
    private boolean validateCaptcha(@NotNull final SlingHttpServletRequest request) {
        return Optional.of(this.recaptchaValidatorFactory)
            .flatMap(factory -> factory.getValidator(request.getResource()))
            .map(val -> val.validate(request.getParameter(RECAPTCHA_TOKEN_PARAMETER)))
            .orElse(Boolean.FALSE);
    }

    /**
     * @see Filter#doFilter(ServletRequest, ServletResponse, FilterChain)
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        formsHandlingServletHelper.handleFilter(request, response, chain, EXTENSION, SELECTOR);
    }

    /**
     * @see Filter#init(FilterConfig)
     */
    @Override
    public void init(final FilterConfig config) {
        // nothing to do!
    }
}

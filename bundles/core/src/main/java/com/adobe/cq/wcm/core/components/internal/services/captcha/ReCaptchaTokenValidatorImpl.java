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
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Recaptcha validator implementation.
 */
public final class ReCaptchaTokenValidatorImpl implements CaptchaTokenValidator {

    /**
     * Default logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ReCaptchaTokenValidatorImpl.class);

    /**
     * Google verify URL.
     */
    private static final String DEFAULT_VERIFY_URL = "https://www.google.com/recaptcha/api/siteverify";

    /**
     * The name of the request property containing the users reCaptcha challenge response token.
     */
    private static final String RECAPTCHA_TOKEN_PARAMETER = "g-recaptcha-response";

    /**
     * Duration in MS to wait while attempting to establish a connection.
     */
    private static final int CONNECTION_TIMEOUT = 1000;

    /**
     * Amount of time to wait after establishing a connection to receive data.
     */
    private static final int SOCKET_TIMEOUT = 1000;

    /**
     * Time to wait for a connection from the manager.
     */
    private static final int CONNECTION_MANAGER_TIMEOUT = 100;

    /**
     * Site key.
     */
    @NotNull
    private final String sKey;

    /**
     * The verify URL.
     */
    @NotNull
    private final String verifyURL;

    /**
     * Construct a verifier with the secret key and verify URL.
     *
     * @param secretKey The secret key.
     * @param verifyAddr The verify URL.
     */
    ReCaptchaTokenValidatorImpl(@NotNull final String secretKey, @Nullable final String verifyAddr) {
        this.sKey = secretKey;
        this.verifyURL = Optional.ofNullable(verifyAddr).filter(StringUtils::isNotEmpty).orElse(DEFAULT_VERIFY_URL);
    }

    @Override
    public boolean validate(@NotNull final HttpServletRequest request) {
        return this.validate(request.getParameter(RECAPTCHA_TOKEN_PARAMETER));
    }

    @Override
    public boolean validate(@Nullable final String userResponse) {

        // try to execute the post
        LOG.debug("Verifying users response");

        // make sure the secret key isn't empty
        if (StringUtils.isEmpty(this.sKey)) {
            LOG.error("Cannot verify captcha: secret key is empty.");
            return false;
        }

        // make sure user response isn't null or empty
        if (StringUtils.isBlank(userResponse)) {
            LOG.debug("Cannot verify captcha: user response is blank.");
            return false;
        }

        // make sure it isn't null or empty
        try (InputStream responseInputStream = ReCaptchaTokenValidatorImpl.sendVerificationRequest(this.sKey, userResponse, this.verifyURL)){
            // get the response object
            JsonObject responseObject = new JsonParser().parse(
                new InputStreamReader(responseInputStream, StandardCharsets.UTF_8)
            ).getAsJsonObject();

            return Optional.ofNullable(responseObject.get("success"))
                .map(JsonElement::getAsBoolean)
                .orElse(false);
        } catch (Exception e) {
            // any error sending validation request or interpreting result
            LOG.error("Error validating reCAPTCHA.", e);
        }
        return false;
    }

    /**
     * Send a verification request and get the input stream of the response.
     *
     * @param secretKey The secret key.
     * @param response The users response to the challenge.
     * @param verifyAddr The verify URL.
     * @return The input stream of request response.
     * @throws IOException If the request could not be made.
     */
    private static InputStream sendVerificationRequest(@NotNull final String secretKey, @NotNull final String response, @NotNull final String verifyAddr) throws IOException {
        return HttpClientBuilder.create()
            .setDefaultRequestConfig(RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_MANAGER_TIMEOUT)
                .setConnectTimeout(CONNECTION_TIMEOUT)
                .setSocketTimeout(SOCKET_TIMEOUT)
                .build())
            .setRetryHandler(new DefaultHttpRequestRetryHandler(0, false))
            .build()
            .execute(RequestBuilder.post()
                .setUri(verifyAddr)
                .setEntity(MultipartEntityBuilder.create()
                    .addTextBody("secret", secretKey)
                    .addTextBody("response", response)
                    .build())
                .build())
            .getEntity()
            .getContent();
    }

}

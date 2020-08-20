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

import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.RequestContext;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.localserver.LocalServerTestBase;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Starts a recaptcha verifier local server.
 * Unit tests that want to test against a local recaptcha verifier should extend this class.
 */
public class RecaptchaLocalServerTestBase extends LocalServerTestBase {

    /**
     * The HTTP host.
     */
    protected HttpHost httpHost;

    @BeforeEach
    public void setUp() throws Exception {
        startServer();
        // register a mock recaptcha verifier.
        this.localServer.register("/recaptcha/api/siteverify", (HttpRequest httpRequest, HttpResponse httpResponse, HttpContext httpContext) -> {
            try {
                // create a new test response object, this holds the response and is converted to JSON as the reply
               MockRecaptchaResponse recaptchaResponse = new MockRecaptchaResponse();
                // get the form parameters
                Map<String, String> formParameters = this.getFormParameters(httpRequest);
                String captchaResponse = formParameters.get("response");
                String captchaSecret = formParameters.get("secret");

                // if either is not set then this request fails
                if (captchaResponse == null || captchaSecret == null) {
                    // add the correct error code(s)
                    if (captchaResponse == null) {
                        recaptchaResponse.errorCodes.add("missing-input-response");
                    }
                    if (captchaSecret == null) {
                        recaptchaResponse.errorCodes.add("missing-input-secret");
                    }
                    // set success to false
                    recaptchaResponse.success = false;
                } else {
                    // both parameters are set, we will check if they are the correct responses
                    if (!captchaResponse.equals("validResponse") || !captchaSecret.equals("validSecret")) {
                        // one of them (or both) isn't correct, add the correct error codes(s)
                        if (!captchaResponse.equals("validResponse")) {
                            recaptchaResponse.errorCodes.add("invalid-input-response");
                        }
                        if (!captchaSecret.equals("validSecret")) {
                            recaptchaResponse.errorCodes.add("invalid-input-secret");
                        }
                        // set success to false
                        recaptchaResponse.success = false;
                    } else {
                        // both are set and valid, success to true, no error codes are set
                        recaptchaResponse.success = true;
                    }
                }

                // set the status code to 200
                httpResponse.setStatusCode(200);
                // set the response entity to the json response object
                httpResponse.setEntity(new StringEntity(new Gson().toJson(recaptchaResponse)));
            } catch (FileUploadException e) {
                // throw a runtime exception if we fail
                throw new RuntimeException(e);
            }
        });

        this.httpHost = getServerHttp();
    }

    @AfterEach
    public void shutDown() throws Exception {
        this.shutDownServer();
    }

    /**
     * Get the text form parameters from a multipart form HttpRequest.
     *
     * @param httpRequest The request to get the form parameters from.
     * @return Map of the form parameters with the parameter name as the key.
     * @throws IOException
     * @throws FileUploadException
     */
    private Map<String, String> getFormParameters(@Nonnull final HttpRequest httpRequest) throws IOException, FileUploadException {
        if (httpRequest instanceof HttpEntityEnclosingRequest) {
            HttpEntity entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
            String entityString = EntityUtils.toString(entity);
            RequestContext requestContext = new CaptchaRequestContext(UTF_8, entity.getContentType().getValue(), entityString.getBytes());

            return new ServletFileUpload(new DiskFileItemFactory())
                    .parseRequest(requestContext)
                    .stream()
                    .filter(FileItem::isFormField)
                    .collect(Collectors.toMap(FileItem::getFieldName, FileItem::getString));
        }

        return Collections.emptyMap();
    }

    private static final class CaptchaRequestContext implements RequestContext {
        private final Charset charset;
        private final String contentType;
        private final byte[] content;

        CaptchaRequestContext(final Charset charset, final String contentType, final byte[] content) {
            this.charset = charset;
            this.contentType = contentType;
            this.content = content;
        }

        public String getCharacterEncoding() {
            return charset.displayName();
        }

        public String getContentType() {
            return contentType;
        }

        @Deprecated
        public int getContentLength() {
            return content.length;
        }

        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }
    }
}

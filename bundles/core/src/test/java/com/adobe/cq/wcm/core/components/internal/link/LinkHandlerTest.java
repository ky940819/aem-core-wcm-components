/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2021 Adobe
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
package com.adobe.cq.wcm.core.components.internal.link;

import java.util.Optional;

import com.adobe.cq.wcm.core.components.commons.link.LinkHandler;
import org.apache.sling.api.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;
import com.adobe.cq.wcm.core.components.testing.Utils;
import com.day.cq.wcm.api.Page;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;

import static com.adobe.cq.wcm.core.components.commons.link.Link.*;
import static com.adobe.cq.wcm.core.components.internal.link.LinkTestUtils.assertValidLink;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(AemContextExtension.class)
class LinkHandlerTest {

    private final AemContext context = CoreComponentTestContext.newAemContext();

    private Page page;
    private LinkHandler underTest;

    @BeforeEach
    void setUp() {
        page = context.create().page("/content/links/site1/en/");
        context.currentPage(page);
        underTest = context.request().adaptTo(LinkHandler.class);
    }

    @Test
    void testResourceEmpty() {
        Resource linkResource = context.create().resource(page, "link");
        Optional<Link<Page>> link = underTest.getLink(linkResource);
        assertEquals(Optional.empty(), link);
    }

    @Test
    void testResourceExternalLink() {
        Resource linkResource = context.create().resource(page, "link",
                PN_LINK_URL, "http://myhost");
        Optional<Link<Page>> link = underTest.getLink(linkResource);

        assertValidLink(link.get(), "http://myhost");
        assertNull(link.map(Link::getReference).orElse(null));
        assertEquals("http://myhost", link.get().getMappedURL());
    }

    @ParameterizedTest
    @ValueSource(strings = {"_blank", "_parent", "_top"})
    void testResourceExternalLinkWithAllowedTargetsAndAllAttributes(String target) {
        Resource linkResource = context.create().resource(page, "link",
                PN_LINK_URL, "http://myhost",
                PN_LINK_TARGET, target,
                PN_LINK_ACCESSIBILITY_LABEL, "My Host Label",
                PN_LINK_TITLE_ATTRIBUTE, "My Host Title");
        Optional<Link<Page>> link = underTest.getLink(linkResource);

        assertValidLink(link.get(), "http://myhost", "My Host Label", "My Host Title", target);
        assertNull(link.map(Link::getReference).orElse(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"_self","_invalid"})
    void testResourceExternalLinkWithInvalidTargets(String target) {
        Resource linkResource = context.create().resource(page, "link",
                PN_LINK_URL, "http://myhost",
                PN_LINK_TARGET, target);
        Optional<Link<Page>> link = underTest.getLink(linkResource);

        // invalid target or _self target should be stripped away
        assertValidLink(link.get(), "http://myhost");
        assertNull(link.map(Link::getReference).orElse(null));
    }

    @Test
    void testResourcePageLink() {
        Resource linkResource = context.create().resource(page, "link",
                PN_LINK_URL, page.getPath());
        Optional<Link<Page>> link = underTest.getLink(linkResource);
        assertValidLink(link.get(), page.getPath() + ".html");
        assertEquals(page, link.map(Link::getReference).orElse(null));
        assertEquals((page.getPath() + ".html").replaceAll("^\\/content\\/links\\/site1\\/(.+)","/content/site1/$1"),
                link.get().getMappedURL());
    }

    @Test
    void testResourcePageLinkWithNoInjectedPageManager() {
        context.request().setContextPath("/core");
        Resource linkResource = context.create().resource(page, "link",
                PN_LINK_URL, page.getPath());
        Optional<Link<Page>> link = underTest.getLink(linkResource);

        // TODO: this link should be handled as invalid. but we keep this behavior for now to keep backwards compatibility
        assertEquals("/core/content/site1/en.html", link.get().getMappedURL());
        assertEquals(page, link.map(Link::getReference).orElse(null));
    }

    @Test
    void testMalformedURLLink() {
        String malformedURL = "https://a:80:b/c";
        Optional<Link<Page>> link = underTest.getLink("https://a:80:b/c", null);
        assertEquals(malformedURL, link.get().getURL());
    }

    @Test
    void testResourceInvalidPageLink() {
        Resource linkResource = context.create().resource(page, "link",
                PN_LINK_URL, "/content/non-existing");
        Optional<Link<Page>> link = underTest.getLink(linkResource);

        // TODO: this link should be handled as invalid. but we keep this behavior for now to keep backwards compatibility
        assertValidLink(link.get(), "/content/non-existing");
        assertNull(link.get().getReference());
    }

    @Test
    void testPageLink() {
        Optional<Link<Page>> link = underTest.getLink(page);

        assertValidLink(link.get(), page.getPath() + ".html");
        assertEquals("https://example.org" + page.getPath() + ".html", link.map(Link::getExternalizedURL).orElse(null));
        assertEquals(page, link.map(Link::getReference).orElse(null));
    }

    @Test
    void testPageLink_Null() {
        Optional<Link<Page>> link = underTest.getLink((Page)null);

        assertFalse(link.isPresent());
    }

    @Test
    void testEmptyLink() {
        Optional<Link<Page>> link = underTest.getLink("", "");
        if (link.isPresent()) {
            assertNull(link.get().getURL());
            assertNull(link.get().getMappedURL());
            assertNull(link.get().getExternalizedURL());
            assertFalse(link.get().isValid());
        } else {
            fail("noLink");
        }
    }

    @Test
    void testLinkURLPageLinkWithTarget() {
        Optional<Link<Page>> link = underTest.getLink(page.getPath(), "_blank", null, null);

        assertValidLink(link.get(), page.getPath() + ".html", "_blank");
        assertEquals(page, link.map(Link::getReference).orElse(null));
    }

}

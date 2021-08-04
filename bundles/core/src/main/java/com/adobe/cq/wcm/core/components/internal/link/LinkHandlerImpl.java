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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.commons.link.LinkHandler;
import com.adobe.cq.wcm.core.components.services.link.PathProcessor;
import com.day.cq.wcm.api.Page;
import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;
import javax.inject.Named;

import static com.adobe.cq.wcm.core.components.commons.link.Link.PN_LINK_ACCESSIBILITY_LABEL;
import static com.adobe.cq.wcm.core.components.commons.link.Link.PN_LINK_TARGET;
import static com.adobe.cq.wcm.core.components.commons.link.Link.PN_LINK_TITLE_ATTRIBUTE;
import static com.adobe.cq.wcm.core.components.commons.link.Link.PN_LINK_URL;
import static com.adobe.cq.wcm.core.components.internal.link.LinkImpl.ATTR_ARIA_LABEL;
import static com.adobe.cq.wcm.core.components.internal.link.LinkImpl.ATTR_TARGET;
import static com.adobe.cq.wcm.core.components.internal.link.LinkImpl.ATTR_TITLE;

/**
 * Simple implementation for resolving and validating links from model's resources.
 * This is a Sling model that can be injected into other models using the <code>@Self</code> annotation.
 */
@Model(adaptables = SlingHttpServletRequest.class,
    adapters = {LinkHandler.class}
)
public final class LinkHandlerImpl implements LinkHandler {

    /**
     * List of allowed/supported values for link target.
     * <code>_self</code> is used in the edit dialog but not listed as allowed here as we do not
     * want to render a target attribute at all when <code>_self</code> is selected.
     */
    private static final Set<String> VALID_LINK_TARGETS = ImmutableSet.of("_blank", "_parent", "_top");

    /**
     * The current {@link SlingHttpServletRequest}.
     */
    @NotNull
    private final SlingHttpServletRequest request;

    /**
     * Registered path processors.
     */
    @NotNull
    private final List<PathProcessor> pathProcessors;

    @Inject
    public LinkHandlerImpl(@Named("sling-object") @NotNull final SlingHttpServletRequest request,
                           @Named("osgi-services") @NotNull final List<PathProcessor> pathProcessorList) {
        this.request = request;
        this.pathProcessors = pathProcessorList;
    }

    /**
     * Resolves a link from the properties of the given resource.
     *
     * @param resource Resource
     * @return {@link Optional} of {@link Link}
     */
    @NotNull
    public Optional<Link<@Nullable Page>> getLink(@NotNull final Resource resource) {
        return getLink(resource, PN_LINK_URL);
    }

    /**
     * Resolves a link from the properties of the given resource.
     *
     * @param resource            Resource
     * @param linkURLPropertyName Property name to read link URL from.
     * @return {@link Optional} of {@link Link}
     */
    @NotNull
    public Optional<Link<@Nullable Page>> getLink(@NotNull final Resource resource,
                                                  @NotNull final String linkURLPropertyName) {
        ValueMap props = resource.getValueMap();
        String linkURL = props.get(linkURLPropertyName, String.class);
        if (linkURL == null) {
            return Optional.empty();
        }
        String linkTarget = props.get(PN_LINK_TARGET, String.class);
        String linkAccessibilityLabel = props.get(PN_LINK_ACCESSIBILITY_LABEL, String.class);
        String linkTitleAttribute = props.get(PN_LINK_TITLE_ATTRIBUTE, String.class);
        return getLink(linkURL, linkTarget, linkAccessibilityLabel, linkTitleAttribute);
    }

    /**
     * Builds a link pointing to the given target page.
     * @param page Target page
     *
     * @return {@link Optional} of {@link Link<Page>}
     */
    @NotNull
    public Optional<Link<@NotNull Page>> getLink(@Nullable final Page page) {
        return Optional.ofNullable(page)
            .flatMap(p -> buildLink(getPageLinkURL(page), request, page, null));
    }

    /**
     * Builds a link with the given Link URL and target.
     * @param linkURL Link URL
     * @param target Target
     *
     * @return {@link Optional} of {@link Link<Page>}
     */
    @NotNull
    public Optional<Link<@Nullable Page>> getLink(@Nullable final String linkURL, @Nullable final String target) {
        String resolvedLinkURL = validateAndResolveLinkURL(linkURL);
        String resolvedLinkTarget = validateAndResolveLinkTarget(target);
        Page targetPage = getPage(linkURL).orElse(null);
        return buildLink(resolvedLinkURL, request, targetPage,
                new HashMap<String, String>() {{ put(ATTR_TARGET, resolvedLinkTarget); }});
    }

    /**
     * Builds a link with the given Link URL, target, accessibility label, title.
     * @param linkURL Link URL
     * @param target Target
     * @param linkAccessibilityLabel Link Accessibility Label
     * @param linkTitleAttribute Link Title Attribute
     *
     * @return {@link Optional} of {@link Link<Page>}
     */
    @NotNull
    public Optional<Link<@Nullable Page>> getLink(@Nullable final String linkURL,
                                                  @Nullable final String target,
                                                  @Nullable final String linkAccessibilityLabel,
                                                  @Nullable final String linkTitleAttribute) {
        String resolvedLinkURL = validateAndResolveLinkURL(linkURL);
        String resolvedLinkTarget = validateAndResolveLinkTarget(target);
        String validatedLinkAccessibilityLabel = validateLinkAccessibilityLabel(linkAccessibilityLabel);
        String validatedLinkTitleAttribute = validateLinkTitleAttribute(linkTitleAttribute);
        Page targetPage = getPage(linkURL).orElse(null);
        return Optional.of(buildLink(resolvedLinkURL, request, targetPage,
                new HashMap<String, String>() {{
                    put(ATTR_TARGET, resolvedLinkTarget);
                    put(ATTR_ARIA_LABEL, validatedLinkAccessibilityLabel);
                    put(ATTR_TITLE, validatedLinkTitleAttribute);
                }}))
                .orElse(null);
    }

    @NotNull
    private Optional<Link<@Nullable Page>> buildLink(@Nullable final String path,
                                                     @NotNull final SlingHttpServletRequest request,
                                                     @Nullable final Page page,
                                                     @Nullable final Map<String, String> htmlAttributes) {
        if (StringUtils.isNotEmpty(path)) {
            return pathProcessors.stream()
                .filter(pathProcessor -> pathProcessor.accepts(path, request))
                .findFirst().map(pathProcessor -> new LinkImpl<>(
                    pathProcessor.sanitize(path, request),
                    pathProcessor.map(path, request),
                    pathProcessor.externalize(path, request),
                    page,
                    pathProcessor.processHtmlAttributes(path, htmlAttributes)
                ));
        } else {
            return Optional.of(new LinkImpl<>(path, path, path, page, htmlAttributes));
        }
    }

    /**
     * Validates and resolves a link URL.
     *
     * @param linkURL Link URL
     * @return The validated link URL or {@code null} if not valid
     */
    @Nullable
    private String validateAndResolveLinkURL(@Nullable final String linkURL) {
        return Optional.ofNullable(linkURL)
            .filter(StringUtils::isNotEmpty)
            .map(this::getLinkURL)
            .orElse(null);
    }

    /**
     * Validates and resolves the link target.
     * @param linkTarget Link target
     *
     * @return The validated link target or {@code null} if not valid
     */
    @Nullable
    private String validateAndResolveLinkTarget(@Nullable final String linkTarget) {
        return Optional.ofNullable(linkTarget)
            .filter(VALID_LINK_TARGETS::contains)
            .orElse(null);
    }

    /**
     * Validates the link accessibility label.
     * @param linkAccessibilityLabel Link accessibility label
     *
     * @return The validated link accessibility label or {@code null} if not valid
     */
    @Nullable
    private String validateLinkAccessibilityLabel(@Nullable final String linkAccessibilityLabel) {
        return Optional.ofNullable(linkAccessibilityLabel)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .orElse(null);
    }

    /**
     * Validates the link title attribute.
     * @param linkTitleAttribute Link title attribute
     *
     * @return The validated link title attribute or {@code null} if not valid
     */
    @Nullable
    private String validateLinkTitleAttribute(@Nullable final String linkTitleAttribute) {
        return Optional.ofNullable(linkTitleAttribute)
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .orElse(null);
    }

    /**
     * If the provided {@code path} identifies a {@link Page}, this method will generate the correct URL for the page. Otherwise the
     * original {@code String} is returned.
     * @param path the page path
     *
     * @return the URL of the page identified by the provided {@code path}, or the original {@code path} if this doesn't identify a {@link Page}
     */
    @NotNull
    private String getLinkURL(@NotNull final String path) {
        return getPage(path)
                .map(this::getPageLinkURL)
                .orElse(path);
    }

    /**
     * Given a {@link Page}, this method returns the correct URL with the extension
     * @param page the page
     *
     * @return the URL of the provided (@code page}
     */
    @NotNull
    private String getPageLinkURL(@NotNull final Page page) {
        return page.getPath() + HTML_EXTENSION;
    }

    /**
     * Given a path, tries to resolve to the corresponding page.
     *
     * @param path The path
     * @return The {@link Page} corresponding to the path
     */
    @NotNull
    private Optional<Page> getPage(@Nullable final String path) {
        return Optional.ofNullable(path)
                .flatMap(p -> Optional.ofNullable(this.request.getResourceResolver().adaptTo(PageManager.class))
                    .map(pm -> pm.getPage(p)));
    }

}

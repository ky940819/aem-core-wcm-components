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

import com.adobe.cq.wcm.core.components.commons.link.Link;
import com.adobe.cq.wcm.core.components.internal.models.v2.PageImpl;
import com.adobe.cq.wcm.core.components.services.link.PathProcessor;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.designer.Style;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
@Model(adaptables = SlingHttpServletRequest.class, adapters = {LinkHandler.class})
public class LinkHandlerImpl implements LinkHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkHandlerImpl.class);

    /**
     * List of allowed/supported values for link target.
     * <code>_self</code> is used in the edit dialog but not listed as allowed here as we do not
     * want to render a target attribute at all when <code>_self</code> is selected.
     */
    private static final Set<String> VALID_LINK_TARGETS = ImmutableSet.of("_blank", "_parent", "_top");

    /**
     * The current {@link SlingHttpServletRequest}.
     */
    @Self
    private SlingHttpServletRequest request;

    /**
     * The current resource properties
     */
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private ValueMap properties;

    /**
     * The current resource style/policies
     */
    @ScriptVariable(injectionStrategy = InjectionStrategy.OPTIONAL)
    private Style currentStyle;

    /**
     * Reference to {@link PageManager}
     */
    @ScriptVariable
    @org.apache.sling.models.annotations.Optional
    private PageManager pageManager;

    @OSGiService
    private List<PathProcessor> pathProcessors;

    /**
     * Variable that defines how to handle pages that redirect. Given pages PageA and PageB where PageA redirects to PageB,
     * when shadowing is disabled, the link will point to the original page (PageA).
     */
    private Boolean shadowingDisabled;


    @NotNull
    public Optional<Link<Page>> getLink(@NotNull Resource resource) {
        return getLink(resource, PN_LINK_URL);
    }

   @NotNull
    public Optional<Link<Page>> getLink(@NotNull Resource resource, @NotNull String linkURLPropertyName) {
        ValueMap props = resource.getValueMap();
        String linkURL = props.get(linkURLPropertyName, String.class);
        if (linkURL == null) {
            return Optional.empty();
        }
        String linkTarget = props.get(PN_LINK_TARGET, String.class);
        String linkAccessibilityLabel = props.get(PN_LINK_ACCESSIBILITY_LABEL, String.class);
        String linkTitleAttribute = props.get(PN_LINK_TITLE_ATTRIBUTE, String.class);
        return Optional.ofNullable(getLink(linkURL, linkTarget, linkAccessibilityLabel, linkTitleAttribute).orElse(null));
    }

    @NotNull
    public Optional<Link<Page>> getLink(@Nullable Page page) {
        if (page == null) {
            return Optional.empty();
        }
        Pair<Page, String> pair = resolvePage(page);
        return buildLink(pair.getRight(), request, pair.getLeft(), null);
    }

    @NotNull
    public Optional<Link<Page>> getLink(@Nullable String linkURL, @Nullable String target) {
        Pair<Page, String> pair = resolvePage(getPage(linkURL).orElse(null));
        linkURL = StringUtils.isNotEmpty(pair.getRight()) ? pair.getRight() : linkURL;
        String resolvedLinkURL = validateAndResolveLinkURL(linkURL);
        String resolvedLinkTarget = validateAndResolveLinkTarget(target);
        return buildLink(resolvedLinkURL, request, pair.getLeft(),
                new HashMap<String, String>() {{ put(ATTR_TARGET, resolvedLinkTarget); }});
    }

    @NotNull
    public Optional<Link<Page>> getLink(@Nullable String linkURL, @Nullable String target, @Nullable String linkAccessibilityLabel, @Nullable String linkTitleAttribute) {
        Pair<Page, String> pair = resolvePage(getPage(linkURL).orElse(null));
        linkURL = StringUtils.isNotEmpty(pair.getRight()) ? pair.getRight() : linkURL;
        String resolvedLinkURL = validateAndResolveLinkURL(linkURL);
        String resolvedLinkTarget = validateAndResolveLinkTarget(target);
        String validatedLinkAccessibilityLabel = validateLinkAccessibilityLabel(linkAccessibilityLabel);
        String validatedLinkTitleAttribute = validateLinkTitleAttribute(linkTitleAttribute);
        return Optional.of(buildLink(resolvedLinkURL, request, pair.getLeft(),
                new HashMap<String, String>() {{
                    put(ATTR_TARGET, resolvedLinkTarget);
                    put(ATTR_ARIA_LABEL, validatedLinkAccessibilityLabel);
                    put(ATTR_TITLE, validatedLinkTitleAttribute);
                }}))
                .orElse(null);
    }

    private Optional<Link<Page>> buildLink(String path, SlingHttpServletRequest request, Page page,
                                           Map<String, String> htmlAttributes) {
        if (StringUtils.isNotEmpty(path)) {
            return pathProcessors.stream()
                    .filter(pathProcessor -> pathProcessor.accepts(path, request))
                    .findFirst().map(pathProcessor -> new LinkImpl<>(pathProcessor.sanitize(path, request), pathProcessor.map(path,
                            request), pathProcessor.externalize(path, request), page, pathProcessor.processHtmlAttributes(path, htmlAttributes)));
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
    private String validateAndResolveLinkURL(String linkURL) {
        if (!StringUtils.isEmpty(linkURL)) {
            return getLinkURL(linkURL);
        } else {
            return null;
        }
    }

    /**
     * Validates and resolves the link target.
     * @param linkTarget Link target
     *
     * @return The validated link target or {@code null} if not valid
     */
    private String validateAndResolveLinkTarget(String linkTarget) {
        if (linkTarget != null && VALID_LINK_TARGETS.contains(linkTarget)) {
            return linkTarget;
        }
        else {
            return null;
        }
    }

    /**
     * Validates the link accessibility label.
     * @param linkAccessibilityLabel Link accessibility label
     *
     * @return The validated link accessibility label or {@code null} if not valid
     */
    private String validateLinkAccessibilityLabel(String linkAccessibilityLabel) {
        if (!StringUtils.isBlank(linkAccessibilityLabel)) {
            return linkAccessibilityLabel.trim();
        }
        else {
            return null;
        }
    }

    /**
     * Validates the link title attribute.
     * @param linkTitleAttribute Link title attribute
     *
     * @return The validated link title attribute or {@code null} if not valid
     */
    private String validateLinkTitleAttribute(String linkTitleAttribute) {
        if (!StringUtils.isBlank(linkTitleAttribute)) {
            return linkTitleAttribute.trim();
        }
        else {
            return null;
        }
    }

    /**
     * If the provided {@code path} identifies a {@link Page}, this method will generate the correct URL for the page. Otherwise the
     * original {@code String} is returned.
     * @param path the page path
     *
     * @return the URL of the page identified by the provided {@code path}, or the original {@code path} if this doesn't identify a {@link Page}
     */
    @NotNull
    private String getLinkURL(@NotNull String path) {
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
    private String getPageLinkURL(@NotNull Page page) {
        return page.getPath() + HTML_EXTENSION;
    }

    /**
     * Given a path, tries to resolve to the corresponding page.
     *
     * @param path The path
     * @return The {@link Page} corresponding to the path
     */
    @NotNull
    private Optional<Page> getPage(@Nullable String path) {
        if (pageManager == null) {
            pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        }
        if (pageManager == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pageManager.getPage(path));
    }

    /**
     * Attempts to resolve a Link URL and page for the given page. Redirect chains are followed, if
     * shadowing is not disabled.
     *
     * @param page Page
     * @return A pair of {@link String} and {@link Page} the page resolves to.
     */
    @NotNull
    private Pair<Page, String> resolvePage(@Nullable final Page page) {
        Page resolved = page;
        String redirectTarget = null;
        String linkURL = null;
        if (!isShadowingDisabled()) {
            Pair<Page, String> pair = resolveRedirects(page);
            resolved = pair.getLeft();
            redirectTarget = pair.getRight();
        }
        if (resolved == null) {
            if (StringUtils.isNotEmpty(redirectTarget)) {
                return new ImmutablePair<>(page, redirectTarget);
            } else {
                resolved = page;
            }
        }
        if (resolved != null) {
            linkURL = getPageLinkURL(resolved);
        }
        return new ImmutablePair<>(resolved, linkURL);
    }

    @NotNull
    public Pair<Page, String> resolveRedirects(@Nullable final Page page) {
        Page result = page;
        String redirectTarget = null;
        if (page != null && page.getPageManager() != null) {
            Set<String> redirectCandidates = new LinkedHashSet<>();
            redirectCandidates.add(page.getPath());
            while (result != null && StringUtils
                    .isNotEmpty((redirectTarget = result.getProperties().get(PageImpl.PN_REDIRECT_TARGET, String.class)))) {
                result = page.getPageManager().getPage(redirectTarget);
                if (result != null) {
                    if (!redirectCandidates.add(result.getPath())) {
                        LOGGER.warn("Detected redirect loop for the following pages: {}.", redirectCandidates);
                        break;
                    }
                }
            }
        }
        return new ImmutablePair<>(result, redirectTarget);
    }

    /**
     * Checks if redirect page shadowing is disabled
     *
     * @return {@code true} if page shadowing is disabled, {@code false} otherwise
     */
    private boolean isShadowingDisabled() {
        if (shadowingDisabled == null) {
            shadowingDisabled = PROP_DISABLE_SHADOWING_DEFAULT;
            if (currentStyle != null) {
                shadowingDisabled = currentStyle.get(PN_DISABLE_SHADOWING, shadowingDisabled);
            }
            if (properties != null) {
                shadowingDisabled = properties.get(PN_DISABLE_SHADOWING, shadowingDisabled);
            }
        }
        return shadowingDisabled;
    }
}

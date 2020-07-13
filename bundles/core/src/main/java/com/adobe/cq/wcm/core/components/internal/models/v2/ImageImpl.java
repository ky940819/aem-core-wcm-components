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
package com.adobe.cq.wcm.core.components.internal.models.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.internal.Utils;
import com.adobe.cq.wcm.core.components.internal.models.v1.ImageAreaImpl;
import com.adobe.cq.wcm.core.components.internal.servlets.AdaptiveImageServlet;
import com.adobe.cq.wcm.core.components.models.Image;
import com.adobe.cq.wcm.core.components.models.ImageArea;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;

/**
 * V2 Image model implementation.
 */
@Model(adaptables = SlingHttpServletRequest.class,
    adapters = {Image.class, ComponentExporter.class},
    resourceType = ImageImpl.RESOURCE_TYPE)
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME, extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class ImageImpl extends com.adobe.cq.wcm.core.components.internal.models.v1.ImageImpl implements Image {

    /**
     * The resource type.
     */
    public static final String RESOURCE_TYPE = "core/wcm/components/image/v2/image";

    /**
     * Standard logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ImageImpl.class);

    /**
     * The width variable to use when building {@link #srcUriTemplate}.
     */
    private static final String SRC_URI_TEMPLATE_WIDTH_VAR = "{.width}";

    /**
     * The path of the delegated content policy.
     */
    private static final String CONTENT_POLICY_DELEGATE_PATH = "contentPolicyDelegatePath";
    /**
     * Area regular expression.
     */
    private static final String AREA_REGEX =
        "(?<shape>[a-z]+)"
            + "\\((?<coords>(?:\\d+)(?:,(?:\\d+))*)\\)"
            + "(?:\"(?<href>[^\"]*)\""
            + "(?:\\|\"(?<target>[^\"]*)\""
            + "(?:\\|\"(?<alt>[^\"]*)\""
            + "(?:\\|\\((?<relativeCoordinates>(?:-?\\d+\\.?\\d*(?:,-?\\d+\\.?\\d*))+)\\))?)?)?)";

    /**
     * The compiled Pattern for {@link #AREA_REGEX}.
     */
    private static final Pattern AREA_PATTERN = Pattern.compile(AREA_REGEX);

    /**
     * Placeholder for the SRC URI template.
     */
    private String srcUriTemplate;

    /**
     * Placeholder for the areas.
     */
    private List<ImageArea> areas;

    /**
     * Placeholder for the number of pixels, in advance of becoming visible, at which point this image should load.
     */
    private int lazyThreshold;

    /**
     * Placeholder for the referenced assed ID.
     */
    protected String uuid;

    /**
     * Construct the model.
     *
     * Note: Using this constructor does not imply constructor injection, it does not initialize the model.
     * The model must be initialized via the {@link org.apache.sling.models.factory.ModelFactory}.
     */
    public ImageImpl() {
        selector = AdaptiveImageServlet.CORE_DEFAULT_SELECTOR;
    }

    /**
     * Initialize the model.
     */
    @PostConstruct
    protected void initModel() {
        super.initModel();
        boolean altValueFromDAM = properties.get(PN_ALT_VALUE_FROM_DAM, currentStyle.get(PN_ALT_VALUE_FROM_DAM, true));
        boolean titleValueFromDAM = properties.get(PN_TITLE_VALUE_FROM_DAM, currentStyle.get(PN_TITLE_VALUE_FROM_DAM, true));
        displayPopupTitle = properties.get(PN_DISPLAY_POPUP_TITLE, currentStyle.get(PN_DISPLAY_POPUP_TITLE, true));
        boolean uuidDisabled = currentStyle.get(PN_UUID_DISABLED, false);
        if (StringUtils.isNotEmpty(fileReference)) {
            // the image is coming from DAM
            final Resource assetResource = request.getResourceResolver().getResource(fileReference);
            if (assetResource != null) {
                Asset asset = assetResource.adaptTo(Asset.class);
                if (asset != null) {
                    if (!uuidDisabled) {
                        uuid = asset.getID();
                    } else {
                        uuid = null;
                    }
                    if (!isDecorative && altValueFromDAM) {
                        String damDescription = asset.getMetadataValue(DamConstants.DC_DESCRIPTION);
                        if(StringUtils.isEmpty(damDescription)) {
                            damDescription = asset.getMetadataValue(DamConstants.DC_TITLE);
                        }
                        if (StringUtils.isNotEmpty(damDescription)) {
                            alt = damDescription;
                        }
                    }
                    if (titleValueFromDAM) {
                        String damTitle = asset.getMetadataValue(DamConstants.DC_TITLE);
                        if (StringUtils.isNotEmpty(damTitle)) {
                            title = damTitle;
                        }
                    }
                } else {
                    LOGGER.error("Unable to adapt resource '{}' used by image '{}' to an asset.", fileReference,
                            request.getResource().getPath());
                }
            } else {
                LOGGER.error("Unable to find resource '{}' used by image '{}'.", fileReference, request.getResource().getPath());
            }
        }
        if (hasContent) {
            disableLazyLoading = currentStyle.get(PN_DESIGN_LAZY_LOADING_ENABLED, true);

            String staticSelectors = selector;
            if (smartSizes.length > 0) {
                // only include the quality selector in the URL, if there are sizes configured
                staticSelectors += DOT + jpegQuality;
            }
            srcUriTemplate = baseResourcePath + DOT + staticSelectors +
                SRC_URI_TEMPLATE_WIDTH_VAR + DOT + extension +
                (inTemplate ? templateRelativePath : "") + (lastModifiedDate > 0 ?("/" + lastModifiedDate +
                (StringUtils.isNotBlank(imageName) ? ("/" + imageName): "") + DOT + extension): "");

            // if content policy delegate path is provided pass it to the image Uri
            String policyDelegatePath = request.getParameter(CONTENT_POLICY_DELEGATE_PATH);
            if (StringUtils.isNotBlank(policyDelegatePath)) {
                srcUriTemplate += "?" + CONTENT_POLICY_DELEGATE_PATH + "=" + policyDelegatePath;
                src += "?" + CONTENT_POLICY_DELEGATE_PATH + "=" + policyDelegatePath;
            }
            buildJson();
        }

        this.lazyThreshold = currentStyle.get(PN_DESIGN_LAZY_THRESHOLD, 0);
    }

    @NotNull
    @Override
    public int[] getWidths() {
        return Arrays.copyOf(smartSizes, smartSizes.length);
    }

    @Override
    public String getSrcUriTemplate() {
        return srcUriTemplate;
    }

    @Override
    public boolean isLazyEnabled() {
        return !disableLazyLoading;
    }

    @Override
    public int getLazyThreshold() {
        return this.lazyThreshold;
    }

    @Override
    public List<ImageArea> getAreas() {
        if (areas == null) {
            if (hasContent) {
                this.areas = Optional.ofNullable(properties.get(Image.PN_MAP, String.class))
                    .filter(StringUtils::isNotEmpty)
                    .map(mapProperty -> StringUtils.split(mapProperty, "]["))
                    .map(Arrays::stream)
                    .orElseGet(Stream::empty)
                    .map(AREA_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> Optional.ofNullable(matcher.group("href"))
                        .filter(StringUtils::isNotBlank)
                        .map(href -> href.startsWith("/") ? Utils.getURL(request, pageManager, href) : href)
                        .map(href -> new ImageAreaImpl(
                            Optional.ofNullable(matcher.group("shape")).orElse(""),
                            Optional.ofNullable(matcher.group("coords")).orElse(""),
                            Optional.ofNullable(matcher.group("relativeCoordinates")).orElse(""),
                            href,
                            Optional.ofNullable(matcher.group("target")).orElse(""),
                            Optional.ofNullable(matcher.group("alt")).orElse("")))
                        .orElse(null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            } else {
                this.areas = new ArrayList<>();
            }
        }
        return Collections.unmodifiableList(areas);
    }

    @Override
    public String getUuid() {
        return uuid;
    }
}

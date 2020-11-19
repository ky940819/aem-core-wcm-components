/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2019 Adobe
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
package com.adobe.cq.wcm.core.components.internal.models.v1;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;
import org.jetbrains.annotations.NotNull;

import com.adobe.cq.wcm.core.components.models.LayoutContainer;

/**
 * Layout container model implementation.
 */
@Model(adaptables = SlingHttpServletRequest.class, adapters = LayoutContainer.class, resourceType = LayoutContainerImpl.RESOURCE_TYPE_V1)
public class LayoutContainerImpl extends AbstractContainerImpl implements LayoutContainer {

    /**
     * The resource type.
     */
    protected static final String RESOURCE_TYPE_V1 = "core/wcm/components/container/v1/container";

    /**
     * The layout type.
     */
    private LayoutType layout;

    /**
     * Map of the child items to be exported wherein the key is the child name, and the value is the child model.
     */
    private LinkedHashMap<String, ComponentExporter> itemModels;

    /**
     * Initialize the model.
     */
    @PostConstruct
    protected void initModel() {
        // Note: this can be simplified using Optional.or() in JDK 11
        this.layout = Optional.ofNullable(
            Optional.ofNullable(resource.getValueMap().get(LayoutContainer.PN_LAYOUT, String.class))
                .orElseGet(() -> Optional.ofNullable(currentStyle)
                    .map(style -> currentStyle.get(LayoutContainer.PN_LAYOUT, String.class))
                    .orElse(null)
                ))
            .map(LayoutType::getLayoutType)
            .orElse(LayoutType.SIMPLE);
    }

    @Override
    @NotNull
    protected List<ResourceListItemImpl> readItems() {
        return getChildren().stream()
            .map(res -> new ResourceListItemImpl(res, getId(), component))
            .collect(Collectors.toList());
    }

    @Override
    @NotNull
    public final LinkedHashMap<String, ? extends ComponentExporter> getExportedItems() {
        if (this.itemModels == null) {
            this.itemModels = ComponentUtils.getComponentModels(this.slingModelFilter,
                this.modelFactory,
                this.getChildren(),
                this.request,
                ComponentExporter.class);
        }
        return this.itemModels;
    }

    @Override
    public String[] getDataLayerShownItems() {
        return null;
    }

    @Override
    public @NotNull LayoutType getLayout() {
        return layout;
    }
}

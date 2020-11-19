/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2018 Adobe
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
package com.adobe.cq.wcm.core.components.models;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.annotation.versioning.ConsumerType;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ContainerExporter;

/**
 * A base interface to be extended by all containers.
 *
 * A container is a component that provides access to child resources.
 * If the container contains panels, such as the {@link Carousel}, {@link Tabs} and {@link Accordion} models, then
 * the {@link PanelContainer} class should be used instead.
 *
 * @since com.adobe.cq.wcm.core.components.models 12.5.0
 */
@ConsumerType
public interface Container extends Component, ContainerExporter {

    /**
     * Name of the configuration policy property that indicates if background images are enabled
     *
     * @since com.adobe.cq.wcm.core.components.models 12.8.0
     */
    String PN_BACKGROUND_IMAGE_ENABLED = "backgroundImageEnabled";

    /**
     * Name of the configuration policy property that indicates if background colors are enabled
     *
     * @since com.adobe.cq.wcm.core.components.models 12.8.0
     */
    String PN_BACKGROUND_COLOR_ENABLED = "backgroundColorEnabled";

    /**
     * Name of the configuration policy property that indicates if background colors are to be restricted to predefined values
     *
     * @since com.adobe.cq.wcm.core.components.models 12.8.0
     */
    String PN_BACKGROUND_COLOR_SWATCHES_ONLY = "backgroundColorSwatchesOnly";

    /**
     * Name of the resource property that indicates that path to the background image
     *
     * @since com.adobe.cq.wcm.core.components.models 12.8.0
     */
    String PN_BACKGROUND_IMAGE_REFERENCE = "backgroundImageReference";

    /**
     * Name of the resource property that indicates the background color
     *
     * @since com.adobe.cq.wcm.core.components.models 12.8.0
     */
    String PN_BACKGROUND_COLOR = "backgroundColor";

    /**
     * Returns a list of container items
     *
     * @return List of container items
     * @since com.adobe.cq.wcm.core.components.models 12.5.0
     * @deprecated since 12.17.0 - use {@link #getChildren()}
     */
    @NotNull
    @Deprecated
    @JsonIgnore
    default List<ListItem> getItems() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns a list of container items.
     *
     * @return List of container items.
     * @since com.adobe.cq.wcm.core.components.models 12.17.0
     */
    @NotNull
    @JsonIgnore
    default List<? extends ContainerItem> getChildren() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the background CSS style to be applied to the component's root element
     *
     * @return CSS style string for the component's root element
     * @since com.adobe.cq.wcm.core.components.models 12.8.0
     */
    @Nullable
    default String getBackgroundStyle() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ContainerExporter#getExportedItems()
     * @since com.adobe.cq.wcm.core.components.models 12.5.0
     */
    @NotNull
    @Override
    default Map<String, ? extends ComponentExporter> getExportedItems() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see ContainerExporter#getExportedItemsOrder()
     * @since com.adobe.cq.wcm.core.components.models 12.5.0
     */
    @NotNull
    @Override
    default String[] getExportedItemsOrder() {
        throw new UnsupportedOperationException();
    }
}

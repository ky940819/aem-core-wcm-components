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
package com.adobe.cq.wcm.core.components.internal.models.v1;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.Container;
import com.adobe.cq.wcm.core.components.models.ListItem;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

/**
 * Abstract panel container model.
 */
public abstract class AbstractPanelContainerImpl extends AbstractContainerImpl implements Container {

    /**
     * Map of the child items to be exported wherein the key is the child name, and the value is the child model.
     */
    protected LinkedHashMap<String, ComponentExporter> itemModels;

    @Override
    @NotNull
    protected List<PanelContainerListItemImpl> readItems() {
        return getChildren().stream()
            .map(res -> new PanelContainerListItemImpl(res, getId(), component))
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
            this.itemModels.entrySet().forEach(entry ->
                getItems().stream()
                    .filter(Objects::nonNull)
                    .filter(item -> StringUtils.isNotEmpty(item.getName()) && StringUtils.equals(item.getName(), entry.getKey()))
                    .findFirst()
                    .ifPresent(match -> entry.setValue(new JsonWrapper(entry.getValue(), match)))
            );
        }
        return this.itemModels;
    }

    /**
     * Wrapper class used to add specific properties of the container items to the JSON serialization of the underlying container item model
     */
    static class JsonWrapper implements ComponentExporter {

        /**
         * The wrapped ComponentExporter.
         */
        @NotNull
        private final ComponentExporter inner;

        /**
         * The panel title.
         */
        private final String panelTitle;

        /**
         * Construct the wrapper.
         *
         * @param inner The ComponentExporter to be wrapped.
         * @param item The panel item.
         */
        JsonWrapper(@NotNull final ComponentExporter inner, @NotNull final ListItem item) {
            this.inner = inner;
            this.panelTitle = item.getTitle();
        }

        /**
         * Get the underlying ComponentExporter that is wrapped by this wrapper.
         *
         * @return the underlying container item model
         */
        @JsonUnwrapped
        @NotNull
        public ComponentExporter getInner() {
            return this.inner;
        }

        /**
         * Get the panel title.
         *
         * @return the container item title
         */
        @JsonProperty(PanelContainerListItemImpl.PN_PANEL_TITLE)
        @JsonInclude()
        public String getPanelTitle() {
            return this.panelTitle;
        }

        @NotNull
        @Override
        @JsonIgnore
        public String getExportedType() {
            return this.inner.getExportedType();
        }
    }
}

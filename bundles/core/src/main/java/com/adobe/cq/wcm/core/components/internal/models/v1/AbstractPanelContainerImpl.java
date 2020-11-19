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
package com.adobe.cq.wcm.core.components.internal.models.v1;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.adobe.cq.wcm.core.components.models.PanelContainer;
import com.adobe.cq.wcm.core.components.util.ComponentUtils;
import org.jetbrains.annotations.NotNull;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.wcm.core.components.models.ListItem;


/**
 * Abstract panel container model.
 */
public abstract class AbstractPanelContainerImpl extends AbstractContainerImpl implements PanelContainer {

    /**
     * Map of the child items to be exported wherein the key is the child name, and the value is the child model.
     */
    private LinkedHashMap<String, ? extends ComponentExporter> itemModels;

    /**
     * List of child panels in this panel container.
     */
    private List<PanelContainerItemImpl> panelItems;

    @Override
    @NotNull
    @Deprecated
    protected final List<ListItem> readItems() {
        return getChildren().stream()
            .map(res -> new PanelContainerListItemImpl(res.getResource(), getId(), this.component))
            .collect(Collectors.toList());
    }

    @NotNull
    @Override
    public final List<PanelContainerItemImpl> getChildren() {
        if (this.panelItems == null) {
            this.panelItems = ComponentUtils.getChildComponents(this.resource).stream()
                .map(item -> new PanelContainerItemImpl(item, this))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList));
        }
        return this.panelItems;
    }

    @NotNull
    @Override
    public final LinkedHashMap<String, ? extends ComponentExporter> getExportedItems() {
        if (this.itemModels == null) {
            this.itemModels = this.getChildren().stream()
                .map(i -> i.getComponentExporter(this.slingModelFilter, this.modelFactory, this.request))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));
        }
        return this.itemModels;
    }
}

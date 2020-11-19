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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.adobe.cq.export.json.ComponentExporter;
import org.jetbrains.annotations.NotNull;

import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;
import com.adobe.cq.wcm.core.components.models.Container;
import com.adobe.cq.wcm.core.components.models.ContainerItem;
import com.adobe.cq.wcm.core.components.models.ListItem;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(AemContextExtension.class)
public class AbstractContainerImplTest {

    private static final String TEST_BASE = "/container";
    private static final String CONTENT_ROOT = "/content";
    // private static final String CONTEXT_PATH = "/core";
    // private static final String TEST_ROOT_PAGE = "/content/container";
    // private static final String TEST_ROOT_PAGE_GRID = "/jcr:content/root/responsivegrid";
    // private static final String CONTAINER_1 = TEST_ROOT_PAGE + TEST_ROOT_PAGE_GRID + "/container-1";
    private static final String TEST_APPS_ROOT = "/apps/core/wcm/components";

    public final AemContext context = CoreComponentTestContext.newAemContext();

    @BeforeEach
    public void setUp() {
        context.load().json(TEST_BASE + CoreComponentTestContext.TEST_CONTENT_JSON, CONTENT_ROOT);
        context.load().json(TEST_BASE + CoreComponentTestContext.TEST_APPS_JSON, TEST_APPS_ROOT);
    }

    @Test
    public void testEmptyContainer() {
        Container container = new ContainerImpl();
        List<? extends ContainerItem> items = container.getChildren();
        assertEquals(0, items.size());
    }


    private static class ContainerImpl extends AbstractContainerImpl {

        @Override
        @NotNull
        @Deprecated
        protected final List<ListItem> readItems() {
            throw new UnsupportedOperationException();
        }

        @Override
        @NotNull
        public List<? extends ContainerItem> getChildren() {
            return new ArrayList<>();
        }

        @Override
        @NotNull
        public LinkedHashMap<String, ? extends ComponentExporter> getExportedItems() {
            return new LinkedHashMap<>();
        }

        @Override
        public String[] getDataLayerShownItems() {
            return null;
        }
    }
}

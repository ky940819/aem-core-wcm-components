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
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import com.adobe.cq.wcm.core.components.internal.ComponentFileUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;

import com.adobe.cq.wcm.core.components.internal.Utils;
import com.adobe.cq.wcm.core.components.models.ComponentFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(
        adaptables = SlingHttpServletRequest.class,
        adapters = ComponentFiles.class
)
public class ComponentFilesImpl implements ComponentFiles {
    private static final Logger LOG = LoggerFactory.getLogger(ComponentFilesImpl.class);

    /**
     * Name of the subservice used to authenticate as in order to be able to read details about components and
     * client libraries.
     */
    public static final String COMPONENTS_SERVICE = "components-service";

    @Inject
    @Named(OPTION_RESOURCE_TYPES)
    Object resourceTypes;

    @Inject
    @Named(OPTION_FILTER_REGEX)
    String filterRegex;

    @Inject
    @Named(OPTION_INHERITED)
    @Default(booleanValues = OPTION_INHERITED_DEFAULT)
    boolean inherited;

    @OSGiService
    ResourceResolverFactory resolverFactory;

    private Set<String> resourceTypeSet;
    private Pattern pattern;
    private List<String> paths;

    @PostConstruct
    public void init() {
        resourceTypeSet = Utils.getStrings(resourceTypes);
        pattern = Pattern.compile(filterRegex);
    }

    @Override
    public List<String> getPaths() {
        if (paths == null) {
            try (ResourceResolver resourceResolver = resolverFactory.getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, COMPONENTS_SERVICE))) {
                this.paths = ComponentFileUtils.getPaths(
                    this.resourceTypeSet,
                    this.pattern,
                    this.inherited,
                    resourceResolver
                );
            } catch (LoginException e) {
                LOG.error("Cannot login as a service user", e);
            }
        }
        return paths;
    }

}

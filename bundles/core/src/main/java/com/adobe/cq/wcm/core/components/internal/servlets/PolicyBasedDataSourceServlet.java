/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Copyright 2024 Adobe
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
package com.adobe.cq.wcm.core.components.internal.servlets;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;
import javax.servlet.Servlet;

import com.adobe.granite.ui.components.Config;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

import com.adobe.granite.ui.components.Value;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;


/**
 * Servlet for a datasource whose values are derived from a multi-value policy option.
 *
 * <pre>
 * The multi-value field in the policy may be either a composite or non-composite field.
 *
 * <strong>For composite fields, the datasource can be used as follows:</strong>
 *
 * Design Dialog:
 * {@code
 * <options
 *     jcr:primaryType="nt:unstructured"
 *     sling:resourceType="granite/ui/components/coral/foundation/form/multifield"
 *     composite="{Boolean}true"
 *     fieldLabel="Options">
 *     <field
 *         jcr:primaryType="nt:unstructured"
 *         sling:resourceType="granite/ui/components/coral/foundation/container"
 *         name="./options">
 *         <items jcr:primaryType="nt:unstructured">
 *             <text
 *                 jcr:primaryType="nt:unstructured"
 *                 sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
 *                 emptyText="Text"
 *                 name="text"
 *                 required="{Boolean}true">
 *             </text>
 *             <value
 *                 jcr:primaryType="nt:unstructured"
 *                 sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
 *                 emptyText="Value"
 *                 name="value"
 *                 required="{Boolean}true">
 *             </value>
 *         </items>
 *     </field>
 * </options>
 * }
 *
 * Component Dialog:
 * {@code
 * <datasource
 *    jcr:primaryType="nt:unstructured"
 *    sling:resourceType="core/wcm/components/commons/datasources/policyListValue/v1"
 *    compositeFieldName="options"
 *    compositeTextPropName="text"
 *    compositeValuePropName="value"
 * />
 * }
 *
 * <hr><br>
 *
 * <strong>For non-composite fields, the datasource can be used as follows:</strong><br>
 *
 * Design Dialog:
 * {@code
 * <options
 *     jcr:primaryType="nt:unstructured"
 *     sling:resourceType="granite/ui/components/coral/foundation/form/multifield"
 *     fieldLabel="Options">
 *     <field
 *         jcr:primaryType="nt:unstructured"
 *         sling:resourceType="granite/ui/components/coral/foundation/form/textfield"
 *         required="true"
 *         name="./options">
 *     </field>
 * </options>
 * }
 *
 * Component Dialog:
 * {@code
 * <datasource
 *    jcr:primaryType="nt:unstructured"
 *    sling:resourceType="core/wcm/components/commons/datasources/policyListValue/v1"
 *    propName="options"
 * />
 * }
 * </pre>
 */
@Component(
    service = {Servlet.class},
    property = {
        "sling.servlet.resourceTypes=" + PolicyBasedDataSourceServlet.RESOURCE_TYPE_V1,
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
    }
)
public final class PolicyBasedDataSourceServlet extends SlingSafeMethodsServlet {

    /**
     * Resource type for the servlet.
     */
    public final static String RESOURCE_TYPE_V1 = "core/wcm/components/commons/datasources/policyListValue/v1";

    /**
     * Datasource configuration property name whose value is the property name in the policy that contains the options.
     */
    private final static String PN_PROP_NAME = "propName";

    /**
     * Datasource configuration property name whose value is the property name in the policy that contains the node name for composite field options.
     */
    private final static String PN_COMPOSITE_FIELD_NAME = "compositeFieldName";

    /**
     * Datasource configuration property name whose value is the property name containing the "text" in a composite field.
     */
    private final static String PN_COMPOSITE_TEXT_NAME = "compositeTextPropName";

    /**
     * Datasource configuration property name whose value is the property name containing the "value" in a composite field.
     */
    private final static String PN_COMPOSITE_VALUE_NAME = "compositeValuePropName";

    @Override
    protected void doGet(@NotNull final SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response) {
        DataSource dataSource = new SimpleDataSource(getDataSourceIterator(request));
        request.setAttribute(DataSource.class.getName(), dataSource);
    }

    /**
     * Get the data source configuration.
     *
     * @param request The current request.
     * @return The data source configuration, or empty if the requested resource does not contain a datasource child resource.
     */
    @NotNull
    private static Optional<ValueMap> getDataSourceConfig(@NotNull final SlingHttpServletRequest request) {
        return Optional.ofNullable(request.getResource().getChild(Config.DATASOURCE)).map(Resource::getValueMap);
    }

    /**
     * Get data source resource iterator.
     *
     * @param request The current request.
     * @return Iterator of the data source resources, or empty if options could not be determined.
     */
    @NotNull
    private static Iterator<Resource> getDataSourceIterator(@NotNull final SlingHttpServletRequest request) {
        return getDataSourceConfig(request)
            .flatMap(dataSourceProps -> {
                if (dataSourceProps.get(PN_COMPOSITE_FIELD_NAME, String.class) != null) {
                    return getCompositeDataSourceIterator(request);
                } else {
                    return getNonCompositeDataSourceIterator(request);
                }
            })
            .orElseGet(Collections::emptyIterator);
    }

    /**
     * Get data source resource iterator for non-composite field.
     *
     * @param request The current request.
     * @return Iterator of the data source resources, or empty if options could not be determined.
     */
    @NotNull
    private static Optional<Iterator<Resource>> getNonCompositeDataSourceIterator(@NotNull final SlingHttpServletRequest request) {
        return getDataSourceConfig(request).map(dsc -> dsc.get(PN_PROP_NAME, String.class))
            .flatMap(propName -> getContentPolicy(request)
                .map(ContentPolicy::getProperties)
                .map(props -> props.get(propName, String[].class))
                .map(vals -> Arrays.stream(vals)
                    .map(val -> createResource(request.getResourceResolver(), val, val))
                    .iterator()));
    }

    /**
     * Get data source resource iterator for composite field.
     *
     * @param request The current request.
     * @return Iterator of the data source resources, or empty if options could not be determined.
     */
    @NotNull
    private static Optional<Iterator<Resource>> getCompositeDataSourceIterator(@NotNull final SlingHttpServletRequest request) {
        Optional<ValueMap> dataSourceProps = getDataSourceConfig(request);
        String titlePropName = dataSourceProps.map(dsc -> dsc.get(PN_COMPOSITE_TEXT_NAME, String.class)).orElse("text");
        String valuePropName = dataSourceProps.map(dsc -> dsc.get(PN_COMPOSITE_VALUE_NAME, String.class)).orElse("value");

        return dataSourceProps.map(dsc -> dsc.get(PN_COMPOSITE_FIELD_NAME, String.class))
            .flatMap(fieldName -> getContentPolicy(request)
                .map(contentPolicy -> request.getResourceResolver().getResource(contentPolicy.getPath()))
                .map(policyResource -> policyResource.getChild(fieldName))
                .map(Resource::listChildren)
                .map(it -> Spliterators.spliteratorUnknownSize(it, Spliterator.ORDERED))
                .map(sit -> StreamSupport.stream(sit, false)
                    .flatMap(option -> Optional.ofNullable(option.getValueMap().get(titlePropName, String.class))
                        .flatMap(text -> Optional.ofNullable(option.getValueMap().get(valuePropName, String.class))
                            .map(value -> createResource(request.getResourceResolver(), text, value)))
                        .stream())
                    .iterator()));
    }

    /**
     * Create data source entry resource.
     *
     * @param resourceResolver The ResourceResolver.
     * @param text             The data source entry display text.
     * @param value            The data source entry value.
     * @return DataSource entry resource.
     */
    @NotNull
    private static Resource createResource(@NotNull final ResourceResolver resourceResolver, @NotNull final String text, @NotNull final String value) {
        return new ValueMapResource(resourceResolver, new ResourceMetadata(), JcrConstants.NT_RESOURCE, new ValueMapDecorator(
            Map.ofEntries(
                Map.entry("value", value),
                Map.entry("text", text)
            )));
    }

    /**
     * Get the content policy for the requested component.
     *
     * @param request The request.
     * @return The content policy for the requested component, or empty if no policy can be resolved.
     */
    @NotNull
    private static Optional<ContentPolicy> getContentPolicy(@NotNull final SlingHttpServletRequest request) {
        return Optional.ofNullable((String) request.getAttribute(Value.CONTENTPATH_ATTRIBUTE))
            .map(request.getResourceResolver()::getResource)
            .flatMap(contentResource -> Optional.ofNullable(request.getResourceResolver().adaptTo(ContentPolicyManager.class))
                .map(policyManager -> policyManager.getPolicy(contentResource)));
    }
}

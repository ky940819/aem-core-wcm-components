package com.adobe.cq.wcm.core.components.internal.servlets;

import com.adobe.cq.wcm.core.components.context.CoreComponentTestContext;
import com.adobe.granite.ui.components.Value;
import com.adobe.granite.ui.components.ds.DataSource;
import com.day.cq.wcm.api.policies.ContentPolicyMapping;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Tests for the {@link PolicyBasedDataSourceServlet} DataSource servlet.
 */
@ExtendWith(AemContextExtension.class)
public final class PolicyBasedDataSourceServletTest {

    /**
     * Folder under which test content is found.
     */
    private static final String TEST_BASE = "/com/adobe/cq/wcm/core/components/internal/servlets/policyBasedDataSourceServlet";

    /**
     * The test context.
     */
    public final AemContext context = CoreComponentTestContext.newAemContext();

    /**
     * Set up the tests.
     *
     * @throws PersistenceException Error initializing the content.
     */
    @BeforeEach
    void setup() throws PersistenceException {
        this.context.load().json(TEST_BASE + "/content.json", "/content");

        // set up the policy for composite datasource
        ContentPolicyMapping compositeMapping = this.context.contentPolicyMapping("core/wcm/components/component_composite", new HashMap<>());
        String compositeMappingPath = compositeMapping.getPolicy().getPath();
        this.context.resourceResolver().delete(Objects.requireNonNull(this.context.resourceResolver().getResource(compositeMappingPath)));
        this.context.load().json(TEST_BASE + "/composite_policy.json", compositeMappingPath);


        // set up the policy for non-composite datasource
        ContentPolicyMapping nonCompositeMapping = this.context.contentPolicyMapping("core/wcm/components/component_non_composite", new HashMap<>());
        String nonCompositeMappingPath = nonCompositeMapping.getPolicy().getPath();
        this.context.resourceResolver().delete(Objects.requireNonNull(this.context.resourceResolver().getResource(nonCompositeMappingPath)));
        this.context.load().json(TEST_BASE + "/non_composite_policy.json", nonCompositeMappingPath);
    }

    /**
     * Test the servlet when the DataSource is based on a composite multifield in the policy.
     */
    @Test
    void testCompositeDataSource() {
        // set up the datasource request
        this.context.currentResource(Objects.requireNonNull(this.context.resourceResolver().getResource("/content/compositeDataSource")));
        MockSlingHttpServletRequest request = this.context.request();
        request.setAttribute(Value.CONTENTPATH_ATTRIBUTE, "/content/component_composite");

        // invoke the servlet
        PolicyBasedDataSourceServlet servlet = new PolicyBasedDataSourceServlet();
        servlet.doGet(request, context.response());

        // check the result
        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        List<Resource> resources = StreamSupport
            .stream(((Iterable<Resource>) dataSource::iterator).spliterator(), false)
            .collect(Collectors.toList());

        Assertions.assertEquals(2, resources.size());

        Resource firstOption = resources.get(0);
        Assertions.assertEquals("First Option Text", firstOption.getValueMap().get("text", String.class));
        Assertions.assertEquals("first_option_value", firstOption.getValueMap().get("value", String.class));

        Resource secondOption = resources.get(1);
        Assertions.assertEquals("Second Option Text", secondOption.getValueMap().get("text", String.class));
        Assertions.assertEquals("second_option_value", secondOption.getValueMap().get("value", String.class));
    }

    /**
     * Test the servlet when the DataSource is based on a NON-composite multifield in the policy.
     */
    @Test
    void testNonCompositeDataSource() {
        // set up the datasource request
        this.context.currentResource(Objects.requireNonNull(this.context.resourceResolver().getResource("/content/nonCompositeDataSource")));
        MockSlingHttpServletRequest request = this.context.request();
        request.setAttribute(Value.CONTENTPATH_ATTRIBUTE, "/content/component_non_composite");

        // invoke the servlet
        PolicyBasedDataSourceServlet servlet = new PolicyBasedDataSourceServlet();
        servlet.doGet(request, context.response());

        // check the result
        DataSource dataSource = (DataSource) request.getAttribute(DataSource.class.getName());
        List<Resource> resources = StreamSupport
            .stream(((Iterable<Resource>) dataSource::iterator).spliterator(), false)
            .collect(Collectors.toList());

        Assertions.assertEquals(2, resources.size());

        Resource firstOption = resources.get(0);
        Assertions.assertEquals("first_option_value", firstOption.getValueMap().get("text", String.class));
        Assertions.assertEquals("first_option_value", firstOption.getValueMap().get("value", String.class));

        Resource secondOption = resources.get(1);
        Assertions.assertEquals("second_option_value", secondOption.getValueMap().get("text", String.class));
        Assertions.assertEquals("second_option_value", secondOption.getValueMap().get("value", String.class));
    }
}

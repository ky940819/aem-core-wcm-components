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
package com.adobe.cq.wcm.core.components.internal.models.v1;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import com.day.cq.commons.LanguageUtil;
import com.day.cq.wcm.msm.api.LiveRelationship;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Exporter;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.cq.export.json.ComponentExporter;
import com.adobe.cq.export.json.ExporterConstants;
import com.adobe.cq.wcm.core.components.models.Navigation;
import com.adobe.cq.wcm.core.components.models.NavigationItem;
import com.day.cq.wcm.api.*;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.msm.api.LiveRelationshipManager;

/**
 * Navigation model implementation.
 */
@Model(adaptables = SlingHttpServletRequest.class,
    adapters = {Navigation.class, ComponentExporter.class},
    resourceType = {NavigationImpl.RESOURCE_TYPE})
@Exporter(name = ExporterConstants.SLING_MODEL_EXPORTER_NAME , extensions = ExporterConstants.SLING_MODEL_EXTENSION)
public class NavigationImpl extends AbstractComponentImpl implements Navigation {

    /**
     * The resource navigation component resource type.
     */
    public static final String RESOURCE_TYPE = "core/wcm/components/navigation/v1/navigation";

    /**
     * Name of the resource / configuration policy property that defines the accessibility label.
     */
    private static final String PN_ACCESSIBILITY_LABEL = "accessibilityLabel";

    /**
     * The current request.
     */
    @Self
    private SlingHttpServletRequest request;

    /**
     * The current page.
     */
    @ScriptVariable
    private Page currentPage;

    /**
     * The current style.
     */
    @ScriptVariable
    private Style currentStyle;

    /**
     * The language manager service.
     */
    @OSGiService
    private LanguageManager languageManager;

    /**
     * The relationship manager service.
     */
    @OSGiService
    private LiveRelationshipManager relationshipManager;

    /**
     * The accessibility label.
     */
    @Nullable
    private String accessibilityLabel;

    /**
     * Number indicating how many levels below the navigation root the navigation should start.
     */
    private int structureStart;

    /**
     * Number indicating how many levels below the navigation root should be included in the results.
     * Use "-1" for no limit.
     */
    private int structureDepth;

    /**
     * The root page from which to build the navigation.
     */
    private Page navigationRootPage;

    /**
     * Flag indicating if showing is disabled.
     * If shadowing is enabled, then the navigation will traverse redirects - where possible - to present information
     * relevant to the target page that the user would navigate to if selected.
     */
    private boolean isShadowingDisabled;

    /**
     * Placeholder for the list of results.
     */
    private List<NavigationItem> items;

    /**
     * Initialize the model.
     */
    @PostConstruct
    private void initModel() {
        ValueMap properties = this.resource.getValueMap();
        structureDepth = properties.get(PN_STRUCTURE_DEPTH, currentStyle.get(PN_STRUCTURE_DEPTH, -1));
        boolean collectAllPages = properties.get(PN_COLLECT_ALL_PAGES, currentStyle.get(PN_COLLECT_ALL_PAGES, true));
        if (collectAllPages) {
            structureDepth = -1;
        }
        if (currentStyle.containsKey(PN_STRUCTURE_START) || properties.containsKey(PN_STRUCTURE_START)) {
            //workaround to maintain the content of Navigation component of users in case they update to the current i.e. the `structureStart` version.
            structureStart = properties.get(PN_STRUCTURE_START, currentStyle.get(PN_STRUCTURE_START, 1));
        } else {
            boolean skipNavigationRoot = properties.get(PN_SKIP_NAVIGATION_ROOT, currentStyle.get(PN_SKIP_NAVIGATION_ROOT, true));
            if (skipNavigationRoot) {
                structureStart = 1;
            } else {
                structureStart = 0;
            }
        }
        isShadowingDisabled = properties.get(PageListItemImpl.PN_DISABLE_SHADOWING,
            currentStyle.get(PageListItemImpl.PN_DISABLE_SHADOWING, PageListItemImpl.PROP_DISABLE_SHADOWING_DEFAULT));
    }

    /**
     * Get the effective navigation root page.
     *
     * @return The effective navigation root page.
     */
    private Page getNavigationRoot() {
        if (this.navigationRootPage == null) {
            String navigationRootPath = Optional.ofNullable(this.resource.getValueMap().get(PN_NAVIGATION_ROOT, String.class))
                .orElseGet(() -> currentStyle.get(PN_NAVIGATION_ROOT, String.class));
            PageManager pageManager = currentPage.getPageManager();
            Page rootPage = pageManager.getPage(navigationRootPath);
            // only deal with sibling languages or live copies if the navigation root is not within the current site
            // TODO: this next statement is wrong.
            if (rootPage != null /*&& !(currentPage.getPath() + "/").startsWith(rootPage.getPath() + "/")*/) {

                // try to correct the language if the referenced navigation root comes from a sibling language
                String rootPagePath = rootPage.getPath();
                rootPage = getPageResource(currentPage).map(pageResource -> languageManager.getLanguageRoot(pageResource))
                    // skip if the navigation root is within the current language root
                    .filter(languageRoot -> !(rootPagePath + "/").startsWith(languageRoot.getPath() + "/"))
                    .map(Page::getName)
                    .map(LanguageUtil::getLanguage)
                    .flatMap(language ->
                        // get the resource specified by rootPagePath in adjacent languages
                        Optional.ofNullable(languageManager.getAdjacentLanguageInfo(this.resource.getResourceResolver(), rootPagePath))
                            // stream the result set
                            .map(i -> i.entrySet().stream())
                            .orElse(Stream.empty())
                            //only keep solutions for which the locale is the same as the current page locale
                            .filter(i -> i.getKey().getLocale().equals(language.getLocale()))
                            // get the value
                            .map(Map.Entry::getValue)
                            // keep only those where the rootPagePath (adjusted for language) exists
                            .filter(LanguageManager.Info::exists)
                            // get the page
                            .map(LanguageManager.Info::getPath)
                            .map(pageManager::getPage)
                            .filter(Objects::nonNull)
                            .findAny())
                    .orElse(rootPage);

                // correct the navigation root if the referenced navigation root points to a page that is the source of the current pages live relationship
                // i.e. the current site is a live copy of the referenced navigation root.
                rootPage = this.safeGetLiveRelationships(rootPage)
                    .map(liveRelationshipIterator -> StreamSupport.stream(((Iterable<LiveRelationship>) () -> liveRelationshipIterator).spliterator(), false))
                    .orElseGet(Stream::empty)
                    .map(LiveRelationship::getTargetPath)
                    .filter(target -> (currentPage.getPath() + "/").startsWith(target + "/"))
                    .map(pageManager::getPage)
                    .findFirst()
                    .orElse(rootPage);
            }
            this.navigationRootPage = rootPage;
        }
        return this.navigationRootPage;
    }

    /**
     * Get the live relationships iterator for the specified page.
     * This method is safe in that the possible exception is suppressed.
     *
     * @param page The page for which to get a stream of live relationships.
     * @return The stream of live relationships, or empty stream if exception.
     */
    @SuppressWarnings("unchecked")
    private Optional<Iterator<LiveRelationship>> safeGetLiveRelationships(@NotNull final Page page) {
        try {
            return Optional.ofNullable(
                (Iterator<LiveRelationship>) relationshipManager.getLiveRelationships(page.adaptTo(Resource.class), null, null)
            );
        } catch (WCMException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<NavigationItem> getItems() {
        if (this.items == null) {
            this.items = Optional.ofNullable(this.getNavigationRoot())
                .map(navigationRoot -> getRootItems(navigationRoot, structureStart))
                .orElseGet(Stream::empty)
                .map(item -> this.createNavigationItem(item, getItems(item)))
                .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(items);
    }

    @Override
    @Nullable
    public String getAccessibilityLabel() {
        if (this.accessibilityLabel == null) {
            this.accessibilityLabel = this.resource.getValueMap().get(PN_ACCESSIBILITY_LABEL, String.class);
        }
        return this.accessibilityLabel;
    }

    @NotNull
    @Override
    public String getExportedType() {
        return this.resource.getResourceType();
    }

    /**
     * Builds the navigation tree for a {@code navigationRoot} page.
     *
     * @param subtreeRoot The page for which to generate the sub-tree.
     * @return the list of collected navigation trees
     */
    private List<NavigationItem> getItems(@NotNull final Page subtreeRoot) {
        if (this.structureDepth < 0 || subtreeRoot.getDepth() - this.getNavigationRoot().getDepth() < this.structureDepth) {
            Iterator<Page> childIterator = subtreeRoot.listChildren(new PageFilter());
            return StreamSupport.stream(((Iterable<Page>) () -> childIterator).spliterator(), false)
                .map(item -> this.createNavigationItem(item, getItems(item)))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * Gets a stream of the top level of pages in the navigation.
     *
     * @param navigationRoot The navigation root page.
     * @param structureStart The number of levels under the root page to begin collecting pages.
     * @return Stream of all descendant pages of navigationRoot that are exactly structureStart levels deeper.
     */
    private Stream<Page> getRootItems(@NotNull final Page navigationRoot, final int structureStart) {
        if (structureStart < 1) {
            return Stream.of(navigationRoot);
        }
        Iterator<Page> childIterator = navigationRoot.listChildren(new PageFilter());
        return StreamSupport.stream(((Iterable<Page>) () -> childIterator).spliterator(), false)
            .flatMap(child -> getRootItems(child, structureStart - 1));
    }

    /**
     * Create a navigation item for the given page/children.
     *
     * @param page The page for which to create a navigation item.
     * @param children The child navigation items.
     * @return The newly created navigation item.
     */
    private NavigationItemImpl createNavigationItem(@NotNull final Page page, @NotNull final List<NavigationItem> children) {
        int level = page.getDepth() - (this.getNavigationRoot().getDepth() + structureStart);
        boolean selected = checkSelected(page);
        return new NavigationItemImpl(page, selected, request, level, children, getId(), isShadowingDisabled);
    }

    /**
     * Checks if the specified page is selected.
     * A page is selected if it is either:
     * <ul>
     *     <li>The current page; or,</li>
     *     <li>A page that redirects to the current page; or,</li>
     *     <li>The current page is a child of the specified page</li>
     * </ul>
     *
     * @param page The page to check.
     * @return True if the page is selected, false if not.
     */
    private boolean checkSelected(@NotNull final Page page) {
        return this.currentPage.equals(page)
            || this.currentPage.getPath().startsWith(page.getPath() + "/")
            || (!this.isShadowingDisabled && currentPageIsRedirectTarget(page));
    }

    /**
     * Checks if the specified page redirects to the current page.
     *
     * @param page The page to check.
     * @return True if the specified page redirects to the current page.
     */
    private boolean currentPageIsRedirectTarget(@NotNull final Page page) {
        return NavigationItemImpl.getRedirectTarget(page)
            .filter(target -> target.equals(currentPage))
            .isPresent();
    }

    /**
     * Gets the resource representation of the specified page.
     *
     * @param page The page to adapt to a resource.
     * @return the resource for the specified page, empty if the resource could not be resolved.
     */
    @NotNull
    final Optional<Resource> getPageResource(@NotNull final Page page) {
        return Optional.ofNullable(
            Optional.of(page)
                // get the parent of the content resource
                .map(Page::getContentResource)
                .map(Resource::getParent)
                // if content resource is missing, resolve resource at page path
                .orElseGet(() -> this.resource.getResourceResolver().getResource(page.getPath())));
    }

}

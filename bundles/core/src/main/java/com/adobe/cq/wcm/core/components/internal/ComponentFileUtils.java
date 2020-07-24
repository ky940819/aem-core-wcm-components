package com.adobe.cq.wcm.core.components.internal;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ComponentFileUtils {

    public static List<String> getPaths(Set<String> resourceTypes, @NotNull final Pattern pattern, boolean inherited, ResourceResolver resourceResolver) {
        List<String> paths = new LinkedList<>();

        Set<String> seenResourceTypes = new HashSet<>();
        for (String resourceType : resourceTypes) {
            addPaths(resourceType, paths, pattern, inherited, seenResourceTypes, resourceResolver);
        }
        return paths;
    }


    private static void addPaths(@Nullable final String resourceType,
                          @NotNull final Collection<String> paths,
                          @NotNull final Pattern pattern,
                          boolean inherited,
                          @NotNull final Set<String> seenResourceTypes,
                          @NotNull final ResourceResolver resourceResolver) {
        if (resourceType != null && !seenResourceTypes.contains(resourceType)) {
            Resource resource = resourceResolver.getResource(resourceType);
            if (resource != null) {
                boolean matched = false;
                for (Resource child : resource.getChildren()) {
                    if (pattern.matcher(child.getName()).matches()) {
                        paths.add(child.getPath());
                        matched = true;
                    }
                }
                if (inherited && !matched) {
                    addPaths(resource.getResourceSuperType(), paths, pattern, inherited, seenResourceTypes, resourceResolver);
                }
            }
        }
    }
}

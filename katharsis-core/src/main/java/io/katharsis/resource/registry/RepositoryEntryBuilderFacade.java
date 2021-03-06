package io.katharsis.resource.registry;

import io.katharsis.locator.JsonServiceLocator;
import io.katharsis.repository.NotFoundRepository;
import io.katharsis.repository.RepositoryInstanceBuilder;
import io.katharsis.resource.registry.repository.DirectResponseResourceEntry;
import io.katharsis.resource.registry.repository.ResourceEntry;
import io.katharsis.resource.registry.repository.ResponseRelationshipEntry;

import java.util.LinkedList;
import java.util.List;

/**
 * Contains a strategy to decide which implementation of an entry will be provided. Keep in mind that there can be a
 * case in which there will be two repositories of the same types.
 */
public class RepositoryEntryBuilderFacade implements RepositoryEntryBuilder {

    private final DirectRepositoryEntryBuilder directRepositoryEntryBuilder;
    private final AnnotatedRepositoryEntryBuilder annotatedRepositoryEntryBuilder;

    public RepositoryEntryBuilderFacade(JsonServiceLocator jsonServiceLocator) {
        this.directRepositoryEntryBuilder = new DirectRepositoryEntryBuilder(jsonServiceLocator);
        this.annotatedRepositoryEntryBuilder = new AnnotatedRepositoryEntryBuilder(jsonServiceLocator);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ResourceEntry<?, ?> buildResourceRepository(ResourceLookup lookup, final Class<?> resourceClass) {
    	ResourceEntry<?, ?> resourceEntry =  annotatedRepositoryEntryBuilder.buildResourceRepository(lookup, resourceClass);
        if (resourceEntry == null) {
            resourceEntry = directRepositoryEntryBuilder.buildResourceRepository(lookup, resourceClass);
        }
        if (resourceEntry == null) {
            RepositoryInstanceBuilder repositoryInstanceBuilder = new RepositoryInstanceBuilder<>(new JsonServiceLocator() {
                @Override
                public <T> T getInstance(Class<T> clazz) {
                    return (T) new NotFoundRepository<>(resourceClass);
                }
            }, NotFoundRepository.class);
            resourceEntry = new DirectResponseResourceEntry(repositoryInstanceBuilder);
        }

        return resourceEntry;
    }

    @Override
    public List<ResponseRelationshipEntry<?, ?>> buildRelationshipRepositories(ResourceLookup lookup, Class<?> resourceClass) {
        List<ResponseRelationshipEntry<?, ?>> annotationEntries = annotatedRepositoryEntryBuilder
            .buildRelationshipRepositories(lookup, resourceClass);
        List<ResponseRelationshipEntry<?, ?>> targetEntries = new LinkedList<>(annotationEntries);
        List<ResponseRelationshipEntry<?, ?>> directEntries = directRepositoryEntryBuilder
            .buildRelationshipRepositories(lookup, resourceClass);

        for (ResponseRelationshipEntry<?, ?> directEntry : directEntries) {
            if (!contains(targetEntries, directEntry)) {
                targetEntries.add(directEntry);
            }
        }

        return targetEntries;
    }

    private boolean contains(List<ResponseRelationshipEntry<?, ?>> targetEntries, ResponseRelationshipEntry<?, ?> directEntry) {
        boolean contains = false;
        for (ResponseRelationshipEntry<?, ?> targetEntry : targetEntries) {
            if (targetEntry.getTargetAffiliation().equals(directEntry.getTargetAffiliation())) {
                contains = true;
                break;
            }
        }
        return contains;
    }
}

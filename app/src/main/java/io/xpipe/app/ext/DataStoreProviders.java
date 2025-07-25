package io.xpipe.app.ext;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.JacksonMapper;
import io.xpipe.core.ModuleLayerLoader;

import com.fasterxml.jackson.databind.jsontype.NamedType;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class DataStoreProviders {

    private static List<DataStoreProvider> ALL;

    public static void init() {
        DataStoreProviders.getAll().forEach(dataStoreProvider -> {
            try {
                dataStoreProvider.init();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
            }
        });
    }

    public static void reset() {
        DataStoreProviders.getAll().forEach(dataStoreProvider -> {
            try {
                dataStoreProvider.reset();
            } catch (Exception e) {
                ErrorEventFactory.fromThrowable(e).omit().handle();
            }
        });
    }

    public static Optional<DataStoreProvider> byId(String id) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return ALL.stream().filter(d -> d.getId().equalsIgnoreCase(id)).findAny();
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataStoreProvider> Optional<T> byStoreIfPresent(DataStore store) {
        if (ALL == null) {
            throw new IllegalStateException("Not initialized");
        }

        return (Optional<T>) ALL.stream()
                .filter(d -> d.getStoreClasses().contains(store.getClass()))
                .findAny();
    }

    public static <T extends DataStoreProvider> T byStore(DataStore store) {
        return DataStoreProviders.<T>byStoreIfPresent(store)
                .orElseThrow(() -> new IllegalArgumentException("Unknown store class"));
    }

    public static List<DataStoreProvider> getAll() {
        return ALL;
    }

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            TrackEvent.info("Loading extension providers ...");
            ALL = ServiceLoader.load(layer, DataStoreProvider.class).stream()
                    .map(ServiceLoader.Provider::get)
                    .collect(Collectors.toList());
            ALL.removeIf(p -> {
                try {
                    p.validate();
                    return false;
                } catch (Throwable e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                    return true;
                }
            });

            for (DataStoreProvider p : getAll()) {
                JacksonMapper.configure(objectMapper -> {
                    for (Class<?> storeClass : p.getStoreClasses()) {
                        objectMapper.registerSubtypes(new NamedType(storeClass));
                    }
                });
            }
        }
    }
}

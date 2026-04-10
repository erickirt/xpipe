package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.hub.comp.StoreListChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public class MultiIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        MultiIdentityStore st = (MultiIdentityStore) store.getValue();

        var initialAllIdentities = new ArrayList<>(st.getIdentities());
        var initialAvailableIdentities = new ArrayList<>(st.getAvailableIdentities());
        var identities = new SimpleListProperty<>(FXCollections.observableArrayList(st.getAvailableIdentities()));
        var perUser = new SimpleBooleanProperty(st.isPerUser());

        return new OptionsBuilder()
                .nameAndDescription("multiIdentityList")
                .addComp(
                        new StoreListChoiceComp<>(
                                identities,
                                IdentityStore.class,
                                ref -> !(ref.getStore() instanceof MultiIdentityStore) && !identities.contains(ref),
                                StoreViewState.get().getAllIdentitiesCategory()),
                        identities)
                .nameAndDescription(
                        DataStorageUserHandler.getInstance().getActiveUser() != null
                                ? "identityPerUser"
                                : "identityPerUserDisabled")
                .addToggle(perUser)
                .disable(DataStorageUserHandler.getInstance().getActiveUser() == null)
                .bind(
                        () -> {
                            // User made no changes in GUI
                            if (identities.getValue().equals(st.getAvailableIdentities())) {
                                return MultiIdentityStore.builder()
                                        .identities(st.getIdentities())
                                        .perUser(perUser.get())
                                        .build();
                            }

                            var all = new ArrayList<UUID>();
                            // All currently selected ones are added
                            for (DataStoreEntryRef<IdentityStore> identity : identities) {
                                all.add(identity.get().getUuid());
                            }

                            // Include non-available ones
                            for (UUID storeIdentity : initialAllIdentities) {
                                var isAvailable = initialAvailableIdentities.stream()
                                        .anyMatch(ref -> ref.get().getUuid().equals(storeIdentity));
                                if (!isAvailable) {
                                    all.add(storeIdentity);
                                }
                            }

                            return MultiIdentityStore.builder()
                                    .identities(all)
                                    .perUser(perUser.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return MultiIdentityStore.builder().identities(new ArrayList<>()).build();
    }

    @Override
    public String getId() {
        return "multiIdentity";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(MultiIdentityStore.class);
    }
}

/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.components;

import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ImprovedRefreshTreeGrid<T extends ImprovedRefreshTreeGrid.TreeItem<T>>
        extends StateRetainingTreeGrid<ImprovedRefreshTreeGrid.WrappedItem<T>> {
    private List<WrappedItem<T>> items;
    private TreeData<WrappedItem<T>> treeData = new TreeData<>();
    private Supplier<List<T>> itemsSupplier;

    public ImprovedRefreshTreeGrid() {
        super();
    }

    public void improvedSetValues(Supplier<List<T>> itemsSupplier) {
        if (items == null) {
            this.itemsSupplier = itemsSupplier;
            buildTree();
            this.setDataProvider(new CustomTreeDataProvider<>(treeData));
        } else {
            this.itemsSupplier = itemsSupplier;
            improvedRefresh();
        }
    }

    private void buildTree() {
        treeData.clear();
        var items = itemsSupplier.get();
        treeData.addRootItems(items.stream()
                .filter(item -> item.getParent() == null)
                .map(this::wrap)
                .toList());
        items.stream().filter(item -> item.getParent() != null).forEach(this::addItemToTree);
        this.items = items.stream().map(this::wrap).toList();
    }

    public void improvedRefresh() {
        var refreshAll = new AtomicBoolean(false);
        var itemsToRefresh = new HashSet<WrappedItem<T>>();
        var newItems = itemsSupplier.get().stream().map(this::wrap).toList();
        compareLists(
                this.items,
                newItems,
                (oldItem, newItem) -> {
                    oldItem.set(newItem.get());
                    itemsToRefresh.add(oldItem);
                },
                (oldItem, newItem) -> {
                    buildTree();
                    refreshAll.set(true);
                },
                (newItem) -> {
                    buildTree();
                    refreshAll.set(true);
                },
                (removedItem) -> {
                    treeData.removeItem(removedItem);
                    refreshAll.set(true);
                });
        if (refreshAll.get()) {
            buildTree();
            this.getDataProvider().refreshAll();
        } else {
            itemsToRefresh.forEach(item -> this.getDataProvider().refreshItem(item, false));
        }
        this.items = newItems;
    }

    private WrappedItem<T> wrap(T item) {
        return WrappedItem.of(item);
    }

    private void addItemToTree(T item) {
        if (treeData.contains(wrap(item))) {
            return;
        }
        // TreeData requires that the parent is added before the child
        if (item.getParent() != null && !treeData.contains(wrap(item.getParent()))) {
            addItemToTree(item.getParent());
        }
        treeData.addItem(wrap(item.getParent()), wrap(item));
    }

    public abstract static class TreeItem<IT extends TreeItem<IT>> {
        /**
         * The parent of this item, or null if this item has no parent (and is a root item).
         */
        public abstract IT getParent();

        /**
         * return A unique identifier for this item. This method will be called frequently, so it should be fast.
         */
        public abstract String getId();

        /**
         * Returns true iff each field in this object is equal to the corresponding field in the other object.
         */
        public abstract boolean deepEquals(IT other);

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TreeItem<?> treeItem = (TreeItem<?>) o;
            return Objects.equals(getId(), treeItem.getId());
        }

        @Override
        public int hashCode() {
            return getId().hashCode();
        }
    }

    private void compareLists(
            List<WrappedItem<T>> oldList,
            List<WrappedItem<T>> newList,
            BiConsumer<WrappedItem<T>, WrappedItem<T>> updateFunction,
            BiConsumer<WrappedItem<T>, WrappedItem<T>> parentChangedFunction,
            Consumer<WrappedItem<T>> addFunction,
            Consumer<WrappedItem<T>> removeFunction) {

        Set<WrappedItem<T>> oldSet = new HashSet<>(oldList);
        Set<WrappedItem<T>> newSet = new HashSet<>(newList);
        oldList.stream().filter(item -> !newSet.contains(item)).forEach(removeFunction);
        newList.stream().filter(item -> !oldSet.contains(item)).forEach(addFunction);

        Map<String, WrappedItem<T>> newItemsMap = newList.stream()
                .collect(HashMap::new, (map, newItem) -> map.put(newItem.get().getId(), newItem), HashMap::putAll);
        oldList.forEach(oldItem -> {
            var newItem = newItemsMap.get(oldItem.get().getId());
            if (newItem == null) {
                return;
            }
            if ((oldItem.get().getParent() != null && newItem.get().getParent() == null)
                    || (oldItem.get().getParent() == null && newItem.get().getParent() != null)
                    || oldItem.get().getParent() != null
                            && newItem.get().getParent() != null
                            && !oldItem.get()
                                    .getParent()
                                    .getId()
                                    .equals(newItem.get().getParent().getId())) {
                parentChangedFunction.accept(oldItem, newItem);
            }
            if (!oldItem.get().deepEquals(newItem.get())) {
                updateFunction.accept(oldItem, newItem);
            }
        });
    }

    private static class CustomTreeDataProvider<Tx extends ImprovedRefreshTreeGrid.TreeItem<Tx>>
            extends TreeDataProvider<WrappedItem<Tx>> {

        /**
         * Constructs a new TreeDataProvider.
         * <p>
         * This data provider should be refreshed after making changes to the
         * underlying {@link TreeData} instance.
         *
         * @param treeData the backing {@link TreeData} for this provider, not
         *                 {@code null}
         */
        public CustomTreeDataProvider(TreeData<WrappedItem<Tx>> treeData) {
            super(treeData);
        }

        @Override
        public Object getId(WrappedItem<Tx> item) {
            return item.get().getId();
        }
    }

    /**
     * Unfortunately, the ItemWrapper class is necessary because the TreeGrid class does not allow replacing an
     * outdated item with a new item. This class is a wrapper around the actual item, and allows the item to be replaced
     * without rebuilding the TreeGrid.
     */
    public static class WrappedItem<T> {
        private T item;

        public static <T> WrappedItem<T> of(T item) {
            if (item == null) {
                return null;
            }
            return new WrappedItem<>(item);
        }

        private WrappedItem(@NonNull T item) {
            this.item = item;
        }

        public T get() {
            return item;
        }

        private void set(T item) {
            this.item = item;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null) {
                return false;
            }
            if (other instanceof WrappedItem tw) {
                return tw.item.equals(item);
            }
            return item.equals(other);
        }

        @Override
        public int hashCode() {
            var item = this.item;
            if (item == null) {
                return 0;
            }
            return item.hashCode();
        }
    }
}

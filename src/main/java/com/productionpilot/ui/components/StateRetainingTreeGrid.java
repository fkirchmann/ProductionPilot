/*
 * Copyright (c) 2022-2023 Felix Kirchmann.
 * Distributed under the MIT License (license terms are at http://opensource.org/licenses/MIT).
 */
package com.productionpilot.ui.components;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.treegrid.CollapseEvent;
import com.vaadin.flow.component.treegrid.ExpandEvent;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.selection.SelectionEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StateRetainingTreeGrid<T> extends TreeGrid<T> {
    public StateRetainingTreeGrid() {
        super();
        addCollapseListener(this::retainCollapsedState);
        addExpandListener(this::retainExpandedState);
        addSelectionListener(this::retainSelectionState);
    }

    private final Set<T> expandedItems = ConcurrentHashMap.newKeySet();
    private final Set<T> selectedItems = ConcurrentHashMap.newKeySet();

    @Override
    public void select(T item) {
        super.select(item);
        selectedItems.add(item);
    }

    @Override
    public void deselect(T item) {
        super.deselect(item);
        selectedItems.remove(item);
    }

    // When the items are refreshed,  setDataProvider(DataProvider<T, ?> dataProvider) is called, which calls
    // deselectAll()
    public void deselectAllAndRetain() {
        super.deselectAll();
        selectedItems.clear();
    }

    private void retainExpandedState(ExpandEvent<T, TreeGrid<T>> treeGridExpandEvent) {
        expandedItems.addAll(treeGridExpandEvent.getItems());
    }

    private void retainCollapsedState(CollapseEvent<T, TreeGrid<T>> treeGridCollapseEvent) {
        expandedItems.removeAll(treeGridCollapseEvent.getItems());
    }

    /**
     * Unlike expand / collapse, if we refresh this grid, we will get an onSelect event (with fromClient being false)
     * with no items selected. So we need to track the selected items ourselves.
     * To still allow the server to select items, we need to override the select / deselect methods.
     */
    private void retainSelectionState(SelectionEvent<Grid<T>, T> gridSelectionEvent) {
        if (gridSelectionEvent.isFromClient()) {
            selectedItems.clear();
            selectedItems.addAll(gridSelectionEvent.getAllSelectedItems());
        }
    }

    public void restoreState() {
        super.expand(new HashSet<>(expandedItems));
        new HashSet<>(selectedItems).forEach(super::select);
    }

    public void refreshWhileKeepingState() {
        getDataProvider().refreshAll();
        restoreState();
    }
}

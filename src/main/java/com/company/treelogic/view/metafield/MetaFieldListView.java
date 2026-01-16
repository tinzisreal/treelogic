package com.company.treelogic.view.metafield;

import com.company.treelogic.entity.MetaField;
import com.company.treelogic.view.main.MainView;
import com.vaadin.flow.data.selection.MultiSelectionEvent;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;

import java.util.*;
import java.util.stream.Collectors;

@Route(value = "meta-fields", layout = MainView.class)
@ViewController("MetaField.list")
@ViewDescriptor("meta-field-list-view.xml")
@LookupComponent("metaFieldsDataGrid")
@DialogMode(width = "64em")
public class MetaFieldListView extends StandardListView<MetaField> {

    @ViewComponent
    private TreeDataGrid<MetaField> metaFieldsDataGrid;

    @ViewComponent
    private CollectionContainer<MetaField> metaFieldsDc;

    private boolean isProgrammaticChange = false;

    @Subscribe("metaFieldsDataGrid")
    public void onMetaFieldsDataGridSelection(final SelectionEvent<TreeDataGrid<MetaField>, MetaField> event) {
        if (isProgrammaticChange || !event.isFromClient() || !(event instanceof MultiSelectionEvent)) {
            return;
        }

        MultiSelectionEvent<TreeDataGrid<MetaField>, MetaField> multiEvent =
                (MultiSelectionEvent<TreeDataGrid<MetaField>, MetaField>) event;

        processSelectionLogic(multiEvent);
    }

    private void processSelectionLogic(MultiSelectionEvent<TreeDataGrid<MetaField>, MetaField> event) {
        try {
            isProgrammaticChange = true;

            Set<MetaField> workingSelection = new HashSet<>(metaFieldsDataGrid.getSelectedItems());
            boolean isChanged = false;

            for (MetaField item : event.getAddedSelection()) {
                if (propagateSelectionDown(item, workingSelection)) isChanged = true;
                if (propagateSelectionUp(item, workingSelection)) isChanged = true;
            }

            for (MetaField item : event.getRemovedSelection()) {
                if (propagateDeselectionDown(item, workingSelection)) isChanged = true;
                if (propagateDeselectionUp(item, workingSelection)) isChanged = true;
            }

            if (isChanged) {
                metaFieldsDataGrid.asMultiSelect().setValue(workingSelection);
            }

        } finally {
            isProgrammaticChange = false;
        }
    }

    private boolean propagateSelectionDown(MetaField parent, Set<MetaField> selection) {
        boolean changed = false;
        for (MetaField child : getChildrenInContainer(parent)) {
            if (selection.add(child)) {
                changed = true;
            }
            if (propagateSelectionDown(child, selection)) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean propagateSelectionUp(MetaField child, Set<MetaField> selection) {
        MetaField parent = getActualParent(child);
        if (parent == null) return false;

        boolean changed = false;
        if (selection.add(parent)) {
            changed = true;
            if (propagateSelectionUp(parent, selection)) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean propagateDeselectionDown(MetaField parent, Set<MetaField> selection) {
        boolean changed = false;
        for (MetaField child : getChildrenInContainer(parent)) {
            if (selection.remove(child)) {
                changed = true;
            }
            if (propagateDeselectionDown(child, selection)) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean propagateDeselectionUp(MetaField child, Set<MetaField> selection) {
        MetaField parent = getActualParent(child);
        if (parent == null || !selection.contains(parent)) return false;

        boolean changed = false;

        boolean hasSelectedSiblings = getChildrenInContainer(parent).stream()
                .anyMatch(selection::contains);

        if (!hasSelectedSiblings) {
            selection.remove(parent);
            changed = true;
            if (propagateDeselectionUp(parent, selection)) {
                changed = true;
            }
        }
        return changed;
    }

    private List<MetaField> getChildrenInContainer(MetaField parent) {
        return metaFieldsDc.getItems().stream()
                .filter(item -> Objects.equals(parent, item.getParent()))
                .collect(Collectors.toList());
    }

    private MetaField getActualParent(MetaField child) {
        if (child.getParent() == null) return null;
        return metaFieldsDc.getItemOrNull(child.getParent().getId());
    }
}
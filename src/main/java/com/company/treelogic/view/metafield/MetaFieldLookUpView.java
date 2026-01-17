package com.company.treelogic.view.metafield;

import com.company.treelogic.entity.MetaField;
import com.company.treelogic.generic.TreeCheckboxHandler;
import com.company.treelogic.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "meta-fields-lookup", layout = MainView.class)
@ViewController(id = "MetaField.lookup")
@ViewDescriptor(path = "meta-field-lookup-view.xml")
@LookupComponent("metaFieldsDataGrid")
@DialogMode(width = "64em")
public class MetaFieldLookUpView extends StandardListView<MetaField> {

    @Autowired
    private UiComponents uiComponents;
    @ViewComponent
    private CollectionContainer<MetaField> metaFieldsDc;
    @ViewComponent
    private TreeDataGrid<MetaField> metaFieldsDataGrid;

    // Khai báo Handler
    private TreeCheckboxHandler<MetaField> treeCheckboxHandler;

    @Subscribe
    public void onInit(final InitEvent event) {
        // Khởi tạo handler 1 lần duy nhất
        treeCheckboxHandler = new TreeCheckboxHandler<>(
                metaFieldsDataGrid,
                metaFieldsDc,
                MetaField::getParent,    // Hàm lấy cha
                MetaField::getIncluded,  // Hàm lấy giá trị boolean
                MetaField::setIncluded   // Hàm set giá trị boolean
        );
    }

    @Supply(to = "metaFieldsDataGrid.included", subject = "renderer")
    private Renderer<MetaField> metaFieldsDataGridIncludedRenderer() {
        return new ComponentRenderer<>(item -> {
            if (item == null) return new Span();

            JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
            checkbox.setValue(Boolean.TRUE.equals(item.getIncluded()));

            checkbox.addValueChangeListener(event -> {
                // Gọi handler xử lý toàn bộ logic
                boolean isChecked = Boolean.TRUE.equals(event.getValue());
                treeCheckboxHandler.onItemCheckedChange(item, isChecked);
            });

            return checkbox;
        });
    }
}
package com.company.treelogic.view.metafield;

import com.company.treelogic.entity.MetaField;
import com.company.treelogic.generic.ContainerInMemoryFilter; // Import Generic Filter
import com.company.treelogic.generic.TreeCheckboxHandler;
import com.company.treelogic.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.component.button.JmixButton;
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

    // Inject nút bấm từ XML
    @ViewComponent
    private JmixButton onlyIncludedBtn;

    private TreeCheckboxHandler<MetaField> treeCheckboxHandler;

    // Khai báo Filter Generic
    private ContainerInMemoryFilter<MetaField> inMemoryFilter;

    @Subscribe
    public void onInit(final InitEvent event) {
        // 1. Setup Checkbox Tree Handler
        treeCheckboxHandler = new TreeCheckboxHandler<>(
                metaFieldsDataGrid, metaFieldsDc,
                MetaField::getParent, MetaField::getIncluded, MetaField::setIncluded
        );

        // 2. Setup Filter Generic (3 dòng code)
        inMemoryFilter = new ContainerInMemoryFilter<>(metaFieldsDc);

        inMemoryFilter.bindToggleButton(
                onlyIncludedBtn,                            // Nút bấm
                item -> Boolean.TRUE.equals(item.getIncluded()), // Điều kiện lọc
                "Show all",                                 // Text khi đang lọc
                "Only included"                             // Text khi chưa lọc
        );
    }

    @Supply(to = "metaFieldsDataGrid.included", subject = "renderer")
    private Renderer<MetaField> metaFieldsDataGridIncludedRenderer() {
        return new ComponentRenderer<>(item -> {
            if (item == null) return new Span();
            JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
            checkbox.setValue(Boolean.TRUE.equals(item.getIncluded()));

            checkbox.addValueChangeListener(event -> {
                boolean isChecked = Boolean.TRUE.equals(event.getValue());

                // 1. Cập nhật Logic cây (Cha/Con/Anh em)
                treeCheckboxHandler.onItemCheckedChange(item, isChecked);

                // 2. LOGIC MỚI: Nếu đang ở chế độ "Only included" -> Refresh ngay lập tức
                if (inMemoryFilter.isFiltered()) {
                    inMemoryFilter.refresh();
                }
            });
            return checkbox;
        });
    }
}
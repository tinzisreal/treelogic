package com.company.treelogic.view.metafield;

import com.company.treelogic.entity.MetaField;
import com.company.treelogic.generic.ContainerInMemoryFilter; // Import class Generic
import com.company.treelogic.generic.TreeCheckboxHandler;
import com.company.treelogic.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.checkbox.JmixCheckbox;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.action.BaseAction; // Import BaseAction
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.button.JmixButtonActionSupport;
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

    // Inject nút từ XML để set Action
    @ViewComponent
    private JmixButton onlyIncludedBtn;

    // Khai báo các Handler Generic
    private TreeCheckboxHandler<MetaField> treeCheckboxHandler;
    private ContainerInMemoryFilter<MetaField> inMemoryFilter;

    @Subscribe
    public void onInit(final InitEvent event) {
        // 1. Init Checkbox Handler
        treeCheckboxHandler = new TreeCheckboxHandler<>(
                metaFieldsDataGrid, metaFieldsDc,
                MetaField::getParent, MetaField::getIncluded, MetaField::setIncluded
        );

        inMemoryFilter = new ContainerInMemoryFilter<>(metaFieldsDc);

        BaseAction filterAction = new BaseAction("toggleFilter")
                .withText("Only included")
                .withIcon(VaadinIcon.FILTER.create())
                .withHandler(e -> {
                    // Gọi vào Generic Handler
                    boolean isNowFiltered = inMemoryFilter.toggle(
                            item -> Boolean.TRUE.equals(item.getIncluded())
                    );

                    // Cập nhật UI của Action (Text/Icon)
                    if (isNowFiltered) {
                        e.getSource().setText("Show all");
                        e.getSource().setIcon(VaadinIcon.CLOSE_CIRCLE.create());
                    } else {
                        e.getSource().setText("Only included");
                        e.getSource().setIcon(VaadinIcon.FILTER.create());
                    }

                    // Refresh grid UI
                    metaFieldsDataGrid.getDataProvider().refreshAll();
                });

        onlyIncludedBtn.setAction(filterAction);
    }

    @Supply(to = "metaFieldsDataGrid.included", subject = "renderer")
    private Renderer<MetaField> metaFieldsDataGridIncludedRenderer() {
        return new ComponentRenderer<>(item -> {
            if (item == null) return new Span();
            JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
            checkbox.setValue(Boolean.TRUE.equals(item.getIncluded()));

            checkbox.addValueChangeListener(event -> {
                boolean isChecked = Boolean.TRUE.equals(event.getValue());

                // 1. Logic cây
                treeCheckboxHandler.onItemCheckedChange(item, isChecked);

                // 2. Logic Filter (Nếu đang lọc thì refresh ngay)
                if (inMemoryFilter.isFiltered()) {
                    inMemoryFilter.refresh();
                }
            });
            return checkbox;
        });
    }
}
package com.company.treelogic.view.metafield;

import com.company.treelogic.entity.MetaField;
import com.company.treelogic.generic.TreeCheckboxHandler;
import com.company.treelogic.view.main.MainView;
import com.vaadin.flow.component.ClickEvent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    // 1. Biến lưu trữ trạng thái lọc
    private boolean isFilteringIncluded = false;

    // 2. Biến backup danh sách đầy đủ (để khi Show All không bị mất dữ liệu)
    private List<MetaField> allMetaFieldsBackup = null;
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
                boolean isChecked = Boolean.TRUE.equals(event.getValue());
                treeCheckboxHandler.onItemCheckedChange(item, isChecked);
            });

            return checkbox;
        });
    }

    @ViewComponent
    private JmixButton onlyIncludedBtn;

    @Subscribe(id = "onlyIncludedBtn", subject = "clickListener")
    public void onOnlyIncludedBtnClick(final ClickEvent<JmixButton> event) {
        if (!isFilteringIncluded) {
            // ---> CHUYỂN SANG CHẾ ĐỘ: ONLY INCLUDED

            // A. Backup dữ liệu gốc hiện tại (đang chứa cả true/false/null)
            // Phải copy ra ArrayList mới, vì getItems() trả về list unmodifiable hoặc live list
            if (allMetaFieldsBackup == null) {
                allMetaFieldsBackup = new ArrayList<>(metaFieldsDc.getItems());
            } else {
                // Update lại backup nếu user có load thêm trang (ít khi xảy ra với tree load all)
                // Nhưng an toàn nhất là lấy snapshot hiện tại
                allMetaFieldsBackup = new ArrayList<>(metaFieldsDc.getItems());
            }

            // B. Lọc lấy những thằng có included = true
            List<MetaField> filteredList = allMetaFieldsBackup.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getIncluded()))
                    .collect(Collectors.toList());

            // C. Set lại vào container (Làm UI thay đổi, không reload DB)
            metaFieldsDc.setItems(filteredList);

            // D. Đổi text nút
            event.getSource().setText("Show all");
            isFilteringIncluded = true;

        } else {
            // ---> CHUYỂN SANG CHẾ ĐỘ: SHOW ALL

            // A. Khôi phục lại danh sách gốc từ backup
            if (allMetaFieldsBackup != null) {
                metaFieldsDc.setItems(allMetaFieldsBackup);
            }

            // B. Reset backup (để lần sau backup mới nhất)
            allMetaFieldsBackup = null;

            // C. Đổi text nút
            event.getSource().setText("Only included");
            isFilteringIncluded = false;
        }

        // Refresh grid để chắc chắn UI vẽ đúng
        metaFieldsDataGrid.getDataProvider().refreshAll();
    }
}

    

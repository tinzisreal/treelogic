package com.company.treelogic.view.metafield;

import com.company.treelogic.entity.MetaField;
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

import java.util.List;
import java.util.Objects;
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

    @Supply(to = "metaFieldsDataGrid.included", subject = "renderer")
    private Renderer<MetaField> metaFieldsDataGridIncludedRenderer() {
        return new ComponentRenderer<>(item -> {
            // FIX TỪ FORUM: Nếu item null hoặc điều kiện không thỏa,
            // trả về Span rỗng thay vì NULL để tránh NullPointerException.
            if (item == null) {
                return new Span();
            }

            JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
            // Xử lý null-safe cho giá trị boolean
            checkbox.setValue(Boolean.TRUE.equals(item.getIncluded()));

            checkbox.addValueChangeListener(event -> {
                // 1. Cập nhật giá trị cho item hiện tại
                Boolean isChecked = Boolean.TRUE.equals(event.getValue());
                item.setIncluded(isChecked);

                // 2. Chạy logic đệ quy
                if (isChecked) {
                    // Case A: Tích chọn
                    updateChildrenRecursive(item, true); // Cha -> Con
                    updateAncestorsCheck(item);          // Con -> Cha
                } else {
                    // Case B: Bỏ chọn
                    updateChildrenRecursive(item, false); // Cha -> Con
                    updateAncestorsUncheck(item);         // Con -> Cha (có điều kiện)
                }

                // 3. QUAN TRỌNG: Refresh Grid để hiển thị các dòng bị ảnh hưởng
                metaFieldsDataGrid.getDataProvider().refreshAll();
            });

            return checkbox;
        });
    }

    // =========================================================================
    // LOGIC ĐỆ QUY (RECURSIVE LOGIC)
    // =========================================================================

    /**
     * Logic 1 (Xuống): Cập nhật tất cả con cháu theo giá trị của cha.
     */
    private void updateChildrenRecursive(MetaField parent, boolean value) {
        List<MetaField> children = getChildrenInContainer(parent);
        for (MetaField child : children) {
            // Chỉ cập nhật nếu giá trị khác nhau để tối ưu
            if (!Objects.equals(child.getIncluded(), value)) {
                child.setIncluded(value);
                // Tiếp tục đệ quy xuống dưới
                updateChildrenRecursive(child, value);
            }
        }
    }

    /**
     * Logic 2 (Lên - Tích): Tích con -> Tự động tích Cha (và Ông, Cụ...).
     */
    private void updateAncestorsCheck(MetaField child) {
        MetaField parent = getActualParent(child);
        if (parent != null) {
            // Nếu cha chưa tích -> Tích cha và đi tiếp lên trên
            if (!Boolean.TRUE.equals(parent.getIncluded())) {
                parent.setIncluded(true);
                updateAncestorsCheck(parent);
            }
        }
    }

    /**
     * Logic 3 (Lên - Bỏ tích): Bỏ con -> Chỉ bỏ Cha nếu TẤT CẢ anh em đều không được chọn.
     */
    private void updateAncestorsUncheck(MetaField child) {
        MetaField parent = getActualParent(child);
        if (parent != null) {
            // Kiểm tra xem cha có nên bị bỏ tích không?
            if (Boolean.TRUE.equals(parent.getIncluded())) {

                boolean hasAnySiblingSelected = getChildrenInContainer(parent).stream()
                        .anyMatch(sibling -> Boolean.TRUE.equals(sibling.getIncluded()));

                // Nếu không còn đứa con nào được chọn -> Bỏ cha
                if (!hasAnySiblingSelected) {
                    parent.setIncluded(false);
                    // Tiếp tục kiểm tra lên trên
                    updateAncestorsUncheck(parent);
                }
            }
        }
    }

    // =========================================================================
    // HELPER METHODS (DATA ACCESS)
    // =========================================================================

    /**
     * Lấy danh sách con trực tiếp từ Container.
     */
    private List<MetaField> getChildrenInContainer(MetaField parent) {
        return metaFieldsDc.getItems().stream()
                .filter(item -> Objects.equals(parent, item.getParent()))
                .collect(Collectors.toList());
    }

    /**
     * Lấy đối tượng Cha thực sự đang nằm trong Container (Tránh lỗi Proxy/Clone).
     */
    private MetaField getActualParent(MetaField child) {
        if (child.getParent() == null) return null;
        return metaFieldsDc.getItemOrNull(child.getParent().getId());
    }
}
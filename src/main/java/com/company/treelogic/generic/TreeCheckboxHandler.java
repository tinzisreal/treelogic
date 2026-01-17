package com.company.treelogic.generic;

import io.jmix.core.entity.EntityValues;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.model.CollectionContainer;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Generic Handler xử lý logic Checkbox đệ quy trên TreeDataGrid (cho cột Boolean tùy chỉnh).
 * <p>
 * <strong>Logic hoạt động:</strong>
 * <ul>
 *   <li>Tích cha &rarr; Tự động tích tất cả con cháu.</li>
 *   <li>Tích con &rarr; Tự động tích ngược lên cha, ông (đánh dấu nhánh được chọn).</li>
 *   <li>Bỏ tích cha &rarr; Tự động bỏ tích tất cả con cháu.</li>
 *   <li>Bỏ tích con &rarr; Chỉ bỏ tích cha nếu <b>TẤT CẢ</b> anh em khác đều không được chọn.</li>
 * </ul>
 *
 * <strong>Cách sử dụng:</strong>
 * <pre>{@code
 * // 1. Khai báo biến trong Controller
 * private TreeCheckboxHandler<MetaField> treeCheckboxHandler;
 *
 * // 2. Khởi tạo trong onInit
 * @Subscribe
 * public void onInit(final InitEvent event) {
 *     treeCheckboxHandler = new TreeCheckboxHandler<>(
 *         metaFieldsDataGrid,      // Grid hiển thị
 *         metaFieldsDc,            // Container dữ liệu
 *         MetaField::getParent,    // Hàm lấy cha
 *         MetaField::getIncluded,  // Hàm lấy giá trị boolean
 *         MetaField::setIncluded   // Hàm set giá trị boolean
 *     );
 * }
 *
 * // 3. Gọi trong Renderer (cho cột checkbox)
 * @Supply(to = "metaFieldsDataGrid.included", subject = "renderer")
 * private Renderer<MetaField> includedRenderer() {
 *     return new ComponentRenderer<>(item -> {
 *         JmixCheckbox checkbox = uiComponents.create(JmixCheckbox.class);
 *         checkbox.setValue(Boolean.TRUE.equals(item.getIncluded()));
 *
 *         checkbox.addValueChangeListener(e -> {
 *             // GỌI HÀM NÀY ĐỂ XỬ LÝ LOGIC:
 *             boolean isChecked = Boolean.TRUE.equals(e.getValue());
 *             treeCheckboxHandler.onItemCheckedChange(item, isChecked);
 *         });
 *         return checkbox;
 *     });
 * }
 * }</pre>
 *
 * @param <T> Kiểu Entity (Ví dụ: MetaField)
 */
public class TreeCheckboxHandler<T> {

    private final TreeDataGrid<T> treeDataGrid;
    private final CollectionContainer<T> container;
    private final Function<T, T> parentGetter;
    private final Function<T, Boolean> checkedGetter;
    private final BiConsumer<T, Boolean> checkedSetter;

    /**
     * Constructor
     *
     * @param treeDataGrid  Grid hiển thị (để refresh UI)
     * @param container     Data Container chứa dữ liệu
     * @param parentGetter  Hàm lấy parent (VD: Entity::getParent)
     * @param checkedGetter Hàm lấy giá trị boolean (VD: Entity::getIncluded)
     * @param checkedSetter Hàm set giá trị boolean (VD: Entity::setIncluded)
     */
    public TreeCheckboxHandler(TreeDataGrid<T> treeDataGrid,
                               CollectionContainer<T> container,
                               Function<T, T> parentGetter,
                               Function<T, Boolean> checkedGetter,
                               BiConsumer<T, Boolean> checkedSetter) {
        this.treeDataGrid = treeDataGrid;
        this.container = container;
        this.parentGetter = parentGetter;
        this.checkedGetter = checkedGetter;
        this.checkedSetter = checkedSetter;
    }

    /**
     * Hàm Entry Point: Gọi hàm này khi người dùng tick vào checkbox
     *
     * @param item  Item vừa được thao tác
     * @param value Giá trị mới (true/false)
     */
    public void onItemCheckedChange(T item, boolean value) {
        // 1. Cập nhật giá trị cho item hiện tại
        checkedSetter.accept(item, value);

        // 2. Chạy logic đệ quy
        if (value) {
            // Case A: Tích chọn
            propagateDown(item, true); // Cha -> Con
            propagateUpCheck(item);    // Con -> Cha
        } else {
            // Case B: Bỏ chọn
            propagateDown(item, false); // Cha -> Con
            propagateUpUncheck(item);   // Con -> Cha (có điều kiện)
        }

        // 3. Refresh Grid để UI cập nhật các dòng bị ảnh hưởng
        treeDataGrid.getDataProvider().refreshAll();
    }

    // =========================================================================
    // LOGIC ĐỆ QUY (INTERNAL)
    // =========================================================================

    private void propagateDown(T parent, boolean value) {
        List<T> children = getChildrenInContainer(parent);
        for (T child : children) {
            // Chỉ update nếu giá trị khác nhau (tối ưu)
            if (!Objects.equals(checkedGetter.apply(child), value)) {
                checkedSetter.accept(child, value);
                // Tiếp tục đệ quy
                propagateDown(child, value);
            }
        }
    }

    private void propagateUpCheck(T child) {
        T parent = getActualParent(child);
        if (parent != null) {
            // Nếu cha chưa tích -> Tích cha và đi tiếp lên trên
            if (!Boolean.TRUE.equals(checkedGetter.apply(parent))) {
                checkedSetter.accept(parent, true);
                propagateUpCheck(parent);
            }
        }
    }

    private void propagateUpUncheck(T child) {
        T parent = getActualParent(child);
        if (parent != null) {
            // Kiểm tra xem cha có đang được tích không
            if (Boolean.TRUE.equals(checkedGetter.apply(parent))) {

                // Kiểm tra xem còn anh em nào được chọn không?
                boolean hasSelectedSiblings = getChildrenInContainer(parent).stream()
                        .anyMatch(sibling -> Boolean.TRUE.equals(checkedGetter.apply(sibling)));

                // Nếu KHÔNG còn ai -> Bỏ tích cha
                if (!hasSelectedSiblings) {
                    checkedSetter.accept(parent, false);
                    // Tiếp tục kiểm tra lên trên
                    propagateUpUncheck(parent);
                }
            }
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private List<T> getChildrenInContainer(T parent) {
        return container.getItems().stream()
                .filter(item -> Objects.equals(parent, parentGetter.apply(item)))
                .collect(Collectors.toList());
    }

    private T getActualParent(T child) {
        T parentRef = parentGetter.apply(child);
        if (parentRef == null) {
            return null;
        }
        // Dùng EntityValues để lấy ID an toàn cho mọi Jmix Entity
        Object id = EntityValues.getId(parentRef);
        return container.getItemOrNull(id);
    }
}
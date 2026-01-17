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
 * Generic handler xử lý logic Checkbox đệ quy trên TreeDataGrid (Boolean Column).
 * <p>
 * Logic hoạt động:
 * - Tích cha: tự động tích tất cả con cháu.
 * - Tích con: tự động tích ngược lên cha, ông.
 * - Bỏ tích cha: tự động bỏ tích tất cả con cháu.
 * - Bỏ tích con: chỉ bỏ tích cha nếu TẤT CẢ anh em khác đều không được chọn.
 * <p>
 * Sử dụng trong Renderer:
 * <pre>{@code
 * TreeCheckboxHandler<MetaField> handler = new TreeCheckboxHandler<>(
 *     treeDataGrid,
 *     metaFieldsDc,
 *     MetaField::getParent,
 *     MetaField::getIncluded,
 *     MetaField::setIncluded
 * );
 *
 * // Trong ValueChangeListener của Checkbox:
 * handler.onItemCheckedChange(item, event.getValue());
 * }</pre>
 *
 * @param <T> Entity type
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
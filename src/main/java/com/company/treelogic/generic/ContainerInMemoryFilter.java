package com.company.treelogic.generic;

import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
/**
 * Helper class hỗ trợ lọc dữ liệu trực tiếp trên RAM (In-Memory) cho CollectionContainer.
 * <p>
 * Thường được sử dụng cho các thuộc tính <b>@Transient</b> (không lưu DB) nên không thể dùng
 * Data Loader (JPQL) để lọc.
 *
 * <strong>Cách sử dụng:</strong>
 * <pre>{@code
 * // 1. Khai báo biến trong Controller
 * private ContainerInMemoryFilter<MetaField> inMemoryFilter;
 *
 * // 2. Khởi tạo trong onInit
 * @Subscribe
 * public void onInit(final InitEvent event) {
 *     inMemoryFilter = new ContainerInMemoryFilter<>(metaFieldsDc);
 *
 *     // Gắn vào nút bấm (Tự động đổi text Show all / Only included)
 *     inMemoryFilter.bindToggleButton(
 *         onlyIncludedBtn,                                  // Button component
 *         item -> Boolean.TRUE.equals(item.getIncluded()),  // Điều kiện lọc
 *         "Show all",                                       // Text khi đang lọc
 *         "Only included"                                   // Text khi chưa lọc
 *     );
 * }
 *
 * // 3. (Tuỳ chọn) Gọi khi dữ liệu thay đổi để list tự biến mất dòng không thỏa mãn
 * // Ví dụ: gọi trong sự kiện thay đổi checkbox
 * if (inMemoryFilter.isFiltered()) {
 *     inMemoryFilter.refresh();
 * }
 * }</pre>
 *
 * @param <T> Kiểu Entity trong Container
 */
public class ContainerInMemoryFilter<T> {

    private final CollectionContainer<T> container;
    private List<T> backupList = null;
    private boolean isFiltered = false;

    // Thêm biến này để lưu điều kiện lọc gần nhất
    private Predicate<T> lastCriteria;

    public ContainerInMemoryFilter(CollectionContainer<T> container) {
        this.container = container;
    }

    public void toggleFilter(Predicate<T> criteria) {
        if (isFiltered) {
            restoreData();
        } else {
            filterData(criteria);
        }
    }

    /**
     * HÀM MỚI: Gọi hàm này khi dữ liệu thay đổi và muốn danh sách lọc cập nhật ngay lập tức.
     */
    public void refresh() {
        if (isFiltered && lastCriteria != null) {
            // Lọc lại dựa trên backup list (vì các object trong backup list được cập nhật theo tham chiếu)
            List<T> filteredItems = backupList.stream()
                    .filter(lastCriteria)
                    .collect(Collectors.toList());
            container.setItems(filteredItems);
        }
    }

    public void bindToggleButton(JmixButton button, Predicate<T> criteria, String textWhenFilterOn, String textWhenFilterOff) {
        button.setText(isFiltered ? textWhenFilterOn : textWhenFilterOff);
        button.addClickListener(event -> {
            toggleFilter(criteria);
            button.setText(isFiltered ? textWhenFilterOn : textWhenFilterOff);
        });
    }

    private void filterData(Predicate<T> criteria) {
        this.lastCriteria = criteria; // Lưu lại điều kiện

        if (backupList == null) {
            backupList = new ArrayList<>(container.getItems());
        } else {
            backupList = new ArrayList<>(container.getItems());
        }

        List<T> filteredItems = backupList.stream()
                .filter(criteria)
                .collect(Collectors.toList());

        container.setItems(filteredItems);
        isFiltered = true;
    }

    private void restoreData() {
        if (backupList != null) {
            container.setItems(backupList);
        }
        backupList = null;
        isFiltered = false;
        // Không xóa lastCriteria để có thể re-apply nếu muốn
    }

    public boolean isFiltered() {
        return isFiltered;
    }
}
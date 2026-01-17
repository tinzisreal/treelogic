package com.company.treelogic.generic;

import io.jmix.flowui.model.CollectionContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic Utility hỗ trợ lọc dữ liệu trực tiếp trên RAM (In-Memory) cho CollectionContainer.
 * <p>
 * Class này hoạt động theo nguyên lý:
 * <ol>
 *     <li><b>Backup:</b> Khi bắt đầu lọc, nó chụp lại snapshot toàn bộ dữ liệu hiện có.</li>
 *     <li><b>Filter:</b> Nó lọc dữ liệu và set lại vào Container (làm UI thay đổi).</li>
 *     <li><b>Refresh:</b> Nếu dữ liệu gốc thay đổi, nó lọc lại dựa trên snapshot.</li>
 *     <li><b>Reset:</b> Khi tắt lọc, nó trả lại dữ liệu gốc từ snapshot.</li>
 * </ol>
 * <p>
 * Thường dùng cho các trường <b>@Transient</b> (không thể query DB) hoặc lọc tạm thời.
 *
 * <h2>HƯỚNG DẪN SỬ DỤNG:</h2>
 * <pre>{@code
 * // 1. Khai báo trong Controller
 * private ContainerInMemoryFilter<MetaField> inMemoryFilter;
 *
 * // 2. Khởi tạo trong onInit
 * @Subscribe
 * public void onInit(final InitEvent event) {
 *     inMemoryFilter = new ContainerInMemoryFilter<>(metaFieldsDc);
 *
 *     // Tạo Action cho nút bấm
 *     BaseAction filterAction = new BaseAction("toggleFilter")
 *         .withText("Only included")
 *         .withIcon(VaadinIcon.FILTER.create())
 *         .withHandler(e -> {
 *             // -- GỌI LOGIC LỌC TẠI ĐÂY --
 *             boolean isFiltered = inMemoryFilter.toggle(
 *                 item -> Boolean.TRUE.equals(item.getIncluded()) // Điều kiện lọc
 *             );
 *
 *             // Cập nhật giao diện nút bấm
 *             if (isFiltered) {
 *                 e.getSource().setText("Show all");
 *                 e.getSource().setIcon(VaadinIcon.CLOSE.create());
 *             } else {
 *                 e.getSource().setText("Only included");
 *                 e.getSource().setIcon(VaadinIcon.FILTER.create());
 *             }
 *         });
 *
 *     onlyIncludedBtn.setAction(filterAction);
 * }
 *
 * // 3. (Tuỳ chọn) Refresh ngay lập tức khi data thay đổi (VD: Bỏ tick checkbox)
 * checkbox.addValueChangeListener(e -> {
 *      // ... logic update data ...
 *      if (inMemoryFilter.isFiltered()) {
 *          inMemoryFilter.refresh(); // Gọi hàm này để dòng vừa bỏ tick biến mất ngay
 *      }
 * });
 * }</pre>
 *
 * @param <T> Kiểu Entity
 */
public class ContainerInMemoryFilter<T> {

    private final CollectionContainer<T> container;
    private List<T> snapshotList = null; // Backup dữ liệu gốc
    private Predicate<T> activePredicate = null; // Điều kiện lọc đang áp dụng

    /**
     * @param container Container dữ liệu cần lọc
     */
    public ContainerInMemoryFilter(CollectionContainer<T> container) {
        this.container = container;
    }

    /**
     * Bật bộ lọc.
     * @param criteria Điều kiện lọc (VD: item -> item.getStatus() == ACTIVE)
     */
    public void filter(Predicate<T> criteria) {
        this.activePredicate = criteria;

        // 1. Tạo snapshot nếu chưa có (lần lọc đầu tiên)
        if (snapshotList == null) {
            snapshotList = new ArrayList<>(container.getItems());
        }

        // 2. Thực hiện lọc
        applyFilterInternal();
    }

    /**
     * Tắt bộ lọc, khôi phục dữ liệu gốc.
     */
    public void reset() {
        if (snapshotList != null) {
            container.setItems(snapshotList);
            snapshotList = null; // Xóa snapshot để giải phóng bộ nhớ
        }
        activePredicate = null;
    }

    /**
     * Toggle: Nếu đang tắt thì bật, đang bật thì tắt.
     * @return true nếu kết quả là ĐANG LỌC, false nếu là RESET.
     */
    public boolean toggle(Predicate<T> criteria) {
        if (isFiltered()) {
            reset();
            return false;
        } else {
            filter(criteria);
            return true;
        }
    }

    /**
     * Làm mới kết quả lọc dựa trên dữ liệu hiện tại trong RAM.
     * (Dùng khi thuộc tính của item thay đổi và muốn danh sách update ngay lập tức).
     */
    public void refresh() {
        if (isFiltered()) {
            applyFilterInternal();
        }
    }

    /**
     * Kiểm tra xem có đang ở chế độ lọc không.
     */
    public boolean isFiltered() {
        return snapshotList != null;
    }

    // --- INTERNAL ---

    private void applyFilterInternal() {
        if (snapshotList != null && activePredicate != null) {
            List<T> filteredResults = snapshotList.stream()
                    .filter(activePredicate)
                    .collect(Collectors.toList());
            container.setItems(filteredResults);
        }
    }
}
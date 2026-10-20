package web;

import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import javax.swing.*;

/**
 * Inline basket. Prices are only a preview; CREATE_ORDER revalidates on the
 * server.
 */
final class FoodOrderPanel extends JPanel {

    private final JPanel list = new JPanel();
    private final Map<String, Integer> quantities = new LinkedHashMap<>();
    private final Map<String, BigDecimal> prices = new LinkedHashMap<>();
    private final Map<String, String> names = new LinkedHashMap<>();
    private final Runnable changed;
    private boolean loaded;

    FoodOrderPanel(Runnable changed) {
        super(new BorderLayout(0, 12));
        this.changed = changed;
        setOpaque(false);
        setPreferredSize(new Dimension(252, 240));
        JLabel title = new JLabel("BẮP NƯỚC & ĐỒ ĂN");
        title.setFont(UiTheme.font(14, Font.BOLD));
        title.setForeground(UiTheme.PRIMARY);
        add(title, BorderLayout.NORTH);
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        add(UiTheme.scroll(list), BorderLayout.CENTER);
        reset();
    }

    void reset() {
        loaded = false;
        quantities.clear();
        prices.clear();
        names.clear();
        list.removeAll();
        list.add(new JLabel("Chọn suất chiếu để tải danh mục."));
        revalidate();
        repaint();
    }

    boolean loaded() {
        return loaded;
    }

    void products(List<String[]> products) {
        loaded = true;
        quantities.clear();
        prices.clear();
        names.clear();
        list.removeAll();
        for (String[] p : products) {
            String id = p[0];
            quantities.put(id, 0);
            prices.put(id, new BigDecimal(p[3]));
            names.put(id, p[1]);
            UiTheme.RoundedPanel card = UiTheme.surface();
            card.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            card.setLayout(new BorderLayout(0, 8));
            JTextArea name = new JTextArea(p[1]);
            name.setEditable(false);
            name.setFocusable(false);
            name.setOpaque(false);
            name.setLineWrap(true);
            name.setWrapStyleWord(true);
            name.setFont(UiTheme.font(13, Font.BOLD));
            name.setRows(2);
            card.add(name, BorderLayout.NORTH);
            JPanel bottom = new JPanel(new BorderLayout(4, 0));
            bottom.setOpaque(false);
            JLabel price = new JLabel(BasicClientUI.formatMoney(p[3]));
            price.setForeground(UiTheme.PRIMARY);
            bottom.add(price, BorderLayout.WEST);
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
            controls.setOpaque(false);
            JButton minus = UiTheme.subtle("−"), plus = UiTheme.secondary("+");
            minus.setPreferredSize(new Dimension(28, 28));
            plus.setPreferredSize(new Dimension(28, 28));
            minus.setBorder(BorderFactory.createEmptyBorder());
            plus.setBorder(BorderFactory.createEmptyBorder());
            JLabel value = new JLabel("0", SwingConstants.CENTER);
            value.setPreferredSize(new Dimension(22, 28));
            minus.setEnabled(false);
            minus.getAccessibleContext().setAccessibleName("Giảm " + p[1]);
            plus.getAccessibleContext().setAccessibleName("Tăng " + p[1]);
            java.util.function.IntConsumer update = delta -> {
                int count = Math.max(0, Math.min(20, quantities.get(id) + delta));
                quantities.put(id, count);
                value.setText(String.valueOf(count));
                minus.setEnabled(count > 0);
                plus.setEnabled(count < 20);
                changed.run();
            };
            minus.addActionListener(e -> update.accept(-1));
            plus.addActionListener(e -> update.accept(1));
            controls.add(minus);
            controls.add(value);
            controls.add(plus);
            bottom.add(controls, BorderLayout.EAST);
            card.add(bottom, BorderLayout.CENTER);
            card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            card.setAlignmentX(LEFT_ALIGNMENT);
            list.add(card);
            list.add(Box.createVerticalStrut(10));
        }
        if (products.isEmpty()) {
            list.add(new JLabel("Hiện chưa có sản phẩm bán kèm."));
        }
        list.add(Box.createVerticalGlue());
        revalidate();
        repaint();
        changed.run();
    }

    BigDecimal total() {
        BigDecimal total = BigDecimal.ZERO;
        for (String id : quantities.keySet()) {
            total = total.add(prices.get(id).multiply(BigDecimal.valueOf(quantities.get(id))));
        }
        return total;
    }

    String items() {
        List<String> items = new ArrayList<>();
        quantities.forEach((id, count) -> {
            if (count > 0) {
                items.add(id + ":" + count);

            }
        });
        return String.join(",", items);
    }

    String summary() {
        List<String> items = new ArrayList<>();
        quantities.forEach((id, count) -> {
            if (count > 0) {
                items.add(names.get(id) + " × " + count);

            }
        });
        return items.isEmpty() ? "Không chọn đồ ăn/nước uống" : String.join("\n", items);
    }
}

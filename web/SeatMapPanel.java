package web;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/** Bố cục theo mã ghế thực: A1..A99, B1..B99. Lối đi ở giữa chỉ là khoảng hiển thị. */
public final class SeatMapPanel extends JPanel {
    public SeatMapPanel() {
        super(new GridBagLayout());
        setBackground(UiTheme.WHITE);
    }

    List<UiTheme.SeatButton> render(List<String[]> seats, Set<String> selected, Runnable changed) {
        removeAll();
        Map<Character,SortedMap<Integer,Boolean>> rows = new TreeMap<>();
        int columns=0;
        for (String[] seat : seats) {
            String code=seat[0];
            if (!code.matches("[A-Z][1-9][0-9]?")) continue;
            int column=Integer.parseInt(code.substring(1));
            rows.computeIfAbsent(code.charAt(0), key -> new TreeMap<>()).put(column,"taken".equals(seat[1]));
            columns=Math.max(columns,column);
        }
        List<UiTheme.SeatButton> available = new ArrayList<>();
        if (rows.isEmpty()) {
            add(new JLabel("Suất chiếu không có ghế hoặc đã bị xóa"));
        } else {
            int aisleAfter=columns>=4?columns/2:columns;
            int span=columns+3+(aisleAfter<columns?1:0);
            JLabel screen=new JLabel("MÀN CHIẾU",SwingConstants.CENTER);
            screen.setOpaque(true);
            screen.setBackground(UiTheme.PRIMARY_PALE);
            screen.setForeground(UiTheme.PRIMARY);
            screen.setBorder(new EmptyBorder(9,15,9,15));
            GridBagConstraints top=cell(0,0);
            top.gridwidth=span; top.fill=GridBagConstraints.HORIZONTAL; top.insets=new Insets(8,10,25,10);
            add(screen,top);
            int y=1;
            for (Map.Entry<Character,SortedMap<Integer,Boolean>> row : rows.entrySet()) {
                JLabel name=new JLabel(String.valueOf(row.getKey()),SwingConstants.CENTER);
                name.setFont(UiTheme.font(14,Font.BOLD));
                name.setPreferredSize(new Dimension(28,42));
                add(name,cell(0,y));
                for (int col=1; col<=columns; col++) {
                    int x=col+(col>aisleAfter?1:0);
                    Boolean taken=row.getValue().get(col);
                    if (taken==null) {
                        add(Box.createRigidArea(new Dimension(60,42)),cell(x,y));
                    } else {
                        String code=row.getKey().toString()+col;
                        UiTheme.SeatButton button=new UiTheme.SeatButton(code,taken);
                        button.setPreferredSize(new Dimension(60,42));
                        button.setSelected(!taken && selected.contains(code));
                        button.addActionListener(event -> changed.run());
                        add(button,cell(x,y));
                        if (!taken) available.add(button);
                    }
                }
                if (aisleAfter<columns) {
                    add(Box.createRigidArea(new Dimension(26,42)),cell(aisleAfter+1,y));
                }
                y++;
            }
        }
        revalidate(); repaint();
        return available;
    }

    private static GridBagConstraints cell(int x,int y) {
        GridBagConstraints c=new GridBagConstraints();
        c.gridx=x; c.gridy=y; c.insets=new Insets(4,4,4,4);
        c.anchor=GridBagConstraints.CENTER;
        return c;
    }
}

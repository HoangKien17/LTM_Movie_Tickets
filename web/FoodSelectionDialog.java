package web;

import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;
import javax.swing.*;

final class FoodSelectionDialog {
    private FoodSelectionDialog(){ }
    static String choose(Component owner,List<String[]> products) {
        JPanel list=new JPanel(new GridBagLayout());
        GridBagConstraints c=new GridBagConstraints();c.gridy=0;c.insets=new Insets(6,8,6,8);c.anchor=GridBagConstraints.WEST;
        Map<String,JSpinner> quantities=new LinkedHashMap<>();
        for(String[] p:products) {
            c.gridx=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;
            list.add(new JLabel(p[1]+("drink".equals(p[2])?" (Nước uống)":" (Đồ ăn)")),c);
            c.gridx=1;c.weightx=0;c.fill=GridBagConstraints.NONE;list.add(new JLabel(format(p[3])),c);
            JSpinner quantity=new JSpinner(new SpinnerNumberModel(0,0,20,1));
            c.gridx=2;list.add(quantity,c);quantities.put(p[0],quantity);c.gridy++;
        }
        JPanel root=new JPanel(new BorderLayout(0,10));
        root.add(new JLabel("Chọn thêm đồ ăn, nước uống (có thể bỏ qua)"),BorderLayout.NORTH);
        root.add(new JScrollPane(list),BorderLayout.CENTER);root.setPreferredSize(new Dimension(540,300));
        if(JOptionPane.showConfirmDialog(owner,root,"Đồ ăn & nước uống",JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE)!=JOptionPane.OK_OPTION)return null;
        List<String> selected=new ArrayList<>();
        for(Map.Entry<String,JSpinner> e:quantities.entrySet()) {
            int quantity=(Integer)e.getValue().getValue();if(quantity>0)selected.add(e.getKey()+":"+quantity);
        }
        return String.join(",",selected);
    }
    private static String format(String amount){return String.format(new Locale("vi","VN"),"%,.0f ₫",new BigDecimal(amount));}
}

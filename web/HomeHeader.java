package web;

import java.awt.*;
import javax.swing.*;

/** One row on a large desktop; moves navigation to a second row on narrow windows. */
final class HomeHeader extends JPanel {
    private final JComponent brand, nav, search, bell, account, controls;
    HomeHeader(JComponent nav, JTextField input, JComponent bell, JComponent account, JComponent controls) {
        this.nav=nav;this.bell=bell;this.account=account;this.controls=controls;
        setName("home-header");setLayout(null);setOpaque(false);setPreferredSize(new Dimension(0,80));
        brand=new JComponent() {
            protected void paintComponent(Graphics graphics) {
                Graphics2D g=(Graphics2D)graphics.create();AuthStyle.quality(g);
                AuthStyle.orbit(g,0,4,57);g.setFont(UiTheme.font(getWidth()<260?21:24,Font.BOLD));
                g.setColor(UiTheme.TEXT);g.drawString("GALAXY",69,39);
                int x=69+g.getFontMetrics().stringWidth("GALAXY ");g.setColor(UiTheme.PURPLE);g.drawString("CINEMA",x,39);g.dispose();
            }
        };
        search=new JPanel(new BorderLayout(10,0)) {
            protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();AuthStyle.quality(g);g.setColor(new Color(14,23,38));g.fillRoundRect(1,1,getWidth()-2,getHeight()-2,16,16);g.setColor(UiTheme.LINE);g.drawRoundRect(1,1,getWidth()-3,getHeight()-3,16,16);g.dispose();}
        };search.setOpaque(false);
        JLabel icon=new JLabel(HomeIcons.icon("search",21));icon.setForeground(UiTheme.MUTED);
        search.setBorder(BorderFactory.createEmptyBorder(0,14,0,12));
        input.setOpaque(false);input.setBorder(BorderFactory.createEmptyBorder());input.setFont(UiTheme.font(15,Font.PLAIN));
        search.add(icon,BorderLayout.WEST);search.add(input,BorderLayout.CENTER);
        add(brand);add(nav);add(search);add(bell);add(account);add(controls);
    }
    JComponent brand() { return brand; }
    @Override public void doLayout() {
        int w=getWidth(), pad=24, brandW=w<1300?245:285, navW=nav.getPreferredSize().width;
        int accountW=w<1250?137:167, controlsW=99, bellW=43;
        boolean wrapped=w-pad*2-brandW-navW-accountW-controlsW-bellW-54<190;
        sizeNavigation(nav,wrapped?46:80);
        int height=wrapped?126:80;
        if(getPreferredSize().height!=height){setPreferredSize(new Dimension(0,height));revalidate();}
        brand.setBounds(pad,9,brandW,62);
        controls.setBounds(w-controlsW,0,controlsW,32);
        int accountX=w-controlsW-accountW-10;
        account.setBounds(accountX,25,accountW,38);
        bell.setBounds(accountX-bellW-12,24,bellW,40);
        int left=wrapped?pad+brandW+14:pad+brandW+navW+14;
        search.setBounds(left,18,Math.max(130,accountX-bellW-26-left),46);
        nav.setBounds(wrapped?pad:pad+brandW,wrapped?79:0,wrapped?w-pad*2:navW,wrapped?46:80);
    }
    private void sizeNavigation(Container root,int height){
        for(Component child:root.getComponents()){
            if(child instanceof JButton){Dimension d=child.getPreferredSize();if(d.height!=height)child.setPreferredSize(new Dimension(d.width,height));}
            else if(child instanceof Container)sizeNavigation((Container)child,height);
        }
    }
    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g=(Graphics2D)graphics.create();AuthStyle.quality(g);
        g.setPaint(new GradientPaint(0,0,new Color(7,17,30),getWidth(),getHeight(),new Color(11,12,28)));
        g.fillRect(0,0,getWidth(),getHeight());g.setColor(UiTheme.LINE);g.drawLine(0,getHeight()-1,getWidth(),getHeight()-1);g.dispose();
    }
}

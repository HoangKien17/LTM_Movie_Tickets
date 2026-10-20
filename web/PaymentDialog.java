package web;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JFrame;

/**
 * Entry point shared by payment screens.
 */
final class PaymentDialog {

    private PaymentDialog() {
    }

    static void show(JFrame owner, String[] payment,
            BiConsumer<String, Consumer<List<String>>> send, Runnable paid) {
        show(owner, payment, "", send, paid);
    }

    static boolean show(JFrame owner, String[] payment, String summary,
            BiConsumer<String, Consumer<List<String>>> send, Runnable paid) {
        PaymentView view = new PaymentView(owner, payment, summary, send, paid);
        view.open();
        return view.finished;
    }
}

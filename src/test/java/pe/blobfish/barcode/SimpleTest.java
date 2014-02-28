package pe.blobfish.barcode;

import javax.swing.*;
import java.awt.*;

public class SimpleTest extends JFrame {
    public SimpleTest() throws HeadlessException {
        Toolkit.getDefaultToolkit().addAWTEventListener(new BarcodeAwareAWTEventListener(new BarcodeCapturedListener() {
            @Override
            public void barcodeCaptured(String barcode) {
                JOptionPane.showMessageDialog(SimpleTest.this, "barcode captured: " + barcode);
            }
        }), AWTEvent.KEY_EVENT_MASK);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(new JTextField());
    }

    public static void main(String[] args) {
        SimpleTest simpleTest = new SimpleTest();
        simpleTest.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        simpleTest.setVisible(true);
        simpleTest.pack();
    }
}

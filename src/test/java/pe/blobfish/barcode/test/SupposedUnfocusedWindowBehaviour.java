package pe.blobfish.barcode.test;

import pe.blobfish.barcode.BarcodeAwareAWTEventListener;
import pe.blobfish.barcode.BarcodeCapturedListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

// TODO migrate to an automated test
public class SupposedUnfocusedWindowBehaviour {
    private JPanel mainPanel;
    private JButton button1;
    private JPanel panel2;

    public SupposedUnfocusedWindowBehaviour(final JFrame frame) {

        Toolkit.getDefaultToolkit().addAWTEventListener(new BarcodeAwareAWTEventListener(new BarcodeCapturedListener() {
            @Override
            public void barcodeCaptured(String barcodeString) {
                System.out.println("Barcode captured: " + barcodeString);
            }
        }), AWTEvent.KEY_EVENT_MASK);


        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                panel2.setVisible(false);

                // TODO check it bad fix!!, last resort!!!
//                frame.requestFocusInWindow();
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("SupposedUnfocusedWindowBehaviour");
        frame.setContentPane(new SupposedUnfocusedWindowBehaviour(frame).mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}

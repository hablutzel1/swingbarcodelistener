package pe.blobfish.barcode.test;

import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import pe.blobfish.barcode.BarcodeAwareAWTEventListener;
import pe.blobfish.barcode.BarcodeCapturedListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BarcodeCaptureTests extends JFrame {
    private static FrameFixture festFrameFixture;
    private JPanel mainPanel;
    private volatile JTextArea textArea1;
    private JButton evento1Button;
    private JButton evento2Button;
    private JButton evento3Button;
    private JButton evento4Button;
    private JButton evento5Button;
    private JButton evento6Button;


    public BarcodeCaptureTests() {
        this.setContentPane(mainPanel);

        final BarcodeCapturedListener barcodeCapturedListenerParameter = new BarcodeCapturedListener() {
            @Override
            public void barcodeCaptured(String barcodeString) {
                JOptionPane.showMessageDialog(BarcodeCaptureTests.this, barcodeString);
//                System.out.println("Barcode captured: " + s);
            }
        };

        // important: take into account that timing is not that precise under debugging (specially with field, method... breakpoints)
        Toolkit.getDefaultToolkit().addAWTEventListener(new BarcodeAwareAWTEventListener(barcodeCapturedListenerParameter, BarcodeAwareAWTEventListener.LF_SUFFIX), AWTEvent.KEY_EVENT_MASK);

        evento1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            festFrameFixture.textBox().enterText("1");

                            Thread.sleep(30);

                            festFrameFixture.textBox().enterText("\n");

                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }.start();
            }
        });
        evento2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            festFrameFixture.textBox().enterText("1");

                            Thread.sleep(30);
                            festFrameFixture.textBox().enterText("2");

                            Thread.sleep(30);

                            festFrameFixture.textBox().enterText("\n");

                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }.start();
            }
        });
        evento3Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            festFrameFixture.textBox().enterText("1");

                            Thread.sleep(60);

                            festFrameFixture.textBox().enterText("\n");

                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }.start();
            }
        });
        evento4Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            festFrameFixture.textBox().enterText("1");

                            Thread.sleep(30);
                            festFrameFixture.textBox().enterText("2");
////
                            Thread.sleep(30);
//
                            festFrameFixture.textBox().enterText("\n");
//
                            Thread.sleep(40);

                            festFrameFixture.textBox().enterText("\n");

                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }.start();
            }
        });
        evento5Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            festFrameFixture.textBox().enterText("1");

                            Thread.sleep(30);
                            festFrameFixture.textBox().enterText("2");

                            Thread.sleep(51);

                            festFrameFixture.textBox().enterText("\n");


                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }.start();
            }
        });
        evento6Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            festFrameFixture.textBox().enterText("1");

                            Thread.sleep(30);
                            festFrameFixture.textBox().enterText("2");

                            Thread.sleep(30);

                            festFrameFixture.textBox().enterText("\n");

                            Thread.sleep(51);

                            festFrameFixture.textBox().enterText("\n");


                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }.start();
            }
        });
    }


    public static void main(String[] args) {


        BarcodeCaptureTests frame = GuiActionRunner.execute(new GuiQuery<BarcodeCaptureTests>() {
            protected BarcodeCaptureTests executeInEDT() {
                BarcodeCaptureTests frameManagedWithSwingFest = new BarcodeCaptureTests();
                frameManagedWithSwingFest.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                return frameManagedWithSwingFest;
            }
        });
        festFrameFixture = new FrameFixture(frame);
        festFrameFixture.robot.settings().delayBetweenEvents(0);
        festFrameFixture.show(); // shows the frame to test

    }

}

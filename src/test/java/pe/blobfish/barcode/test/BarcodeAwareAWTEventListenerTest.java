package pe.blobfish.barcode.test;

import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import pe.blobfish.barcode.BarcodeAwareAWTEventListener;
import pe.blobfish.barcode.BarcodeCapturedListener;

import javax.swing.*;
import java.awt.*;

// FIXME migrate to appropiate fest-junit (?) test
public class BarcodeAwareAWTEventListenerTest extends JFrame {

    // nasty test, TODO rewrite it, it is failing randomly
    @org.junit.Test
    public void testRedirectToUncaughtExcHandlerIfExceptionOccursInListener() throws Exception {
        BarcodeAwareAWTEventListenerTest frame = GuiActionRunner.execute(new GuiQuery<BarcodeAwareAWTEventListenerTest>() {
            protected BarcodeAwareAWTEventListenerTest executeInEDT() {
                return BarcodeAwareAWTEventListenerTest.this;
            }
        });
        festFrameFixture = new FrameFixture(frame);
        festFrameFixture.robot.settings().delayBetweenEvents(0);
        festFrameFixture.show(); // shows the frame to test

        festFrameFixture.textBox().enterText("1");
        Thread.sleep(30);
        festFrameFixture.textBox().enterText("2");
        Thread.sleep(30);
        festFrameFixture.textBox().enterText("\n");


        festFrameFixture.textBox().enterText("1");
        Thread.sleep(30);
        festFrameFixture.textBox().enterText("2");
        Thread.sleep(30);
        festFrameFixture.textBox().enterText("\n");

        Thread.sleep(1000); // wait for EDT to complete :P

        Assert.assertThat(secondTime, CoreMatchers.is(true));

        Thread.sleep(2000); // to don't kill the app yet, hehe
    }

    private static FrameFixture festFrameFixture;

    // FIXME these testing variables are cryptic
    public boolean flag;
    private boolean secondTime;

    public BarcodeAwareAWTEventListenerTest() throws HeadlessException {
        Toolkit.getDefaultToolkit().addAWTEventListener(new BarcodeAwareAWTEventListener(new BarcodeCapturedListener() {
            @Override
            public void barcodeCaptured(String barcodeString) {
                if (!flag){
                    flag = true;
                    throw new RuntimeException();
                } else {
                  secondTime = true;
                }
            }
        }, BarcodeAwareAWTEventListener.LF_SUFFIX), AWTEvent.KEY_EVENT_MASK);
        getContentPane().setLayout(new FlowLayout());
        getContentPane().add(new JTextField(25));
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

}

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.fest.swing.edt.GuiActionRunner;
import org.fest.swing.edt.GuiQuery;
import org.fest.swing.fixture.FrameFixture;
import sun.awt.AWTAccessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.*;

public class BarcodeCaptureDemo extends JFrame {
    private static FrameFixture festFrameFixture;
    private JPanel mainPanel;
    private volatile JTextArea textArea1;
    private JButton emulateUserOperationButton;

    private final ArrayDeque<KeyEvent> generatedEventsDeque;


    public BarcodeCaptureDemo() {
        this.setContentPane(mainPanel);

        generatedEventsDeque = new ArrayDeque<KeyEvent>(); // TODO check!! do we really need a COllection.synchronizable??

        final BarcodeCaptureListener barcodeCaptureListenerParameter = new BarcodeCaptureListener() {

            @Override
            public void barcodeCaptured(String s) {
                System.out.println("Barcode captured: " + s);
            }
        };

        // important: take into account that timing is not that precise under debugging (specially with field, method... breakpoints)
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {

            {
                new Thread(new Runnable() {

                    public ArrayDeque<KeyEvent[]> currentDeque = new ArrayDeque<KeyEvent[]>();

                    // the minimun the best
                    //TODO determine if this can be reduced even more
                    private int maxStandardDeviation;


                    public KeyEvent lastKeyPressedEvent;
                    public long lastKeyPressedEventsFlushing;
                    public int minBarcodeLenIncludingTrailingEnter = 3;
                    public BarcodeCaptureListener barcodeCaptureListener = barcodeCaptureListenerParameter;

                    @Override
                    public void run() {
                        // the minimum the best to not capture wrong entries, maybe calculate from the max deviation, or the inverse??
                        int maxTimeBetweenCharsFromBarcodeReader = 50;

                        maxStandardDeviation = 5;

//                        if (true) {return;}

                        while (true) {

                            final KeyEvent firstAndPulledFromMainDeque;
                            synchronized (generatedEventsDeque) {
                                firstAndPulledFromMainDeque = generatedEventsDeque.pollFirst();
                            }

                            if (firstAndPulledFromMainDeque != null) {

                                if (firstAndPulledFromMainDeque.getID() == 401) { // KEY_PRESSED

                                    if (lastKeyPressedEvent != null) {
                                        flushEvent(lastKeyPressedEvent);
                                    }
                                    lastKeyPressedEvent = firstAndPulledFromMainDeque;

                                } else if (firstAndPulledFromMainDeque.getID() == 400) { // KEY_TYPED

//                                    System.out.println("Polled event: " + firstAndPulledFromMainDeque.getWhen() + ", keyChar: " + firstAndPulledFromMainDeque.getKeyChar() + ", type: " + getTypeFromKeyEvent(firstAndPulledFromMainDeque));

                                    if (currentDeque.size() == 0) {

                                        addToCurrentDequeWithKeyPressedPair(firstAndPulledFromMainDeque);

                                    } else if (firstAndPulledFromMainDeque.getWhen() - currentDeque.getLast()[1].getWhen() > maxTimeBetweenCharsFromBarcodeReader) {

                                        checkForBarcodeInCurrentDequeOrFlushIfNotFound();

                                        addToCurrentDequeWithKeyPressedPair(firstAndPulledFromMainDeque);
                                    } else {

                                        if (currentDeque.size() < 2) {  // currentDeque.size() never 0
                                            addToCurrentDequeWithKeyPressedPair(firstAndPulledFromMainDeque);
                                        } else {
                                            double calculatedStandardDeviation = getStandardDeviationFromDequeAndLastEventIfNotNull(firstAndPulledFromMainDeque);
                                            if (calculatedStandardDeviation > maxStandardDeviation) { // not good
                                                if (currentDeque.size() > 2) {
                                                    tryToExtractBarcodeFromDeque();
                                                } else {
                                                    flushAllPendingEvents();
                                                }
                                                addToCurrentDequeWithKeyPressedPair(firstAndPulledFromMainDeque);
                                            } else {
                                                addToCurrentDequeWithKeyPressedPair(firstAndPulledFromMainDeque);
                                            }
                                        }

                                    }


                                }

                            }


                            if (currentDeque.size() > 0) {

                                long currentTime = System.currentTimeMillis();
                                long lastInDequeTime = currentDeque.getLast()[1].getWhen();
                                long difference = currentTime - lastInDequeTime;
                                if (difference > maxTimeBetweenCharsFromBarcodeReader) {
                                    checkForBarcodeInCurrentDequeOrFlushIfNotFound();
                                }

                            }

                            ////////////////// auto flush for KEY_PRESSED without KEY_TYPED pair
                            // FIXME allow key combinations like ALT + F4 to work, maybe the original listener is checking for system generated events
//                            long currentIterationTime;
//                            if ((lastKeyPressedEvent != null) && ((currentIterationTime = System.currentTimeMillis())- lastKeyPressedEventsFlushing > 10)){ // every ten ms
//
//                                flushEvent(lastKeyPressedEvent);
//                                lastKeyPressedEvent = null;
//
//                                lastKeyPressedEventsFlushing = currentIterationTime;
//                            }
                            //////////////////


                        }
                    }

                    private void addToCurrentDequeWithKeyPressedPair(KeyEvent firstAndPulledFromMainDeque) {
                        KeyEvent[] e = new KeyEvent[2];
                        e[0] = lastKeyPressedEvent;
                        e[1] = firstAndPulledFromMainDeque;
                        currentDeque.addLast(e);

                        lastKeyPressedEvent = null; // to notify it has been enqueued
                    }

                    private void checkForBarcodeInCurrentDequeOrFlushIfNotFound() {
                        if (currentDeque.size() > minBarcodeLenIncludingTrailingEnter - 1) {
                            double calculatedStandardDeviation = getStandardDeviationFromDequeAndLastEventIfNotNull(null);
                            if (calculatedStandardDeviation > maxStandardDeviation) { // TODO check: it is currently getting 5.1 > 5 eq false
                                flushAllPendingEvents();
                            } else {
                                tryToExtractBarcodeFromDeque();
                            }
                        } else {
//                            System.out.println("Automatic flusher, currentDeque.size(): " + currentDeque.size());
                            flushAllPendingEvents();
                        }
                    }


                    private void tryToExtractBarcodeFromDeque() {
                        if (currentDeque.getLast()[1].getKeyChar() == '\n') {
                            StringBuilder potentialBarcode = new StringBuilder();
                            for (KeyEvent[] keyEvent : currentDeque) {
                                System.out.println("times in captured barcode: " + keyEvent[1].getWhen());
                                potentialBarcode.append(keyEvent[1].getKeyChar());
                            }
                            currentDeque.clear();
                            barcodeCaptureListener.barcodeCaptured(potentialBarcode.deleteCharAt(potentialBarcode.length() - 1).toString());
                        } else {
                            flushAllPendingEvents();
                        }
                    }

                    private double getStandardDeviationFromDequeAndLastEventIfNotNull(KeyEvent firstInMainDeque) {
                        DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();

                        KeyEvent[] prev = null;
                        for (KeyEvent[] keyEvent : currentDeque) {
                            if (prev != null) {
                                descriptiveStatistics.addValue(keyEvent[1].getWhen() - prev[1].getWhen());
                            }
                            prev = keyEvent;
                        }

                        if (firstInMainDeque != null) {
                            descriptiveStatistics.addValue(firstInMainDeque.getWhen() - prev[1].getWhen());
                        }

                        return descriptiveStatistics.getStandardDeviation();
                    }

                    private void flushAllPendingEvents() {
                        KeyEvent[] keyEvent;
                        while (null != (keyEvent = currentDeque.pollFirst())) {
                            flushEvent(keyEvent[0]);
                            flushEvent(keyEvent[1]);
                        }
                    }

                    private void flushEvent(final KeyEvent finalKeyEvent) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                finalKeyEvent.getComponent().dispatchEvent(finalKeyEvent);
                            }
                        });
                    }
                }, "test1-thread").start();
            }

            public void eventDispatched(AWTEvent awtEvent) {
                if (!AWTAccessor.getAWTEventAccessor().isSystemGenerated(awtEvent)) {
                    return;
                }

                if (awtEvent.getID() == 400 || awtEvent.getID() == 401) {
                    KeyEvent keyEvent = (KeyEvent) awtEvent;
                    KeyEvent generatedEvent = new KeyEvent(keyEvent.getComponent(), keyEvent.getID(), keyEvent.getWhen(), keyEvent.getModifiers(), keyEvent.getKeyCode(), keyEvent.getKeyChar(), keyEvent.getKeyLocation());

                    if (awtEvent.getID() == 400) {
                        System.out.println("Added generated event: " + generatedEvent.getWhen() + ", keyChar: " + generatedEvent.getKeyChar() + ", type: " + getTypeFromKeyEvent(generatedEvent));
                    }

                    synchronized (generatedEventsDeque) {
                        generatedEventsDeque.addLast(generatedEvent);
                    }

                    keyEvent.consume();
                }


            }

            private StringBuilder getTypeFromKeyEvent(KeyEvent firstInMainDeque) {
                StringBuilder str = new StringBuilder();
                switch (firstInMainDeque.getID()) {
                    case KeyEvent.KEY_PRESSED:
                        str.append("KEY_PRESSED");
                        break;
                    case KeyEvent.KEY_RELEASED:
                        str.append("KEY_RELEASED");
                        break;
                    case KeyEvent.KEY_TYPED:
                        str.append("KEY_TYPED");
                        break;
                    default:
                        str.append("unknown type");
                        break;
                }
                return str;
            }

        }, AWTEvent.KEY_EVENT_MASK);


        emulateUserOperationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            festFrameFixture.textBox().enterText("1");

                            Thread.sleep(60);
//                            festFrameFixture.textBox().enterText("2");
//////
//                            Thread.sleep(30);

                            festFrameFixture.textBox().enterText("\n");
////
//                            Thread.sleep(40);
//
//                            festFrameFixture.textBox().enterText("\n");

                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }.start();
            }
        });
    }


    public static void main(String[] args) {


        BarcodeCaptureDemo frame = GuiActionRunner.execute(new GuiQuery<BarcodeCaptureDemo>() {
            protected BarcodeCaptureDemo executeInEDT() {
                BarcodeCaptureDemo frameManagedWithSwingFest = new BarcodeCaptureDemo();
                frameManagedWithSwingFest.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                return frameManagedWithSwingFest;
            }
        });
        festFrameFixture = new FrameFixture(frame);
        festFrameFixture.robot.settings().delayBetweenEvents(0);
        festFrameFixture.show(); // shows the frame to test

    }
}

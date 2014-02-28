package pe.blobfish.barcode;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import sun.awt.AWTAccessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;

public class BarcodeAwareAWTEventListener implements AWTEventListener {

    private final ArrayDeque<KeyEvent> generatedEventsDeque = new ArrayDeque<KeyEvent>(); // TODO check!! do we really need a COllection.synchronizable??;

    public BarcodeAwareAWTEventListener(final BarcodeCapturedListener barcodeCapturedListenerParameter) {

        new Thread(new Runnable() {

            public ArrayDeque<KeyEvent[]> currentDeque = new ArrayDeque<KeyEvent[]>();

            // the minimun the best
            private int maxStandardDeviation = 5;
            public int minBarcodeLenIncludingTrailingEnter = 3;
            // the minimum the best to not capture wrong entries, maybe calculate from the max deviation, or the inverse??
            private int maxTimeBetweenCharsFromBarcodeReader = 50;


            public KeyEvent lastKeyPressedEvent;
            public long lastKeyPressedEventsFlushing;
            public BarcodeCapturedListener barcodeCapturedListener = barcodeCapturedListenerParameter;

            @Override
            public void run() {

                // TODO check if it would be ok to delay before each iteration
                while (true) {

                    final KeyEvent firstAndPulledFromMainDeque;
                    synchronized (generatedEventsDeque) {
                        firstAndPulledFromMainDeque = generatedEventsDeque.pollFirst();
                    }

                    if (firstAndPulledFromMainDeque != null) {

                        if (firstAndPulledFromMainDeque.getID() == KeyEvent.KEY_PRESSED) {

                            if (lastKeyPressedEvent != null) {
                                flushEvent(lastKeyPressedEvent);
                            }
                            lastKeyPressedEvent = firstAndPulledFromMainDeque;

                        } else if (firstAndPulledFromMainDeque.getID() == KeyEvent.KEY_TYPED) { // KEY_TYPED

//                                    System.out.println("Polled event: " + firstAndPulledFromMainDeque.getWhen() + ", keyChar: " + firstAndPulledFromMainDeque.getKeyChar() + ", type: " + getTypeFromKeyEvent(firstAndPulledFromMainDeque));

                            if (currentDeque.size() == 0) {

                                addToCurrentDequeWithKeyPressedPair(firstAndPulledFromMainDeque);

                            } else {

                                if (currentDeque.size() < minBarcodeLenIncludingTrailingEnter - 1) {  // currentDeque.size() never 0
                                    addToCurrentDequeWithKeyPressedPair(firstAndPulledFromMainDeque);
                                } else {
                                    DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics();

                                    KeyEvent[] prev = null;
                                    for (KeyEvent[] keyEvent : currentDeque) {
                                        if (prev != null) {
                                            descriptiveStatistics.addValue(keyEvent[1].getWhen() - prev[1].getWhen());
                                        }
                                        prev = keyEvent;
                                    }

                                    if (firstAndPulledFromMainDeque != null) {
                                        descriptiveStatistics.addValue(firstAndPulledFromMainDeque.getWhen() - prev[1].getWhen());
                                    }

                                    double calculatedStandardDeviation = descriptiveStatistics.getStandardDeviation();
                                    if (calculatedStandardDeviation > maxStandardDeviation) { // not good
                                        if (currentDeque.size() > minBarcodeLenIncludingTrailingEnter - 1) {
                                            lookForBarcodeInDequeAndExtractIfExists();
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
                            if (currentDeque.size() > minBarcodeLenIncludingTrailingEnter - 1) {
                                lookForBarcodeInDequeAndExtractIfExists();
                            } else {
                                flushAllPendingEvents();
                            }
                        }

                    }

                    ////////////////// auto flush for KEY_PRESSED without KEY_TYPED pair
                    // FIXME allow key combinations like ALT + F4 to keep working, maybe the original listener is checking for system generated events, check too what happens if a dialog is opened and ENTER is pressed to confirm and close
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


            private void lookForBarcodeInDequeAndExtractIfExists() {
                if (currentDeque.getLast()[1].getKeyChar() == '\n') {
                    StringBuilder potentialBarcode = new StringBuilder();
                    for (KeyEvent[] keyEvent : currentDeque) {

//                                System.out.println("times in captured barcode: " + keyEvent[1].getWhen());

                        potentialBarcode.append(keyEvent[1].getKeyChar());
                    }
                    currentDeque.clear();
                    barcodeCapturedListener.barcodeCaptured(potentialBarcode.deleteCharAt(potentialBarcode.length() - 1).toString());
                } else {
                    flushAllPendingEvents();
                }
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

        if (awtEvent.getID() == KeyEvent.KEY_PRESSED || awtEvent.getID() == KeyEvent.KEY_TYPED) {
            KeyEvent keyEvent = (KeyEvent) awtEvent;
            keyEvent.consume();

            KeyEvent generatedEvent = new KeyEvent(keyEvent.getComponent(), keyEvent.getID(), keyEvent.getWhen(), keyEvent.getModifiers(), keyEvent.getKeyCode(), keyEvent.getKeyChar(), keyEvent.getKeyLocation());

            synchronized (generatedEventsDeque) {
                generatedEventsDeque.addLast(generatedEvent);
            }
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

}

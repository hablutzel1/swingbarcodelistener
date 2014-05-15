package pe.blobfish.barcode;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import sun.awt.AWTAccessor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;


// TODO check: it is currently possibly conflicting with keyPressed, keyReleased, keyTyped regular per component listeners
// FIXME  keybindings like 'SHIFT + HOME', 'END' requires to be pressed twice to take effect in a JTextField
// FIXME navigating a JTextField contents with 'LEFT', 'RIGHT' keys  is erratic
// TODO evaluate to plug this into the AWT/Swing(?) standard event handling mechanism
public class BarcodeAwareAWTEventListener implements AWTEventListener {

    private final ArrayDeque<KeyEvent> generatedEventsDeque = new ArrayDeque<KeyEvent>(); // TODO check!! do we really need a COllection.synchronizable??;

    private final Logger logger = Logger.getLogger(BarcodeAwareAWTEventListener.class);

    private static final char NULL_CHAR = '\u0000';
    public static final char LF_SUFFIX = '\n';

    // TODO check if it is required to enable multi-char suffix for some barcode scanner models (e.g. \r\n), it that case it would be received as a String or an array of chars
    private char suffixChar = NULL_CHAR;

    /**
     * Allows to specify the suffix char sent by the barcode reader.
     *
     * @param barcodeCapturedListenerParameter
     * @param suffixChar the char sent by the barcode reader after the actual string, for example '\n', if you are not sure do use the constructor that doesn't take this parameter
     */
    public BarcodeAwareAWTEventListener(final BarcodeCapturedListener barcodeCapturedListenerParameter, char suffixChar) {
        this(barcodeCapturedListenerParameter);
        this.suffixChar = suffixChar;
    }

    /**
     * It assumes the barcode reader doesn't sent any suffix character after every barcode read. <br /><br />Take into account that if the barcode reader actually sent a suffix character it will be added to the captured barcode string, so you would capture something like "1234\n", and then you would need to trim that string by yourself, e.g.:
     * <br />
     *
     * <pre>
     * Toolkit.getDefaultToolkit().addAWTEventListener(new BarcodeAwareAWTEventListener(
         new BarcodeCapturedListener() {
           @Override
           public void barcodeCaptured(String barcodeString) {
             barcodeString = barcodeString.trim();
             JOptionPane.showMessageDialog(null, "barcode captured: " + barcodeString);
           }
         }), AWTEvent.KEY_EVENT_MASK);
     *
     * </pre>
     *
     *
     *
     * @param barcodeCapturedListenerParameter
     * @see pe.blobfish.barcode.BarcodeAwareAWTEventListener#BarcodeAwareAWTEventListener(BarcodeCapturedListener, char)
     */
    public BarcodeAwareAWTEventListener(final BarcodeCapturedListener barcodeCapturedListenerParameter) {

        // to overcome problem mentioned in pe.blobfish.barcode.test.SupposedUnfocusedWindowBehaviour
        DefaultKeyboardFocusManager.getCurrentKeyboardFocusManager().
                addKeyEventPostProcessor(new KeyEventPostProcessor() {


                    public KeyEvent lastKeyPressed;

                    @Override
                    public boolean postProcessKeyEvent(KeyEvent currentEvent) {

                        if (AWTAccessor.getAWTEventAccessor().isSystemGenerated(currentEvent)) {



                            if (!currentEvent.isConsumed()) {
                                if (currentEvent.getID() == KeyEvent.KEY_PRESSED) {
                                    lastKeyPressed = currentEvent;
                                } else if (currentEvent.getID() == KeyEvent.KEY_TYPED) {
                                    lastKeyPressed.consume();
                                    currentEvent.consume();
                                    synchronized (generatedEventsDeque) {
                                        generatedEventsDeque.addLast(cloneKeyEvent(lastKeyPressed));
                                        generatedEventsDeque.addLast(cloneKeyEvent(currentEvent));
                                    }

                                }
                            }
                        }

                        return false;
                    }
                });


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

                            if (logger.isDebugEnabled()) {
                                logger.debug("Polled event: " + firstAndPulledFromMainDeque.getWhen() + ", keyChar: " + firstAndPulledFromMainDeque.getKeyChar() + ", type: " + getTypeFromKeyEvent(firstAndPulledFromMainDeque));
                            }


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

                                    descriptiveStatistics.addValue(firstAndPulledFromMainDeque.getWhen() - prev[1].getWhen());


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
                    // FIXME allow key combinations like ALT + F4 to keep working, maybe the original listener is checking for system generated events, check too what happens if a dialog is opened and ENTER is pressed to confirm and close, it is currently not allowing acute characters like á in a text area. It seems that the ENTER key is working for a text field with something like this 'jTextField.addActionListener(someAction)'. Debug and fix these problems using appropiate unit tests
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
                // TODO check/profile this algorithm it could be maybe improved for performance
                // TODO check: currently 'currentDeque.getLast()[1].getKeyChar() == suffixChar' is being used as a first method to discard reads when a 'suffixChar' is absolutely required, when no suffix char is expected this check is just ignored
                if (suffixChar == NULL_CHAR || currentDeque.getLast()[1].getKeyChar() == suffixChar) {
                    StringBuilder potentialBarcode = new StringBuilder();
                    for (KeyEvent[] keyEvent : currentDeque) {
                        potentialBarcode.append(keyEvent[1].getKeyChar());
                    }
                    currentDeque.clear();

                    try {
                        String barcodeString = (suffixChar != NULL_CHAR ? (potentialBarcode.deleteCharAt(potentialBarcode.length() - 1)) : potentialBarcode).toString();
                        if (logger.isDebugEnabled()){
                            logger.debug("barcodeCaptured length: " + barcodeString.length());
                        }
                        barcodeCapturedListener.barcodeCaptured(barcodeString);
                    } catch (Exception e) {
                        // TODO research the way EDT manage exceptions in detail, try to plug into it, and test related behaviour appropiately, starting point: pe.blobfish.barcode.test.BarcodeAwareAWTEventListenerTests.testRedirectToUncaughtExcHandlerIfExceptionOccursInListener() (rewrite this test method!!!)
                        Thread.currentThread().getUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                    }
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


        if (logger.isDebugEnabled()) {
            logger.debug("pe.blobfish.barcode.BarcodeAwareAWTEventListener.eventDispatched(" + awtEvent + ")");
        }


        if (awtEvent.getID() == KeyEvent.KEY_PRESSED || awtEvent.getID() == KeyEvent.KEY_TYPED) {
            KeyEvent keyEvent = (KeyEvent) awtEvent;
            keyEvent.consume();

            KeyEvent generatedEvent = cloneKeyEvent(keyEvent);

            synchronized (generatedEventsDeque) {
                generatedEventsDeque.addLast(generatedEvent);
            }
        }


    }

    private KeyEvent cloneKeyEvent(KeyEvent keyEvent) {
        return new KeyEvent(keyEvent.getComponent(), keyEvent.getID(), keyEvent.getWhen(), keyEvent.getModifiers(), keyEvent.getKeyCode(), keyEvent.getKeyChar(), keyEvent.getKeyLocation());
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;

public class GeneratingKeyEventsAndRedirectingThemToAnotherCompoent {
    private JTextField textField1;
    private JPanel mainPanel;
    private JTextArea textArea1;


    public GeneratingKeyEventsAndRedirectingThemToAnotherCompoent() {
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener(){

            public int previousId;


//            public AWTEvent currentEvent;
            private KeyEvent lastGeneratedKeyEvent;

            @Override
            public void eventDispatched(AWTEvent event1) {
                KeyEvent event = (KeyEvent) event1;

                if (lastGeneratedKeyEvent == event){
                    return; // escape from StackOverflow
                }


//                ((KeyEvent) event).consume();

                int currentId = event.getID();
                if ( currentId == 401 || currentId == 400) { // KEY_PRESSED or KEY_TYPED

                    // regenerating KeyEvent and redirecting to another text component
//                    lastGeneratedKeyEvent = new KeyEvent(textArea1, event.getID(), event.getWhen(), event.getModifiers(), event.getKeyCode(), event.getKeyChar(), event.getKeyLocation());
//                    textArea1.dispatchEvent(lastGeneratedKeyEvent);


                    // reflowing the original event
                    lastGeneratedKeyEvent = event;
                    textArea1.dispatchEvent(event);

                    System.out.println(event.isActionKey());
                    System.out.println(event.isAltDown());
                    System.out.println(event.isAltGraphDown());
                    System.out.println(event.isControlDown());
                    System.out.println(event.isMetaDown());
                    System.out.println(event.isShiftDown());
                    System.out.println("-");
//                    System.out.println(event.is);

                }

                previousId = currentId;
            }
        }, AWTEvent.KEY_EVENT_MASK);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("GeneratingKeyEventsAndRedirectingThemToAnotherCompoent");
        frame.setContentPane(new GeneratingKeyEventsAndRedirectingThemToAnotherCompoent().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}

#Swing Barcode Listener

This project aims to allow to user to create a listener for barcodes being read whenever he is in the Swing GUI.

Sample usage "src/test/java/pe/blobfish/barcode/test/SimpleTest.java"

```java
package pe.blobfish.barcode.test;

import pe.blobfish.barcode.BarcodeAwareAWTEventListener;
import pe.blobfish.barcode.BarcodeCapturedListener;

import javax.swing.*;
import java.awt.*;

public class SimpleTest extends JFrame {
    public SimpleTest() throws HeadlessException {

        Toolkit.getDefaultToolkit().addAWTEventListener(new BarcodeAwareAWTEventListener(new BarcodeCapturedListener() {
            @Override
            public void barcodeCaptured(String barcodeString) {
                JOptionPane.showMessageDialog(SimpleTest.this, "barcode captured: " + barcodeString);
            }
        }), AWTEvent.KEY_EVENT_MASK);


        getContentPane().setLayout(new FlowLayout());
        getContentPane().add(new JLabel("Capture barcode demo"));
        getContentPane().add(new JTextField(25));
    }

    public static void main(String[] args) {
        SimpleTest simpleTest = new SimpleTest();
        simpleTest.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        simpleTest.setVisible(true);
        simpleTest.pack();
    }
}
```

Just download source code, execute the previous class, and start reading with your barcode scanner plugged in.

##WARNING

- It is currently just a 'proof of concept' so use in production is in no way recommended.
- There are several keyboard related issues, for example after installing the BarcodeAwareAWTEventListener 'ALT + F4' combination will stop working.
- Sometimes (specially after just starting the app) the barcodes will be captured cropped, for example a original barcode '20202020' could be get as '20202'. It seems to be related to a combination of OS/JVM/AWT factors around the CPU priority given to processing keyboard events, this requires more research, and you are welcome to get into is ;)

Anyway it could be useful for you (for a prototype maybe) and you could try to fix the existing issues for everyone, why not?.

##TODOS

- Remove commons-math3 dependency
- Re-enable the processing of ALT+F4, etc events.
- Rewrite unit tests
- Etc

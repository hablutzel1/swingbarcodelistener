package pe.blobfish.barcode;

// TODO evaluate to use for events a similar mechanism that the default used by swing/AWT, something like addBarcodeListener, fireBarcodeRead, etc. It would maybe allow to listen for taken barcodes only in some specific places in the window. or use different listener for different windows.
public interface BarcodeCapturedListener {

    /**
     * Will receive the barcode as a string anytime one barcode is captured. TODO maybe it shouldn't ever return null and it can be specified in the contract
     *
     * @param barcodeString
     */
    void barcodeCaptured(String barcodeString);

}

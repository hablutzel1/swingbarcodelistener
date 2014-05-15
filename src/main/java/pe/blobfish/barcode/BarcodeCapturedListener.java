package pe.blobfish.barcode;

public interface BarcodeCapturedListener {

    /**
     * Will receive the barcode as a string anytime one barcode is captured. TODO maybe it shouldn't ever return null and it can be specified in the contract
     *
     * @param barcodeString
     */
    void barcodeCaptured(String barcodeString);

}

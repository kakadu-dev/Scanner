package bz.kakadu.scanner

enum class ScannerError {
    /**
     * Low storage error
     */
    STORAGE_LOW,
    /**
     * Detector dependencies are not yet available
     */
    NOT_AVAILABLE,
    /**
     * Could not start camera source
     */
    NOT_START,
    /**
     * Do not have permission to start the camera
     */
    NOT_PERMISSION,
}
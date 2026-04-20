package com.accessswitch.scanning

/**
 * Scanning mode determines how the user interacts with the scanning engine.
 */
enum class ScanMode {
    /** App cycles through items automatically; switch press = select */
    AUTO,

    /** Switch 1 = advance to next item; Switch 2 = select current item */
    STEP,

    /** Switch held = scan advances; switch released = select */
    INVERSE,

    /** Switch 1 scans rows; press enters row; then scans columns */
    ROW_COLUMN
}

package com.gamaliev.notes.common.codes;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class RequestCode {

    /* External storage. Permissions */
    public static final int REQUEST_CODE_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 101;
    public static final int REQUEST_CODE_PERMISSIONS_READ_EXTERNAL_STORAGE = 102;

    /* Notes. Import. Export */
    public static final int REQUEST_CODE_NOTES_IMPORT               = 111;
    public static final int REQUEST_CODE_NOTES_EXPORT               = 112;


    /*
        Init
     */

    private RequestCode() {}
}

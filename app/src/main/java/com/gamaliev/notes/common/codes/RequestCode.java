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
    public static final int REQUEST_CODE_NOTES_IMPORT   = 111;
    public static final int REQUEST_CODE_NOTES_EXPORT   = 112;
    public static final int REQUEST_CODE_CHANGE_USER    = 113;
    public static final int REQUEST_CODE_SYNC_NOTES     = 114;
    public static final int REQUEST_CODE_SETTINGS       = 115;

    /* Conflicting entries */
    public static final int REQUEST_CODE_CONFLICTING    = 121;
    public static final int REQUEST_CODE_CONFLICT_DIALOG_SELECT = 122;

    /* Color picker. Select */
    public static final int REQUEST_CODE_COLOR_PICKER_SELECT = 131;



    /*
        Init
     */

    private RequestCode() {}
}

package com.gamaliev.notes.common.codes;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

public class ResultCode {

    /* Notes. Import. Export */
    public static final int RESULT_CODE_NOTES_IMPORTED = 101;
    public static final int RESULT_CODE_NOTES_EXPORTED = 102;

    /* Color picker */
    public static final int RESULT_CODE_COLOR_PICKER_SELECTED = 111;

    /* Entry */
    public static final int RESULT_CODE_ENTRY_CANCEL    = 121;
    public static final int RESULT_CODE_ENTRY_ADDED     = 122;
    public static final int RESULT_CODE_ENTRY_EDITED    = 123;
    public static final int RESULT_CODE_ENTRY_DELETED   = 124;

    /* Mock entries */
    public static final int RESULT_CODE_MOCK_ENTRIES_ADDED = 131;

    /* List filter */
    public static final int RESULT_CODE_LIST_FILTERED   = 141;

    /* Sync */
    public static final int RESULT_CODE_SYNC_PENDING_START = 151;
    public static final int RESULT_CODE_SYNC_START      = 152;
    public static final int RESULT_CODE_SYNC_SUCCESS    = 153;
    public static final int RESULT_CODE_SYNC_FAILED     = 154;


    /*
        Init
     */

    private ResultCode() {}
}

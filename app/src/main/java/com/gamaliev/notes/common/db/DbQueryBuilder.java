package com.gamaliev.notes.common.db;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static com.gamaliev.notes.common.db.DbHelper.BASE_COLUMN_ID;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_COLOR;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_CREATED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_DESCRIPTION;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_EDITED;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_MANUALLY;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_TITLE;
import static com.gamaliev.notes.common.db.DbHelper.LIST_ITEMS_COLUMN_VIEWED;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_ASCENDING;
import static com.gamaliev.notes.common.db.DbHelper.ORDER_DESCENDING;

/**
 * Helper for create select query, associated with
 * {@link android.database.sqlite.SQLiteDatabase
 * #query(String, String[], String, String[], String, String, String)}}
 *
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
@SuppressWarnings("WeakerAccess")
public final class DbQueryBuilder {

    /* Logger */
    @NonNull private static final String TAG = DbQueryBuilder.class.getSimpleName();

    /* SQL */
    @NonNull public static final String OPERATOR_EQUALS     = " = ";
    @NonNull public static final String OPERATOR_BETWEEN    = " BETWEEN ";
    @NonNull public static final String OPERATOR_LIKE       = " LIKE ";

    @NonNull private static final String OPERATOR_OR        = " OR ";
    @NonNull private static final String OPERATOR_AND       = " AND ";

    @NonNull private static final String SYMBOL_MASK        = " ? ";
    @NonNull private static final String SYMBOL_PERCENT     = "%";

    @Nullable private String[] mSelection;
    @Nullable private String[] mSelectionArgs;

    @NonNull private String mOrder = DbHelper.ORDER_COLUMN_DEFAULT;
    @NonNull private String mAscDesc = DbHelper.ORDER_ASC_DESC_DEFAULT;


    /*
        Init
     */

    public DbQueryBuilder() {}


    /*
        ...
     */

    /**
     * Add clause with 'OR' operator.<br>
     * See also:<br>
     *     {@link #OPERATOR_EQUALS}<br>
     *     {@link #OPERATOR_LIKE}<br>
     *     {@link #OPERATOR_BETWEEN}<br>
     *
     * @param column    Column.
     * @param operator  Operator. See 'OPERATOR_*' constants.
     * @param operands  Operands.
     * @return A reference to this object.
     */
    @NonNull
    public DbQueryBuilder addOr(
            @NonNull final String column,
            @NonNull final String operator,
            @NonNull final String[] operands) {

        return add(
                OPERATOR_OR,
                column,
                operator,
                operands);
    }

    /**
     * Add clause with 'ADD' operator.<br>
     * See also:<br>
     *     {@link #OPERATOR_EQUALS}<br>
     *     {@link #OPERATOR_LIKE}<br>
     *     {@link #OPERATOR_BETWEEN}<br>
     *
     * @param column    Column.
     * @param operator  Operator. See 'OPERATOR_*' constants.
     * @param operands  Operands.
     * @return A reference to this object.
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public DbQueryBuilder addAnd(
            @NonNull final String column,
            @NonNull final String operator,
            @NonNull final String[] operands) {

        return add(
                OPERATOR_AND,
                column,
                operator,
                operands);
    }

    // --Commented out by Inspection START:
    //    /**
    //     * Add selection and selectionArgs from given query builder, to current query builder.<br>
    //     * Add with 'OR' operator.
    //     *
    //     * @param queryBuilder Query builder, whose selection clauses will be added.
    //     * @return A reference to this object.
    //     */
    //    @NonNull
    //    public DbQueryBuilder addOrInner(@NonNull final DbQueryBuilder queryBuilder) {
    //        addInner(queryBuilder, OPERATOR_OR);
    //        return this;
    //    }
    // --Commented out by Inspection STOP

    /**
     * Add selection and selectionArgs from given query builder, to current query builder.<br>
     * Add with 'AND' operator.
     *
     * @param queryBuilder Query builder, whose selection clauses will be added.
     * @return A reference to this object.
     */
    @SuppressWarnings("UnusedReturnValue")
    @NonNull
    public DbQueryBuilder addAndInner(@NonNull final DbQueryBuilder queryBuilder) {
        addInner(queryBuilder, OPERATOR_AND);
        return this;
    }

    /**
     * Helper for {@link #addOr(String, String, String[])} and {@link #addAnd(String, String, String[])}.
     * @param queryBuilder  Query builder, whose selection clauses will be added.
     * @param operator      Operator, 'OR' or 'AND'.
     */
    private void addInner(
            @NonNull final DbQueryBuilder queryBuilder,
            @SuppressWarnings("SameParameterValue") @NonNull final String operator) {

        // Create inner clause.
        final StringBuilder sb = new StringBuilder();

        // If clause is first in current query builder, then add without operator 'OR' or 'ADD'.
        if (mSelection != null) {
            sb.append(operator);
        }

        // Example:
        // if first in current query builder, then  "   (created BETWEEN ? AND ?) ",
        // else                                     "OR (created BETWEEN ? AND ?) "
        sb      .append(" (")
                .append(queryBuilder.getSelectionResult())
                .append(") ");

        final String[] selectionArgs = queryBuilder.getSelectionArgs();
        if (selectionArgs == null) {
            Log.e(TAG, "Selection arguments is null.");
        } else {
            updateSelectionClauses(sb.toString(), selectionArgs);
        }
    }

    /**
     * Basic logic functional for added clauses.<br>
     * Add one clause.
     *
     * @param operatorPrimary   {@link #OPERATOR_OR}, {@link #OPERATOR_AND}.
     * @param column            Column.
     * @param operatorSecondary {@link #OPERATOR_EQUALS}, {@link #OPERATOR_BETWEEN}, {@link #OPERATOR_LIKE}.
     * @param operands          Operands.
     * @return A reference to this object.
     */
    @NonNull
    private DbQueryBuilder add(
            @Nullable final String operatorPrimary,
            @NonNull final String column,
            @NonNull final String operatorSecondary,
            @NonNull final String[] operands) {

        // Check supported operations.
        if (!(OPERATOR_BETWEEN.equals(operatorSecondary)
                || OPERATOR_LIKE.equals(operatorSecondary)
                || OPERATOR_EQUALS.equals(operatorSecondary))) {
            throw new IllegalArgumentException(
                    "Supported only 'BETWEEN', 'LIKE' and '=' operators");
        }

        /*
            Example: "OR (title LIKE ?) "
            Example: "OR (color = ?) "
            Example: "OR (created BETWEEN ? AND ?) "

            Example: "OR (title LIKE %Droid%) "
            Example: "OR (color = 12345) "
            Example: "OR (created BETWEEN 2017-04-25 21:25:35 AND 2017-04-25 21:25:35) "
        */
        final StringBuilder sb = new StringBuilder();

        // If clause is first in current query builder, then add without operator 'OR' or 'ADD'.
        if (mSelection != null) {
            sb.append(operatorPrimary);
        }

        sb      .append('(')
                .append(column);

        switch (operatorSecondary) {
            // Example: " LIKE ?"
            // Example: " LIKE %Droid%"
            case OPERATOR_LIKE:
                // Check exist one operand.
                if (operands.length != 1) {
                    throw new IllegalArgumentException(
                            "Operator 'LIKE' must operate with 1 operand.");
                } else {
                    sb      .append(OPERATOR_LIKE)
                            .append(SYMBOL_MASK);
                }
                operands[0] = SYMBOL_PERCENT
                                + operands[0]
                                + SYMBOL_PERCENT;
                break;

            // Example: " = ?"
            // Example: " = 12345"
            case OPERATOR_EQUALS:
                // Check exist one operand.
                if (operands.length != 1) {
                    throw new IllegalArgumentException(
                            "Operator '=' must operate with 1 operand.");
                } else {
                    sb      .append(OPERATOR_EQUALS)
                            .append(SYMBOL_MASK);
                }
                break;

            // Example: " BETWEEN ? AND ?"
            // Example: " BETWEEN 2017-04-25 21:25:35 AND 2017-04-25 21:25:35"
            case OPERATOR_BETWEEN:
                // Check exist two operands.
                if (operands.length != 2) {
                    throw new IllegalArgumentException(
                            "Operator 'BETWEEN' must operate with two operands.");
                } else {
                    sb      .append(OPERATOR_BETWEEN)
                            .append(SYMBOL_MASK)
                            .append(OPERATOR_AND)
                            .append(SYMBOL_MASK);
                }
                break;

            default:
                break;
        }

        sb.append(") ");
        updateSelectionClauses(sb.toString(), operands);
        return this;
    }

    /**
     * Update selection clauses.<br>
     * See: {@link #mSelection}, {@link #mSelectionArgs}.
     *
     * @param newSelectionString    New selection clause with mask.
     * @param newOperands           New operands.
     */
    private void updateSelectionClauses(
            @NonNull final String newSelectionString,
            @NonNull final String[] newOperands) {

        String[] newSelection       = new String[] {newSelectionString};
        String[] newSelectionArgs   = newOperands;

        // Update if clauses exists.
        if (mSelection != null && mSelectionArgs != null) {
            newSelection        = new String[mSelection.length       + 1];
            newSelectionArgs    = new String[mSelectionArgs.length   + newOperands.length];

            System.arraycopy(mSelection,     0, newSelection,        0, mSelection.length);
            System.arraycopy(mSelectionArgs, 0, newSelectionArgs,    0, mSelectionArgs.length);

            // Add new data to new array.
            newSelection[newSelection.length - 1] = newSelectionString;
            System.arraycopy(
                    newOperands,
                    0,
                    newSelectionArgs,
                    newSelectionArgs.length - newOperands.length,
                    newOperands.length);
        }

        mSelection = newSelection;
        mSelectionArgs = newSelectionArgs;
    }


    /*
        Setters
     */

    /**
     * Set sort order of query. If null or Empty, then set default value
     * {@link DbHelper#ORDER_COLUMN_DEFAULT}<br>
     *     See also: <br>
     *     {@link DbHelper#BASE_COLUMN_ID}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_MANUALLY}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_TITLE}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_DESCRIPTION}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_COLOR}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_CREATED}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_EDITED}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_VIEWED}<br>
     *
     * @param order Column.
     */
    public void setOrder(@NonNull final String order) {
        if (!(BASE_COLUMN_ID.equals(order)
                || LIST_ITEMS_COLUMN_MANUALLY.equals(order)
                || LIST_ITEMS_COLUMN_TITLE.equals(order)
                || LIST_ITEMS_COLUMN_DESCRIPTION.equals(order)
                || LIST_ITEMS_COLUMN_COLOR.equals(order)
                || LIST_ITEMS_COLUMN_CREATED.equals(order)
                || LIST_ITEMS_COLUMN_EDITED.equals(order)
                || LIST_ITEMS_COLUMN_VIEWED.equals(order))) {

            throw new IllegalArgumentException(
                    String.format("Unsupported sorting order: %s.", order));
        }
        mOrder = order;
    }

    /**
     * Set ascending / descending of sorting order. If null or Empty, then set default value
     * {@link DbHelper#ORDER_ASC_DESC_DEFAULT}<br>
     * @param ascDesc <br>
     *     {@link DbHelper#ORDER_ASCENDING}<br>
     *     {@link DbHelper#ORDER_DESCENDING}<br>
     */
    public void setAscDesc(@NonNull final String ascDesc) {
        if (!(ORDER_ASCENDING.equals(ascDesc)
                || ORDER_DESCENDING.equals(ascDesc))) {

            throw new IllegalArgumentException(
                    String.format("Unsupported sorting order: %s.", ascDesc));
        }
        mAscDesc = ascDesc;
    }


    /*
        Getters
     */

    /**
     * @return Selection result of query, with mask.
     */
    @Nullable
    public String getSelectionResult() {
        if (mSelection == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (String cause : mSelection) {
            sb      .append(cause)
                    .append(' ');
        }
        return sb.toString();
    }

    /**
     * @return Selection arguments of query.
     */
    @Nullable
    public String[] getSelectionArgs() {
        return mSelectionArgs == null ? null : mSelectionArgs.clone();
    }

    /**
     * @return Get formed sort order. {@link #mOrder} + {@link #mAscDesc}. Default value is "id ASC"
     */
    @NonNull
    public String getSortOrder() {
        return mOrder + " " + mAscDesc;
    }
}

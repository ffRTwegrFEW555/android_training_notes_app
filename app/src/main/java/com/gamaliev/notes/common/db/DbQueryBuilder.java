package com.gamaliev.notes.common.db;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Helper for create select query, associated with
 * {@link android.database.sqlite.SQLiteDatabase
 * #query(String, String[], String, String[], String, String, String)}}
 *
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */
public final class DbQueryBuilder {

    /* Logger */
    @SuppressWarnings("unused")
    private static final String TAG = DbQueryBuilder.class.getSimpleName();

    /* SQL */
    public static final String OPERATOR_OR      = " OR ";
    public static final String OPERATOR_AND     = " AND ";
    public static final String OPERATOR_EQUALS  = " = ";
    public static final String OPERATOR_BETWEEN = " BETWEEN ";
    public static final String OPERATOR_LIKE    = " LIKE ";

    private static final String SYMBOL_MASK     = " ? ";
    private static final String SYMBOL_PERCENT  = "%";

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

    /**
     * Add selection and selectionArgs from given query builder, to current query builder.<br>
     * Add with 'OR' operator.
     *
     * @param queryBuilder Query builder, whose selection clauses will be added.
     * @return A reference to this object.
     */
    @NonNull
    public DbQueryBuilder addOrInner(@NonNull final DbQueryBuilder queryBuilder) {
        addInner(queryBuilder, OPERATOR_OR);
        return this;
    }

    /**
     * Add selection and selectionArgs from given query builder, to current query builder.<br>
     * Add with 'AND' operator.
     *
     * @param queryBuilder Query builder, whose selection clauses will be added.
     * @return A reference to this object.
     */
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
            @NonNull final String operator) {

        // Create inner clause.
        final StringBuilder sb = new StringBuilder();

        // If clause is first in current query builder, then add without operator 'OR' or 'ADD'.
        if(mSelection != null) {
            sb.append(operator);
        }

        // Example:
        // if first in current query builder, then  "   (created BETWEEN ? AND ?) ",
        // else                                     "OR (created BETWEEN ? AND ?) "
        sb      .append(" (")
                .append(queryBuilder.getSelectionResult())
                .append(") ");

        // Add new selection clause to old.
        updateSelectionClauses(
                sb.toString(),
                queryBuilder.getSelectionArgs());
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
            throw new UnsupportedOperationException(
                    "Supported only 'BETWEEN', 'LIKE' and '=' operators");
        }

        /*
            Example: "OR (title LIKE ?) "
            Example: "OR (color = ?) "
            Example: "OR (created BETWEEN ? AND ?) "

            Example: "OR (title LIKE %Droid%) "
            Example: "OR (color = 12345) "
            Example: "OR (created BETWEEN 2017-04-25T21:25:35+05:00 AND 2017-04-25T21:25:35+05:00) "
        */

        // Example: 'OR (title' or '(title'
        final StringBuilder sb = new StringBuilder();

        // If clause is first in current query builder, then add without operator 'OR' or 'ADD'.
        if (mSelection != null) {
            sb.append(operatorPrimary);
        }

        sb      .append("(")
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
            // Example: " BETWEEN 2017-04-25T21:25:35+05:00 AND 2017-04-25T21:25:35+05:00"
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

            //
            default:
                break;
        }

        // Example: ') '
        sb.append(") ");

        // Add new selection clause to old.
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

            // Create new selection, and selectionArgs clauses.
            newSelection        = new String[mSelection.length       + 1];
            newSelectionArgs    = new String[mSelectionArgs.length   + newOperands.length];

            // Copy old array to new array.
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

        // Replacing old clauses with new.
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
     *     {@link DbHelper#LIST_ITEMS_COLUMN_TITLE}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_DESCRIPTION}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_COLOR}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_CREATED}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_EDITED}<br>
     *     {@link DbHelper#LIST_ITEMS_COLUMN_VIEWED}<br>
     *
     * @param order Column.
     */
    public void setOrder(@NonNull String order) {
        if (!TextUtils.isEmpty(order)) {
            mOrder = order;
        }
    }

    /**
     * Set ascending / descending of sorting order. If null or Empty, then set default value
     * {@link DbHelper#ORDER_ASC_DESC_DEFAULT}<br>
     * @param ascDesc <br>
     *     {@link DbHelper#ORDER_ASCENDING}<br>
     *     {@link DbHelper#ORDER_DESCENDING}<br>
     */
    public void setAscDesc(@NonNull String ascDesc) {
        if (!TextUtils.isEmpty(ascDesc)) {
            mAscDesc = ascDesc;
        }
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

        // Convert clauses array to one string clauses.
        final StringBuilder sb = new StringBuilder();
        for (String cause : mSelection) {
            sb      .append(cause)
                    .append(" ");
        }
        return sb.toString();
    }

    /**
     * @return Selection arguments of query.
     */
    @Nullable
    public String[] getSelectionArgs() {
        return mSelectionArgs;
    }

    /**
     * @return Get formed sort order. {@link #mOrder} + {@link #mAscDesc}. Default value is "id ASC"
     */
    @NonNull
    public String getSortOrder() {
        return mOrder + " " + mAscDesc;
    }
}

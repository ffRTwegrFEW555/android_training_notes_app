package com.gamaliev.list.list;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Date;

/**
 * @author Vadim Gamaliev
 *         <a href="mailto:gamaliev-vadim@yandex.com">(e-mail: gamaliev-vadim@yandex.com)</a>
 */

class ListEntry {

    @Nullable private Integer id;
    @Nullable private String name;
    @Nullable private String description;
    @Nullable private Integer color;
    @Nullable private Date createdDate;
    @Nullable private Date modifiedDate;

    ListEntry() {}


    /*
        Setters
     */

    public void setId(@NonNull Integer id) {
        this.id = id;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public void setDescription(@NonNull String description) {
        this.description = description;
    }

    public void setColor(@NonNull Integer color) {
        this.color = color;
    }

    public void setCreatedDate(@NonNull Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setModifiedDate(@NonNull Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }


    /*
        Getters
     */

    @Nullable
    public Integer getId() {
        return id;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    @Nullable
    public Integer getColor() {
        return color;
    }

    @Nullable
    public Date getCreatedDate() {
        return createdDate;
    }

    @Nullable
    public Date getModifiedDate() {
        return modifiedDate;
    }
}

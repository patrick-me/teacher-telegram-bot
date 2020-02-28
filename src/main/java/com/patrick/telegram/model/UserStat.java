package com.patrick.telegram.model;


import java.util.Date;

public interface UserStat {
    Date getStatDate();

    int getTotalTaskCount();

    int getSucceedTaskCount();

    int getFailedTaskCount();
}

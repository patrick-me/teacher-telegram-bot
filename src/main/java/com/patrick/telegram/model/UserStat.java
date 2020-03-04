package com.patrick.telegram.model;


import java.util.Date;

public interface UserStat {
    Date getStatDate();

    String getLessonName();

    int getTotalTaskCount();

    int getSucceedTaskCount();

    int getFailedTaskCount();
}

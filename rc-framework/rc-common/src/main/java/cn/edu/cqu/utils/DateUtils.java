package cn.edu.cqu.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {

    public static Date getDate(String pattern) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return simpleDateFormat.parse(pattern);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
}

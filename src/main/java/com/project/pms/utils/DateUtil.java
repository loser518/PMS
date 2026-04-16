package com.project.pms.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * @className: DateUtil
 * @description: 时间工具类
 * @author: loser
 * @createTime: 2026/2/2 21:03
 */
@Slf4j
@Component
public class DateUtil {

    // ====================== 常量定义 ======================

    // 日期格式常量
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String TIME_FORMAT = "HH:mm:ss";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_TIME_FORMAT_CN = "yyyy年MM月dd日 HH时mm分ss秒";
    public static final String DATE_TIME_MS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    public static final String DATE_FORMAT_YYYYMMDD = "yyyyMMdd";
    public static final String DATE_FORMAT_YYYYMM = "yyyyMM";
    public static final String DATE_FORMAT_YYYY = "yyyy";
    public static final String DATE_FORMAT_YYYY_MM = "yyyy-MM";

    // 时间格式化器
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_FORMAT);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);
    public static final DateTimeFormatter DATE_TIME_MS_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_MS_FORMAT);

    // 时区
    public static final String DEFAULT_TIME_ZONE = "Asia/Shanghai";
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of(DEFAULT_TIME_ZONE);

    // ====================== 获取当前时间 ======================

    /**
     * 获取当前日期字符串（yyyy-MM-dd）
     */
    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * 获取当前时间字符串（HH:mm:ss）
     */
    public static String getCurrentTime() {
        return LocalTime.now().format(TIME_FORMATTER);
    }

    /**
     * 获取当前日期时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String getCurrentDateTime() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }

    /**
     * 获取当前日期时间字符串（自定义格式）
     */
    public static String getCurrentDateTime(String pattern) {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 获取当前时间戳（毫秒）
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }

    /**
     * 获取当前时间戳（秒）
     */
    public static long getCurrentTimestampSeconds() {
        return System.currentTimeMillis() / 1000;
    }

    // ====================== 格式转换 ======================

    /**
     * LocalDateTime 转字符串
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * LocalDateTime 转字符串（默认格式）
     */
    public static String format(LocalDateTime dateTime) {
        return format(dateTime, DATE_TIME_FORMAT);
    }

    /**
     * LocalDate 转字符串
     */
    public static String format(LocalDate date, String pattern) {
        if (date == null) {
            return null;
        }
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * LocalDate 转字符串（默认格式）
     */
    public static String format(LocalDate date) {
        return format(date, DATE_FORMAT);
    }

    /**
     * Date 转字符串
     */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }

    /**
     * Date 转字符串（默认格式）
     */
    public static String format(Date date) {
        return format(date, DATE_TIME_FORMAT);
    }

    /**
     * 字符串转 LocalDateTime
     */
    public static LocalDateTime parseDateTime(String dateTimeStr, String pattern) {
        if (StringUtils.isBlank(dateTimeStr)) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 字符串转 LocalDateTime（默认格式）
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return parseDateTime(dateTimeStr, DATE_TIME_FORMAT);
    }

    /**
     * 字符串转 LocalDate
     */
    public static LocalDate parseDate(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 字符串转 LocalDate（默认格式）
     */
    public static LocalDate parseDate(String dateStr) {
        return parseDate(dateStr, DATE_FORMAT);
    }

    /**
     * 字符串转 Date
     */
    public static Date parse(String dateStr, String pattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        try {
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            log.error("日期解析失败: {}, pattern: {}", dateStr, pattern, e);
            return null;
        }
    }

    /**
     * 字符串转 Date（默认格式）
     */
    public static Date parse(String dateStr) {
        return parse(dateStr, DATE_TIME_FORMAT);
    }

    // ====================== 时间类型转换 ======================

    /**
     * Date 转 LocalDateTime
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    /**
     * Date 转 LocalDate
     */
    public static LocalDate dateToLocalDate(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().atZone(DEFAULT_ZONE_ID).toLocalDate();
    }

    /**
     * LocalDateTime 转 Date
     */
    public static Date localDateTimeToDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return Date.from(dateTime.atZone(DEFAULT_ZONE_ID).toInstant());
    }

    /**
     * LocalDate 转 Date
     */
    public static Date localDateToDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return Date.from(date.atStartOfDay(DEFAULT_ZONE_ID).toInstant());
    }

    /**
     * LocalDateTime 转时间戳（毫秒）
     */
    public static long localDateTimeToTimestamp(LocalDateTime dateTime) {
        return dateTime.atZone(DEFAULT_ZONE_ID).toInstant().toEpochMilli();
    }

    /**
     * 时间戳（毫秒）转 LocalDateTime
     */
    public static LocalDateTime timestampToLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    /**
     * 时间戳（秒）转 LocalDateTime
     */
    public static LocalDateTime timestampSecondsToLocalDateTime(long timestampSeconds) {
        return Instant.ofEpochSecond(timestampSeconds).atZone(DEFAULT_ZONE_ID).toLocalDateTime();
    }

    // ====================== 时间计算 ======================

    /**
     * 获取指定日期的开始时间（00:00:00）
     */
    public static LocalDateTime getStartOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    /**
     * 获取指定日期的开始时间（00:00:00）
     */
    public static LocalDateTime getStartOfDay(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atStartOfDay();
    }

    /**
     * 获取指定日期的结束时间（23:59:59.999）
     */
    public static LocalDateTime getEndOfDay(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }

    /**
     * 获取指定日期的结束时间（23:59:59.999）
     */
    public static LocalDateTime getEndOfDay(LocalDateTime dateTime) {
        return dateTime.toLocalDate().atTime(LocalTime.MAX);
    }

    /**
     * 增加天数
     */
    public static LocalDateTime plusDays(LocalDateTime dateTime, long days) {
        return dateTime.plusDays(days);
    }

    /**
     * 减少天数
     */
    public static LocalDateTime minusDays(LocalDateTime dateTime, long days) {
        return dateTime.minusDays(days);
    }

    /**
     * 增加小时
     */
    public static LocalDateTime plusHours(LocalDateTime dateTime, long hours) {
        return dateTime.plusHours(hours);
    }

    /**
     * 减少小时
     */
    public static LocalDateTime minusHours(LocalDateTime dateTime, long hours) {
        return dateTime.minusHours(hours);
    }

    /**
     * 增加分钟
     */
    public static LocalDateTime plusMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime.plusMinutes(minutes);
    }

    /**
     * 减少分钟
     */
    public static LocalDateTime minusMinutes(LocalDateTime dateTime, long minutes) {
        return dateTime.minusMinutes(minutes);
    }

    /**
     * 获取月份的第一天
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * 获取月份的最后一天
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * 获取年份的第一天
     */
    public static LocalDate getFirstDayOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfYear());
    }

    /**
     * 获取年份的最后一天
     */
    public static LocalDate getLastDayOfYear(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfYear());
    }

    // ====================== 时间差计算 ======================

    /**
     * 计算两个日期之间的天数差
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * 计算两个日期时间之间的小时差
     */
    public static long hoursBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return ChronoUnit.HOURS.between(startTime, endTime);
    }

    /**
     * 计算两个日期时间之间的分钟差
     */
    public static long minutesBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return ChronoUnit.MINUTES.between(startTime, endTime);
    }

    /**
     * 计算两个日期之间的秒数差
     */
    public static long secondsBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return ChronoUnit.SECONDS.between(startTime, endTime);
    }

    /**
     * 计算两个日期之间的毫秒差
     */
    public static long millisecondsBetween(LocalDateTime startTime, LocalDateTime endTime) {
        return Duration.between(startTime, endTime).toMillis();
    }

    // ====================== 日期判断 ======================

    /**
     * 判断日期是否在今天
     */
    public static boolean isToday(LocalDate date) {
        return date.isEqual(LocalDate.now());
    }

    /**
     * 判断日期是否在今天
     */
    public static boolean isToday(LocalDateTime dateTime) {
        return dateTime.toLocalDate().isEqual(LocalDate.now());
    }

    /**
     * 判断日期是否在昨天
     */
    public static boolean isYesterday(LocalDate date) {
        return date.isEqual(LocalDate.now().minusDays(1));
    }

    /**
     * 判断日期是否在明天
     */
    public static boolean isTomorrow(LocalDate date) {
        return date.isEqual(LocalDate.now().plusDays(1));
    }

    /**
     * 判断是否为周末
     */
    public static boolean isWeekend(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    /**
     * 判断是否为工作日
     */
    public static boolean isWorkday(LocalDate date) {
        return !isWeekend(date);
    }

    /**
     * 判断两个日期是否为同一天
     */
    public static boolean isSameDay(LocalDate date1, LocalDate date2) {
        return date1.isEqual(date2);
    }

    /**
     * 判断两个日期时间是否为同一天
     */
    public static boolean isSameDay(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return dateTime1.toLocalDate().isEqual(dateTime2.toLocalDate());
    }

    /**
     * 判断日期是否在指定范围内（包含边界）
     */
    public static boolean isBetween(LocalDate date, LocalDate startDate, LocalDate endDate) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 判断日期时间是否在指定范围内（包含边界）
     */
    public static boolean isBetween(LocalDateTime dateTime, LocalDateTime startTime, LocalDateTime endTime) {
        return !dateTime.isBefore(startTime) && !dateTime.isAfter(endTime);
    }

    // ====================== 年龄计算 ======================

    /**
     * 根据生日计算年龄
     */
    public static int calculateAge(LocalDate birthDate) {
        return calculateAge(birthDate, LocalDate.now());
    }

    /**
     * 根据生日和指定日期计算年龄
     */
    public static int calculateAge(LocalDate birthDate, LocalDate targetDate) {
        return Period.between(birthDate, targetDate).getYears();
    }

    // ====================== 其他实用方法 ======================

    /**
     * 获取指定日期的星期几（中文）
     */
    public static String getChineseDayOfWeek(LocalDate date) {
        String[] weekDays = {"星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"};
        int dayOfWeek = date.getDayOfWeek().getValue() % 7;
        return weekDays[dayOfWeek];
    }

    /**
     * 获取指定日期的星期几（英文缩写）
     */
    public static String getEnglishDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().toString().substring(0, 3);
    }

    /**
     * 获取指定月份有多少天
     */
    public static int getDaysInMonth(int year, int month) {
        return YearMonth.of(year, month).lengthOfMonth();
    }

    /**
     * 获取当前月份有多少天
     */
    public static int getDaysInCurrentMonth() {
        return YearMonth.now().lengthOfMonth();
    }

    /**
     * 判断是否为闰年
     */
    public static boolean isLeapYear(int year) {
        return Year.of(year).isLeap();
    }

    /**
     * 获取当前季度
     */
    public static int getCurrentQuarter() {
        int month = LocalDate.now().getMonthValue();
        return (month - 1) / 3 + 1;
    }

    /**
     * 获取指定日期的季度
     */
    public static int getQuarter(LocalDate date) {
        int month = date.getMonthValue();
        return (month - 1) / 3 + 1;
    }

    /**
     * 获取季度开始日期
     */
    public static LocalDate getQuarterStartDate(int year, int quarter) {
        int month = (quarter - 1) * 3 + 1;
        return LocalDate.of(year, month, 1);
    }

    /**
     * 获取季度结束日期
     */
    public static LocalDate getQuarterEndDate(int year, int quarter) {
        int month = quarter * 3;
        LocalDate date = LocalDate.of(year, month, 1);
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    // ====================== 格式化美化 ======================

    /**
     * 获取相对时间（如：刚刚、3分钟前、2小时前、昨天、3天前等）
     */
    public static String getRelativeTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(dateTime, now);

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return "刚刚";
        }

        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟前";
        }

        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时前";
        }

        long days = hours / 24;
        if (days == 1) {
            return "昨天";
        } else if (days < 30) {
            return days + "天前";
        }

        long months = days / 30;
        if (months < 12) {
            return months + "个月前";
        }

        long years = months / 12;
        return years + "年前";
    }

    /**
     * 获取友好的时间显示（用于聊天、评论等场景）
     */
    public static String getFriendlyTime(LocalDateTime dateTime) {
        LocalDate today = LocalDate.now();
        LocalDate date = dateTime.toLocalDate();

        if (date.isEqual(today)) {
            // 今天：显示时间
            return "今天 " + format(dateTime, "HH:mm");
        } else if (date.isEqual(today.minusDays(1))) {
            // 昨天：显示昨天+时间
            return "昨天 " + format(dateTime, "HH:mm");
        } else if (date.isAfter(today.minusDays(7))) {
            // 一周内：显示星期几+时间
            return getChineseDayOfWeek(date) + " " + format(dateTime, "HH:mm");
        } else {
            // 更早：显示完整日期
            return format(dateTime, "yyyy-MM-dd HH:mm");
        }
    }

    // ====================== 时间段生成 ======================

    /**
     * 生成两个日期之间的所有日期列表
     */
    public static List<LocalDate> generateDateRange(LocalDate startDate, LocalDate endDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }

        return dates;
    }

    /**
     * 获取最近N天的日期列表
     */
    public static List<LocalDate> getRecentDays(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        return generateDateRange(startDate, endDate);
    }

    /**
     * 获取最近N个月的月份列表
     */
    public static List<String> getRecentMonths(int months) {
        List<String> monthList = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDate date = now.minusMonths(i);
            monthList.add(format(date, "yyyy-MM"));
        }

        return monthList;
    }

    // ====================== 测试主方法 ======================

    // ====================== 年份相关方法 ======================

    /**
     * 获取当前年份
     */
    public static int getCurrentYear() {
        return LocalDate.now().getYear();
    }

    /**
     * 获取指定日期的年份
     */
    public static int getYear(LocalDate date) {
        return date.getYear();
    }

    /**
     * 获取指定日期时间的年份
     */
    public static int getYear(LocalDateTime dateTime) {
        return dateTime.getYear();
    }

    /**
     * 获取Date对象的年份
     */
    public static int getYear(Date date) {
        return dateToLocalDate(date).getYear();
    }

    /**
     * 获取当前月份（1-12）
     */
    public static int getCurrentMonth() {
        return LocalDate.now().getMonthValue();
    }

    /**
     * 获取指定日期的月份（1-12）
     */
    public static int getMonth(LocalDate date) {
        return date.getMonthValue();
    }

    /**
     * 获取指定日期时间的月份（1-12）
     */
    public static int getMonth(LocalDateTime dateTime) {
        return dateTime.getMonthValue();
    }

    /**
     * 获取当前月份的名称（中文）
     */
    public static String getCurrentMonthChinese() {
        return getMonthChinese(LocalDate.now());
    }

    /**
     * 获取指定日期月份的名称（中文）
     */
    public static String getMonthChinese(LocalDate date) {
        String[] monthNames = {"一月", "二月", "三月", "四月", "五月", "六月",
                "七月", "八月", "九月", "十月", "十一月", "十二月"};
        return monthNames[date.getMonthValue() - 1];
    }

    /**
     * 获取指定日期月份的名称（英文全称）
     */
    public static String getMonthEnglish(LocalDate date) {
        return date.getMonth().toString();
    }

    /**
     * 获取指定日期月份的名称（英文缩写）
     */
    public static String getMonthEnglishShort(LocalDate date) {
        return date.getMonth().toString().substring(0, 3);
    }

    /**
     * 获取当前日（1-31）
     */
    public static int getCurrentDayOfMonth() {
        return LocalDate.now().getDayOfMonth();
    }

    /**
     * 获取指定日期的日（1-31）
     */
    public static int getDayOfMonth(LocalDate date) {
        return date.getDayOfMonth();
    }

    /**
     * 获取指定日期时间的日（1-31）
     */
    public static int getDayOfMonth(LocalDateTime dateTime) {
        return dateTime.getDayOfMonth();
    }

    /**
     * 获取当前是一年中的第几天（1-366）
     */
    public static int getCurrentDayOfYear() {
        return LocalDate.now().getDayOfYear();
    }

    /**
     * 获取指定日期是一年中的第几天（1-366）
     */
    public static int getDayOfYear(LocalDate date) {
        return date.getDayOfYear();
    }

    /**
     * 获取指定日期时间是一年中的第几天（1-366）
     */
    public static int getDayOfYear(LocalDateTime dateTime) {
        return dateTime.getDayOfYear();
    }

    /**
     * 获取当前是星期几（1-7，1=星期一，7=星期日）
     */
    public static int getCurrentDayOfWeek() {
        return LocalDate.now().getDayOfWeek().getValue();
    }

    /**
     * 获取指定日期是星期几（1-7，1=星期一，7=星期日）
     */
    public static int getDayOfWeek(LocalDate date) {
        return date.getDayOfWeek().getValue();
    }

    /**
     * 获取指定日期时间是星期几（1-7，1=星期一，7=星期日）
     */
    public static int getDayOfWeek(LocalDateTime dateTime) {
        return dateTime.getDayOfWeek().getValue();
    }

    /**
     * 获取当前季度开始日期
     */
    public static LocalDate getCurrentQuarterStartDate() {
        int quarter = getCurrentQuarter();
        return getQuarterStartDate(getCurrentYear(), quarter);
    }

    /**
     * 获取当前季度结束日期
     */
    public static LocalDate getCurrentQuarterEndDate() {
        int quarter = getCurrentQuarter();
        return getQuarterEndDate(getCurrentYear(), quarter);
    }

    /**
     * 获取指定年份的所有月份
     */
    public static List<String> getMonthsOfYear(int year) {
        List<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            months.add(String.format("%04d-%02d", year, i));
        }
        return months;
    }

    /**
     * 获取指定年份的所有季度
     */
    public static List<String> getQuartersOfYear(int year) {
        List<String> quarters = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            quarters.add(year + "Q" + i);
        }
        return quarters;
    }

    /**
     * 判断是否为年初（1月1日）
     */
    public static boolean isStartOfYear(LocalDate date) {
        return date.getMonthValue() == 1 && date.getDayOfMonth() == 1;
    }

    /**
     * 判断是否为年末（12月31日）
     */
    public static boolean isEndOfYear(LocalDate date) {
        return date.getMonthValue() == 12 && date.getDayOfMonth() == 31;
    }

    /**
     * 获取年度开始日期
     */
    public static LocalDate getStartOfYear(int year) {
        return LocalDate.of(year, 1, 1);
    }

    /**
     * 获取年度结束日期
     */
    public static LocalDate getEndOfYear(int year) {
        return LocalDate.of(year, 12, 31);
    }

    /**
     * 获取年度周数（ISO标准，一年有52或53周）
     */
    public static int getWeeksInYear(int year) {
        LocalDate date = LocalDate.of(year, 12, 28); // ISO周计算规则
        return date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }

    /**
     * 获取当前年度周数
     */
    public static int getCurrentWeekOfYear() {
        return LocalDate.now().get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }

    /**
     * 获取指定日期的年度周数
     */
    public static int getWeekOfYear(LocalDate date) {
        return date.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear());
    }

    /**
     * 获取年度第N周的开始日期（ISO标准，周一开始）
     */
    public static LocalDate getStartOfWeek(int year, int week) {
        return LocalDate.of(year, 1, 1)
                .with(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear(), week)
                .with(java.time.temporal.ChronoField.DAY_OF_WEEK, 1); // 周一
    }

    /**
     * 获取年度第N周的结束日期（ISO标准，周日结束）
     */
    public static LocalDate getEndOfWeek(int year, int week) {
        return LocalDate.of(year, 1, 1)
                .with(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear(), week)
                .with(java.time.temporal.ChronoField.DAY_OF_WEEK, 7); // 周日
    }

    /**
     * 获取当前年的开始日期时间
     */
    public static LocalDateTime getStartOfCurrentYear() {
        return LocalDate.of(getCurrentYear(), 1, 1).atStartOfDay();
    }

    /**
     * 获取当前年的结束日期时间
     */
    public static LocalDateTime getEndOfCurrentYear() {
        return LocalDate.of(getCurrentYear(), 12, 31).atTime(23, 59, 59, 999_999_999);
    }

    /**
     * 获取指定年份的开始日期时间
     */
    public static LocalDateTime getStartOfYearDateTime(int year) {
        return LocalDate.of(year, 1, 1).atStartOfDay();
    }

    /**
     * 获取指定年份的结束日期时间
     */
    public static LocalDateTime getEndOfYearDateTime(int year) {
        return LocalDate.of(year, 12, 31).atTime(23, 59, 59, 999_999_999);
    }

    /**
     * 获取最近N年的年份列表
     */
    public static List<Integer> getRecentYears(int years) {
        List<Integer> yearList = new ArrayList<>();
        int currentYear = getCurrentYear();

        for (int i = years - 1; i >= 0; i--) {
            yearList.add(currentYear - i);
        }

        return yearList;
    }

    /**
     * 获取年份范围列表
     */
    public static List<Integer> getYearRange(int startYear, int endYear) {
        List<Integer> yearList = new ArrayList<>();

        for (int year = startYear; year <= endYear; year++) {
            yearList.add(year);
        }

        return yearList;
    }

    /**
     * 判断两个日期是否为同一年
     */
    public static boolean isSameYear(LocalDate date1, LocalDate date2) {
        return date1.getYear() == date2.getYear();
    }

    /**
     * 判断两个日期时间是否为同一年
     */
    public static boolean isSameYear(LocalDateTime dateTime1, LocalDateTime dateTime2) {
        return dateTime1.getYear() == dateTime2.getYear();
    }

    /**
     * 判断日期是否在当前年份
     */
    public static boolean isCurrentYear(LocalDate date) {
        return date.getYear() == getCurrentYear();
    }

    /**
     * 判断日期时间是否在当前年份
     */
    public static boolean isCurrentYear(LocalDateTime dateTime) {
        return dateTime.getYear() == getCurrentYear();
    }
}

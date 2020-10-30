package com.yangjie.JGB28181.common.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss ", Locale.ENGLISH);

	private static DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	
	public static String getGBFormatDate(Date date){
		return dateFormat.format(date).replace(" ", "T");
	}

	public static String getFormatDateTime(Date date) {
		return dateFormat.format(date);
	}

	public static String localDateFormat(LocalDate localDate) {
		return localDate.format(dtf);
	}

	public static LocalDate strFormatToLocalDate(String str) {
		return LocalDate.parse(str, dtf);
	}
}

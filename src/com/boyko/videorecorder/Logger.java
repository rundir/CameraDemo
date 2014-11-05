package com.boyko.videorecorder;

import java.io.FileWriter;

import android.util.Log;

public class Logger {

	private static final boolean IS_NEED_TO_SAVE = true;
	private static FileWriter fileWriter;

	public static void e(String string, String string2, Exception e) {
		Log.e(string, string2, e);
		if (IS_NEED_TO_SAVE)
			printToFile(string + "\t" + string2, e);
	}

	public static void e(String string, String string2) {
		Log.e(string, string2);
		if (IS_NEED_TO_SAVE)
			printToFile(string + "\t" + string2);
	}

	public static void e(String string) {
		Log.e("EMPTY",string);
		if (IS_NEED_TO_SAVE)
			printToFile(string);
	}

	public static void w(String string, String string2) {
		Log.w(string, string2);
		if (IS_NEED_TO_SAVE)
			printToFile(string + "\t" + string2);
	}

	public static void d(String string) {
		d("LOG", string);
		
	}

	public static void d(String tag2, String string) {
		Log.d(tag2, string);
		if (IS_NEED_TO_SAVE)
			printToFile(tag2 + "\t" + string);
	}

	public static void i(String string, String string2) {
		Log.i(string, string2);
		if (IS_NEED_TO_SAVE)
			printToFile(string + "\t" + string2);
	}

	private static void printToFile(CharSequence text) {
		printToFile(text, null);
	}
	
	private static void printToFile(CharSequence text, Exception exc) {

//		File logFile = new File(Environment.getExternalStorageDirectory(),
//				"rapido_log.txt");
//		if (!logFile.exists()) {
//			try {
//				logFile.createNewFile();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//		}
//
//		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
//				Locale.getDefault());
//
//		try {
//			// BufferedWriter for performance, true to set append to file flag
//			fileWriter = new FileWriter(logFile,
//					true);
//			BufferedWriter buf = new BufferedWriter(fileWriter);
//			buf.append(df.format(new Date(System.currentTimeMillis())) + ": "
//					+ text);
//			buf.newLine();
//			
//			if(exc!=null){
//				PrintWriter pw = new PrintWriter(fileWriter);
//				exc.printStackTrace(pw);
//			}
//			buf.newLine();
//			buf.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}
	
	public static FileWriter getFileWriter(){
		return fileWriter;
	}

}

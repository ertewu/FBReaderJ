package zystudio.debug;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.util.Log;

public class FBDebug {

    private static boolean DEBUGABLE = true;
    
    private static final String LOAD_BOOK = "load_book";

    private static final String LIB_TREE="lib_tree";
    
    private static final String COLLECTION_CALL="collection_call";
    
    private static final boolean isWaitDebug=true;
    
    public static void logLoadBook(String msg) {
        mylog(LOAD_BOOK, msg);
    }
    
    public static void logLibTree(String msg){
        mylog(LIB_TREE,msg);
    }
    
    public static void logCollectionCall(String msg){
        mylog(COLLECTION_CALL,msg);
    }

    private static void mylog(String tag, String msg) {
        if (DEBUGABLE) {
            Log.i(tag, msg);
        }
    }
    
    public static boolean waitForDebug( ){
        if(isWaitDebug){
            android.os.Debug.waitForDebugger();
        }
        return true;
    }
    
    //以后用来和service,activity什么得作鉴别之用,进程就只有这两种能启动起来
    public static boolean waitForDebug(Context context){
        if(isWaitDebug){
            android.os.Debug.waitForDebugger();
        }
        return true;
    }

    public static String getProcessInfo(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int myPid = android.os.Process.myPid();
        for (RunningAppProcessInfo info : manager.getRunningAppProcesses()) {
            if (info.pid == myPid) {
                return myPid + ":" + info.processName;
            }
        }
        return "not found";
    }
}

package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.Arrays;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 *
 * Please read:
 *
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 *
 * before you start to get yourself familiarized with ContentProvider.
 *
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 *
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    private SQLiteDatabase grpMsgDb;
    private DataStorage dbStorage;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        grpMsgDb= dbStorage.getWritableDatabase();



        long row_id = grpMsgDb.insertWithOnConflict(TABLE_NAME,null,values,5);

        if (row_id != -1) {
            return uri.withAppendedPath(uri, Long.toString(row_id));
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.

        dbStorage = new DataStorage(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         * 
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         * 
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        grpMsgDb= dbStorage.getReadableDatabase();
        Log.v("res",selection);



        String sel= "key="+"'"+selection+"'";
        Cursor cursor=grpMsgDb.query(TABLE_NAME,null,sel,null,null,null,null);

        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }


    static String TABLE_NAME="GroupMsgTable";
    static int DB_VERSION=2;


    private static class DataStorage extends SQLiteOpenHelper {
        static String DB_NAME="GroupDB";
        static String KEY="key";
        static String VALUE="value";
        String CREATE_DB_TABLE =
                " CREATE TABLE " + TABLE_NAME +
                        " ( key TEXT NOT NULL UNIQUE, value TEXT NOT NULL);";

        DataStorage(Context context){
            super(context, DB_NAME, null, DB_VERSION);
            Log.v("DataStorage","db");
            Log.v("create",CREATE_DB_TABLE);
        }

        @Override
        public void onCreate(SQLiteDatabase db)
        {


            db.execSQL(CREATE_DB_TABLE);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion,
                              int newVersion) {
            db.execSQL(TABLE_NAME);
            onCreate(db);
        }
    }

}

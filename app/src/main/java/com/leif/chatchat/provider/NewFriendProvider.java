package com.leif.chatchat.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by leif on 6/3/16.
 */
public class NewFriendProvider extends ContentProvider {

    private static final String AUTHORITY = NewFriendProvider.class.getCanonicalName();

    public static final String NEW_FRIEND_TABLE = "friend";

    private static final String DB_NAME = "friend.db";

    private static final int DB_VERSION = 1;


    public static final Uri NEW_FRIEND_URI = Uri.parse("content://" + AUTHORITY + "/" + NEW_FRIEND_TABLE);

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;
    private static final UriMatcher URI_MATCHER;

    public static final int NEW_FRIEND = 1;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, NEW_FRIEND_TABLE, NEW_FRIEND);
    }

    public boolean onCreate() {
        dbHelper = new NewFriendDatabaseHelper(getContext());
        return (dbHelper == null) ? false : true;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        db = dbHelper.getWritableDatabase();
        Uri result = null;
        long rowId = 0;

        switch (URI_MATCHER.match(uri)) {
            case NEW_FRIEND:
                rowId = db.insert(NEW_FRIEND_TABLE, NewFriendColumns.ACCOUNT, contentValues);
                result = ContentUris.withAppendedId(uri, rowId);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            default:
        }
        Uri noteUri = ContentUris.withAppendedId(uri, rowId);
        getContext().getContentResolver().notifyChange(noteUri, null);
        return result;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        db = dbHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case NEW_FRIEND:
                count = db.delete(NEW_FRIEND_TABLE, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            default:
        }
            getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        db = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        Cursor ret = null;

        switch (URI_MATCHER.match(uri)) {
            case NEW_FRIEND:
                qb.setTables(NEW_FRIEND_TABLE);
                ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            default:
        }
        ret.setNotificationUri(getContext().getContentResolver(), uri);
        return ret;
    }

    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        db = dbHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case NEW_FRIEND:
                count = db.update(NEW_FRIEND_TABLE, contentValues, selection, selectionArgs);
                getContext().getContentResolver().notifyChange(uri, null);
                break;

            default:
        }

            Uri noteUri = ContentUris.withAppendedId(uri, count);
            getContext().getContentResolver().notifyChange(noteUri, null);
        return count;
    }

    public String getType(Uri uri) {
        return null;
    }

    private class NewFriendDatabaseHelper extends SQLiteOpenHelper {

        public NewFriendDatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + NewFriendProvider.NEW_FRIEND_TABLE + " ("
                    + NewFriendProvider.NewFriendColumns._ID + " INTEGER PRIMARY KEY, "
                    + NewFriendProvider.NewFriendColumns.ACCOUNT + " TEXT, "
                    + NewFriendProvider.NewFriendColumns.NAME + " TEXT, "
                    + NewFriendProvider.NewFriendColumns.AVATAR + " TEXT);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + NewFriendProvider.NEW_FRIEND_TABLE);
            onCreate(db);
        }
    }

    public static class NewFriendColumns implements BaseColumns {
        public static final String _ID = "id";
        public static final String ACCOUNT = "account";
        public static final String NAME = "name";
        public static final String AVATAR = "avatar";
    }
}

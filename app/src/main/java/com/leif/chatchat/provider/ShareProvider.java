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
public class ShareProvider extends ContentProvider {

    private static final String AUTHORITY = ShareProvider.class.getCanonicalName();

    public static final String SHARE_TABLE = "share";
    private static final String DB_NAME = "share.db";

    private static final int DB_VERSION = 1;

    public static final Uri SHARE_URI = Uri.parse("content://" + AUTHORITY + "/" + SHARE_TABLE);

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;
    private static final UriMatcher URI_MATCHER;

    public static final int SHARE = 1;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, SHARE_TABLE, SHARE);
    }

    public boolean onCreate() {
        dbHelper = new ShareDatabaseHelper(getContext());
        return (dbHelper == null) ? false : true;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        db = dbHelper.getWritableDatabase();
        Uri result = null;
        long rowId = 0;

        switch (URI_MATCHER.match(uri)) {
            case SHARE:
                rowId = db.insert(SHARE_TABLE, ShareColumns.ACCOUNT, contentValues);
                break;
            default:
        }
        result = ContentUris.withAppendedId(uri, rowId);
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    public int delete(Uri uri, String selection, String[] selectionArgs) {
        db = dbHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case SHARE:
                count = db.delete(SHARE_TABLE, selection, selectionArgs);
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
            case SHARE:
                qb.setTables(SHARE_TABLE);
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
            case SHARE:
                count = db.update(SHARE_TABLE, contentValues, selection, selectionArgs);
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

    private class ShareDatabaseHelper extends SQLiteOpenHelper {

        public ShareDatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + ShareProvider.SHARE_TABLE + " ("
                    + ShareProvider.ShareColumns._ID + " INTEGER PRIMARY KEY, "
                    + ShareProvider.ShareColumns.ACCOUNT + " TEXT, "
                    + ShareProvider.ShareColumns.DATE + " TEXT, "
                    + ShareProvider.ShareColumns.CONTENT + " TEXT, "
                    + ShareProvider.ShareColumns.PHOTO + " TEXT);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + ShareProvider.SHARE_TABLE);
            onCreate(db);
        }
    }

    public static class ShareColumns implements BaseColumns {
        public static final String _ID = "id";
        public static final String ACCOUNT = "account";
        public static final String DATE = "date";
        public static final String PHOTO = "photo";
        public static final String CONTENT = "content";
    }
}

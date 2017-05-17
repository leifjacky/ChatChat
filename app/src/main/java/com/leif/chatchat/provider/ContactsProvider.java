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

import com.leif.chatchat.ui.fragment.ContactFragment;
import com.leif.chatchat.ui.fragment.Dialog.NewFriendDialogFragment;

/**
 * Created by leif on 6/3/16.
 */
public class ContactsProvider extends ContentProvider {

    private static final String AUTHORITY = ContactsProvider.class.getCanonicalName();

    public static final String CONTACT_TABLE = "contact";
    private static final String CONTACT_GROUP_TABLE = "group";
    private static final String DB_NAME = "contact.db";

    private static final int DB_VERSION = 1;

    public static final Uri CONTACT_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTACT_TABLE);
    public static final Uri CONTACT_GROUP_URI = Uri.parse("content://" + AUTHORITY + "/" + CONTACT_GROUP_TABLE);

    private SQLiteOpenHelper dbHelper;
    private SQLiteDatabase db;
    private static final UriMatcher URI_MATCHER;

    public static final int CONTACTS = 1;
    public static final int GROUPS = 2;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(AUTHORITY, CONTACT_TABLE, CONTACTS);
        URI_MATCHER.addURI(AUTHORITY, CONTACT_GROUP_TABLE, GROUPS);
    }

    public boolean onCreate() {
        dbHelper = new ContactDatabaseHelper(getContext());
        return (dbHelper == null) ? false : true;
    }

    public Uri insert(Uri uri, ContentValues contentValues) {
        db = dbHelper.getWritableDatabase();
        Uri result = null;
        long rowId = 0;

        switch (URI_MATCHER.match(uri)) {
            case CONTACTS:
                rowId = db.insert(CONTACT_TABLE, ContactColumns.ACCOUNT, contentValues);
                result = ContentUris.withAppendedId(uri, rowId);
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
            case CONTACTS:
                count = db.delete(CONTACT_TABLE, selection, selectionArgs);
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
            case CONTACTS:
                qb.setTables(CONTACT_TABLE);
                ret = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
                break;
            case GROUPS:
                qb.setTables(CONTACT_TABLE);
                ret = qb.query(db, projection, selection, selectionArgs, ContactColumns.SECTION, null, sortOrder);
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
            case CONTACTS:
                count = db.update(CONTACT_TABLE, contentValues, selection, selectionArgs);
                break;

            default:
        }
        Uri noteUri = ContentUris.withAppendedId(uri, count);
        getContext().getContentResolver().notifyChange(noteUri, null);
        getContext().getContentResolver().notifyChange(ShareProvider.SHARE_URI, null);
        getContext().getContentResolver().notifyChange(NewFriendProvider.NEW_FRIEND_URI, null);
        getContext().getContentResolver().notifyChange(SMSProvider.SMS_URI, null);
        return count;
    }

    public String getType(Uri uri) {
        return null;
    }

    private class ContactDatabaseHelper extends SQLiteOpenHelper {

        public ContactDatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + ContactsProvider.CONTACT_TABLE + " ("
                    + ContactsProvider.ContactColumns._ID + " INTEGER PRIMARY KEY, "
                    + ContactsProvider.ContactColumns.ACCOUNT + " TEXT, "
                    + ContactsProvider.ContactColumns.NAME + " TEXT, "
                    + ContactsProvider.ContactColumns.SORT + " TEXT, "
                    + ContactsProvider.ContactColumns.SECTION + " TEXT,"
                    + ContactsProvider.ContactColumns.AVATAR + " TEXT);");
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + ContactsProvider.CONTACT_TABLE);
            onCreate(db);
        }
    }

    public static class ContactColumns implements BaseColumns {
        public static final String _ID = "id";
        public static final String ACCOUNT = "account";
        public static final String NAME = "name";
        public static final String SORT = "sort";
        public static final String SECTION = "section";
        public static final String AVATAR = "avatar";
        public static final String PASSWORD = "password";
    }
}

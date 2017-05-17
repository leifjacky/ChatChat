package com.leif.chatchat.util;

import android.database.Cursor;

import com.leif.chatchat.IM;
import com.leif.chatchat.provider.ContactsProvider;

import java.util.HashMap;

/**
 * Created by leif on 6/7/16.
 */
public class StringUtils {
    public static HashMap<String, String> analyse(String st) {
        String key, value;
        HashMap<String, String> map = new HashMap<String, String>();
        while (!st.equals("")) {
            value = st.substring(st.lastIndexOf('=') + 1, st.length());
            key = st.substring(st.lastIndexOf('&') + 1, st.lastIndexOf('='));
            map.put(key, value);
            System.out.println(key + ' ' + value);
            st = st.substring(0, st.lastIndexOf('&'));
        }
        return map;
    }

    public static String getSessionID(String strA, String strB) {
        if (strA.compareTo(strB) <= 0) {
            return strA + "_" + strB;
        } else {
            return strB + "_" + strA;
        }
    }

    public static String getSessionName(String st) {
        String strA = st.substring(0, st.lastIndexOf('_'));
        String strB = st.substring(st.lastIndexOf('_') + 1);
        Cursor cursor;
        if (strA.equals(IM.account)) {
            cursor = IM.im.getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{strB}, null);
        } else {
            cursor = IM.im.getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{strA}, null);
        }
        cursor.moveToFirst();
        return cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.NAME));
    }

    public static String getOhterAccount(String st) {
        String strA = st.substring(0, st.lastIndexOf('_'));
        String strB = st.substring(st.lastIndexOf('_') + 1);
        Cursor cursor;
        if (strA.equals(IM.account)) {
            cursor = IM.im.getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{strB}, null);
        } else {
            cursor = IM.im.getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{strA}, null);
        }
        cursor.moveToFirst();
        return cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.ACCOUNT));
    }
}

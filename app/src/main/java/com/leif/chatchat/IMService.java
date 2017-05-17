package com.leif.chatchat;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.leif.chatchat.provider.ContactsProvider;
import com.leif.chatchat.provider.NewFriendProvider;
import com.leif.chatchat.provider.SMSProvider;
import com.leif.chatchat.provider.ShareProvider;
import com.leif.chatchat.ui.view.ScreenListener;
import com.leif.chatchat.util.PinYin;
import com.leif.chatchat.util.StringUtils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class IMService extends Service {

    private static final int DB_VERSION = 1;

    private static final String DB_NAME = "contact.db";
    private static final String TAG = "IMService";
    private static final String CONTENT = "content";
    private static final String PHOTO = "photo";

    private final int OK = 0;
    private final int ERROR_ACCOUNT = 1;
    private final int ERROR_CONNECT = 2;
    private final int ERROR_UNKNOWN = -1;

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private ConnectivityManager connectivityManager;
    private NetworkInfo info;
    private ScreenListener screenListener;

    public static boolean Online, Pause, LockScreen;

    public void onCreate() {
        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editor = preferences.edit();

        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mReceiver, mFilter);

        Pause = LockScreen = false;
        screenListener = new ScreenListener(this);
        screenListener.begin(new ScreenListener.ScreenStateListener() {
            public void onScreenOn() {
            }

            public void onScreenOff() {
                LockScreen = true;
            }

            public void onUserPresent() {
                LockScreen = false;
            }
        });
    }

    Socket init(String host, int port) {
        Socket socket = null;
        try {
            socket = new Socket(host, port);
            return socket;
        } catch (IOException e) {
            e.printStackTrace();
            return socket;
        }
    }

    public int login(String account, String password) {

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new LoginThread(account, password, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class LoginThread implements Callable<Integer> {

        String account, password, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        public LoginThread(String account, String password, CountDownLatch latch) {
            this.account = account;
            this.password = password;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("LOGIN" + "&account=" + account + "&password=" + password);
                result = reader.readLine();
                Log.d(TAG, result);
                if (result.startsWith("ACCEPT")) {
                    HashMap<String, String> map = StringUtils.analyse(result.substring("ACCEPT".length()));
                    IM.account_id = Integer.valueOf(map.get("id"));
                    IM.avatar = map.get("avatar");
                    IM.account = map.get("account");
                    IM.nickname = map.get("nickname");
                    IM.key = map.get("key");

                    String last = preferences.getString("lastAccount", "");
                    if (!account.equals(last)) {
                        getContentResolver().delete(ContactsProvider.CONTACT_URI, null, null);
                        getContentResolver().delete(ShareProvider.SHARE_URI, null, null);
                        getContentResolver().delete(NewFriendProvider.NEW_FRIEND_URI, null, null);
                        getContentResolver().delete(SMSProvider.SMS_URI, null, null);
                    }

                    Uri uri = null;
                    while (true) {
                        result = reader.readLine();
                        if (result.equals("END"))
                            break;

                        map = StringUtils.analyse(result);

                        ContentValues values = new ContentValues();
                        values.put(ContactsProvider.ContactColumns.ACCOUNT, map.get(ContactsProvider.ContactColumns.ACCOUNT));
                        values.put(ContactsProvider.ContactColumns.NAME, map.get(ContactsProvider.ContactColumns.NAME));
                        String sortStr = PinYin.getPinYin(map.get(ContactsProvider.ContactColumns.NAME));
                        values.put(ContactsProvider.ContactColumns.SORT, sortStr);
                        values.put(ContactsProvider.ContactColumns.SECTION, sortStr.substring(0, 1).toUpperCase(Locale.ENGLISH));
                        values.put(ContactsProvider.ContactColumns.AVATAR, map.get(ContactsProvider.ContactColumns.AVATAR));
                        if (!map.get(ContactsProvider.ContactColumns.AVATAR).equals("null") && !new File(Environment.getExternalStorageDirectory() + map.get(ContactsProvider.ContactColumns.AVATAR)).exists()) {
                            getFile(map.get(ContactsProvider.ContactColumns.AVATAR), Environment.getExternalStorageDirectory() + map.get(ContactsProvider.ContactColumns.AVATAR));
                        }

                        if (getContentResolver().update(ContactsProvider.CONTACT_URI, values, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{map.get(ContactsProvider.ContactColumns.ACCOUNT)}) == 0) {
                            getContentResolver().insert(ContactsProvider.CONTACT_URI, values);
                        }
                    }

                    editor.putString("lastAccount", IM.account);
                    editor.commit();

                    ret = OK;
                } else if (result.startsWith("FAILED")) {
                    ret = ERROR_ACCOUNT;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            } finally {
                latch.countDown();
                return ret;
            }
        }
    }

    public int sign(String account, String password) {

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new SignThread(account, password, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }


    }

    private class SignThread implements Callable<Integer> {

        String account, password, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        public SignThread(String account, String password, CountDownLatch latch) {
            this.password = password;
            this.account = account;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("REGISTER" + "&account=" + account + "&password=" + password);
                result = reader.readLine();
                Log.d(TAG, result);

                if (result.equals("ACCEPT")) {
                    ret = OK;
                } else if (result.equals("FAILED")) {
                    ret = ERROR_ACCOUNT;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            }
            latch.countDown();
            return ret;
        }
    }

    public int changSetting(String information) {

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new cSettingThread(information, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class cSettingThread implements Callable<Integer> {

        String information, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        public cSettingThread(String information, CountDownLatch latch) {
            this.information = information;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("UPDATE" + information);
                printer.flush();
                result = reader.readLine();
                Log.d(TAG, result);
                if (result.equals("UPDATED")) {
                    ret = OK;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            }
            latch.countDown();
            return ret;
        }
    }

    public int upload(String resource, String path) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new UploadThread(resource, path, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class UploadThread implements Callable<Integer> {

        String resource, path;
        CountDownLatch latch;
        int ret = ERROR_UNKNOWN;

        public UploadThread(String resource, String path, CountDownLatch latch) {
            this.resource = resource;
            this.path = path;
            this.latch = latch;
        }

        public Integer call() throws Exception {
            int length = 0;
            double sumL = 0;
            byte[] sendBytes = null;
            Socket fileSocket = null;
            DataOutputStream dataOutputStream = null;
            FileInputStream fileInputStream = null;
            boolean bool = false;

            try {
                File file = new File(resource); //要传输的文件路径
                long l = file.length();
                fileSocket = init(IM.HOST, IM.FILEPORT);
                dataOutputStream = new DataOutputStream(fileSocket.getOutputStream());
                fileInputStream = new FileInputStream(file);

                dataOutputStream.writeUTF("UPLOAD&" + path + "\r\n");
                dataOutputStream.flush();

                sendBytes = new byte[1024];
                while ((length = fileInputStream.read(sendBytes, 0, sendBytes.length)) > 0) {
                    sumL += length;
                    Log.d("UPLOAD", ("已传输：" + ((sumL / l) * 100) + "%"));
                    dataOutputStream.write(sendBytes, 0, length);
                    dataOutputStream.flush();
                }

                if (sumL == l) {
                    ret = OK;
                    bool = true;
                }
            } catch (Exception e) {
                Log.d("UPLOAD", "客户端文件传输异常");
                bool = false;
                e.printStackTrace();
                ret = ERROR_CONNECT;
            } finally {
                try {
                    if (dataOutputStream != null)
                        dataOutputStream.close();
                    if (fileInputStream != null)
                        fileInputStream.close();
                    if (fileSocket != null)
                        fileSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Log.d("UPLOAD", bool ? "成功" : "失败");
                latch.countDown();
                return ret;
            }
        }
    }

    public int getFile(String resource, String path) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new GetThread(resource, path, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class GetThread implements Callable<Integer> {

        String resource, path;
        CountDownLatch latch;
        Socket socket;
        InputStream in;
        PrintStream printer;
        FileOutputStream out;
        int ret = ERROR_UNKNOWN;

        public GetThread(String resource, String path, CountDownLatch latch) {
            this.resource = resource;
            this.path = path;
            this.latch = latch;
        }

        public Integer call() throws Exception {
            try {
                socket = init(IM.HOST, IM.FILEPORT);
                if (socket == null) {
                    throw new IOException();
                }

                File file = new File(path);
                File parentFile = file.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }

                in = socket.getInputStream();
                out = new FileOutputStream(file);
                printer = new PrintStream(socket.getOutputStream(), true);
                printer.println("GET&" + resource);
                printer.flush();

                int len = 0;
                byte b[] = new byte[1024];
                while ((len = in.read(b, 0, 1024)) > 0) {
                    out.write(b, 0, len);
                    out.flush();
                }
                out.close();
                in.close();
                socket.close();

                ret = OK;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            } finally {
                latch.countDown();
                return ret;
            }
        }
    }

    public int search(String account) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new SearchThread(account, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class SearchThread implements Callable<Integer> {

        String id, account, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        private SharedPreferences preferences;
        private SharedPreferences.Editor editor;

        public SearchThread(String account, CountDownLatch latch) {
            this.account = account;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("SEARCH&" + ContactsProvider.ContactColumns.ACCOUNT + "=" + account);
                printer.flush();
                result = reader.readLine();
                Log.d(TAG, result);
                if (result.startsWith("FOUND")) {
                    String account, nickname, avatar;
                    Map<String, String> map = StringUtils.analyse(result.substring("FOUND".length()));

                    id = map.get(ContactsProvider.ContactColumns._ID);
                    account = map.get(ContactsProvider.ContactColumns.ACCOUNT);
                    nickname = map.get(ContactsProvider.ContactColumns.NAME);
                    avatar = map.get(ContactsProvider.ContactColumns.AVATAR);
                    if (!avatar.equals("null")) {
                        getFile(avatar, Environment.getExternalStorageDirectory() + avatar);
                    }

                    editor = preferences.edit();
                    editor.putString("search_id", id);
                    editor.putString("search_account", account);
                    editor.putString("search_nickname", nickname);
                    editor.putString("search_avatar", avatar);
                    editor.commit();

                    ret = OK;
                } else {
                    ret = ERROR_ACCOUNT;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            }
            latch.countDown();
            return ret;
        }
    }

    public int addContact(String account, String id) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new addContactThread(account, id, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class addContactThread implements Callable<Integer> {

        String account, id, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        public addContactThread(String account, String id, CountDownLatch latch) {
            this.account = account;
            this.id = id;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("CONTACT&fromAccount=" + IM.account + "&toAccount=" + id);
                printer.flush();
                result = reader.readLine();
                Log.d(TAG, result);
                if (result.startsWith("OK")) {
                    ret = OK;
                } else {
                    ret = ERROR_ACCOUNT;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            }
            latch.countDown();
            return ret;
        }
    }

    public int addShare(String fromAccount, String content, String photo) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new addShareThread(fromAccount, content, photo, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class addShareThread implements Callable<Integer> {

        String fromAccount, content, photo, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        public addShareThread(String fromAccount, String content, String photo, CountDownLatch latch) {
            this.fromAccount = fromAccount;
            this.content = content;
            this.photo = photo;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("SHARE&fromAccount" + "=" + fromAccount
                        + "&" + CONTENT + "=" + content
                        + "&" + PHOTO + "=" + photo);
                printer.flush();
                result = reader.readLine();
                Log.d(TAG, result);
                if (result.equals("OK")) {
                    ret = OK;
                } else {
                    ret = ERROR_ACCOUNT;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            }
            latch.countDown();
            return ret;
        }
    }

    public int friendResult(String fromAccount, String toAccount) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new friendResultThread(fromAccount, toAccount, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class friendResultThread implements Callable<Integer> {

        String fromAccount, toAccount, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        public friendResultThread(String fromAccount, String toAccount, CountDownLatch latch) {
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("ACCEPTFRIEND&fromAccount" + "=" + fromAccount
                        + "&toAccount=" + toAccount);
                printer.flush();
                result = reader.readLine();
                Log.d(TAG, result);
                if (result.equals("OK")) {
                    ret = OK;
                } else {
                    ret = ERROR_ACCOUNT;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            }
            latch.countDown();
            return ret;
        }
    }

    public int sendSMS(String fromAccount, String toAccount, String content) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new SMSThread(fromAccount, toAccount, content, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class SMSThread implements Callable<Integer> {

        String fromAccount, toAccount, content, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        public SMSThread(String fromAccount, String toAccount, String content, CountDownLatch latch) {
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.content = content;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("SMS&fromAccount" + "=" + fromAccount
                        + "&toAccount=" + toAccount
                        + "&content=" + content);
                printer.flush();
                result = reader.readLine();
                Log.d(TAG, result);
                if (result.equals("OK")) {
                    ret = OK;
                } else {
                    ret = ERROR_ACCOUNT;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            }
            latch.countDown();
            return ret;
        }
    }

    public int sendPHOTO(String fromAccount, String toAccount, String resource, String path) {
        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new UploadThread(resource, path, latch));
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }

        latch = new CountDownLatch(1);
        pool = Executors.newCachedThreadPool();

        try {
            Future<Integer> ret = pool.submit(new sendPHOTOThread(fromAccount, toAccount, "/" + path, latch));
            latch.await();
            return ret.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return ERROR_UNKNOWN;
        }
    }

    private class sendPHOTOThread implements Callable<Integer> {

        String fromAccount, toAccount, content, result;
        CountDownLatch latch;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;

        public sendPHOTOThread(String fromAccount, String toAccount, String content, CountDownLatch latch) {
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.content = content;
            this.latch = latch;
        }

        public Integer call() {
            try {
                socket = init(IM.HOST, IM.IMPORT);
                if (socket == null) {
                    throw new IOException();
                }

                printer = new PrintStream(socket.getOutputStream(), true);
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                printer.println("PHOTO&fromAccount" + "=" + fromAccount
                        + "&toAccount=" + toAccount
                        + "&content=" + content);
                printer.flush();
                result = reader.readLine();
                Log.d(TAG, result);
                if (result.equals("OK")) {
                    ret = OK;
                } else {
                    ret = ERROR_ACCOUNT;
                }
            } catch (IOException e) {
                e.printStackTrace();
                ret = ERROR_CONNECT;
            }
            latch.countDown();
            return ret;
        }
    }

    public void onlineService() {
        Online = true;
        ExecutorService pool = Executors.newCachedThreadPool();
        pool.submit(new onlineThread());
    }

    private class onlineThread implements Callable<Void> {

        String result;
        Socket socket;
        PrintStream printer;
        BufferedReader reader;
        int ret = ERROR_UNKNOWN;
        Uri uri;

        public Void call() {
            while (true) {
                if (!Online) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }

                try {
                    socket = init(IM.HOST, IM.ONLINEPORT);
                    if (socket == null) {
                        throw new IOException();
                    }

                    printer = new PrintStream(socket.getOutputStream(), true);
                    reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    HashMap<String, String> map;
                    ContentValues values;

                    while (true) {
                        if (!Online) {
                            return null;
                        }

                        printer.println("SMS&account" + "=" + IM.account);
                        printer.flush();
                        result = reader.readLine();
                        int num = Integer.valueOf(result);
                        if (num > 0) {
                            System.out.println(result);
                            for (int i = 0; i < num; i++) {
                                result = reader.readLine();
                                System.out.println(result);
                                if (result.startsWith("SHARE")) {
                                    map = StringUtils.analyse(result.substring("SHARE".length()));

                                    values = new ContentValues();
                                    values.put(ShareProvider.ShareColumns.ACCOUNT, map.get(ShareProvider.ShareColumns.ACCOUNT));
                                    values.put(ShareProvider.ShareColumns.DATE, map.get(ShareProvider.ShareColumns.DATE));
                                    values.put(ShareProvider.ShareColumns.PHOTO, map.get(ShareProvider.ShareColumns.PHOTO));
                                    values.put(ShareProvider.ShareColumns.CONTENT, map.get(ShareProvider.ShareColumns.CONTENT));

                                    if (!map.get(ShareProvider.ShareColumns.PHOTO).equals("/null")) {
                                        getFile(map.get(ShareProvider.ShareColumns.PHOTO), Environment.getExternalStorageDirectory() + map.get(ShareProvider.ShareColumns.PHOTO));
                                    }
                                    getContentResolver().insert(ShareProvider.SHARE_URI, values);
                                } else if (result.startsWith("UPDATE")) {
                                    map = StringUtils.analyse(result.substring("UPDATE".length()));
                                    values = new ContentValues();
                                    values.put(ContactsProvider.ContactColumns.ACCOUNT, map.get(ContactsProvider.ContactColumns.ACCOUNT));
                                    if (map.get(ContactsProvider.ContactColumns.NAME) != null) {
                                        values.put(ContactsProvider.ContactColumns.NAME, map.get(ContactsProvider.ContactColumns.NAME));
                                    }
                                    if (map.get(ContactsProvider.ContactColumns.AVATAR) != null) {
                                        values.put(ContactsProvider.ContactColumns.AVATAR, map.get(ContactsProvider.ContactColumns.AVATAR));
                                        getFile(map.get(ContactsProvider.ContactColumns.AVATAR), Environment.getExternalStorageDirectory() + map.get(ContactsProvider.ContactColumns.AVATAR));
                                    }
                                    getContentResolver().update(ContactsProvider.CONTACT_URI, values, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{map.get(ContactsProvider.ContactColumns.ACCOUNT)});
                                } else if (result.startsWith("SMS")) {
                                    map = StringUtils.analyse(result.substring("SMS".length()));

                                    String fromAccount, type, avatar, content, session_id, session_name, time, otherAccount;
                                    fromAccount = map.get("fromAccount");
                                    content = map.get("content");
                                    session_id = map.get("session_id");
                                    otherAccount = StringUtils.getOhterAccount(session_id);
                                    session_name = StringUtils.getSessionName(session_id);
                                    type = "SMS";
                                    time = map.get("date");
                                    time = time.substring(0, time.lastIndexOf('.'));
                                    Cursor cursor = getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{fromAccount}, null);
                                    cursor.moveToFirst();
                                    avatar = cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.AVATAR));
                                    cursor.close();

                                    File avatarFile = new File(Environment.getExternalStorageDirectory(), avatar);
                                    if (!avatarFile.exists()){
                                        getFile(avatar, Environment.getExternalStorageDirectory() + avatar);
                                    }

                                    values = new ContentValues();
                                    values.put(SMSProvider.SMSColumns.TYPE, type);
                                    values.put(SMSProvider.SMSColumns.FROM_ACCOUNT, fromAccount);
                                    values.put(SMSProvider.SMSColumns.FROM_AVATAR, avatar);
                                    values.put(SMSProvider.SMSColumns.CONTENT, content);
                                    values.put(SMSProvider.SMSColumns.SESSION_ID, session_id);
                                    values.put(SMSProvider.SMSColumns.SESSION_NAME, session_name);
                                    values.put(SMSProvider.SMSColumns.OTHER_ACCOUNT, otherAccount);
                                    values.put(SMSProvider.SMSColumns.TIME, time);
                                    values.put(SMSProvider.SMSColumns.CLOCK, time.substring(11, 16));
                                    getContentResolver().insert(SMSProvider.SMS_URI, values);

                                    int count = preferences.getInt("count_" + session_id, 0);
                                    editor = preferences.edit();
                                    editor.putInt("count_" + session_id, count + 1);
                                    editor.putString("sessionName_" + session_id, session_name);
                                    editor.putString("otherAvatar_" + session_id, "/chatchat/avatar/avatar_" + otherAccount + ".jpg");
                                    editor.commit();

                                    if (LockScreen || (Pause && !fromAccount.equals(IM.account) && !session_id.equals(IM.currentSession))) {
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

                                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                                        builder.setContentTitle(session_name)
                                                .setContentText(content)
                                                .setContentIntent(pendingIntent)
                                                .setDeleteIntent(pendingIntent)
                                                .setTicker(session_name + ": " + content)
                                                .setWhen(System.currentTimeMillis())
                                                .setPriority(Notification.PRIORITY_DEFAULT)
                                                .setDefaults(Notification.DEFAULT_ALL)
                                                .setOngoing(false)
                                                .setSmallIcon(R.drawable.ic_launcher);
                                        Notification notification = builder.build();
                                        notification.flags = Notification.FLAG_AUTO_CANCEL;

                                        notificationManager.notify(1, notification);
                                    }

                                    getContentResolver().notifyChange(SMSProvider.SMS_URI, null);
                                } else if (result.startsWith("PHOTO")) {
                                    map = StringUtils.analyse(result.substring("PHOTO".length()));

                                    String fromAccount, type, avatar, content, session_id, session_name, time, otherAccount;
                                    fromAccount = map.get("fromAccount");
                                    content = map.get("content");
                                    session_id = map.get("session_id");
                                    otherAccount = StringUtils.getOhterAccount(session_id);
                                    session_name = StringUtils.getSessionName(session_id);
                                    type = "PHOTO";
                                    time = map.get("date");
                                    Cursor cursor = getContentResolver().query(ContactsProvider.CONTACT_URI, null, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{fromAccount}, null);
                                    cursor.moveToFirst();
                                    avatar = cursor.getString(cursor.getColumnIndex(ContactsProvider.ContactColumns.AVATAR));
                                    cursor.close();

                                    int ret = getFile(content, Environment.getExternalStorageDirectory() + content);

                                    values = new ContentValues();
                                    values.put(SMSProvider.SMSColumns.TYPE, type);
                                    values.put(SMSProvider.SMSColumns.FROM_ACCOUNT, fromAccount);
                                    values.put(SMSProvider.SMSColumns.FROM_AVATAR, avatar);
                                    values.put(SMSProvider.SMSColumns.CONTENT, content);
                                    values.put(SMSProvider.SMSColumns.SESSION_ID, session_id);
                                    values.put(SMSProvider.SMSColumns.SESSION_NAME, session_name);
                                    values.put(SMSProvider.SMSColumns.OTHER_ACCOUNT, otherAccount);
                                    values.put(SMSProvider.SMSColumns.TIME, time);
                                    values.put(SMSProvider.SMSColumns.CLOCK, time.substring(11, 16));
                                    getContentResolver().insert(SMSProvider.SMS_URI, values);

                                    int count = preferences.getInt("count_" + session_id, 0);
                                    editor = preferences.edit();
                                    editor.putInt("count_" + session_id, count + 1);
                                    editor.putString("sessionName_" + session_id, session_name);
                                    editor.putString("otherAvatar_" + session_id, "/chatchat/avatar/avatar_" + otherAccount + ".jpg");
                                    editor.commit();

                                    if (LockScreen || (Pause && !fromAccount.equals(IM.account) && !session_id.equals(IM.currentSession))) {
                                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);

                                        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext());
                                        builder.setContentTitle(session_name)
                                                .setContentText("收到一张图片")
                                                .setContentIntent(pendingIntent)
                                                .setDeleteIntent(pendingIntent)
                                                .setTicker(session_name + ": " + content)
                                                .setWhen(System.currentTimeMillis())
                                                .setPriority(Notification.PRIORITY_DEFAULT)
                                                .setDefaults(Notification.DEFAULT_ALL)
                                                .setOngoing(false)
                                                .setSmallIcon(R.drawable.ic_launcher);
                                        Notification notification = builder.build();
                                        notification.flags = Notification.FLAG_AUTO_CANCEL;

                                        notificationManager.notify(1, notification);
                                    }

                                    getContentResolver().notifyChange(SMSProvider.SMS_URI, null);
                                } else if (result.startsWith("NEWFRIEND")) {
                                    map = StringUtils.analyse(result.substring("NEWFRIEND".length()));

                                    values = new ContentValues();
                                    values.put(NewFriendProvider.NewFriendColumns.ACCOUNT, map.get(NewFriendProvider.NewFriendColumns.ACCOUNT));
                                    values.put(NewFriendProvider.NewFriendColumns.NAME, map.get(NewFriendProvider.NewFriendColumns.NAME));
                                    values.put(NewFriendProvider.NewFriendColumns.AVATAR, map.get(NewFriendProvider.NewFriendColumns.AVATAR));
                                    if (!map.get(NewFriendProvider.NewFriendColumns.AVATAR).equals("null") && !new File(Environment.getExternalStorageDirectory() + map.get(NewFriendProvider.NewFriendColumns.AVATAR)).exists()) {
                                        getFile(map.get(NewFriendProvider.NewFriendColumns.AVATAR), Environment.getExternalStorageDirectory() + map.get(NewFriendProvider.NewFriendColumns.AVATAR));
                                    }

                                    if (getContentResolver().update(NewFriendProvider.NEW_FRIEND_URI, values, NewFriendProvider.NewFriendColumns.ACCOUNT + " = ?", new String[]{map.get(NewFriendProvider.NewFriendColumns.ACCOUNT)}) == 0) {
                                        getContentResolver().insert(NewFriendProvider.NEW_FRIEND_URI, values);
                                    }


                                } else if (result.startsWith("ACFRIEND")) {
                                    map = StringUtils.analyse(result.substring("ACFRIEND".length()));

                                    values = new ContentValues();
                                    values.put(ContactsProvider.ContactColumns.ACCOUNT, map.get(ContactsProvider.ContactColumns.ACCOUNT));
                                    values.put(ContactsProvider.ContactColumns.NAME, map.get(ContactsProvider.ContactColumns.NAME));
                                    String sortStr = PinYin.getPinYin(map.get(ContactsProvider.ContactColumns.NAME));
                                    values.put(ContactsProvider.ContactColumns.SORT, sortStr);
                                    values.put(ContactsProvider.ContactColumns.SECTION, sortStr.substring(0, 1).toUpperCase(Locale.ENGLISH));
                                    values.put(ContactsProvider.ContactColumns.AVATAR, map.get(ContactsProvider.ContactColumns.AVATAR));
                                    if (!map.get(ContactsProvider.ContactColumns.AVATAR).equals("null") && !new File(Environment.getExternalStorageDirectory() + map.get(ContactsProvider.ContactColumns.AVATAR)).exists()) {
                                        getFile(map.get(ContactsProvider.ContactColumns.AVATAR), Environment.getExternalStorageDirectory() + map.get(ContactsProvider.ContactColumns.AVATAR));
                                    }

                                    if (getContentResolver().update(ContactsProvider.CONTACT_URI, values, ContactsProvider.ContactColumns.ACCOUNT + " = ?", new String[]{map.get(ContactsProvider.ContactColumns.ACCOUNT)}) == 0) {
                                        getContentResolver().insert(ContactsProvider.CONTACT_URI, values);
                                    }
                                }
                            }
                        }
                        Thread.sleep(2000);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                Log.d("tag", "网络状态已经改变");
                connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                info = connectivityManager.getActiveNetworkInfo();
                if (info != null && info.isAvailable()) {
                    String name = info.getTypeName();
                    Log.d("tag", "当前网络名称：" + name);
                    new Thread(new RecoverThread()).start();
                } else {
                    Log.d("tag", "没有可用网络");
                }
            }
        }
    };

    private class RecoverThread implements Runnable {
        public void run() {
            if (Online) {
                Online = false;
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Online = true;
                onlineService();
            }
        }
    }

    public IBinder onBind(Intent intent) {
        return new IMBinder();
    }

    public class IMBinder extends Binder {
        public IMService getService() {
            return IMService.this;
        }
    }

    public void onDestroy() {
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }
}
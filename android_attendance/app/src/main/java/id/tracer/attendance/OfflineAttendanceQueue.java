package id.tracer.attendance;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;

public class OfflineAttendanceQueue extends SQLiteOpenHelper {
    static final int SYNC_JOB_ID = 7319;
    private final Context context;

    public OfflineAttendanceQueue(Context context) {
        super(context, "attendance_offline.db", null, 3);
        this.context = context.getApplicationContext();
        OfflineSyncJobService.appContext = this.context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
            "CREATE TABLE pending_attendance (" +
            "event_id TEXT PRIMARY KEY," +
            "base_url TEXT NOT NULL," +
            "sync_token TEXT NOT NULL," +
            "username TEXT NOT NULL," +
            "fullname TEXT NOT NULL," +
            "mode TEXT NOT NULL," +
            "shift_id TEXT NOT NULL," +
            "captured_at TEXT NOT NULL," +
            "latitude TEXT NOT NULL," +
            "longitude TEXT NOT NULL," +
            "address TEXT NOT NULL," +
            "face_score INTEGER NOT NULL DEFAULT 0," +
            "device_info TEXT NOT NULL DEFAULT ''," +
            "app_version TEXT NOT NULL DEFAULT ''," +
            "photo_path TEXT NOT NULL)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS pending_attendance (" +
                "event_id TEXT PRIMARY KEY," +
                "base_url TEXT NOT NULL DEFAULT ''," +
                "sync_token TEXT NOT NULL DEFAULT ''," +
                "username TEXT NOT NULL DEFAULT ''," +
                "fullname TEXT NOT NULL DEFAULT ''," +
                "mode TEXT NOT NULL DEFAULT 'clock_in'," +
                "shift_id TEXT NOT NULL DEFAULT '1'," +
                "captured_at TEXT NOT NULL DEFAULT ''," +
                "latitude TEXT NOT NULL DEFAULT ''," +
                "longitude TEXT NOT NULL DEFAULT ''," +
                "address TEXT NOT NULL DEFAULT ''," +
                "face_score INTEGER NOT NULL DEFAULT 0," +
                "device_info TEXT NOT NULL DEFAULT ''," +
                "app_version TEXT NOT NULL DEFAULT ''," +
                "photo_path TEXT NOT NULL DEFAULT '')"
            );
            if (!hasColumn(db, "event_id")) {
                db.execSQL("DROP TABLE IF EXISTS pending_attendance");
                onCreate(db);
                return;
            }
            addColumnIfMissing(db, "base_url", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "sync_token", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "username", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "fullname", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "mode", "TEXT NOT NULL DEFAULT 'clock_in'");
            addColumnIfMissing(db, "shift_id", "TEXT NOT NULL DEFAULT '1'");
            addColumnIfMissing(db, "captured_at", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "latitude", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "longitude", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "address", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "face_score", "INTEGER NOT NULL DEFAULT 0");
            addColumnIfMissing(db, "device_info", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "app_version", "TEXT NOT NULL DEFAULT ''");
            addColumnIfMissing(db, "photo_path", "TEXT NOT NULL DEFAULT ''");
        } catch (Exception ignored) {
            db.execSQL("DROP TABLE IF EXISTS pending_attendance");
            onCreate(db);
        }
    }

    private boolean hasColumn(SQLiteDatabase db, String column) {
        Cursor cursor = db.rawQuery("PRAGMA table_info(pending_attendance)", null);
        try {
            while (cursor.moveToNext()) {
                if (column.equals(cursor.getString(1))) {
                    return true;
                }
            }
        } finally {
            cursor.close();
        }
        return false;
    }

    private void addColumnIfMissing(SQLiteDatabase db, String column, String definition) {
        try {
            if (!hasColumn(db, column)) {
                db.execSQL("ALTER TABLE pending_attendance ADD COLUMN " + column + " " + definition);
            }
        } catch (Exception ignored) {
        }
    }

    public String enqueue(
        String baseUrl,
        String syncToken,
        String username,
        String fullname,
        String mode,
        String shiftId,
        String capturedAt,
        double latitude,
        double longitude,
        String address,
        int faceScore,
        String deviceInfo,
        String appVersion,
        Bitmap photo
    ) throws Exception {
        String eventId = UUID.randomUUID().toString().replace("-", "");
        File queueFolder = new File(getContextFilesDir(), "attendance_queue");
        if (!queueFolder.exists() && !queueFolder.mkdirs()) {
            throw new Exception("Folder antrean tidak dapat dibuat");
        }
        File photoFile = new File(queueFolder, eventId + ".jpg");
        FileOutputStream output = new FileOutputStream(photoFile);
        photo.compress(Bitmap.CompressFormat.JPEG, 88, output);
        output.flush();
        output.close();

        ContentValues values = new ContentValues();
        values.put("event_id", eventId);
        values.put("base_url", baseUrl);
        values.put("sync_token", syncToken);
        values.put("username", username);
        values.put("fullname", fullname);
        values.put("mode", mode);
        values.put("shift_id", shiftId);
        values.put("captured_at", capturedAt);
        values.put("latitude", String.valueOf(latitude));
        values.put("longitude", String.valueOf(longitude));
        values.put("address", address == null ? "" : address);
        values.put("face_score", Math.max(0, Math.min(100, faceScore)));
        values.put("device_info", deviceInfo == null ? "" : deviceInfo);
        values.put("app_version", appVersion == null ? "" : appVersion);
        values.put("photo_path", photoFile.getAbsolutePath());
        if (getWritableDatabase().insert("pending_attendance", null, values) < 0) {
            photoFile.delete();
            throw new Exception("Data antrean tidak dapat disimpan");
        }
        schedule(getContext());
        OfflineSyncJobService.syncNow(getContext());
        return eventId;
    }

    public int pendingCount() {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT COUNT(*) FROM pending_attendance",
            null
        );
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public int removeEventsAlreadyOnServer(String username, JSONArray serverRows) {
        if (username == null || username.trim().isEmpty() || serverRows == null) {
            return 0;
        }
        Set<String> clockInDates = new HashSet<>();
        Set<String> clockOutDates = new HashSet<>();
        for (int index = 0; index < serverRows.length(); index += 1) {
            JSONObject row = serverRows.optJSONObject(index);
            if (row == null) {
                continue;
            }
            String date = row.optString("date_key", "").trim();
            if (date.isEmpty()) {
                String rawDate = row.optString("tanggal", "").trim();
                if (rawDate.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                    date = rawDate.substring(0, 10);
                }
            }
            if (date.isEmpty()) {
                continue;
            }
            String clockIn = row.optString("jam", row.optString("clock_in", "")).trim();
            String clockOut = row.optString("clock_out", "").trim();
            if (!clockIn.isEmpty() && !"-".equals(clockIn)) {
                clockInDates.add(date);
            }
            if (!clockOut.isEmpty() && !"-".equals(clockOut)) {
                clockOutDates.add(date);
            }
        }

        SQLiteDatabase database = getWritableDatabase();
        Cursor cursor = database.rawQuery(
            "SELECT event_id,mode,captured_at,photo_path FROM pending_attendance " +
            "WHERE LOWER(username)=LOWER(?) ORDER BY captured_at,event_id",
            new String[]{username}
        );
        ArrayList<String> eventIds = new ArrayList<>();
        ArrayList<String> photoPaths = new ArrayList<>();
        try {
            while (cursor.moveToNext()) {
                String capturedAt = cursor.getString(2);
                String date = capturedAt != null && capturedAt.length() >= 10
                    ? capturedAt.substring(0, 10)
                    : "";
                String mode = cursor.getString(1);
                boolean covered =
                    ("clock_in".equals(mode) && clockInDates.contains(date)) ||
                    ("clock_out".equals(mode) && clockOutDates.contains(date));
                if (covered) {
                    eventIds.add(cursor.getString(0));
                    photoPaths.add(cursor.getString(3));
                }
            }
        } finally {
            cursor.close();
        }

        int removed = 0;
        for (int index = 0; index < eventIds.size(); index += 1) {
            removed += database.delete(
                "pending_attendance",
                "event_id=?",
                new String[]{eventIds.get(index)}
            );
            String photoPath = photoPaths.get(index);
            if (photoPath != null && !photoPath.trim().isEmpty()) {
                new File(photoPath).delete();
            }
        }
        return removed;
    }

    public String latestPendingMode(String username) {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT mode FROM pending_attendance WHERE LOWER(username)=LOWER(?) " +
            "ORDER BY captured_at DESC,event_id DESC LIMIT 1",
            new String[]{username}
        );
        try {
            return cursor.moveToFirst() ? cursor.getString(0) : "";
        } finally {
            cursor.close();
        }
    }

    public String latestPendingShift(String username) {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT shift_id FROM pending_attendance WHERE LOWER(username)=LOWER(?) " +
            "ORDER BY captured_at DESC,event_id DESC LIMIT 1",
            new String[]{username}
        );
        try {
            return cursor.moveToFirst() ? cursor.getString(0) : "1";
        } finally {
            cursor.close();
        }
    }

    public JSONArray pendingHistoryRows(String username) {
        JSONArray rows = new JSONArray();
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT mode,shift_id,captured_at,latitude,longitude,address," +
            "face_score,photo_path,device_info,app_version " +
            "FROM pending_attendance WHERE LOWER(username)=LOWER(?) " +
            "ORDER BY captured_at,event_id",
            new String[]{username}
        );
        JSONObject latestClockIn = null;
        try {
            while (cursor.moveToNext()) {
                String mode = cursor.getString(0);
                String capturedAt = cursor.getString(2);
                if ("clock_out".equals(mode) && latestClockIn != null) {
                    latestClockIn.put(
                        "clock_out",
                        capturedAt.length() >= 19 ? capturedAt.substring(11, 19) : "-"
                    );
                    latestClockIn.put("clock_out_face_score", cursor.getInt(6));
                    latestClockIn.put("clock_out_local_photo_path", cursor.getString(7));
                    latestClockIn.put("clock_out_device_info", cursor.getString(8));
                    latestClockIn.put("app_version", cursor.getString(9));
                    continue;
                }
                if (!"clock_in".equals(mode)) {
                    continue;
                }
                JSONObject row = new JSONObject();
                row.put(
                    "tanggal",
                    capturedAt.length() >= 10 ? capturedAt.substring(0, 10) : capturedAt
                );
                try {
                    Date capturedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(capturedAt);
                    row.put(
                        "hari",
                        capturedDate == null
                            ? "-"
                            : new SimpleDateFormat("EEEE", new Locale("id", "ID")).format(capturedDate)
                    );
                } catch (Exception ignored) {
                    row.put("hari", "-");
                }
                row.put("shift", "Shift " + cursor.getString(1));
                row.put(
                    "jam",
                    capturedAt.length() >= 19 ? capturedAt.substring(11, 19) : "-"
                );
                row.put("zona", "");
                row.put("clock_out", "-");
                row.put("lokasi", cursor.getString(5));
                row.put(
                    "koordinat",
                    cursor.getString(3) + ", " + cursor.getString(4)
                );
                row.put("status", "Menunggu Sinkronisasi");
                row.put("status_class", "pending");
                row.put("face_score", cursor.getInt(6));
                row.put("local_photo_path", cursor.getString(7));
                row.put("device_info", cursor.getString(8));
                row.put("app_version", cursor.getString(9));
                rows.put(row);
                latestClockIn = row;
            }
        } catch (Exception ignored) {
        } finally {
            cursor.close();
        }
        return rows;
    }

    public JSONObject latestPendingAttendance(String username) {
        Cursor cursor = getReadableDatabase().rawQuery(
            "SELECT mode,shift_id,captured_at,latitude,longitude,address,photo_path," +
            "face_score,device_info,app_version " +
            "FROM pending_attendance WHERE LOWER(username)=LOWER(?) " +
            "ORDER BY captured_at,event_id",
            new String[]{username}
        );
        JSONObject attendance = null;
        try {
            while (cursor.moveToNext()) {
                String mode = cursor.getString(0);
                String capturedAt = cursor.getString(2);
                if ("clock_in".equals(mode)) {
                    attendance = new JSONObject();
                    attendance.put(
                        "tanggal",
                        capturedAt.length() >= 10 ? capturedAt.substring(0, 10) : capturedAt
                    );
                    attendance.put(
                        "clock_in",
                        capturedAt.length() >= 19 ? capturedAt.substring(11, 19) : "-"
                    );
                    attendance.put("clock_out", "");
                    attendance.put("shift_id", cursor.getString(1));
                    attendance.put("shift_label", "Shift " + cursor.getString(1));
                    attendance.put("latitude", cursor.getString(3));
                    attendance.put("longitude", cursor.getString(4));
                    attendance.put("address", cursor.getString(5));
                    attendance.put("local_photo_path", cursor.getString(6));
                    attendance.put("face_score", cursor.getInt(7));
                    attendance.put("device_info", cursor.getString(8));
                    attendance.put("app_version", cursor.getString(9));
                } else if ("clock_out".equals(mode) && attendance != null) {
                    attendance.put(
                        "clock_out",
                        capturedAt.length() >= 19 ? capturedAt.substring(11, 19) : "-"
                    );
                    attendance.put("clock_out_face_score", cursor.getInt(7));
                    attendance.put("clock_out_local_photo_path", cursor.getString(6));
                    attendance.put("clock_out_device_info", cursor.getString(8));
                    attendance.put("app_version", cursor.getString(9));
                }
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            cursor.close();
        }
        return attendance;
    }

    private Context getContext() {
        return context;
    }

    private File getContextFilesDir() {
        return getContext().getFilesDir();
    }

    public static void schedule(Context context) {
        try {
            OfflineSyncJobService.appContext = context.getApplicationContext();
            JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (scheduler == null) {
                return;
            }
            JobInfo job = new JobInfo.Builder(
                SYNC_JOB_ID,
                new ComponentName(context, OfflineSyncJobService.class)
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setOverrideDeadline(1000)
                .setBackoffCriteria(30000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .build();
            scheduler.schedule(job);
        } catch (RuntimeException ignored) {
        }
    }
}

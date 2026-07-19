package id.tracer.attendance;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class OfflineSyncJobService extends JobService {
    private static final String SERVER_URL = "https://icommerce.asyscntr.com";
    static Context appContext;

    private static final class SendResult {
        final boolean removeFromQueue;

        SendResult(boolean removeFromQueue) {
            this.removeFromQueue = removeFromQueue;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = getApplicationContext();
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        appContext = getApplicationContext();
        new Thread(() -> {
            boolean retry = true;
            try {
                retry = !syncPending(getApplicationContext());
            } catch (Exception ignored) {
                retry = true;
            }
            try {
                jobFinished(params, retry);
            } catch (Exception ignored) {
            }
        }).start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }

    public static void syncNow(Context context) {
        if (context == null) {
            return;
        }
        Context safeContext = context.getApplicationContext();
        appContext = safeContext;
        new Thread(() -> {
            try {
                syncPendingNow(safeContext);
            } catch (Exception ignored) {
            }
        }).start();
    }

    public static boolean syncPendingNow(Context context) {
        if (context == null) {
            return false;
        }
        Context safeContext = context.getApplicationContext();
        appContext = safeContext;
        return syncPending(safeContext);
    }

    private static synchronized boolean syncPending(Context context) {
        OfflineAttendanceQueue queue = new OfflineAttendanceQueue(context);
        SQLiteDatabase database = queue.getWritableDatabase();
        while (true) {
            Cursor cursor = database.rawQuery(
                "SELECT event_id,base_url,sync_token,username,fullname,mode," +
                "shift_id,captured_at,latitude,longitude,address,face_score," +
                "device_info,app_version,photo_path " +
                "FROM pending_attendance ORDER BY captured_at,event_id LIMIT 1",
                null
            );
            try {
                if (!cursor.moveToFirst()) {
                    return true;
                }
                String eventId = cursor.getString(0);
                String photoPath = cursor.getString(14);
                SendResult result = send(cursor);
                if (!result.removeFromQueue) {
                    return false;
                }
                database.delete(
                    "pending_attendance",
                    "event_id=?",
                    new String[]{eventId}
                );
                new File(photoPath).delete();
            } catch (Exception error) {
                return false;
            } finally {
                cursor.close();
            }
        }
    }

    private static SendResult send(Cursor row) throws Exception {
        String boundary = "TracerOffline" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(
            normalizedBaseUrl(row.getString(1)) + "/api/attendance_sync"
        ).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(12000);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty(
            "Content-Type",
            "multipart/form-data; boundary=" + boundary
        );
        connection.setRequestProperty("User-Agent", "AttendanceApp/1.0 Android OfflineSync");

        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
        writeField(output, boundary, "event_id", row.getString(0));
        writeField(output, boundary, "sync_token", row.getString(2));
        writeField(output, boundary, "username", row.getString(3));
        writeField(output, boundary, "fullname", row.getString(4));
        writeField(output, boundary, "mode", row.getString(5));
        writeField(output, boundary, "shift_id", row.getString(6));
        writeField(output, boundary, "captured_at", row.getString(7));
        writeField(output, boundary, "latitude", row.getString(8));
        writeField(output, boundary, "longitude", row.getString(9));
        writeField(output, boundary, "address", row.getString(10));
        writeField(output, boundary, "face_score", row.getString(11));
        writeField(output, boundary, "device_info", row.getString(12));
        writeField(output, boundary, "app_version", row.getString(13));
        String photoPath = row.getString(14);
        if (photoPath != null && !photoPath.trim().isEmpty()) {
            File photo = new File(photoPath);
            if (photo.exists() && photo.length() > 0) {
                writeFile(output, boundary, photo);
            }
        }
        output.writeBytes("--" + boundary + "--\r\n");
        output.flush();
        output.close();

        int status = connection.getResponseCode();
        InputStream stream = status >= 400
            ? connection.getErrorStream()
            : connection.getInputStream();
        String response = readStream(stream);
        connection.disconnect();

        boolean success = status >= 200 && status < 300 &&
            (response.contains("\"success\":true") || response.contains("\"success\": true"));
        if (success) {
            return new SendResult(true);
        }

        String normalizedResponse = response == null ? "" : response.toLowerCase(Locale.US);
        boolean staleClockOut = status == 409 &&
            normalizedResponse.contains("belum ada clock in");
        boolean invalidEvent = status == 400 && (
            normalizedResponse.contains("event_id") ||
            normalizedResponse.contains("mode tidak valid") ||
            normalizedResponse.contains("username wajib")
        );
        boolean deletedAccount = status == 404 &&
            normalizedResponse.contains("user tidak ditemukan");
        return new SendResult(staleClockOut || invalidEvent || deletedAccount);
    }

    private static String normalizedBaseUrl(String queuedBaseUrl) {
        String value = queuedBaseUrl == null ? "" : queuedBaseUrl.trim();
        if (value.startsWith("https://icommerce.asyscntr.com") ||
            value.startsWith("http://icommerce.asyscntr.com")) {
            return SERVER_URL;
        }
        return SERVER_URL;
    }

    private static void writeField(DataOutputStream output, String boundary, String name, String value)
        throws Exception {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        output.write(value.getBytes("UTF-8"));
        output.writeBytes("\r\n");
    }

    private static void writeFile(DataOutputStream output, String boundary, File file)
        throws Exception {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes(
            "Content-Disposition: form-data; name=\"photo\"; filename=\"offline.jpg\"\r\n"
        );
        output.writeBytes("Content-Type: image/jpeg\r\n\r\n");
        FileInputStream input = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) > 0) {
            output.write(buffer, 0, count);
        }
        input.close();
        output.writeBytes("\r\n");
    }

    private static String readStream(InputStream stream) throws Exception {
        if (stream == null) {
            return "";
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            result.append(line);
        }
        reader.close();
        return result.toString();
    }
}

package id.tracer.attendance;

import android.animation.ValueAnimator;
import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.DecelerateInterpolator;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int PERMISSION_REQUEST = 42;
    private static final int CAMERA_REQUEST = 43;
    private static final int LEAVE_ATTACHMENT_REQUEST = 44;
    private static final int PROFILE_PHOTO_REQUEST = 45;
    // Mobile attendance selalu memakai API VPS agar tetap berfungsi saat server Windows mati.
    private static final String SERVER_URL = "https://attendance-api.asyscntr.com";
    private static final String DEFAULT_BASE_URL = SERVER_URL;
    private static final double ATTENDANCE_CENTER_LATITUDE = -6.261473599217103;
    private static final double ATTENDANCE_CENTER_LONGITUDE = 106.58763553681588;
    private static final float ATTENDANCE_RADIUS_METERS = 100f;
    private static final float MAX_LOCATION_ACCURACY_METERS = 300f;

    private final CookieManager cookieManager = new CookieManager();
    private final Handler handler = new Handler();

    private static final int COLOR_ACCENT = 0xFFE30613;
    private static final int COLOR_ACCENT_SOFT = 0xFFE30613;
    private static final int COLOR_ACCENT_DARK = 0xFFCF0010;
    private static final int COLOR_TEXT = 0xFF111827;
    private static final int COLOR_MUTED = 0xFF667085;
    private static final int COLOR_SUBTLE = 0xFF7A8494;
    private static final int COLOR_PANEL = 0xFFFFFFFF;
    private static final int COLOR_FIELD = 0xFFFFFFFF;
    private static final int COLOR_STROKE = 0xFFEAECF0;
    private static final int COLOR_SOFT_ACCENT = 0xFFFEE2E2;
    private static final int COLOR_ACCENT_STROKE = 0xFFFECACA;
    private static final int COLOR_APP_BG = 0xFFFFFFFF;
    private static final int COLOR_SUCCESS = 0xFF16A34A;
    private static final int COLOR_SOFT_GREEN = 0xFFE6F7EA;
    private static final int COLOR_WARNING = 0xFFF59E0B;
    private static final int COLOR_SOFT_ORANGE = 0xFFFEF3C7;
    private static final int COLOR_ERROR = 0xFFEF4444;
    private static final int COLOR_SOFT_RED = 0xFFFEE2E2;

    private String webUrl = "";
    private String baseUrl = "";
    private String appToken = "";
    private String syncToken = "";
    private String mode = "clock_in";
    private String fullname = "-";
    private String username = "-";
    private String userRole = "";
    private String appDisplayName = "";
    private String department = "";
    private String profilePhotoUrl = "";
    private String lastPhotoUrl = "";
    private String selectedShiftId = "1";
    private int totalShift = 1;
    private String shift1ClockIn = "";
    private String shift1ClockOut = "";
    private String shift2ClockIn = "";
    private String shift2ClockOut = "";
    private String address = "";
    private String locationSummary = "";
    private double latitude = 0;
    private double longitude = 0;
    private float locationAccuracyMeters = Float.MAX_VALUE;
    private float distanceFromAttendanceCenter = Float.MAX_VALUE;
    private float activeAttendanceRadiusMeters = ATTENDANCE_RADIUS_METERS;
    private String nearestAttendanceLocationName = "Kantor Utama";
    private JSONArray attendanceLocations = new JSONArray();
    private Bitmap selfieBitmap;
    private Bitmap leaveAttachmentBitmap;
    private Bitmap profilePhotoBitmap;
    private int currentStep = 0;
    private boolean gpsLocked = false;
    private boolean disableLocationLock = false;
    private boolean faceValidated = false;
    private boolean offlineMode = false;
    private int faceScore = 0;
    private JSONArray nativeHistoryRows = new JSONArray();
    private LinearLayout nativeHistoryList;
    private TextView historyAllTab;
    private TextView historyOnTimeTab;
    private TextView historyLateTab;
    private TextView historyAbsentTab;
    private String activeHistoryFilter = "all";

    private TextView dateDay;
    private TextView dateFull;
    private TextView dateTime;
    private TextView nameText;
    private TextView usernameText;
    private TextView avatarText;
    private TextView readyBadge;
    private TextView stepOne;
    private TextView stepTwo;
    private TextView stepThree;
    private TextView faceText;
    private TextView faceBadge;
    private TextView faceScoreValue;
    private ProgressBar faceScoreProgress;
    private TextView locationText;
    private TextView gpsBadge;
    private TextView gpsAddress;
    private TextView gpsCoordinate;
    private TextView gpsSummary;
    private TextView attendanceLocationAddress;
    private TextView attendanceLocationAccuracy;
    private TextView attendanceLocationAreaBadge;
    private TextView photoFacePrompt;
    private TextView homeGreeting;
    private TextView homeNameView;
    private TextView homeRoleView;
    private TextView homeStatusValue;
    private TextView homeStatusDate;
    private TextView homeActionLabel;
    private FrameLayout homeActionButton;
    private TextView homeLocationValue;
    private TextView homeLocationBadge;
    private TextView homeFaceValue;
    private TextView homeFaceBadge;
    private TextView homeScheduleValue;
    private TextView homeScheduleBadge;
    private TextView homeOnTimeCount;
    private TextView homeLateCount;
    private TextView homeAbsentCount;
    private TextView homeAttendanceRate;
    private TextView homeNotificationBadge;
    private ImageView homeBellIcon;
    private FrameLayout homeConnectionIndicator;
    private ImageView homeConnectionIcon;
    private WebView mapView;
    private TextView reviewDate;
    private TextView reviewTime;
    private TextView reviewAddress;
    private TextView reviewProvince;
    private TextView reviewCity;
    private TextView reviewDistrict;
    private TextView reviewShift;
    private TextView reviewFace;
    private TextView statusText;
    private TextView successView;
    private TextView successTitle;
    private TextView successDate;
    private TextView successShift;
    private TextView successClockIn;
    private TextView successClockOut;
    private TextView successStatus;
    private TextView successCutoff;
    private TextView successAddress;
    private TextView successCoordinate;
    private TextView successFaceScore;
    private TextView photoHint;
    private ImageView preview;
    private ImageView photoPlaceholderIcon;
    private ImageView avatarImage;
    private ImageView reviewPhoto;
    private ImageView successPhoto;
    private ScrollView attendanceScroll;
    private LinearLayout attendanceContent;
    private LinearLayout photoCard;
    private FrameLayout photoRingContainer;
    private FaceScoreRingView faceScoreRing;
    private LinearLayout faceCard;
    private LinearLayout locationCard;
    private LinearLayout gpsView;
    private LinearLayout reviewView;
    private LinearLayout stepsView;
    private LinearLayout shiftSelector;
    private LinearLayout successPanel;
    private LinearLayout actions;
    private LinearLayout actionRow;
    private View mainRoot;
    private boolean showingHistory = false;
    private Button retakeButton;
    private Button nextButton;
    private Button captureButton;
    private Button historyButton;
    private Button syncButton;
    private Button shiftOneButton;
    private Button shiftTwoButton;
    private Button clockOutButton;
    private EditText loginUsername;
    private EditText loginPassword;
    private EditText leaveTypeInput;
    private EditText leaveDateInput;
    private EditText leaveNoteInput;
    private Spinner leaveTypeSpinner;
    private TextView leaveAttachmentLabel;
    private LinearLayout leaveHistoryList;
    private TextView serverText;
    private Button loginButton;
    private Button bellButton;
    private ProgressBar progressBar;
    private boolean offlineSyncPollingActive = false;
    private boolean offlineSyncInProgress = false;
    private boolean notificationPollingActive = false;
    private ValueAnimator notificationBlinkAnimator;
    private PopupWindow updateNotificationPopup;
    private int shownUpdateMessageIdThisLogin = 0;
    private String currentSection = "";
    private float swipeStartX = 0f;
    private float swipeStartY = 0f;
    private float homePullStartY = -1f;
    private boolean homeRefreshing = false;
    private boolean successPhotoExpanded = false;

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();
            handler.postDelayed(this, 1000);
        }
    };

    private final Runnable offlineSyncPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!offlineSyncPollingActive) {
                return;
            }
            int pending = 0;
            try {
                pending = new OfflineAttendanceQueue(MainActivity.this).pendingCount();
            } catch (Exception ignored) {
            }
            if (pending <= 0) {
                stopOfflineSyncPolling();
                return;
            }
            syncOfflineNow(false);
            handler.postDelayed(this, 60000);
        }
    };

    private final Runnable notificationPollRunnable = new Runnable() {
        @Override
        public void run() {
            if (!notificationPollingActive || !"Beranda".equals(currentSection)) {
                return;
            }
            loadUnreadMessageCount();
            checkServerConnectionAndSync(false);
            loadHomeOverview();
            handler.postDelayed(this, 30000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            applySystemBars();
            OfflineAttendanceQueue.schedule(this);
            OfflineSyncJobService.syncNow(this);
            CookieHandler.setDefault(cookieManager);
            requestRuntimePermissions();
            handler.post(clockRunnable);
            Uri data = getIntent() == null ? null : getIntent().getData();
            if (data != null && data.getQueryParameter("web_url") != null) {
                buildLayout();
                loadFromIntent(getIntent());
            } else {
                showLoginScreen();
            }
        } catch (Throwable error) {
            showStartupError(error);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            OfflineAttendanceQueue.schedule(this);
            OfflineSyncJobService.syncNow(this);
            refreshOfflineSyncStatus(false);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(clockRunnable);
        stopOfflineSyncPolling();
        stopNotificationPolling();
        if (updateNotificationPopup != null) {
            updateNotificationPopup.dismiss();
            updateNotificationPopup = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadFromIntent(intent);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event != null && isPrimarySection(currentSection)) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                swipeStartX = event.getX();
                swipeStartY = event.getY();
            } else if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                float deltaX = event.getX() - swipeStartX;
                float deltaY = event.getY() - swipeStartY;
                if (Math.abs(deltaX) >= dp(72) && Math.abs(deltaX) > Math.abs(deltaY) * 1.45f) {
                    navigatePrimarySection(deltaX < 0f ? 1 : -1);
                    return true;
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (showingHistory && mainRoot != null) {
            openHomePage();
            return;
        }
        super.onBackPressed();
    }

    private boolean isPrimarySection(String section) {
        return "Beranda".equals(section) || "Riwayat".equals(section) ||
            "Izin".equals(section) || "Akun".equals(section);
    }

    private void navigatePrimarySection(int direction) {
        String[] sections = {"Beranda", "Riwayat", "Izin", "Akun"};
        int currentIndex = 0;
        for (int index = 0; index < sections.length; index += 1) {
            if (sections[index].equals(currentSection)) {
                currentIndex = index;
                break;
            }
        }
        int nextIndex = Math.max(0, Math.min(sections.length - 1, currentIndex + direction));
        if (nextIndex == currentIndex) {
            return;
        }
        String next = sections[nextIndex];
        if ("Beranda".equals(next)) {
            openHomePage();
        } else if ("Riwayat".equals(next)) {
            openHistory();
        } else if ("Izin".equals(next)) {
            openLeavePage();
        } else {
            openAccountPage();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == CAMERA_REQUEST) {
            Object bitmap = data.getExtras() == null ? null : data.getExtras().get("data");
            if (bitmap instanceof Bitmap) {
                selfieBitmap = (Bitmap) bitmap;
                preview.setImageBitmap(selfieBitmap);
                showPhotoCaptured();
            }
            return;
        }
        if (requestCode == LEAVE_ATTACHMENT_REQUEST || requestCode == PROFILE_PHOTO_REQUEST) {
            Bitmap selected = bitmapFromUri(data.getData());
            if (selected == null) {
                notifyUser("Foto tidak dapat dibaca.");
                return;
            }
            if (requestCode == LEAVE_ATTACHMENT_REQUEST) {
                leaveAttachmentBitmap = selected;
                if (leaveAttachmentLabel != null) {
                    leaveAttachmentLabel.setText("Lampiran siap diunggah");
                    leaveAttachmentLabel.setTextColor(COLOR_SUCCESS);
                }
            } else {
                profilePhotoBitmap = selected;
                notifyUser("Foto profil dipilih. Tekan Simpan Perubahan.");
            }
        }
    }

    private Bitmap bitmapFromUri(Uri uri) {
        if (uri == null) {
            return null;
        }
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            return BitmapFactory.decodeStream(stream);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void showStartupError(Throwable error) {
        LinearLayout root = vertical();
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));
        root.setBackgroundColor(0xFFFFFFFF);
        TextView title = text("Aplikasi belum bisa dibuka", 20, COLOR_ACCENT, true);
        title.setGravity(Gravity.CENTER);
        root.addView(title, matchWrap());
        String message = error == null || error.getMessage() == null
            ? "Terjadi error saat membuka aplikasi."
            : error.getMessage();
        TextView detail = text(message, 13, 0xFF667085, false);
        detail.setGravity(Gravity.CENTER);
        detail.setPadding(0, dp(10), 0, dp(16));
        root.addView(detail, matchWrap());
        Button retry = button("BUKA ULANG", COLOR_ACCENT_SOFT, 0xFFFFFFFF, 0);
        root.addView(retry, new LinearLayout.LayoutParams(-1, dp(46)));
        retry.setOnClickListener(v -> recreate());
        setContentView(root);
    }
    private void showLoginScreen() {
        currentSection = "";
        stopNotificationPolling();
        setSystemBarStyle(true);
        SharedPreferences prefs = getSharedPreferences("attendance_app", MODE_PRIVATE);
        String savedBaseUrl = prefs.getString("base_url", "");
        if (savedBaseUrl != null && savedBaseUrl.length() > 0 && !isLocalIpBaseUrl(savedBaseUrl) && !isOldServerBaseUrl(savedBaseUrl)) {
            baseUrl = savedBaseUrl;
        } else {
            baseUrl = DEFAULT_BASE_URL;
            prefs.edit().putString("base_url", baseUrl).apply();
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFFFFFFF);
        ImageView logisticsBackground = new ImageView(this);
        logisticsBackground.setImageResource(R.drawable.bg);
        logisticsBackground.setScaleType(ImageView.ScaleType.CENTER_CROP);
        logisticsBackground.setAlpha(0.07f);
        root.addView(logisticsBackground, new FrameLayout.LayoutParams(-1, -1));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout page = vertical();
        page.setMinimumHeight(getResources().getDisplayMetrics().heightPixels);
        scroll.addView(page, new ScrollView.LayoutParams(-1, -1));

        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int headerHeight = Math.max(dp(205), Math.min(dp(235), (int) (screenHeight * 0.32f)));
        FrameLayout hero = new FrameLayout(this);
        page.addView(hero, new LinearLayout.LayoutParams(-1, headerHeight + dp(76)));
        hero.addView(new LoginHeaderView(this), new FrameLayout.LayoutParams(-1, headerHeight));
        HomeWaveView loginWave = new HomeWaveView(this);
        FrameLayout.LayoutParams loginWaveParams = new FrameLayout.LayoutParams(-1, dp(72), Gravity.TOP);
        loginWaveParams.setMargins(0, headerHeight - dp(72), 0, 0);
        hero.addView(loginWave, loginWaveParams);

        LinearLayout headerCopy = vertical();
        TextView welcome = text("Tracer", 20, 0xFFFFFFFF, true);
        welcome.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        TextView title = text("Silakan Login", 29, 0xFFFFFFFF, true);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        title.setPadding(0, dp(3), 0, dp(3));
        TextView subtitle = text("Aplikasi Absensi Karyawan", 13, 0xF2FFFFFF, false);
        headerCopy.addView(welcome);
        headerCopy.addView(title);
        headerCopy.addView(subtitle);
        FrameLayout.LayoutParams copyParams = new FrameLayout.LayoutParams(dp(220), -2, Gravity.TOP | Gravity.LEFT);
        copyParams.setMargins(dp(24), dp(50), 0, 0);
        hero.addView(headerCopy, copyParams);

        FrameLayout loginBadge = new FrameLayout(this);
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(72), dp(72), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        badgeParams.setMargins(0, headerHeight + dp(1), 0, 0);
        hero.addView(loginBadge, badgeParams);

        ImageView calendarIcon = icon(R.drawable.ic_calendar, COLOR_ACCENT);
        loginBadge.addView(calendarIcon, new FrameLayout.LayoutParams(dp(60), dp(60), Gravity.CENTER));
        ImageView userIcon = icon(R.drawable.ic_user, COLOR_ACCENT);
        FrameLayout.LayoutParams userIconParams = new FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER);
        userIconParams.setMargins(0, dp(13), 0, 0);
        loginBadge.addView(userIcon, userIconParams);

        TextView badgeCheck = text("\u2713", 12, 0xFFFFFFFF, true);
        badgeCheck.setGravity(Gravity.CENTER);
        badgeCheck.setBackground(round(COLOR_ACCENT, dp(13), 0xFFFFFFFF, 2));
        FrameLayout.LayoutParams checkParams = new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.RIGHT | Gravity.BOTTOM);
        checkParams.setMargins(0, 0, dp(8), dp(8));
        loginBadge.addView(badgeCheck, checkParams);

        LinearLayout form = vertical();
        form.setPadding(dp(24), 0, dp(24), 0);
        LinearLayout.LayoutParams formParams = matchWrap();
        formParams.setMargins(0, dp(4), 0, 0);
        page.addView(form, formParams);

        TextView usernameLabel = text("Username", 14, 0xFF344054, true);
        usernameLabel.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        form.addView(usernameLabel, matchWrap());
        loginUsername = addLoginInput(form, "Masukkan username Anda", "user", false);

        LinearLayout passwordLabels = horizontal();
        passwordLabels.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams passwordLabelParams = matchWrap();
        passwordLabelParams.setMargins(0, dp(14), 0, 0);
        form.addView(passwordLabels, passwordLabelParams);
        TextView passwordLabel = text("Password", 14, 0xFF344054, true);
        passwordLabel.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        passwordLabels.addView(passwordLabel, new LinearLayout.LayoutParams(0, -2, 1));
        TextView forgotPassword = text("Lupa Password?", 12, COLOR_ACCENT, true);
        forgotPassword.setPadding(dp(10), dp(4), 0, dp(4));
        forgotPassword.setOnClickListener(v -> openAdminWhatsApp("bantuan reset password"));
        passwordLabels.addView(forgotPassword);
        loginPassword = addLoginInput(form, "Masukkan password Anda", "lock", true);

        loginButton = button("Login", COLOR_ACCENT, 0xFFFFFFFF, 0);
        loginButton.setTextSize(15);
        loginButton.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        loginButton.setBackground(round(COLOR_ACCENT, dp(7), 0, 0));
        LinearLayout.LayoutParams loginParams = new LinearLayout.LayoutParams(-1, dp(50));
        loginParams.setMargins(0, dp(16), 0, 0);
        form.addView(loginButton, loginParams);
        loginButton.setOnClickListener(v -> submitNativeLogin());

        LinearLayout adminRow = horizontal();
        adminRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams adminParams = matchWrap();
        adminParams.setMargins(0, dp(16), 0, 0);
        page.addView(adminRow, adminParams);
        adminRow.addView(text("Belum punya akun?", 12, 0xFF667085, false));
        TextView contactAdmin = text(" Hubungi Admin", 12, COLOR_ACCENT, true);
        contactAdmin.setPadding(dp(2), dp(5), dp(4), dp(5));
        contactAdmin.setOnClickListener(v -> openAdminWhatsApp("pembuatan atau bantuan akun absensi"));
        adminRow.addView(contactAdmin);

        page.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout footer = vertical();
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(20), dp(8), dp(20), dp(12));
        page.addView(footer, new LinearLayout.LayoutParams(-1, dp(66)));
        serverText = text("Server: apk.asyscntr.com", 11, 0xFF667085, false);
        serverText.setGravity(Gravity.CENTER);
        footer.addView(serverText, new LinearLayout.LayoutParams(-1, dp(21)));
        TextView website = text("Tracer  |  Versi " + appVersionName(), 11, 0xFF98A2B3, false);
        website.setGravity(Gravity.CENTER);
        footer.addView(website, new LinearLayout.LayoutParams(-1, dp(21)));

        setContentView(root);
    }

    private EditText addLoginInput(LinearLayout parent, String hint, String iconName, boolean password) {
        FrameLayout field = new FrameLayout(this);
        field.setBackground(round(0xFFFFFFFF, dp(7), 0xFFD0D5DD, 1));
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(-1, dp(50));
        fieldParams.setMargins(0, dp(6), 0, 0);
        parent.addView(field, fieldParams);

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(0xFF98A2B3);
        input.setHint(hint);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setPadding(dp(13), 0, password ? dp(46) : dp(13), 0);
        android.graphics.drawable.Drawable leading = loginGlyph(iconName, 0xFF98A2B3);
        leading.setBounds(0, 0, dp(17), dp(17));
        input.setCompoundDrawablePadding(dp(10));
        input.setCompoundDrawables(leading, null, null, null);
        if (password) {
            input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        field.addView(input, new FrameLayout.LayoutParams(-1, -1));

        if (password) {
            ImageView eye = new ImageView(this);
            eye.setImageDrawable(loginGlyph("eye", 0xFF98A2B3));
            eye.setPadding(dp(11), dp(11), dp(11), dp(11));
            eye.setContentDescription("Tampilkan password");
            final boolean[] visible = {false};
            eye.setOnClickListener(v -> {
                visible[0] = !visible[0];
                input.setTransformationMethod(
                    visible[0] ? null : PasswordTransformationMethod.getInstance()
                );
                input.setSelection(input.length());
                eye.setColorFilter(visible[0] ? COLOR_ACCENT : 0xFF98A2B3);
                eye.setContentDescription(visible[0] ? "Sembunyikan password" : "Tampilkan password");
            });
            field.addView(eye, new FrameLayout.LayoutParams(dp(44), -1, Gravity.RIGHT | Gravity.CENTER_VERTICAL));
        }
        return input;
    }

    private android.graphics.drawable.Drawable loginGlyph(String name, int color) {
        return new android.graphics.drawable.Drawable() {
            private final Paint glyphPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Path glyphPath = new Path();

            @Override
            public void draw(Canvas canvas) {
                android.graphics.Rect bounds = getBounds();
                float size = Math.min(bounds.width(), bounds.height());
                float left = bounds.left + (bounds.width() - size) / 2f;
                float top = bounds.top + (bounds.height() - size) / 2f;
                float unit = size / 24f;
                glyphPaint.setColor(color);
                glyphPaint.setStyle(Paint.Style.STROKE);
                glyphPaint.setStrokeWidth(Math.max(1f, 1.8f * unit));
                glyphPaint.setStrokeCap(Paint.Cap.ROUND);
                glyphPaint.setStrokeJoin(Paint.Join.ROUND);

                if ("lock".equals(name)) {
                    canvas.drawRoundRect(
                        new RectF(left + 4 * unit, top + 10 * unit, left + 20 * unit, top + 21 * unit),
                        2 * unit,
                        2 * unit,
                        glyphPaint
                    );
                    glyphPath.reset();
                    glyphPath.moveTo(left + 7 * unit, top + 10 * unit);
                    glyphPath.lineTo(left + 7 * unit, top + 8 * unit);
                    glyphPath.cubicTo(
                        left + 7 * unit, top + 1 * unit,
                        left + 17 * unit, top + 1 * unit,
                        left + 17 * unit, top + 8 * unit
                    );
                    glyphPath.lineTo(left + 17 * unit, top + 10 * unit);
                    canvas.drawPath(glyphPath, glyphPaint);
                    canvas.drawLine(left + 12 * unit, top + 14 * unit, left + 12 * unit, top + 17 * unit, glyphPaint);
                } else if ("eye".equals(name)) {
                    glyphPath.reset();
                    glyphPath.moveTo(left + 2 * unit, top + 12 * unit);
                    glyphPath.cubicTo(
                        left + 5 * unit, top + 6 * unit,
                        left + 9 * unit, top + 5 * unit,
                        left + 12 * unit, top + 5 * unit
                    );
                    glyphPath.cubicTo(
                        left + 17 * unit, top + 5 * unit,
                        left + 20 * unit, top + 9 * unit,
                        left + 22 * unit, top + 12 * unit
                    );
                    glyphPath.cubicTo(
                        left + 19 * unit, top + 18 * unit,
                        left + 15 * unit, top + 19 * unit,
                        left + 12 * unit, top + 19 * unit
                    );
                    glyphPath.cubicTo(
                        left + 7 * unit, top + 19 * unit,
                        left + 4 * unit, top + 15 * unit,
                        left + 2 * unit, top + 12 * unit
                    );
                    canvas.drawPath(glyphPath, glyphPaint);
                    canvas.drawCircle(left + 12 * unit, top + 12 * unit, 3 * unit, glyphPaint);
                } else if ("qr".equals(name)) {
                    canvas.drawRect(left + 3 * unit, top + 3 * unit, left + 10 * unit, top + 10 * unit, glyphPaint);
                    canvas.drawRect(left + 14 * unit, top + 3 * unit, left + 21 * unit, top + 10 * unit, glyphPaint);
                    canvas.drawRect(left + 3 * unit, top + 14 * unit, left + 10 * unit, top + 21 * unit, glyphPaint);
                    canvas.drawLine(left + 14 * unit, top + 14 * unit, left + 17 * unit, top + 14 * unit, glyphPaint);
                    canvas.drawLine(left + 17 * unit, top + 14 * unit, left + 17 * unit, top + 18 * unit, glyphPaint);
                    canvas.drawLine(left + 21 * unit, top + 14 * unit, left + 21 * unit, top + 21 * unit, glyphPaint);
                    canvas.drawLine(left + 14 * unit, top + 21 * unit, left + 18 * unit, top + 21 * unit, glyphPaint);
                } else {
                    canvas.drawCircle(left + 12 * unit, top + 8 * unit, 4 * unit, glyphPaint);
                    canvas.drawArc(
                        new RectF(left + 4 * unit, top + 14 * unit, left + 20 * unit, top + 25 * unit),
                        180,
                        180,
                        false,
                        glyphPaint
                    );
                }
            }

            @Override
            public void setAlpha(int alpha) {
                glyphPaint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(android.graphics.ColorFilter colorFilter) {
                glyphPaint.setColorFilter(colorFilter);
            }

            @Override
            public int getOpacity() {
                return android.graphics.PixelFormat.TRANSLUCENT;
            }
        };
    }

    private void openAdminWhatsApp(String subject) {
        String message = "Halo Admin Tracer, saya membutuhkan " + subject + ".";
        Uri uri = Uri.parse(
            "https://wa.me/6287815890193?text=" + Uri.encode(message)
        );
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception error) {
            notifyUser("WhatsApp admin: +62 878 1589 0193");
        }
    }

    private void buildLayout() {
        currentSection = "";
        stopNotificationPolling();
        setSystemBarStyle(false);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFFFFFFF);

        LinearLayout page = vertical();
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), 0, dp(14), 0);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(72)));

        FrameLayout back = new FrameLayout(this);
        ImageView backIcon = icon(R.drawable.ic_back, COLOR_TEXT);
        back.addView(backIcon, new FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER));
        back.setOnClickListener(v -> openHomePage());
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(52)));

        TextView screenTitle = text(isClockOut() ? "Absen Pulang" : "Absen Masuk", 17, COLOR_TEXT, true);
        screenTitle.setGravity(Gravity.CENTER);
        header.addView(screenTitle, new LinearLayout.LayoutParams(0, dp(52), 1));
        header.addView(new View(this), new LinearLayout.LayoutParams(dp(46), dp(52)));

        View headerDivider = new View(this);
        headerDivider.setBackgroundColor(0xFFF2F3F5);
        page.addView(headerDivider, new LinearLayout.LayoutParams(-1, dp(1)));

        ScrollView scroll = new ScrollView(this);
        attendanceScroll = scroll;
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout content = vertical();
        attendanceContent = content;
        content.setPadding(dp(18), dp(18), dp(18), dp(96));
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));

        locationCard = horizontal();
        locationCard.setGravity(Gravity.CENTER_VERTICAL);
        locationCard.setPadding(dp(14), dp(14), dp(14), dp(14));
        locationCard.setBackground(softRedPanel(dp(14)));
        content.addView(locationCard, new LinearLayout.LayoutParams(-1, dp(98)));

        FrameLayout locationIconWrap = new FrameLayout(this);
        ImageView locationIcon = icon(R.drawable.ic_location, COLOR_ACCENT);
        locationIconWrap.addView(locationIcon, new FrameLayout.LayoutParams(dp(34), dp(34), Gravity.CENTER));
        locationCard.addView(locationIconWrap, new LinearLayout.LayoutParams(dp(48), dp(54)));

        LinearLayout locationCopy = vertical();
        LinearLayout.LayoutParams locationCopyParams = new LinearLayout.LayoutParams(0, -2, 1);
        locationCopyParams.setMargins(dp(8), 0, dp(8), 0);
        locationCard.addView(locationCopy, locationCopyParams);
        locationCopy.addView(text("Lokasi Aktif", 13, COLOR_TEXT, true));
        attendanceLocationAddress = text("Menunggu lokasi...", 14, COLOR_TEXT, false);
        attendanceLocationAddress.setSingleLine(true);
        attendanceLocationAddress.setPadding(0, dp(4), 0, 0);
        locationCopy.addView(attendanceLocationAddress);
        attendanceLocationAccuracy = text("Akurasi GPS: -  |  Jarak: -", 11, COLOR_MUTED, false);
        attendanceLocationAccuracy.setPadding(0, dp(4), 0, 0);
        locationCopy.addView(attendanceLocationAccuracy);

        attendanceLocationAreaBadge = text("Mencari...", 11, COLOR_MUTED, true);
        attendanceLocationAreaBadge.setGravity(Gravity.CENTER);
        attendanceLocationAreaBadge.setPadding(dp(12), dp(7), dp(12), dp(7));
        attendanceLocationAreaBadge.setBackground(round(0xFFF2F4F7, dp(18), 0, 0));
        locationCard.addView(attendanceLocationAreaBadge, new LinearLayout.LayoutParams(-2, -2));
        readyBadge = attendanceLocationAreaBadge;

        photoFacePrompt = text("Pastikan wajah Anda berada di dalam area", 13, COLOR_TEXT, false);
        photoFacePrompt.setGravity(Gravity.CENTER);
        photoFacePrompt.setPadding(0, dp(22), 0, dp(14));
        content.addView(photoFacePrompt, matchWrap());

        int faceSize = responsiveFaceSize();
        FrameLayout photoWrap = new FrameLayout(this);
        photoRingContainer = photoWrap;
        LinearLayout.LayoutParams photoWrapParams = new LinearLayout.LayoutParams(faceSize, faceSize);
        photoWrapParams.gravity = Gravity.CENTER_HORIZONTAL;
        content.addView(photoWrap, photoWrapParams);
        photoCard = vertical();
        photoCard.setPadding(dp(5), dp(5), dp(5), dp(5));
        photoCard.setBackground(round(0xFFFFFFFF, faceSize / 2, 0, 0));
        photoWrap.addView(photoCard, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout photoBox = new FrameLayout(this);
        photoBox.setBackground(round(0xFFE5E7EB, dp(145), 0, 0));
        photoBox.setClipToOutline(true);
        photoBox.setOnClickListener(v -> openCamera());
        photoCard.addView(photoBox, new LinearLayout.LayoutParams(-1, -1));

        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoBox.addView(preview, new FrameLayout.LayoutParams(-1, -1));

        photoPlaceholderIcon = icon(R.drawable.ic_face, 0xFF98A2B3);
        FrameLayout.LayoutParams placeholderIconParams = new FrameLayout.LayoutParams(dp(54), dp(54), Gravity.CENTER);
        placeholderIconParams.setMargins(0, 0, 0, dp(24));
        photoBox.addView(photoPlaceholderIcon, placeholderIconParams);

        photoHint = text("Ketuk untuk mengambil foto", 12, COLOR_MUTED, true);
        photoHint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams photoHintParams = new FrameLayout.LayoutParams(-1, dp(44), Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        photoHintParams.setMargins(dp(20), 0, dp(20), dp(44));
        photoBox.addView(photoHint, photoHintParams);

        faceScoreRing = new FaceScoreRingView(this);
        photoWrap.addView(faceScoreRing, new FrameLayout.LayoutParams(-1, -1));

        faceCard = vertical();
        faceCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        faceCard.setBackground(round(0xFFFFFFFF, dp(14), COLOR_STROKE, 1));
        setSoftElevation(faceCard, 2);
        LinearLayout.LayoutParams faceParams = matchWrap();
        faceParams.setMargins(0, dp(18), 0, 0);
        content.addView(faceCard, faceParams);

        LinearLayout scoreTop = horizontal();
        scoreTop.setGravity(Gravity.CENTER_VERTICAL);
        faceCard.addView(scoreTop, matchWrap());
        LinearLayout scoreCopy = vertical();
        scoreTop.addView(scoreCopy, new LinearLayout.LayoutParams(0, -2, 1));
        scoreCopy.addView(text("Face Score", 13, COLOR_TEXT, true));
        faceScoreValue = text("--", 31, COLOR_SUCCESS, true);
        faceScoreValue.setPadding(0, dp(2), 0, 0);
        scoreCopy.addView(faceScoreValue);

        faceBadge = text("Menunggu Foto", 11, 0xFF16833B, true);
        faceBadge.setGravity(Gravity.CENTER);
        faceBadge.setPadding(dp(13), dp(7), dp(13), dp(7));
        faceBadge.setBackground(round(COLOR_SOFT_GREEN, dp(18), 0, 0));
        scoreTop.addView(faceBadge);

        faceScoreProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        faceScoreProgress.setMax(100);
        faceScoreProgress.setProgress(0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            faceScoreProgress.setProgressTintList(ColorStateList.valueOf(COLOR_SUCCESS));
            faceScoreProgress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFE7EAEE));
        }
        LinearLayout.LayoutParams scoreProgressParams = new LinearLayout.LayoutParams(-1, dp(8));
        scoreProgressParams.setMargins(0, dp(10), 0, 0);
        faceCard.addView(faceScoreProgress, scoreProgressParams);

        faceText = text("Pastikan pencahayaan cukup dan wajah terlihat jelas", 12, COLOR_MUTED, false);
        faceText.setPadding(0, dp(10), 0, 0);
        faceCard.addView(faceText);

        actions = vertical();
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionsParams = matchWrap();
        actionsParams.setMargins(0, dp(18), 0, 0);
        content.addView(actions, actionsParams);

        shiftSelector = horizontal();
        shiftSelector.setGravity(Gravity.CENTER);
        shiftSelector.setPadding(dp(4), dp(4), dp(4), dp(4));
        shiftSelector.setBackground(round(0xFFFFFFFF, dp(14), COLOR_STROKE, 1));
        shiftOneButton = button("SHIFT 1", COLOR_ACCENT, 0xFFFFFFFF, 0);
        shiftTwoButton = button("SHIFT 2", 0xFFFFFFFF, COLOR_ACCENT, COLOR_ACCENT_STROKE);
        shiftSelector.addView(shiftOneButton, shiftButtonParams());
        shiftSelector.addView(shiftTwoButton, shiftButtonParams());
        shiftOneButton.setOnClickListener(v -> { selectedShiftId = "1"; updateShiftSelector(); });
        shiftTwoButton.setOnClickListener(v -> { selectedShiftId = "2"; updateShiftSelector(); });
        actions.addView(shiftSelector, matchWrap());

        actionRow = horizontal();
        actionRow.setGravity(Gravity.CENTER);
        actionRow.setVisibility(View.GONE);
        actions.addView(actionRow, matchWrap());
        retakeButton = button("FOTO ULANG", 0xFFFFFFFF, COLOR_ACCENT, COLOR_ACCENT_STROKE);
        nextButton = button("LANJUT", COLOR_ACCENT, 0xFFFFFFFF, 0);
        actionRow.addView(retakeButton, actionButtonParams());
        actionRow.addView(nextButton, actionButtonParams());
        retakeButton.setOnClickListener(v -> onRetake());
        nextButton.setOnClickListener(v -> onNext());

        captureButton = button(isClockOut() ? "Absen Pulang" : "Absen Masuk", COLOR_ACCENT, 0xFFFFFFFF, 0);
        captureButton.setTextSize(15);
        actions.addView(captureButton, new LinearLayout.LayoutParams(-1, dp(54)));
        captureButton.setOnClickListener(v -> onCapture());

        TextView cancel = text("Batal", 15, COLOR_TEXT, true);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, dp(16), 0, dp(2));
        cancel.setOnClickListener(v -> openHomePage());
        actions.addView(cancel, matchWrap());

        gpsView = vertical();
        gpsView.setPadding(dp(14), dp(14), dp(14), dp(14));
        gpsView.setBackground(round(0xFFFFFFFF, dp(14), COLOR_STROKE, 1));
        LinearLayout.LayoutParams gpsParams = matchWrap();
        gpsParams.setMargins(0, dp(16), 0, 0);
        content.addView(gpsView, gpsParams);
        gpsView.addView(text("Lokasi GPS", 14, COLOR_TEXT, true));
        mapView = new WebView(this);
        WebSettings mapSettings = mapView.getSettings();
        mapSettings.setJavaScriptEnabled(true);
        mapSettings.setDomStorageEnabled(true);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(-1, dp(160));
        mapParams.setMargins(0, dp(10), 0, dp(10));
        gpsView.addView(mapView, mapParams);
        gpsAddress = text("Mengambil alamat...", 12, COLOR_TEXT, true);
        gpsView.addView(gpsAddress);
        gpsSummary = text("", 12, COLOR_MUTED, false);
        gpsSummary.setPadding(0, dp(6), 0, 0);
        gpsView.addView(gpsSummary);
        gpsCoordinate = text("-", 11, COLOR_MUTED, false);
        gpsCoordinate.setPadding(0, dp(6), 0, 0);
        gpsView.addView(gpsCoordinate);
        gpsView.setVisibility(View.GONE);

        reviewView = vertical();
        reviewView.setPadding(dp(14), dp(14), dp(14), dp(14));
        reviewView.setBackground(round(0xFFFFFFFF, dp(14), COLOR_STROKE, 1));
        content.addView(reviewView, gpsParams);
        reviewPhoto = new ImageView(this);
        reviewPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        reviewView.addView(reviewPhoto, new LinearLayout.LayoutParams(-1, dp(170)));
        reviewDate = addReviewField(reviewView, "Tanggal", R.drawable.ic_calendar);
        reviewTime = addReviewField(reviewView, "Jam", R.drawable.ic_clock);
        reviewAddress = addReviewField(reviewView, "Alamat", R.drawable.ic_location);
        reviewProvince = addReviewField(reviewView, "Provinsi", R.drawable.ic_building);
        reviewCity = addReviewField(reviewView, "Kota", R.drawable.ic_map);
        reviewDistrict = addReviewField(reviewView, "Kecamatan", R.drawable.ic_location);
        reviewShift = addReviewField(reviewView, "Shift", R.drawable.ic_user);
        reviewFace = addReviewField(reviewView, "Deteksi Wajah", R.drawable.ic_face);
        reviewView.setVisibility(View.GONE);

        successView = text("", 13, COLOR_TEXT, false);
        successView.setVisibility(View.GONE);
        content.addView(successView);
        successPanel = vertical();
        successPanel.setPadding(dp(14), dp(14), dp(14), dp(14));
        successPanel.setBackground(round(0xFFFFFFFF, dp(14), COLOR_STROKE, 1));
        content.addView(successPanel, gpsParams);
        successTitle = text(isClockOut() ? "CLOCK OUT BERHASIL" : "CLOCK IN BERHASIL", 23, COLOR_SUCCESS, true);
        successTitle.setGravity(Gravity.CENTER);
        successTitle.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        successPanel.addView(successTitle);
        successPhoto = new ImageView(this);
        successPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        successPhoto.setContentDescription("Ketuk untuk memperbesar foto absensi");
        successPhoto.setOnClickListener(v -> toggleSuccessPhotoSize());
        LinearLayout.LayoutParams successPhotoParams = new LinearLayout.LayoutParams(-1, responsiveSuccessPhotoHeight());
        successPhotoParams.setMargins(0, dp(12), 0, dp(8));
        successPanel.addView(successPhoto, successPhotoParams);
        successDate = addReviewField(successPanel, "Tanggal", R.drawable.ic_calendar);
        successClockIn = addReviewField(successPanel, "Clock In", R.drawable.ic_clock);
        successClockOut = addReviewField(successPanel, "Clock Out", R.drawable.ic_clock);
        successStatus = addReviewField(successPanel, "Status", R.drawable.ic_face);
        successFaceScore = addReviewField(successPanel, "Face Score", R.drawable.ic_face);
        successCutoff = addReviewField(successPanel, "Batas Absen", R.drawable.ic_clock);
        successShift = addReviewField(successPanel, "Shift", R.drawable.ic_user);
        successAddress = addReviewField(successPanel, "Lokasi", R.drawable.ic_location);
        successCoordinate = addReviewField(successPanel, "GPS", R.drawable.ic_map);
        spaceSuccessFields();
        successPanel.setVisibility(View.GONE);

        LinearLayout hiddenSupport = vertical();
        hiddenSupport.setVisibility(View.GONE);
        dateDay = text("-", 1, COLOR_TEXT, false);
        dateFull = text("-", 1, COLOR_TEXT, false);
        dateTime = text("-", 1, COLOR_TEXT, false);
        nameText = text("-", 1, COLOR_TEXT, false);
        usernameText = text("-", 1, COLOR_TEXT, false);
        avatarText = text(initialText(), 1, COLOR_TEXT, false);
        avatarImage = new ImageView(this);
        locationText = text("Mengambil lokasi...", 1, COLOR_TEXT, false);
        gpsBadge = text("GPS Nonaktif", 1, COLOR_TEXT, false);
        stepsView = horizontal();
        stepOne = text("1", 1, COLOR_TEXT, false);
        stepTwo = text("2", 1, COLOR_TEXT, false);
        stepThree = text("3", 1, COLOR_TEXT, false);
        stepsView.addView(stepOne);
        stepsView.addView(stepTwo);
        stepsView.addView(stepThree);
        hiddenSupport.addView(dateDay);
        hiddenSupport.addView(dateFull);
        hiddenSupport.addView(dateTime);
        hiddenSupport.addView(nameText);
        hiddenSupport.addView(usernameText);
        hiddenSupport.addView(avatarText);
        hiddenSupport.addView(avatarImage, new LinearLayout.LayoutParams(dp(1), dp(1)));
        hiddenSupport.addView(locationText);
        hiddenSupport.addView(gpsBadge);
        hiddenSupport.addView(stepsView);
        content.addView(hiddenSupport, new LinearLayout.LayoutParams(dp(1), dp(1)));

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);
        content.addView(progressBar, new LinearLayout.LayoutParams(dp(1), dp(1)));
        statusText = text("", 1, COLOR_MUTED, false);
        statusText.setVisibility(View.GONE);
        content.addView(statusText, new LinearLayout.LayoutParams(dp(1), dp(1)));
        historyButton = button("Riwayat Absen", 0xFFFFFFFF, COLOR_TEXT, COLOR_STROKE);
        syncButton = button("SYNC OFFLINE", 0xFFFFFFFF, COLOR_ACCENT, COLOR_ACCENT_STROKE);
        clockOutButton = button("CLOCK OUT", COLOR_ACCENT, 0xFFFFFFFF, 0);
        historyButton.setVisibility(View.GONE);
        syncButton.setVisibility(View.GONE);
        clockOutButton.setVisibility(View.GONE);
        content.addView(historyButton, new LinearLayout.LayoutParams(dp(1), dp(1)));
        content.addView(syncButton, new LinearLayout.LayoutParams(dp(1), dp(1)));
        content.addView(clockOutButton, new LinearLayout.LayoutParams(dp(1), dp(1)));
        historyButton.setOnClickListener(v -> openHistory());
        syncButton.setOnClickListener(v -> syncOfflineNow(true));
        clockOutButton.setOnClickListener(v -> startClockOutFlow());

        addFixedBottomNav(root, "Beranda");
        mainRoot = root;
        setContentView(root);
        resetPhotoStep();
        refreshOfflineSyncStatus(false);
    }

    private void buildLegacyLayout() {
        FrameLayout shell = new FrameLayout(this);
        shell.setBackgroundColor(COLOR_APP_BG);

        LinearLayout root = vertical();
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        shell.addView(root, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        card.setBackground(round(COLOR_PANEL, dp(24), 0, 0));
        setSoftElevation(card, 8);
        root.addView(card, new LinearLayout.LayoutParams(-1, -1));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(header, matchWrap());

        Button backButton = button("<", 0x00FFFFFF, COLOR_TEXT, 0);
        backButton.setTextSize(26);
        header.addView(backButton, new LinearLayout.LayoutParams(dp(46), dp(46)));
        backButton.setOnClickListener(v -> openHomePage());

        TextView screenTitle = text(isClockOut() ? "Absen Pulang" : "Absen Masuk", 18, COLOR_TEXT, true);
        screenTitle.setGravity(Gravity.CENTER);
        header.addView(screenTitle, new LinearLayout.LayoutParams(0, dp(46), 1));

        View headerSpacer = new View(this);
        header.addView(headerSpacer, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout dateCard = vertical();
        dateCard.setVisibility(View.GONE);
        dateDay = text("-", 12, COLOR_TEXT, true);
        dateFull = text("-", 11, COLOR_SUBTLE, false);
        dateTime = text("-", 11, COLOR_ACCENT, true);
        dateCard.addView(dateDay);
        dateCard.addView(dateFull);
        dateCard.addView(dateTime);
        card.addView(dateCard);

        LinearLayout userCard = horizontal();
        userCard.setGravity(Gravity.CENTER_VERTICAL);
        userCard.setPadding(dp(16), dp(14), dp(16), dp(14));
        userCard.setBackground(softRedPanel(dp(14)));
        setSoftElevation(userCard, 3);
        LinearLayout.LayoutParams userParams = matchWrap();
        userParams.setMargins(0, dp(16), 0, 0);
        card.addView(userCard, userParams);

        FrameLayout avatarWrap = new FrameLayout(this);
        avatarWrap.setBackground(round(COLOR_SOFT_ACCENT, dp(28), COLOR_STROKE, 1));
        avatarImage = new ImageView(this);
        avatarImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        avatarText = text("A", 22, COLOR_ACCENT, true);
        avatarText.setGravity(Gravity.CENTER);
        avatarWrap.addView(avatarImage, new FrameLayout.LayoutParams(-1, -1));
        avatarWrap.addView(avatarText, new FrameLayout.LayoutParams(-1, -1));
        userCard.addView(avatarWrap, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout userInfo = vertical();
        LinearLayout.LayoutParams userInfoParams = new LinearLayout.LayoutParams(0, -2, 1);
        userInfoParams.setMargins(dp(12), 0, 0, 0);
        userCard.addView(userInfo, userInfoParams);
        nameText = text("Lokasi Aktif", 13, COLOR_TEXT, true);
        usernameText = text("Menunggu lokasi...", 14, COLOR_TEXT, false);
        readyBadge = text("GPS", 11, 0xFF047857, true);
        readyBadge.setPadding(dp(12), dp(6), dp(12), dp(6));
        readyBadge.setBackground(round(COLOR_SOFT_GREEN, dp(16), 0, 0));
        userInfo.addView(nameText);
        userInfo.addView(usernameText);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(-2, -2);
        badgeParams.setMargins(0, dp(8), 0, 0);
        userInfo.addView(readyBadge, badgeParams);

        shiftSelector = horizontal();
        shiftSelector.setGravity(Gravity.CENTER);
        shiftSelector.setPadding(dp(5), dp(5), dp(5), dp(5));
        shiftSelector.setBackground(round(COLOR_FIELD, dp(16), COLOR_STROKE, 1));
        LinearLayout.LayoutParams shiftParams = matchWrap();
        shiftParams.setMargins(0, 0, 0, dp(6));
        shiftOneButton = button("SHIFT 1", COLOR_ACCENT_SOFT, COLOR_TEXT, 0);
        shiftTwoButton = button("SHIFT 2", COLOR_FIELD, COLOR_TEXT, COLOR_STROKE);
        shiftSelector.addView(shiftOneButton, shiftButtonParams());
        shiftSelector.addView(shiftTwoButton, shiftButtonParams());
        shiftOneButton.setOnClickListener(v -> {
            selectedShiftId = "1";
            updateShiftSelector();
        });
        shiftTwoButton.setOnClickListener(v -> {
            selectedShiftId = "2";
            updateShiftSelector();
        });
        updateShiftSelector();

        stepsView = horizontal();
        stepsView.setGravity(Gravity.CENTER);
        stepsView.setPadding(dp(10), dp(10), dp(10), dp(10));
        stepsView.setBackground(round(COLOR_FIELD, dp(18), COLOR_STROKE, 1));
        setSoftElevation(stepsView, 3);
        LinearLayout.LayoutParams stepsParams = matchWrap();
        stepsParams.setMargins(0, dp(12), 0, 0);
        card.addView(stepsView, stepsParams);
        stepsView.setVisibility(View.GONE);
        stepOne = addStep(stepsView, "1", "Foto Selfie");
        addLine(stepsView);
        stepTwo = addStep(stepsView, "2", "Lokasi GPS");
        addLine(stepsView);
        stepThree = addStep(stepsView, "3", isClockOut() ? "Kirim Clock Out" : "Kirim Absen");

        photoCard = vertical();
        photoCard.setPadding(dp(7), dp(7), dp(7), dp(7));
        photoCard.setBackground(round(0xFFFFFFFF, dp(110), 0xFF52C76D, 5));
        setSoftElevation(photoCard, 0);
        LinearLayout.LayoutParams photoParams = new LinearLayout.LayoutParams(-1, 0, 1);
        photoParams.setMargins(dp(54), dp(18), dp(54), dp(12));
        card.addView(photoCard, photoParams);

        FrameLayout photoBox = new FrameLayout(this);
        photoBox.setBackground(round(0xFFE5E7EB, dp(102), 0, 0));
        photoBox.setOnClickListener(v -> openCamera());
        photoCard.addView(photoBox, new LinearLayout.LayoutParams(-1, 0, 1));
        preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photoBox.addView(preview, new FrameLayout.LayoutParams(-1, -1));
        photoHint = text("Pastikan wajah berada di dalam area", 12, COLOR_TEXT, false);
        photoHint.setGravity(Gravity.CENTER);
        photoBox.addView(photoHint, new FrameLayout.LayoutParams(-1, -1));

        faceCard = infoCard("Validasi Wajah", "Ambil foto dulu", "0%");
        faceText = (TextView) faceCard.getChildAt(1);
        faceBadge = (TextView) faceCard.getChildAt(2);
        faceCard.setVisibility(View.GONE);
        card.addView(faceCard);

        locationCard = infoCard("Lokasi", "Mengambil lokasi...", "GPS Nonaktif");
        locationText = (TextView) locationCard.getChildAt(1);
        gpsBadge = (TextView) locationCard.getChildAt(2);
        card.addView(locationCard);

        gpsView = vertical();
        gpsView.setPadding(dp(13), dp(13), dp(13), dp(13));
        gpsView.setBackground(processPanelBackground(dp(18)));
        setSoftElevation(gpsView, 4);
        LinearLayout.LayoutParams gpsParams = new LinearLayout.LayoutParams(-1, 0, 1);
        gpsParams.setMargins(0, dp(12), 0, 0);
        card.addView(gpsView, gpsParams);
        gpsView.addView(text("Lokasi GPS", 15, COLOR_TEXT, true));
        mapView = new WebView(this);
        mapView.setBackground(round(0xFFF1F5F9, dp(12), 0, 0));
        WebSettings mapSettings = mapView.getSettings();
        mapSettings.setJavaScriptEnabled(true);
        mapSettings.setDomStorageEnabled(true);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(-1, 0, 1);
        mapParams.setMargins(0, dp(9), 0, dp(8));
        gpsView.addView(mapView, mapParams);
        gpsAddress = text("Mengambil alamat...", 12, COLOR_MUTED, true);
        gpsAddress.setPadding(0, dp(8), 0, 0);
        gpsView.addView(gpsAddress);
        gpsSummary = text("", 12, COLOR_SUBTLE, false);
        gpsSummary.setPadding(0, dp(8), 0, 0);
        gpsView.addView(gpsSummary);
        gpsView.addView(text("Koordinat", 12, COLOR_TEXT, true));
        gpsCoordinate = text("-", 11, COLOR_SUBTLE, false);
        gpsView.addView(gpsCoordinate);
        gpsView.setVisibility(View.GONE);

        reviewView = vertical();
        reviewView.setPadding(dp(11), dp(11), dp(11), dp(11));
        reviewView.setBackground(processPanelBackground(dp(18)));
        setSoftElevation(reviewView, 4);
        LinearLayout.LayoutParams reviewParams = new LinearLayout.LayoutParams(-1, 0, 1);
        reviewParams.setMargins(0, dp(12), 0, 0);
        card.addView(reviewView, reviewParams);
        reviewPhoto = new ImageView(this);
        reviewPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams reviewPhotoParams = new LinearLayout.LayoutParams(-1, dp(150));
        reviewPhotoParams.setMargins(0, 0, 0, dp(8));
        reviewView.addView(reviewPhoto, reviewPhotoParams);
        reviewDate = addReviewField(reviewView, "Tanggal", R.drawable.ic_calendar);
        reviewTime = addReviewField(reviewView, "Jam", R.drawable.ic_clock);
        reviewAddress = addReviewField(reviewView, "Alamat", R.drawable.ic_location);
        reviewProvince = addReviewField(reviewView, "Provinsi", R.drawable.ic_building);
        reviewCity = addReviewField(reviewView, "Kota", R.drawable.ic_map);
        reviewDistrict = addReviewField(reviewView, "Kecamatan", R.drawable.ic_location);
        reviewShift = addReviewField(reviewView, "Shift", R.drawable.ic_user);
        reviewFace = addReviewField(reviewView, "Deteksi Wajah", R.drawable.ic_face);
        reviewView.setVisibility(View.GONE);

        actions = vertical();
        actions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionParams = matchWrap();
        actionParams.setMargins(0, dp(9), 0, 0);
        card.addView(actions, actionParams);
        retakeButton = button("FOTO ULANG", 0xFFFFFFFF, COLOR_ACCENT, COLOR_ACCENT_STROKE);
        nextButton = button("LANJUT", COLOR_ACCENT_SOFT, 0xFFFFFFFF, 0);
        captureButton = button("AMBIL FOTO", COLOR_ACCENT_SOFT, 0xFFFFFFFF, 0);
        actionRow = horizontal();
        actionRow.setGravity(Gravity.CENTER);
        actions.addView(shiftSelector, shiftParams);
        actions.addView(actionRow, matchWrap());
        actionRow.addView(retakeButton, actionButtonParams());
        actionRow.addView(nextButton, actionButtonParams());
        actions.addView(captureButton, fullButtonParams());
        retakeButton.setOnClickListener(v -> onRetake());
        nextButton.setOnClickListener(v -> onNext());
        captureButton.setOnClickListener(v -> onCapture());

        successView = text("", 14, COLOR_TEXT, false);
        successView.setPadding(dp(16), dp(14), dp(16), dp(14));
        successView.setGravity(Gravity.CENTER);
        successView.setBackground(processPanelBackground(dp(22)));
        LinearLayout.LayoutParams successParams = matchWrap();
        successParams.setMargins(0, dp(14), 0, 0);
        card.addView(successView, successParams);
        successView.setVisibility(View.GONE);

        successPanel = vertical();
        successPanel.setPadding(dp(12), dp(11), dp(12), dp(11));
        successPanel.setBackground(processPanelBackground(dp(18)));
        setSoftElevation(successPanel, 4);
        LinearLayout.LayoutParams successPanelParams = new LinearLayout.LayoutParams(-1, 0, 1);
        successPanelParams.setMargins(0, dp(11), 0, 0);
        card.addView(successPanel, successPanelParams);
        successTitle = text(isClockOut() ? "CLOCK OUT BERHASIL" : "CLOCK IN BERHASIL", 19, COLOR_TEXT, true);
        successTitle.setGravity(Gravity.CENTER);
        successPanel.addView(successTitle);
        successPhoto = new ImageView(this);
        successPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        LinearLayout.LayoutParams successPhotoParams = new LinearLayout.LayoutParams(-1, 0, 1);
        successPhotoParams.setMargins(0, dp(10), 0, dp(8));
        successPanel.addView(successPhoto, successPhotoParams);
        successDate = addReviewField(successPanel, "Tanggal", R.drawable.ic_calendar);
        successClockIn = addReviewField(successPanel, "Clock In", R.drawable.ic_clock);
        successClockOut = addReviewField(successPanel, "Clock Out", R.drawable.ic_clock);
        successStatus = addReviewField(successPanel, "Status", R.drawable.ic_face);
        successCutoff = addReviewField(successPanel, "Batas Absen", R.drawable.ic_clock);
        successShift = addReviewField(successPanel, "Shift", R.drawable.ic_user);
        successAddress = addReviewField(successPanel, "Lokasi", R.drawable.ic_location);
        successAddress.setMinHeight(dp(48));
        successCoordinate = addReviewField(successPanel, "GPS", R.drawable.ic_map);
        successPanel.setVisibility(View.GONE);

        progressBar = new ProgressBar(this);
        progressBar.setVisibility(View.GONE);

        statusText = text("Buka aplikasi dari QR/link absensi.", 12, COLOR_SUBTLE, false);
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(dp(8), dp(6), dp(8), 0);
        card.addView(statusText);
        statusText.setVisibility(View.GONE);

        View bottomSpacer = new View(this);
        card.addView(bottomSpacer, new LinearLayout.LayoutParams(-1, dp(7)));

        historyButton = button("Riwayat Absen", 0xFFFFFFFF, COLOR_TEXT, COLOR_STROKE);
        syncButton = button("SYNC OFFLINE", 0xFFFFFFFF, COLOR_ACCENT, COLOR_ACCENT_STROKE);
        clockOutButton = button("CLOCK OUT", COLOR_ACCENT_SOFT, 0xFFFFFFFF, 0);
        LinearLayout.LayoutParams clockOutParams = new LinearLayout.LayoutParams(-1, dp(42));
        clockOutParams.setMargins(0, dp(6), 0, 0);
        card.addView(clockOutButton, clockOutParams);
        clockOutButton.setVisibility(View.GONE);
        clockOutButton.setOnClickListener(v -> startClockOutFlow());

        LinearLayout.LayoutParams syncParams = new LinearLayout.LayoutParams(-1, dp(42));
        syncParams.setMargins(0, dp(6), 0, 0);
        card.addView(syncButton, syncParams);
        syncButton.setVisibility(View.GONE);
        syncButton.setOnClickListener(v -> syncOfflineNow(true));

        LinearLayout.LayoutParams historyParams = new LinearLayout.LayoutParams(-1, dp(42));
        historyParams.setMargins(0, dp(6), 0, 0);
        card.addView(historyButton, historyParams);
        historyButton.setOnClickListener(v -> openHistory());

        LinearLayout bottomNav = horizontal();
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(6), dp(8), dp(6), dp(6));
        bottomNav.setBackground(round(0xFFFFFFFF, dp(18), COLOR_STROKE, 1));
        setSoftElevation(bottomNav, 4);
        LinearLayout.LayoutParams navParams = matchWrap();
        navParams.setMargins(0, dp(8), 0, 0);
        card.addView(bottomNav, navParams);
        bottomNav.addView(navItem("Beranda", R.drawable.ic_home, true, v -> openHomePage()), new LinearLayout.LayoutParams(0, dp(74), 1));
        bottomNav.addView(navItem("Riwayat", R.drawable.ic_clock, false, v -> openHistory()), new LinearLayout.LayoutParams(0, dp(74), 1));
        bottomNav.addView(navItem("Izin", R.drawable.ic_clipboard, false, v -> openLeavePage()), new LinearLayout.LayoutParams(0, dp(74), 1));
        bottomNav.addView(navItem("Akun", R.drawable.ic_user, false, v -> openAccountPage()), new LinearLayout.LayoutParams(0, dp(74), 1));

        mainRoot = shell;
        setContentView(shell);
        resetPhotoStep();
        refreshOfflineSyncStatus(false);
    }

    private void refreshOfflineSyncStatus(boolean updateTextWhenEmpty) {
        try {
            int pending = new OfflineAttendanceQueue(this).pendingCount();
            if (syncButton != null) {
                syncButton.setVisibility(pending > 0 ? View.VISIBLE : View.GONE);
            }
            if (pending > 0) {
                startOfflineSyncPolling();
                if (statusText != null) {
                    statusText.setVisibility(View.VISIBLE);
                    statusText.setText(pending + " data offline menunggu sync. Aplikasi coba sync otomatis tiap 1 menit.");
                }
            } else if (updateTextWhenEmpty) {
                stopOfflineSyncPolling();
                if (statusText != null) {
                    statusText.setVisibility(View.VISIBLE);
                    statusText.setText("Tidak ada data offline yang menunggu sync.");
                }
            } else {
                stopOfflineSyncPolling();
            }
        } catch (Exception ignored) {
        }
    }

    private void startOfflineSyncPolling() {
        if (offlineSyncPollingActive) {
            return;
        }
        offlineSyncPollingActive = true;
        handler.removeCallbacks(offlineSyncPollRunnable);
        handler.postDelayed(offlineSyncPollRunnable, 60000);
    }

    private void stopOfflineSyncPolling() {
        offlineSyncPollingActive = false;
        handler.removeCallbacks(offlineSyncPollRunnable);
    }

    private void syncOfflineNow(boolean showMessage) {
        if (offlineSyncInProgress) {
            return;
        }
        int pendingBefore = 0;
        try {
            pendingBefore = new OfflineAttendanceQueue(this).pendingCount();
        } catch (Exception ignored) {
        }
        if (pendingBefore <= 0) {
            refreshOfflineSyncStatus(showMessage);
            return;
        }
        offlineSyncInProgress = true;
        final int beforeCount = pendingBefore;
        if (statusText != null) {
            statusText.setVisibility(View.VISIBLE);
            statusText.setText("Mencoba sync " + beforeCount + " data offline...");
        }
        if (syncButton != null) {
            syncButton.setEnabled(false);
            syncButton.setText("SYNC...");
        }
        new Thread(() -> {
            boolean success = false;
            int pendingAfter = beforeCount;
            try {
                success = OfflineSyncJobService.syncPendingNow(this);
                pendingAfter = new OfflineAttendanceQueue(this).pendingCount();
            } catch (Exception ignored) {
            }
            final boolean finalSuccess = success;
            final int finalPending = pendingAfter;
            runOnUiThread(() -> {
                offlineSyncInProgress = false;
                if (syncButton != null) {
                    syncButton.setEnabled(true);
                    syncButton.setText("SYNC OFFLINE");
                    syncButton.setVisibility(finalPending > 0 ? View.VISIBLE : View.GONE);
                }
                if (finalPending <= 0) {
                    stopOfflineSyncPolling();
                    if (statusText != null) {
                        statusText.setVisibility(View.VISIBLE);
                        statusText.setText("Tidak ada data offline yang menunggu sync.");
                    }
                    if ("Beranda".equals(currentSection)) {
                        loadHomeOverview();
                    }
                } else if (finalSuccess) {
                    startOfflineSyncPolling();
                    if (statusText != null) {
                        statusText.setVisibility(View.VISIBLE);
                        statusText.setText(finalPending + " data offline masih menunggu sync.");
                    }
                } else {
                    startOfflineSyncPolling();
                    if (statusText != null) {
                        statusText.setVisibility(View.VISIBLE);
                        statusText.setText("Belum bisa sync. " + finalPending + " data masih menunggu. Cek server/koneksi lalu tekan SYNC OFFLINE.");
                    }
                }
            });
        }).start();
    }

    private TextView addStep(LinearLayout parent, String number, String label) {
        LinearLayout wrap = vertical();
        wrap.setGravity(Gravity.CENTER);
        TextView circle = text(number, 12, COLOR_SUBTLE, true);
        circle.setGravity(Gravity.CENTER);
        circle.setBackground(round(COLOR_FIELD, dp(17), COLOR_STROKE, 2));
        wrap.addView(circle, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView labelView = text(label, 11, COLOR_SUBTLE, false);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, dp(4), 0, 0);
        wrap.addView(labelView);
        parent.addView(wrap, new LinearLayout.LayoutParams(0, -2, 1));
        return circle;
    }

    private void addLine(LinearLayout parent) {
        View line = new View(this);
        line.setBackgroundColor(COLOR_STROKE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(16), dp(1));
        params.setMargins(dp(2), 0, dp(2), dp(14));
        parent.addView(line, params);
    }

    private LinearLayout infoCard(String title, String body, String badge) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(9), dp(12), dp(9));
        row.setBackground(round(COLOR_FIELD, dp(14), COLOR_STROKE, 1));
        setSoftElevation(row, 2);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, 0);
        row.setLayoutParams(params);

        TextView titleView = text(title, 12, COLOR_TEXT, true);
        titleView.setVisibility(View.GONE);
        row.addView(titleView);

        TextView bodyView = text(body, 12, COLOR_MUTED, false);
        row.addView(bodyView, new LinearLayout.LayoutParams(0, -2, 1));

        TextView badgeView = text(badge, 11, COLOR_TEXT, true);
        badgeView.setGravity(Gravity.CENTER);
        badgeView.setPadding(dp(10), dp(5), dp(10), dp(5));
        badgeView.setBackground(round(COLOR_SOFT_ACCENT, dp(16), COLOR_ACCENT, 1));
        row.addView(badgeView);
        return row;
    }

    private TextView addReviewField(LinearLayout parent, String label, int iconRes) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));
        parent.addView(row, matchWrap());

        ImageView iconView = icon(iconRes);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(22), dp(22));
        iconParams.setMargins(0, 0, dp(10), 0);
        row.addView(iconView, iconParams);

        TextView labelView = text(label, 12, COLOR_SUBTLE, false);
        row.addView(labelView, new LinearLayout.LayoutParams(0, -2, 1));

        TextView valueView = text("-", 13, COLOR_TEXT, true);
        valueView.setGravity(Gravity.RIGHT);
        valueView.setMaxLines(4);
        row.addView(valueView, new LinearLayout.LayoutParams(0, -2, 1));

        View divider = new View(this);
        divider.setBackgroundColor(0xFFECECEC);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.setMargins(dp(32), 0, 0, 0);
        parent.addView(divider, dividerParams);
        return valueView;
    }

    private void loadFromIntent(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data != null) {
            webUrl = data.getQueryParameter("web_url");
            String intentToken = data.getQueryParameter("token");
            String intentMode = data.getQueryParameter("mode");
            String intentUsername = data.getQueryParameter("username");
            String intentFullname = data.getQueryParameter("fullname");
            String intentLevel = data.getQueryParameter("level");
            String intentPhoto = data.getQueryParameter("last_photo_url");
            if (intentToken != null && intentToken.length() > 0) {
                appToken = intentToken;
            }
            if (intentMode != null && intentMode.length() > 0) {
                mode = intentMode;
            }
            if (intentUsername != null && intentUsername.length() > 0) {
                username = intentUsername;
            }
            if (intentFullname != null && intentFullname.length() > 0) {
                fullname = intentFullname;
            }
            if (intentLevel != null && intentLevel.length() > 0) {
                userRole = intentLevel;
            }
            if (intentPhoto != null && intentPhoto.length() > 0 && !intentPhoto.contains("nophoto.png")) {
                lastPhotoUrl = intentPhoto;
            }
        }

        if (webUrl == null || webUrl.trim().isEmpty()) {
            statusText.setText("Buka aplikasi dari QR/link absensi.");
            return;
        }

        try {
            // Link QR hanya membawa token/akun. Seluruh API tetap menuju VPS.
            baseUrl = SERVER_URL;
            getSharedPreferences("attendance_app", MODE_PRIVATE)
                .edit()
                .putString("base_url", baseUrl)
                .apply();
        } catch (Exception ignored) {
            statusText.setText("Link absensi tidak valid.");
            return;
        }

        readyBadge.setText("GPS");
        stepThree.setText("3");
        captureButton.setText(isClockOut() ? "Absen Pulang" : "Absen Masuk");
        applyAccountView();
        loadAvatarPhoto();
        bootstrapSession();
    }

    private void startAttendanceFromLogin(JSONObject data) {
        username = data.optString("username", username);
        shownUpdateMessageIdThisLogin = 0;
        fullname = data.optString("fullname", username);
        userRole = data.optString("level", userRole);
        appDisplayName = data.optString("display_name", appDisplayName);
        department = data.optString("department", department);
        profilePhotoUrl = data.optString("profile_photo_url", profilePhotoUrl);
        profilePhotoBitmap = null;
        selfieBitmap = null;
        mode = data.optString("mode", "clock_in");
        appToken = data.optString("token", "");
        syncToken = data.optString(
            "sync_token",
            getSharedPreferences("attendance_app", MODE_PRIVATE)
                .getString("sync_token", "")
        );
        webUrl = data.optString("web_url", "");
        lastPhotoUrl = data.optString("last_photo_url", "");
        disableLocationLock = data.optBoolean("disable_location_lock", false);
        gpsLocked = false;
        JSONArray serverAttendanceLocations = data.optJSONArray("attendance_locations");
        if (serverAttendanceLocations != null && serverAttendanceLocations.length() > 0) {
            attendanceLocations = serverAttendanceLocations;
        }
        JSONObject shiftData = data.optJSONObject("shift");
        totalShift = data.optInt("total_shift", 0);
        if (totalShift < 1) {
            totalShift = shiftData != null
                ? shiftData.optInt("total_shift", 1)
                : 1;
        }
        if (shiftData != null) {
            shift1ClockIn = shiftData.optString("shift1_clock_in", "");
            shift1ClockOut = shiftData.optString("shift1_clock_out", "");
            shift2ClockIn = shiftData.optString("shift2_clock_in", "");
            shift2ClockOut = shiftData.optString("shift2_clock_out", "");
        }
        if (totalShift < 2) {
            selectedShiftId = "1";
        }

        try {
            // Jangan kembali ke server Windows dari web_url respons login.
            baseUrl = SERVER_URL;
            getSharedPreferences("attendance_app", MODE_PRIVATE)
                .edit()
                .putString("base_url", baseUrl)
                .putString("sync_token", syncToken)
                .apply();
        } catch (Exception ignored) {}

        buildLayout();
        readyBadge.setText("GPS");
        captureButton.setText(isClockOut() ? "Absen Pulang" : "Absen Masuk");
        applyAccountView();
        loadAvatarPhoto();
        JSONObject activeAttendance = data.optJSONObject("active_attendance");
        if (!offlineMode) {
            JSONObject pendingAttendance = new OfflineAttendanceQueue(this)
                .latestPendingAttendance(username);
            if (pendingAttendance != null) {
                mode = hasCachedClockOut(pendingAttendance) ? "clock_in" : "clock_out";
                cacheAttendanceState(pendingAttendance);
            } else if (activeAttendance != null && activeAttendance.length() > 0) {
                replaceAttendanceStateFromServer(activeAttendance);
            } else {
                clearTodayCachedAttendance(username);
                mode = "clock_in";
            }
            readyBadge.setText("GPS");
            stepThree.setText("3");
            captureButton.setText(isClockOut() ? "Absen Pulang" : "Absen Masuk");
            openHomePage();
            return;
        }
        JSONObject pendingAttendance = new OfflineAttendanceQueue(this).latestPendingAttendance(username);
        if (pendingAttendance != null) {
            mode = hasCachedClockOut(pendingAttendance) ? "clock_in" : "clock_out";
            cacheAttendanceState(pendingAttendance);
            openHomePage();
            return;
        }
        JSONObject cachedAttendance = getTodayCachedAttendance(username);
        if (cachedAttendance != null) {
            mode = hasCachedClockOut(cachedAttendance) ? "clock_in" : "clock_out";
            openHomePage();
            return;
        }
        openHomePage();
    }

    private void bootstrapSession() {
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Menyiapkan sesi absensi...");
        new Thread(() -> {
            try {
                String html = httpGet(webUrl);
                String parsedName = parseHtml(html, "class=\"name\"[^>]*>\\s*([^<]+)");
                String parsedUsername = parseHtml(html, "class=\"role\"[^>]*>\\s*@?([^<]+)");
                if (parsedName.length() > 0) {
                    fullname = parsedName;
                }
                if (parsedUsername.length() > 0) {
                    username = parsedUsername;
                }
                String parsedPhoto = parseHtml(html, "class=\"avatar\"[^>]*src=\"([^\"]+)\"");
                if (parsedPhoto.length() > 0 && !parsedPhoto.contains("nophoto.png")) {
                    lastPhotoUrl = absoluteUrl(parsedPhoto);
                }
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    applyAccountView();
                    loadAvatarPhoto();
                    statusText.setText("Sesi siap. Pencet tombol Ambil Foto.");
                    requestGps();
                    loadAppProfile(false);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    statusText.setText("Gagal menyiapkan sesi: " + error.getMessage());
                    requestGps();
                });
            }
        }).start();
    }

    private void onCapture() {
        String label = captureButton.getText().toString();
        if (label.contains("KIRIM") || "MENGIRIM...".equals(label)) {
            submitAttendance();
            return;
        }
        if (selfieBitmap != null) {
            if (!faceValidated || faceScore < 80) {
                notifyUser("Foto ulang, wajah harus terlihat jelas.");
                return;
            }
            if (!gpsLocked) {
                notifyUser(locationLockMessage());
                requestGps();
                return;
            }
            submitAttendance();
            return;
        }
        openCamera();
    }

    private void submitNativeLogin() {
        String inputUsername = loginUsername == null ? "" : loginUsername.getText().toString().trim();
        String inputPassword = loginPassword == null ? "" : loginPassword.getText().toString().trim();

        if (baseUrl == null || baseUrl.length() == 0) {
            baseUrl = DEFAULT_BASE_URL;
        }
        if (baseUrl == null || baseUrl.length() == 0) {
            notifyUser("Server belum tersambung.");
            return;
        }
        if (inputUsername.length() == 0 || inputPassword.length() == 0) {
            notifyUser("Username dan password wajib diisi.");
            return;
        }

        loginButton.setEnabled(false);
        loginButton.setText("Memuat akun...");

        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("username", inputUsername);
                payload.put("password", inputPassword);
                String activeBaseUrl = normalizeServerBaseUrl(baseUrl);
                baseUrl = activeBaseUrl;
                getSharedPreferences("attendance_app", MODE_PRIVATE)
                    .edit()
                    .putString("base_url", baseUrl)
                    .apply();
                String response = postJson(activeBaseUrl + "/api/app_login", payload.toString());
                JSONObject data = new JSONObject(requireJsonObject(response, "/api/app_login"));
                if (!data.optBoolean("success", false)) {
                    throw new Exception(data.optString("error", "Login gagal"));
                }
                offlineMode = false;
                getSharedPreferences("attendance_app", MODE_PRIVATE)
                    .edit()
                    .putString("cached_login", data.toString())
                    .putString("cached_username", inputUsername)
                    .putString("cached_login_hash", loginHash(inputUsername, inputPassword))
                    .apply();
                OfflineAttendanceQueue.schedule(this);
                OfflineSyncJobService.syncNow(this);
                runOnUiThread(() -> startAttendanceFromLogin(data));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                    if (!startCachedOfflineLogin(inputUsername, inputPassword)) {
                        notifyUser(error.getMessage());
                    }
                });
            }
        }).start();
    }

    private boolean startCachedOfflineLogin(String inputUsername, String inputPassword) {
        try {
            SharedPreferences prefs = getSharedPreferences("attendance_app", MODE_PRIVATE);
            String cachedUsername = prefs.getString("cached_username", "");
            String cachedHash = prefs.getString("cached_login_hash", "");
            String cachedLogin = prefs.getString("cached_login", "");
            if (
                !cachedUsername.equalsIgnoreCase(inputUsername) ||
                !cachedHash.equals(loginHash(inputUsername, inputPassword)) ||
                cachedLogin.length() == 0
            ) {
                return false;
            }
            JSONObject data = new JSONObject(cachedLogin);
            OfflineAttendanceQueue queue = new OfflineAttendanceQueue(this);
            JSONObject pendingAttendance = queue.latestPendingAttendance(inputUsername);
            if (pendingAttendance != null) {
                selectedShiftId = pendingAttendance.optString("shift_id", "1");
                data.put(
                    "mode",
                    hasCachedClockOut(pendingAttendance) ? "clock_in" : "clock_out"
                );
                data.put("active_attendance", pendingAttendance);
            } else {
                JSONObject cachedAttendance = getTodayCachedAttendance(inputUsername);
                if (cachedAttendance != null) {
                    data.put(
                        "mode",
                        hasCachedClockOut(cachedAttendance) ? "clock_in" : "clock_out"
                    );
                    data.put("active_attendance", cachedAttendance);
                }
            }
            offlineMode = true;
            startAttendanceFromLogin(data);
            statusText.setText(
                "Mode offline. Data akan disinkronkan saat server online."
            );
            notifyUser("Login offline berhasil.");
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String loginHash(String inputUsername, String inputPassword) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] result = digest.digest(
            (
                "TRACER_OFFLINE_LOGIN|" +
                inputUsername.toLowerCase(Locale.US) +
                "|" +
                inputPassword
            ).getBytes("UTF-8")
        );
        StringBuilder hex = new StringBuilder();
        for (byte value : result) {
            hex.append(String.format(Locale.US, "%02x", value & 0xff));
        }
        return hex.toString();
    }

    private void onRetake() {
        if (currentStep == 2) {
            showGpsStep();
            return;
        }
        if (currentStep == 1) {
            showPhotoCaptured();
            return;
        }
        selfieBitmap = null;
        faceValidated = false;
        faceScore = 0;
        preview.setImageBitmap(null);
        faceCard.setVisibility(View.GONE);
        resetPhotoStep();
    }

    private void onNext() {
        if (selfieBitmap == null) {
            notifyUser("Ambil foto dulu.");
            return;
        }
        if (!faceValidated || faceScore < 80) {
            notifyUser("Foto ulang, wajah harus terlihat jelas.");
            return;
        }
        if (!gpsLocked) {
            notifyUser(locationLockMessage());
            requestGps();
            return;
        }
        if (currentStep == 0) {
            showGpsStep();
            return;
        }
        if (currentStep == 1) {
            showReviewStep();
        }
    }

    private void resetPhotoStep() {
        currentStep = 0;
        if (attendanceContent != null) {
            attendanceContent.setPadding(dp(18), dp(18), dp(18), dp(96));
        }
        moveValidationCardsAboveGps();
        photoCard.setVisibility(View.VISIBLE);
        photoRingContainer.setVisibility(View.VISIBLE);
        if (photoFacePrompt != null) {
            photoFacePrompt.setVisibility(View.VISIBLE);
        }
        animateStepIn(photoCard);
        locationCard.setVisibility(View.VISIBLE);
        faceCard.setVisibility(View.VISIBLE);
        gpsView.setVisibility(View.GONE);
        reviewView.setVisibility(View.GONE);
        successView.setVisibility(View.GONE);
        successPanel.setVisibility(View.GONE);
        successTitle.setVisibility(View.VISIBLE);
        if (photoFacePrompt != null) {
            photoFacePrompt.setText("Pastikan wajah Anda berada di dalam area");
            photoFacePrompt.setTextSize(13);
            photoFacePrompt.setTextColor(COLOR_TEXT);
            photoFacePrompt.setTypeface(Typeface.DEFAULT);
            photoFacePrompt.setPadding(0, dp(22), 0, dp(14));
            photoFacePrompt.setVisibility(View.VISIBLE);
        }
        clockOutButton.setVisibility(View.GONE);
        updateShiftSelector();
        actions.setVisibility(View.VISIBLE);
        actionRow.setVisibility(View.GONE);
        retakeButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
        captureButton.setText(isClockOut() ? "Absen Pulang" : "Absen Masuk");
        if (photoHint != null) {
            photoHint.setVisibility(View.VISIBLE);
        }
        if (photoPlaceholderIcon != null) {
            photoPlaceholderIcon.setVisibility(View.VISIBLE);
        }
        if (selfieBitmap == null) {
            faceScoreValue.setText("--");
            faceBadge.setText("Menunggu Foto");
            faceScoreProgress.setProgress(0);
        }
        updateFaceRingColor();
        updateSteps();
    }

    private void showPhotoCaptured() {
        currentStep = 0;
        moveValidationCardsAboveGps();
        photoCard.setVisibility(View.VISIBLE);
        photoRingContainer.setVisibility(View.VISIBLE);
        if (photoFacePrompt != null) {
            photoFacePrompt.setVisibility(View.VISIBLE);
        }
        animateStepIn(photoCard);
        gpsView.setVisibility(View.GONE);
        reviewView.setVisibility(View.GONE);
        successPanel.setVisibility(View.GONE);
        locationCard.setVisibility(View.VISIBLE);
        calculateFaceScore();
        faceCard.setVisibility(View.VISIBLE);
        faceText.setText("Pastikan pencahayaan cukup dan wajah terlihat jelas");
        updateFaceBadgeColor();
        animateFaceScore(faceScore);
        if (photoHint != null) {
            photoHint.setVisibility(View.GONE);
        }
        if (photoPlaceholderIcon != null) {
            photoPlaceholderIcon.setVisibility(View.GONE);
        }
        actionRow.setVisibility(View.GONE);
        retakeButton.setVisibility(View.GONE);
        nextButton.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
        captureButton.setText(isClockOut() ? "Absen Pulang" : "Absen Masuk");
        if (!faceValidated) {
            notifyUser("Foto ulang, wajah harus terlihat jelas.");
        }
        updateSteps();
    }

    private void calculateFaceScore() {
        faceScore = 0;
        faceValidated = false;

        if (selfieBitmap == null) {
            return;
        }

        Bitmap bitmap = Bitmap.createScaledBitmap(selfieBitmap, 160, 160, true);
        double centerTexture = sampleRegion(bitmap, .20, .12, .80, .90, "texture");
        double centerSkin = sampleRegion(bitmap, .20, .16, .80, .90, "skin");
        double leftDark = sampleRegion(bitmap, .18, .20, .50, .55, "dark");
        double rightDark = sampleRegion(bitmap, .50, .20, .82, .55, "dark");
        double mouthDark = sampleRegion(bitmap, .30, .55, .70, .85, "dark");
        double sideTexture = (
            sampleRegion(bitmap, .02, .18, .18, .88, "texture") +
            sampleRegion(bitmap, .82, .18, .98, .88, "texture")
        ) / 2.0;

        double score = 0;
        score += Math.min(30, centerTexture * 55);
        score += Math.min(32, centerSkin * 65);
        score += Math.min(18, (leftDark + rightDark + mouthDark) * 70);
        score += Math.min(20, Math.max(0, centerTexture - sideTexture) * 90);

        boolean hasFaceArea = centerTexture >= .12 && centerSkin >= .045;
        boolean hasFeaturePattern =
            (leftDark + rightDark + mouthDark) >= .035 ||
            Math.max(leftDark, rightDark) >= .018;

        faceScore = Math.max(0, Math.min(100, (int)Math.round(score)));
        faceValidated = faceScore >= 80 && hasFaceArea && hasFeaturePattern;

        if (faceValidated && faceScore < 80) {
            faceScore = 80;
        }
    }

    private double sampleRegion(Bitmap bitmap, double rx1, double ry1, double rx2, double ry2, String mode) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int x1 = Math.max(0, (int)Math.floor(width * rx1));
        int x2 = Math.min(width, (int)Math.ceil(width * rx2));
        int y1 = Math.max(0, (int)Math.floor(height * ry1));
        int y2 = Math.min(height, (int)Math.ceil(height * ry2));
        int total = 0;
        int hit = 0;

        for (int y = y1; y < y2; y += 3) {
            for (int x = x1; x < x2; x += 3) {
                int color = bitmap.getPixel(x, y);
                int r = Color.red(color);
                int g = Color.green(color);
                int b = Color.blue(color);
                double brightness = (r * .299) + (g * .587) + (b * .114);
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                int saturation = max - min;

                if ("skin".equals(mode)) {
                    if (r > 50 && g > 32 && b > 20 && r > b && saturation > 10 && brightness > 45 && brightness < 240) {
                        hit += 1;
                    }
                } else if ("dark".equals(mode)) {
                    if (brightness < 135 && saturation > 8) {
                        hit += 1;
                    }
                } else {
                    if (saturation > 15 && brightness > 40 && brightness < 240) {
                        hit += 1;
                    }
                }

                total += 1;
            }
        }

        return total == 0 ? 0 : (double)hit / (double)total;
    }

    private void showGpsStep() {
        currentStep = 1;
        moveValidationCardsBelowGps();
        photoCard.setVisibility(View.GONE);
        photoRingContainer.setVisibility(View.GONE);
        photoFacePrompt.setVisibility(View.GONE);
        gpsView.setVisibility(View.VISIBLE);
        animateStepIn(gpsView);
        successPanel.setVisibility(View.GONE);
        faceCard.setVisibility(View.VISIBLE);
        locationCard.setVisibility(View.VISIBLE);
        reviewView.setVisibility(View.GONE);
        showDualActionRow();
        retakeButton.setVisibility(View.VISIBLE);
        retakeButton.setText("KEMBALI");
        nextButton.setVisibility(View.VISIBLE);
        nextButton.setText("LANJUT");
        captureButton.setVisibility(View.GONE);
        updateSteps();
    }

    private void showReviewStep() {
        currentStep = 2;
        photoCard.setVisibility(View.GONE);
        photoRingContainer.setVisibility(View.GONE);
        photoFacePrompt.setVisibility(View.GONE);
        faceCard.setVisibility(View.GONE);
        locationCard.setVisibility(View.GONE);
        gpsView.setVisibility(View.GONE);
        successPanel.setVisibility(View.GONE);
        reviewView.setVisibility(View.VISIBLE);
        animateStepIn(reviewView);
        reviewPhoto.setImageBitmap(selfieBitmap);
        Date now = new Date();
        reviewDate.setText(new SimpleDateFormat("dd/MM/yyyy", new Locale("id", "ID")).format(now));
        reviewTime.setText(new SimpleDateFormat("HH:mm:ss", new Locale("id", "ID")).format(now));
        reviewAddress.setText(valueOrDash(address));
        reviewProvince.setText(summaryValue("Provinsi"));
        reviewCity.setText(summaryValue("Kota"));
        reviewDistrict.setText(summaryValue("Kecamatan"));
        reviewShift.setText("2".equals(selectedShiftId) ? "Shift 2" : "Shift 1");
        reviewFace.setText(faceScore + "%");
        reviewFace.setTextColor(faceScore >= 80 ? 0xFF047857 : COLOR_TEXT);
        showSingleBackAction();
        nextButton.setVisibility(View.GONE);
        retakeButton.setText("KEMBALI");
        captureButton.setVisibility(View.VISIBLE);
        captureButton.setText(isClockOut() ? "KIRIM CLOCK OUT" : "KIRIM ABSENSI");
        updateSteps();
    }

    private void moveValidationCardsBelowGps() {
        // The reference layout keeps validation panels in one stable order.
    }

    private void moveValidationCardsAboveGps() {
        // The reference layout keeps validation panels in one stable order.
    }

    private void showDualActionRow() {
        actionRow.setVisibility(View.VISIBLE);
        retakeButton.setLayoutParams(actionButtonParams());
        nextButton.setLayoutParams(actionButtonParams());
        retakeButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.VISIBLE);
    }

    private void showSingleBackAction() {
        actionRow.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(42));
        params.setMargins(0, 0, 0, 0);
        retakeButton.setLayoutParams(params);
        retakeButton.setVisibility(View.VISIBLE);
        nextButton.setVisibility(View.GONE);
    }

    private void showSuccess(String response) {
        try {
            JSONObject root = new JSONObject(requireJsonObject(
                response,
                isClockOut() ? "/api/attendance_clock_out" : "/api/attendance"
            ));
            JSONObject attendance = root.optJSONObject("attendance");
            if (attendance == null) {
                throw new Exception("Data absensi tidak ditemukan");
            }
            renderAttendanceSuccess(attendance, true);
        } catch (Exception error) {
            notifyUser("Absensi tersimpan, tetapi detail gagal dimuat: " + error.getMessage());
        }
    }

    private void renderAttendanceSuccess(JSONObject attendance, boolean useCapturedPhoto) {
        currentStep = 3;
        if (attendanceContent != null) {
            attendanceContent.setPadding(dp(18), dp(14), dp(18), dp(88));
        }
        photoCard.setVisibility(View.GONE);
        photoRingContainer.setVisibility(View.GONE);
        faceCard.setVisibility(View.GONE);
        locationCard.setVisibility(View.GONE);
        gpsView.setVisibility(View.GONE);
        reviewView.setVisibility(View.GONE);
        actions.setVisibility(View.GONE);
        successView.setVisibility(View.GONE);
        successPanel.setVisibility(View.VISIBLE);
        animateStepIn(successPanel);
        shiftSelector.setVisibility(View.GONE);

        String clockOut = attendance.optString("clock_out", "");
        boolean hasClockOut = clockOut.trim().length() > 0;
        selectedShiftId = attendance.optString("shift_id", selectedShiftId);
        String shiftLabel = attendance.optString(
            "shift_label",
            "2".equals(selectedShiftId) ? "Shift 2" : "Shift 1"
        );
        String clockIn = attendance.optString(
            "clock_in",
            attendance.optString("jam", "-")
        );
        String attendanceDate = attendance.optString("tanggal", "-");
        String shiftStart = attendance.optString("shift_start", "-");
        String shiftRange = attendance.optString("shift_range", "");
        String attendanceAddress = attendance.optString("address", address);
        String latitudeText = attendance.optString("latitude", "");
        String longitudeText = attendance.optString("longitude", "");
        String successLabel = hasClockOut ? "CLOCK OUT BERHASIL" : "CLOCK IN BERHASIL";
        if (photoFacePrompt != null) {
            photoFacePrompt.setText(successLabel);
            photoFacePrompt.setTextSize(23);
            photoFacePrompt.setTextColor(COLOR_SUCCESS);
            photoFacePrompt.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
            photoFacePrompt.setPadding(0, dp(14), 0, dp(10));
            photoFacePrompt.setVisibility(View.VISIBLE);
        }

        cacheAttendanceState(attendance);
        successTitle.setText(successLabel);
        successTitle.setVisibility(View.GONE);
        successPhotoExpanded = false;
        applySuccessPhotoSize(false);
        successDate.setText(valueOrDash(attendanceDate));
        successShift.setText(
            shiftRange.length() > 0
                ? shiftLabel + " | " + shiftRange
                : shiftLabel
        );
        successClockIn.setText(clockIn);
        successClockOut.setText(hasClockOut ? clockOut : "-");
        successStatus.setText(attendanceStatus(clockIn, shiftStart));
        int displayedScore = hasClockOut
            ? attendance.optInt("clock_out_face_score", attendance.optInt("face_score", faceScore))
            : attendance.optInt("face_score", faceScore);
        successFaceScore.setText(displayedScore > 0 ? displayedScore + "%" : "-");
        successCutoff.setText(valueOrDash(shiftStart));
        successAddress.setText(valueOrDash(attendanceAddress));
        successCoordinate.setText(
            latitudeText.length() > 0 && longitudeText.length() > 0
                ? latitudeText + ", " + longitudeText
                : "-"
        );

        if (useCapturedPhoto && selfieBitmap != null) {
            successPhoto.setImageBitmap(selfieBitmap);
        } else {
            String localPhotoPath = attendance.optString("local_photo_path", "");
            if (localPhotoPath.length() > 0) {
                Bitmap localPhoto = BitmapFactory.decodeFile(localPhotoPath);
                if (localPhoto != null) {
                    successPhoto.setImageBitmap(localPhoto);
                }
            }
            String photoUrl = hasClockOut
                ? attendance.optString("clock_out_photo_url", "")
                : attendance.optString("photo_url", "");
            if (localPhotoPath.length() == 0 && photoUrl.length() == 0) {
                photoUrl = attendance.optString("photo_url", "");
            }
            if (localPhotoPath.length() == 0) {
                loadRemoteImage(successPhoto, absoluteUrl(photoUrl));
            }
        }

        clockOutButton.setVisibility(hasClockOut ? View.GONE : View.VISIBLE);
        statusText.setText(
            offlineMode
                ? "Data tersimpan lokal dan menunggu sinkronisasi."
                : "Data berhasil dikirim."
        );
        refreshOfflineSyncStatus(false);
        updateSteps();
    }

    private void cacheAttendanceState(JSONObject attendance) {
        try {
            String attendanceDate = normalizeAttendanceDate(
                attendance.optString("date_key", attendance.optString("tanggal", ""))
            );
            if (attendanceDate.length() == 0) {
                attendanceDate = todayDate();
            }
            if (!todayDate().equals(attendanceDate)) {
                return;
            }
            JSONObject cached = getTodayCachedAttendance(username);
            JSONObject merged = cached == null ? new JSONObject() : new JSONObject(cached.toString());
            copyIfPresent(merged, attendance, "username");
            copyIfPresent(merged, attendance, "fullname");
            copyIfPresent(merged, attendance, "tanggal");
            copyIfPresent(merged, attendance, "clock_in");
            if (!merged.has("clock_in") || merged.optString("clock_in", "").length() == 0) {
                copyIfPresentAs(merged, attendance, "jam", "clock_in");
            }
            copyIfPresent(merged, attendance, "clock_out");
            copyIfPresent(merged, attendance, "shift_id");
            copyIfPresent(merged, attendance, "shift_label");
            copyIfPresentAs(merged, attendance, "shift", "shift_label");
            copyIfPresent(merged, attendance, "shift_start");
            copyIfPresent(merged, attendance, "shift_range");
            copyIfPresent(merged, attendance, "address");
            copyIfPresentAs(merged, attendance, "lokasi", "address");
            copyIfPresent(merged, attendance, "latitude");
            copyIfPresent(merged, attendance, "longitude");
            String coordinates = attendance.optString("koordinat", "");
            if (
                coordinates.contains(",") &&
                (!merged.has("latitude") || !merged.has("longitude"))
            ) {
                String[] coordinateParts = coordinates.split(",", 2);
                merged.put("latitude", coordinateParts[0].trim());
                merged.put("longitude", coordinateParts[1].trim());
            }
            copyIfPresent(merged, attendance, "photo_url");
            copyIfPresent(merged, attendance, "clock_out_photo_url");
            copyIfPresent(merged, attendance, "local_photo_path");
            copyIfPresent(merged, attendance, "clock_out_local_photo_path");
            copyIfPresent(merged, attendance, "face_score");
            copyIfPresent(merged, attendance, "clock_out_face_score");
            copyIfPresent(merged, attendance, "device_info");
            copyIfPresent(merged, attendance, "clock_out_device_info");
            copyIfPresent(merged, attendance, "ip_address");
            copyIfPresent(merged, attendance, "clock_out_ip_address");
            copyIfPresent(merged, attendance, "app_version");
            merged.put("username", valueOrDash(merged.optString("username", username)));
            merged.put("fullname", valueOrDash(merged.optString("fullname", fullname)));
            merged.put("tanggal", attendanceDate);
            if (!merged.has("clock_in") || merged.optString("clock_in", "").length() == 0) {
                merged.put("clock_in", attendance.optString("jam", ""));
            }
            getSharedPreferences("attendance_app", MODE_PRIVATE)
                .edit()
                .putString("cached_attendance_username", username)
                .putString("cached_attendance_date", attendanceDate)
                .putString("cached_attendance_state", merged.toString())
                .apply();
            updateCachedLoginAttendance(merged);
        } catch (Exception ignored) {
        }
    }

    private void copyIfPresent(JSONObject target, JSONObject source, String key) throws Exception {
        if (source.has(key) && !source.isNull(key)) {
            String value = source.optString(key, "");
            if (value.length() > 0 && !"-".equals(value)) {
                target.put(key, value);
            }
        }
    }

    private void copyIfPresentAs(JSONObject target, JSONObject source, String sourceKey, String targetKey) throws Exception {
        if (source.has(sourceKey) && !source.isNull(sourceKey)) {
            String value = source.optString(sourceKey, "");
            if (value.length() > 0 && !"-".equals(value)) {
                target.put(targetKey, value);
            }
        }
    }

    private void replaceAttendanceStateFromServer(JSONObject attendance) {
        try {
            if (attendance != null) {
                selectedShiftId = attendance.optString("shift_id", selectedShiftId);
            }
            String statusClass = attendance == null
                ? "absent"
                : attendance.optString("status_class", "");
            String clockIn = attendance == null
                ? ""
                : attendance.optString("jam", attendance.optString("clock_in", "")).trim();
            boolean hasClockIn =
                "present".equals(statusClass) ||
                "late".equals(statusClass) ||
                (!clockIn.isEmpty() && !"-".equals(clockIn));
            if (!hasClockIn || "leave".equals(statusClass)) {
                clearTodayCachedAttendance(username);
                mode = "clock_in";
                return;
            }

            JSONObject fresh = new JSONObject();
            String attendanceDate = normalizeAttendanceDate(
                attendance.optString("date_key", attendance.optString("tanggal", ""))
            );
            if (attendanceDate.isEmpty()) {
                attendanceDate = todayDate();
            }
            fresh.put("username", attendance.optString("username", username));
            fresh.put("fullname", attendance.optString("fullname", fullname));
            fresh.put("tanggal", attendanceDate);
            fresh.put("clock_in", clockIn);
            String clockOut = attendance.optString("clock_out", "").trim();
            fresh.put("clock_out", "-".equals(clockOut) ? "" : clockOut);
            copyIfPresent(fresh, attendance, "shift_id");
            copyIfPresent(fresh, attendance, "shift_label");
            copyIfPresentAs(fresh, attendance, "shift", "shift_label");
            copyIfPresent(fresh, attendance, "shift_start");
            copyIfPresent(fresh, attendance, "shift_range");
            copyIfPresent(fresh, attendance, "address");
            copyIfPresentAs(fresh, attendance, "lokasi", "address");
            copyIfPresent(fresh, attendance, "latitude");
            copyIfPresent(fresh, attendance, "longitude");
            String coordinates = attendance.optString("koordinat", "");
            if (coordinates.contains(",")) {
                String[] coordinateParts = coordinates.split(",", 2);
                fresh.put("latitude", coordinateParts[0].trim());
                fresh.put("longitude", coordinateParts[1].trim());
            }
            String[] exactFields = {
                "photo_url", "clock_out_photo_url", "face_score",
                "clock_out_face_score", "device_info", "clock_out_device_info",
                "ip_address", "clock_out_ip_address", "app_version"
            };
            for (String key : exactFields) {
                copyIfPresent(fresh, attendance, key);
            }
            getSharedPreferences("attendance_app", MODE_PRIVATE)
                .edit()
                .putString("cached_attendance_username", username)
                .putString("cached_attendance_date", attendanceDate)
                .putString("cached_attendance_state", fresh.toString())
                .apply();
            updateCachedLoginAttendance(fresh);
            mode = hasCachedClockOut(fresh) ? "clock_in" : "clock_out";
        } catch (Exception ignored) {
        }
    }

    private void updateCachedLoginAttendance(JSONObject attendance) {
        try {
            SharedPreferences prefs = getSharedPreferences("attendance_app", MODE_PRIVATE);
            String cachedLogin = prefs.getString("cached_login", "");
            if (cachedLogin.length() == 0) {
                return;
            }
            JSONObject data = new JSONObject(cachedLogin);
            data.put(
                "mode",
                hasCachedClockOut(attendance) ? "clock_in" : "clock_out"
            );
            data.put("active_attendance", new JSONObject(attendance.toString()));
            prefs.edit()
                .putString("cached_login", data.toString())
                .apply();
        } catch (Exception ignored) {
        }
    }

    private void clearTodayCachedAttendance(String owner) {
        try {
            SharedPreferences prefs = getSharedPreferences("attendance_app", MODE_PRIVATE);
            String cachedOwner = prefs.getString("cached_attendance_username", "");
            if (owner != null && cachedOwner.equalsIgnoreCase(owner)) {
                prefs.edit()
                    .remove("cached_attendance_username")
                    .remove("cached_attendance_date")
                    .remove("cached_attendance_state")
                    .apply();
            }

            String cachedLogin = prefs.getString("cached_login", "");
            if (cachedLogin.length() > 0) {
                JSONObject data = new JSONObject(cachedLogin);
                if (owner == null || data.optString("username", "").equalsIgnoreCase(owner)) {
                    data.remove("active_attendance");
                    data.put("mode", "clock_in");
                    prefs.edit()
                        .putString("cached_login", data.toString())
                        .apply();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private JSONObject getTodayCachedAttendance(String owner) {
        try {
            SharedPreferences prefs = getSharedPreferences("attendance_app", MODE_PRIVATE);
            String cachedOwner = prefs.getString("cached_attendance_username", "");
            String cachedDate = prefs.getString("cached_attendance_date", "");
            String cachedText = prefs.getString("cached_attendance_state", "");
            if (
                owner == null ||
                !cachedOwner.equalsIgnoreCase(owner) ||
                !todayDate().equals(cachedDate) ||
                cachedText.length() == 0
            ) {
                return null;
            }
            return new JSONObject(cachedText);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean hasCachedClockOut(JSONObject attendance) {
        String clockOut = attendance == null ? "" : attendance.optString("clock_out", "");
        return clockOut != null && clockOut.trim().length() > 0 && !"-".equals(clockOut.trim());
    }

    private String todayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    private String normalizeAttendanceDate(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.trim();
        if (clean.length() >= 10 && clean.charAt(4) == '-' && clean.charAt(7) == '-') {
            return clean.substring(0, 10);
        }
        if (clean.length() >= 10 && clean.charAt(2) == '/' && clean.charAt(5) == '/') {
            return clean.substring(6, 10) + "-" + clean.substring(3, 5) + "-" + clean.substring(0, 2);
        }
        return "";
    }

    private void startClockOutFlow() {
        mode = "clock_out";
        selfieBitmap = null;
        faceValidated = false;
        faceScore = 0;
        gpsLocked = false;
        address = "";
        locationSummary = "";
        latitude = 0;
        longitude = 0;
        locationAccuracyMeters = Float.MAX_VALUE;
        distanceFromAttendanceCenter = Float.MAX_VALUE;
        preview.setImageBitmap(null);
        readyBadge.setText("GPS");
        resetPhotoStep();
        requestGps();
    }

    private void updateShiftSelector() {
        if (shiftSelector == null) {
            return;
        }
        shiftSelector.setVisibility(View.GONE);
        boolean secondSelected = "2".equals(selectedShiftId);
        shiftOneButton.setTextColor(secondSelected ? COLOR_ACCENT : 0xFFFFFFFF);
        shiftOneButton.setBackground(round(
            secondSelected ? 0xFFFFFFFF : COLOR_ACCENT_SOFT,
            dp(14),
            secondSelected ? COLOR_ACCENT_STROKE : 0,
            secondSelected ? 1 : 0
        ));
        shiftTwoButton.setTextColor(secondSelected ? 0xFFFFFFFF : COLOR_ACCENT);
        shiftTwoButton.setBackground(round(
            secondSelected ? COLOR_ACCENT_SOFT : 0xFFFFFFFF,
            dp(14),
            secondSelected ? 0 : COLOR_ACCENT_STROKE,
            secondSelected ? 0 : 1
        ));
    }

    private String attendanceStatus(String clockIn, String cutoff) {
        if (clockIn == null || cutoff == null || clockIn.length() < 5 || cutoff.length() < 5) {
            return "-";
        }
        String clockInTime = clockIn.substring(0, 5);
        String cutoffTime = cutoff.substring(0, 5);
        return clockInTime.compareTo(cutoffTime) <= 0 ? "On Time" : "Late";
    }

    private void updateSteps() {
        if (stepsView != null) {
            stepsView.setVisibility(View.GONE);
        }
        setStepActive(stepOne, currentStep >= 0);
        setStepActive(stepTwo, currentStep >= 1);
        setStepActive(stepThree, currentStep >= 2);
        updateShiftSelector();
    }

    private void setStepActive(TextView step, boolean active) {
        step.setTextColor(active ? COLOR_ACCENT : 0xFF777777);
        step.setBackground(round(0xFFFFFFFF, dp(17), active ? COLOR_ACCENT_SOFT : 0xFFD8D8D8, 2));
    }

    private boolean isClockOut() {
        return "clock_out".equals(mode);
    }

    private void updateClock() {
        if (homeGreeting != null) {
            homeGreeting.setText(greetingText());
        }
        if (dateDay == null || dateFull == null || dateTime == null) {
            return;
        }
        Date now = new Date();
        Locale locale = new Locale("id", "ID");
        dateDay.setText(new SimpleDateFormat("EEEE", locale).format(now));
        dateFull.setText(new SimpleDateFormat("dd MMM yyyy", locale).format(now));
        dateTime.setText(new SimpleDateFormat("HH:mm:ss", locale).format(now));
    }

    private String greetingText() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour <= 10) {
            return "Selamat pagi,";
        }
        if (hour <= 17) {
            return "Selamat siang,";
        }
        return "Selamat malam,";
    }

    private EditText input(String hint, boolean password) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(COLOR_SUBTLE);
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setPadding(dp(14), 0, dp(14), 0);
        input.setBackground(round(0xFFFFFFFF, dp(12), COLOR_STROKE, 1));
        input.setInputType(password
            ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD
            : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        return input;
    }

    private LinearLayout.LayoutParams inputParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.setMargins(0, dp(10), 0, 0);
        return params;
    }

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        requestPermissions(
            new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            },
            PERMISSION_REQUEST
        );
    }

    private void openCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestRuntimePermissions();
            return;
        }
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST);
    }

    private void openHistory() {
        if (baseUrl == null || baseUrl.length() == 0) {
            notifyUser("Riwayat belum siap. Buka dari link absensi dulu.");
            return;
        }

        currentSection = "Riwayat";
        stopNotificationPolling();
        showingHistory = true;
        setSystemBarStyle(false);
        FrameLayout historyRoot = new FrameLayout(this);
        historyRoot.setBackgroundColor(0xFFFFFFFF);

        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(4), dp(18), dp(84));
        historyRoot.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        shell.addView(top, new LinearLayout.LayoutParams(-1, dp(62)));
        top.addView(new View(this), new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView historyTitle = text("Riwayat Absen", 16, COLOR_TEXT, true);
        historyTitle.setGravity(Gravity.CENTER);
        top.addView(historyTitle, new LinearLayout.LayoutParams(0, dp(42), 1));
        FrameLayout filter = new FrameLayout(this);
        filter.addView(icon(R.drawable.ic_filter, COLOR_TEXT), new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER));
        top.addView(filter, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout tabs = horizontal();
        tabs.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tabBarParams = new LinearLayout.LayoutParams(-1, dp(38));
        tabBarParams.setMargins(0, dp(4), 0, dp(16));
        shell.addView(tabs, tabBarParams);
        activeHistoryFilter = "all";
        historyAllTab = historyTab("Semua", true);
        historyOnTimeTab = historyTab("On Time", false);
        historyLateTab = historyTab("Late", false);
        historyAbsentTab = historyTab("Tidak Hadir", false);
        TextView[] filterTabs = {historyAllTab, historyOnTimeTab, historyLateTab, historyAbsentTab};
        for (int index = 0; index < filterTabs.length; index += 1) {
            LinearLayout.LayoutParams filterParams = new LinearLayout.LayoutParams(0, -1, 1);
            filterParams.setMargins(index == 0 ? 0 : dp(3), 0, index == filterTabs.length - 1 ? 0 : dp(3), 0);
            tabs.addView(filterTabs[index], filterParams);
        }
        historyAllTab.setOnClickListener(v -> applyHistoryFilter("all"));
        historyOnTimeTab.setOnClickListener(v -> applyHistoryFilter("present"));
        historyLateTab.setOnClickListener(v -> applyHistoryFilter("late"));
        historyAbsentTab.setOnClickListener(v -> applyHistoryFilter("absent"));

        ProgressBar loader = new ProgressBar(this);
        LinearLayout.LayoutParams loaderParams = new LinearLayout.LayoutParams(dp(42), dp(42));
        loaderParams.gravity = Gravity.CENTER_HORIZONTAL;
        loaderParams.setMargins(0, dp(24), 0, 0);
        shell.addView(loader, loaderParams);
        TextView loading = text("Memuat riwayat...", 12, COLOR_MUTED, false);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(10), 0, 0);
        shell.addView(loading, matchWrap());

        addFixedBottomNav(historyRoot, "Riwayat");
        setContentView(historyRoot);
        loadNativeHistory(shell, loader, loading);
    }

    private void openHistoryLegacy() {
        if (baseUrl == null || baseUrl.length() == 0) {
            notifyUser("Riwayat belum siap. Buka dari link absensi dulu.");
            return;
        }

        showingHistory = true;
        FrameLayout historyRoot = new FrameLayout(this);
        historyRoot.setBackgroundColor(COLOR_APP_BG);
        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(18), dp(18), dp(12));
        historyRoot.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        shell.addView(top, matchWrap());

        Button back = button("<", 0x00FFFFFF, COLOR_TEXT, 0);
        back.setTextSize(26);
        top.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));
        back.setOnClickListener(v -> onBackPressed());

        TextView historyTitle = text("Riwayat Absen", 16, COLOR_TEXT, true);
        historyTitle.setGravity(Gravity.CENTER);
        top.addView(historyTitle, new LinearLayout.LayoutParams(0, dp(44), 1));

        TextView filter = text("F", 16, COLOR_TEXT, true);
        filter.setGravity(Gravity.CENTER);
        top.addView(filter, new LinearLayout.LayoutParams(dp(44), dp(44)));

        ProgressBar loader = new ProgressBar(this);
        LinearLayout.LayoutParams loaderParams = new LinearLayout.LayoutParams(dp(48), dp(48));
        loaderParams.gravity = Gravity.CENTER_HORIZONTAL;
        loaderParams.setMargins(0, dp(26), 0, 0);
        shell.addView(loader, loaderParams);

        TextView loading = text("Memuat riwayat...", 13, COLOR_SUBTLE, false);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(12), 0, 0);
        shell.addView(loading, matchWrap());

        setContentView(historyRoot);
        loadNativeHistory(shell, loader, loading);
    }

    private void openHomePage() {
        if (mainRoot == null) {
            return;
        }
        currentSection = "Beranda";
        showingHistory = false;
        setSystemBarStyle(true);

        FrameLayout pageRoot = new FrameLayout(this);
        pageRoot.setBackgroundColor(0xFFFFFFFF);

        FrameLayout redHeader = new FrameLayout(this);
        redHeader.setBackground(redGradient(0));
        pageRoot.addView(redHeader, new FrameLayout.LayoutParams(-1, dp(184), Gravity.TOP));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(20), dp(18), 0);
        redHeader.addView(header, new FrameLayout.LayoutParams(-1, dp(112), Gravity.TOP));

        LinearLayout intro = vertical();
        header.addView(intro, new LinearLayout.LayoutParams(0, -2, 1));
        homeGreeting = text(greetingText(), 14, 0xFFFFFFFF, false);
        intro.addView(homeGreeting);
        TextView homeName = text(displayName(), 23, 0xFFFFFFFF, true);
        homeNameView = homeName;
        homeName.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        homeName.setPadding(0, dp(4), 0, dp(2));
        intro.addView(homeName);
        homeRoleView = text(displayRole(), 14, 0xFFFFFFFF, false);
        intro.addView(homeRoleView);

        FrameLayout bell = new FrameLayout(this);
        homeBellIcon = icon(R.drawable.ic_bell, 0xFFFFFFFF);
        bell.addView(homeBellIcon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        homeNotificationBadge = text("", 9, COLOR_ACCENT, true);
        homeNotificationBadge.setGravity(Gravity.CENTER);
        homeNotificationBadge.setBackground(round(0xFFFFFFFF, dp(11), 0, 0));
        FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.TOP | Gravity.RIGHT);
        badgeParams.setMargins(0, 0, dp(1), 0);
        bell.addView(homeNotificationBadge, badgeParams);
        homeNotificationBadge.setVisibility(View.GONE);
        bell.setContentDescription("Buka notifikasi");
        bell.setClickable(true);
        bell.setOnClickListener(v -> showNotificationOverlay(bell));
        header.addView(bell, new LinearLayout.LayoutParams(dp(46), dp(46)));

        HomeWaveView wave = new HomeWaveView(this);
        redHeader.addView(wave, new FrameLayout.LayoutParams(-1, dp(72), Gravity.BOTTOM));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        pageRoot.addView(scroll, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(164), dp(18), dp(92));
        scroll.addView(shell, new ScrollView.LayoutParams(-1, -2));

        TextView pullRefreshStatus = text("Tarik ke bawah untuk refresh", 12, COLOR_ACCENT, true);
        pullRefreshStatus.setGravity(Gravity.CENTER);
        pullRefreshStatus.setPadding(dp(16), dp(8), dp(16), dp(8));
        pullRefreshStatus.setBackground(round(0xFFFFFFFF, dp(18), 0x22FFFFFF, 1));
        pullRefreshStatus.setElevation(dp(4));
        pullRefreshStatus.setVisibility(View.GONE);
        FrameLayout.LayoutParams refreshStatusParams = new FrameLayout.LayoutParams(
            -2,
            dp(38),
            Gravity.TOP | Gravity.CENTER_HORIZONTAL
        );
        refreshStatusParams.setMargins(0, dp(126), 0, 0);
        pageRoot.addView(pullRefreshStatus, refreshStatusParams);
        attachHomePullToRefresh(scroll, pullRefreshStatus);

        LinearLayout statusCard = vertical();
        statusCard.setPadding(dp(18), dp(17), dp(18), dp(18));
        statusCard.setBackground(round(0xFFFFFFFF, dp(16), 0xFFF0F1F3, 1));
        setSoftElevation(statusCard, 1);
        shell.addView(statusCard, matchWrap());

        LinearLayout statusTop = horizontal();
        statusTop.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.addView(statusTop, matchWrap());
        LinearLayout statusCopy = vertical();
        statusTop.addView(statusCopy, new LinearLayout.LayoutParams(0, -2, 1));
        statusCopy.addView(text("Status Hari Ini", 13, COLOR_TEXT, false));
        homeStatusValue = text(initialHomeStatusLabel(), 25, COLOR_ACCENT, true);
        homeStatusValue.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        homeStatusValue.setPadding(0, dp(4), 0, dp(4));
        statusCopy.addView(homeStatusValue);
        homeStatusDate = text(new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID")).format(new Date()), 13, COLOR_MUTED, false);
        statusCopy.addView(homeStatusDate);

        FrameLayout clockWrap = new FrameLayout(this);
        homeConnectionIndicator = clockWrap;
        homeConnectionIcon = icon(R.drawable.ic_clock, COLOR_SUCCESS);
        clockWrap.addView(homeConnectionIcon, new FrameLayout.LayoutParams(dp(29), dp(29), Gravity.CENTER));
        clockWrap.setClickable(true);
        clockWrap.setOnClickListener(v -> checkServerConnectionAndSync(true));
        statusTop.addView(clockWrap, new LinearLayout.LayoutParams(dp(58), dp(58)));
        updateHomeConnectionIndicator(!offlineMode);

        FrameLayout action = homePrimaryAction(isClockOut() ? "Absen Pulang" : "Absen Masuk");
        homeActionButton = action;
        homeActionLabel = (TextView) action.getChildAt(0);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(52));
        actionParams.setMargins(0, dp(19), 0, 0);
        statusCard.addView(action, actionParams);
        action.setOnClickListener(v -> onHomeAttendanceAction());

        TextView infoTitle = text("Informasi Absen", 14, COLOR_TEXT, true);
        infoTitle.setPadding(dp(2), dp(26), 0, dp(10));
        shell.addView(infoTitle, matchWrap());

        LinearLayout infoCard = vertical();
        infoCard.setBackground(round(0xFFFFFFFF, dp(14), 0xFFF0F1F3, 1));
        setSoftElevation(infoCard, 1);
        shell.addView(infoCard, matchWrap());
        LinearLayout locationInfoRow = homeInfoRow("Lokasi Tercatat", "Memuat data...", "GPS", COLOR_SUCCESS, COLOR_SOFT_GREEN, R.drawable.ic_location);
        homeLocationValue = (TextView) ((LinearLayout) locationInfoRow.getChildAt(1)).getChildAt(1);
        homeLocationBadge = (TextView) locationInfoRow.getChildAt(2);
        infoCard.addView(locationInfoRow);
        infoCard.addView(thinDivider(dp(58)));
        LinearLayout faceInfoRow = homeInfoRow("Face Score", "Memuat data...", "-", 0xFF168CD5, 0xFFE5F4FD, R.drawable.ic_face);
        homeFaceValue = (TextView) ((LinearLayout) faceInfoRow.getChildAt(1)).getChildAt(1);
        homeFaceBadge = (TextView) faceInfoRow.getChildAt(2);
        infoCard.addView(faceInfoRow);
        infoCard.addView(thinDivider(dp(58)));
        LinearLayout scheduleInfoRow = homeInfoRow("Jadwal Kerja", "Memuat data...", "-", 0xFFF97316, 0xFFFFEEE1, R.drawable.ic_clock);
        homeScheduleValue = (TextView) ((LinearLayout) scheduleInfoRow.getChildAt(1)).getChildAt(1);
        homeScheduleBadge = (TextView) scheduleInfoRow.getChildAt(2);
        infoCard.addView(scheduleInfoRow);

        TextView summaryTitle = text("Ringkasan Bulan Ini", 14, COLOR_TEXT, true);
        summaryTitle.setPadding(dp(2), dp(24), 0, dp(10));
        shell.addView(summaryTitle, matchWrap());

        LinearLayout summary = horizontal();
        summary.setGravity(Gravity.CENTER);
        summary.setPadding(dp(10), dp(10), dp(10), dp(10));
        summary.setBackground(round(0xFFFFFFFF, dp(14), 0xFFF0F1F3, 1));
        setSoftElevation(summary, 1);
        shell.addView(summary, new LinearLayout.LayoutParams(-1, dp(138)));
        LinearLayout onTimeItem = homeSummaryItem("-", "On Time", COLOR_SUCCESS, R.drawable.ic_calendar);
        homeOnTimeCount = (TextView) onTimeItem.getChildAt(1);
        summary.addView(onTimeItem, summaryItemParams());
        LinearLayout lateItem = homeSummaryItem("-", "Late", 0xFFF97316, R.drawable.ic_clock);
        homeLateCount = (TextView) lateItem.getChildAt(1);
        summary.addView(lateItem, summaryItemParams());
        LinearLayout absentItem = homeSummaryItem("-", "Tidak Hadir", COLOR_ERROR, R.drawable.ic_close_circle);
        homeAbsentCount = (TextView) absentItem.getChildAt(1);
        summary.addView(absentItem, summaryItemParams());
        LinearLayout rateItem = homeSummaryItem("-", "Kehadiran", 0xFF2563EB, R.drawable.ic_percent);
        homeAttendanceRate = (TextView) rateItem.getChildAt(1);
        summary.addView(rateItem, summaryItemParams());

        FrameLayout bellTouchTarget = new FrameLayout(this);
        bellTouchTarget.setContentDescription("Buka notifikasi");
        bellTouchTarget.setClickable(true);
        bellTouchTarget.setOnClickListener(v -> showNotificationOverlay(bell));
        FrameLayout.LayoutParams bellTouchParams = new FrameLayout.LayoutParams(dp(64), dp(78), Gravity.TOP | Gravity.RIGHT);
        bellTouchParams.setMargins(0, dp(8), dp(4), 0);
        pageRoot.addView(bellTouchTarget, bellTouchParams);

        addFixedBottomNav(pageRoot, "Beranda");
        setContentView(pageRoot);
        try {
            JSONObject cachedHistory = mergePendingHistory(readCachedHomeHistory());
            applyHomeOverview(cachedHistory, preferredHomeAttendance(cachedHistory));
        } catch (Exception ignored) {
        }
        loadHomeOverview();
        loadUnreadMessageCount();
        startNotificationPolling();
        checkServerConnectionAndSync(false);
    }

    private void attachHomePullToRefresh(ScrollView scroll, TextView statusView) {
        scroll.setOverScrollMode(View.OVER_SCROLL_ALWAYS);
        scroll.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                homePullStartY = scroll.getScrollY() <= 0 ? event.getY() : -1f;
            } else if (action == MotionEvent.ACTION_MOVE) {
                if (scroll.getScrollY() > 0) {
                    homePullStartY = -1f;
                    if (!homeRefreshing) {
                        statusView.setVisibility(View.GONE);
                    }
                } else if (homePullStartY >= 0f && !homeRefreshing) {
                    float pullDistance = event.getY() - homePullStartY;
                    if (pullDistance > dp(12)) {
                        statusView.setVisibility(View.VISIBLE);
                        statusView.setText(
                            pullDistance >= dp(86)
                                ? "Lepaskan untuk refresh"
                                : "Tarik ke bawah untuk refresh"
                        );
                    }
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                float pullDistance = homePullStartY < 0f ? 0f : event.getY() - homePullStartY;
                homePullStartY = -1f;
                if (action == MotionEvent.ACTION_UP && scroll.getScrollY() <= 0 && pullDistance >= dp(86)) {
                    refreshHomeFromPull(statusView);
                } else if (!homeRefreshing) {
                    statusView.setVisibility(View.GONE);
                }
            }
            return false;
        });
    }

    private void refreshHomeFromPull(TextView statusView) {
        if (homeRefreshing) {
            return;
        }
        homeRefreshing = true;
        statusView.setVisibility(View.VISIBLE);
        statusView.setText("Memperbarui data...");
        loadAppProfile(false);
        loadUnreadMessageCount();
        checkServerConnectionAndSync(false);
        loadHomeOverview(() -> {
            homeRefreshing = false;
            statusView.setText("Data sudah diperbarui");
            handler.postDelayed(() -> statusView.setVisibility(View.GONE), 850);
        });
    }

    private void loadHomeOverview() {
        loadHomeOverview(null);
    }

    private void loadHomeOverview(Runnable completion) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            showHomeOverviewError();
            if (completion != null) {
                completion.run();
            }
            return;
        }
        final String overviewUrl = baseUrl + "/api/attendance_history?month="
            + new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date())
            + "&_ts=" + System.currentTimeMillis();
        new Thread(() -> {
            try {
                OfflineAttendanceQueue queue = new OfflineAttendanceQueue(this);
                if (queue.pendingCount() > 0) {
                    OfflineSyncJobService.syncPendingNow(this);
                }
                JSONObject response = new JSONObject(requireJsonObject(httpGet(overviewUrl), "/api/attendance_history"));
                if (!response.optBoolean("success", false)) {
                    throw new Exception(response.optString("error", "Gagal memuat status"));
                }
                JSONObject history = response.getJSONObject("history");
                queue.removeEventsAlreadyOnServer(username, history.optJSONArray("rows"));
                final boolean hasPending = queue.latestPendingMode(username).trim().length() > 0;
                JSONObject responseToday = history.optJSONObject("today");
                final JSONObject serverToday = responseToday == null
                    ? new JSONObject()
                    : responseToday;
                if (!hasPending) {
                    replaceAttendanceStateFromServer(serverToday);
                }
                getSharedPreferences("attendance_app", MODE_PRIVATE)
                    .edit()
                    .putString("cached_history_username", username)
                    .putString("cached_history", history.toString())
                    .apply();
                JSONObject mergedHistory = mergePendingHistory(history);
                JSONObject today = hasPending
                    ? preferredHomeAttendance(mergedHistory)
                    : serverToday;
                runOnUiThread(() -> {
                    applyHomeOverview(mergedHistory, today);
                    if (completion != null) {
                        completion.run();
                    }
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> {
                    showHomeOverviewError();
                    if (completion != null) {
                        completion.run();
                    }
                });
            }
        }).start();
    }

    private String initialHomeStatusLabel() {
        try {
            JSONObject cached = getTodayCachedAttendance(username);
            if (cached != null) {
                if (hasCachedClockOut(cached)) {
                    return "Absensi Selesai";
                }
                String clockIn = cached.optString("clock_in", cached.optString("jam", ""));
                if (!clockIn.trim().isEmpty() && !"-".equals(clockIn.trim())) {
                    return "Sudah Absen Masuk";
                }
            }
            String pendingMode = new OfflineAttendanceQueue(this).latestPendingMode(username);
            if ("clock_out".equals(pendingMode)) {
                return "Absensi Selesai";
            }
            if ("clock_in".equals(pendingMode)) {
                return "Sudah Absen Masuk";
            }
        } catch (Exception ignored) {
        }
        return "Belum Absen";
    }

    private void applyHomeOverview(JSONObject history, JSONObject today) {
        if (homeStatusValue == null) {
            return;
        }
        homeOnTimeCount.setText(String.valueOf(history.optInt("on_time_count", 0)));
        homeLateCount.setText(String.valueOf(history.optInt("late_count", 0)));
        homeAbsentCount.setText(String.valueOf(history.optInt("absent_count", 0)));
        homeAttendanceRate.setText(history.optInt("attendance_rate", 0) + "%");

        String statusClass = today.optString("status_class", "absent");
        String clockOut = today.optString("clock_out", "-");
        String clockIn = today.optString("jam", today.optString("clock_in", "-"));
        boolean hasClockIn =
            "present".equals(statusClass) ||
            "late".equals(statusClass) ||
            "pending".equals(statusClass) ||
            (clockIn != null && !clockIn.trim().isEmpty() && !"-".equals(clockIn.trim()));
        boolean hasClockOut = clockOut != null && !clockOut.trim().isEmpty() && !"-".equals(clockOut.trim());

        if ("leave".equals(statusClass)) {
            homeStatusValue.setText(today.optString("status", "Izin"));
            configureHomePrimaryAction("Lihat Riwayat", v -> openHistory());
        } else if (!hasClockIn) {
            mode = "clock_in";
            homeStatusValue.setText("Belum Absen");
            configureHomePrimaryAction("Absen Masuk", v -> onHomeAttendanceAction());
        } else if (!hasClockOut) {
            mode = "clock_out";
            homeStatusValue.setText("Sudah Absen Masuk");
            configureHomePrimaryAction("Absen Pulang", v -> openAttendanceFlowFromHome());
        } else {
            homeStatusValue.setText("Absensi Selesai");
            configureHomePrimaryAction("Lihat Detail", v -> openAttendanceDetail(today));
        }

        String liveDate = today.optString("tanggal", "");
        if (!liveDate.isEmpty()) {
            homeStatusDate.setText(today.optString("hari", "") + ", " + liveDate);
        }
        homeLocationValue.setText(today.optString("lokasi", "-") );
        homeLocationBadge.setText(today.optString("koordinat", "").isEmpty() ? "-" : "GPS");
        int liveFaceScore = today.optInt("face_score", 0);
        homeFaceValue.setText(liveFaceScore > 0 ? liveFaceScore + "%" : "Belum tersedia");
        homeFaceBadge.setText(liveFaceScore >= 90 ? "Sangat Baik" : (liveFaceScore >= 80 ? "Baik" : "-"));
        homeScheduleValue.setText(today.optString("shift_range", "-"));
        homeScheduleBadge.setText(today.optString("shift", "-"));
    }

    private void showHomeOverviewError() {
        if (homeStatusValue == null) {
            return;
        }
        try {
            JSONObject history = readCachedHomeHistory();
            JSONObject mergedHistory = mergePendingHistory(history);
            applyHomeOverview(mergedHistory, preferredHomeAttendance(mergedHistory));
        } catch (Exception ignored) {
            homeStatusValue.setText("Belum Absen");
            homeOnTimeCount.setText("0");
            homeLateCount.setText("0");
            homeAbsentCount.setText("0");
            homeAttendanceRate.setText("0%");
            homeLocationValue.setText("-");
            homeFaceValue.setText("Belum tersedia");
            homeScheduleValue.setText("-");
        }
    }

    private JSONObject readCachedHomeHistory() {
        try {
            SharedPreferences prefs = getSharedPreferences("attendance_app", MODE_PRIVATE);
            String cachedOwner = prefs.getString("cached_history_username", "");
            String cachedText = prefs.getString("cached_history", "");
            if (cachedOwner.equalsIgnoreCase(username) && cachedText.length() > 0) {
                return new JSONObject(cachedText);
            }
        } catch (Exception ignored) {
        }
        JSONObject history = new JSONObject();
        try {
            history.put("on_time_count", 0);
            history.put("late_count", 0);
            history.put("absent_count", 0);
            history.put("attendance_rate", 0);
            history.put("rows", new JSONArray());
            history.put("today", new JSONObject());
        } catch (Exception ignored) {
        }
        return history;
    }

    private JSONObject preferredHomeAttendance(JSONObject history) {
        JSONObject serverOrCachedToday = history == null
            ? null
            : history.optJSONObject("today");
        JSONObject localToday = cachedAttendanceHistoryRow();
        if (
            localToday != null &&
            (hasPendingAttendanceForCurrentUser() || serverOrCachedToday == null || serverOrCachedToday.length() == 0)
        ) {
            return localToday;
        }
        return serverOrCachedToday == null ? new JSONObject() : serverOrCachedToday;
    }

    private boolean hasPendingAttendanceForCurrentUser() {
        try {
            return new OfflineAttendanceQueue(this)
                .latestPendingMode(username)
                .trim()
                .length() > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void updateHomeConnectionIndicator(boolean online) {
        offlineMode = !online;
        if (homeConnectionIndicator == null || homeConnectionIcon == null) {
            return;
        }
        int color = online ? COLOR_SUCCESS : COLOR_ERROR;
        int background = online ? COLOR_SOFT_GREEN : COLOR_SOFT_RED;
        homeConnectionIndicator.setBackground(round(background, dp(30), 0, 0));
        homeConnectionIcon.setColorFilter(color);
        homeConnectionIndicator.setContentDescription(
            online
                ? "Online. Ketuk untuk sinkronisasi manual"
                : "Offline. Ketuk untuk mencoba sinkronisasi"
        );
    }

    private void checkServerConnectionAndSync(boolean showMessage) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            updateHomeConnectionIndicator(false);
            if (showMessage) {
                notifyUser("Server belum tersambung.");
            }
            return;
        }
        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(
                    httpGet(baseUrl + "/api/app_health?_ts=" + System.currentTimeMillis()),
                    "/api/app_health"
                ));
                if (!response.optBoolean("success", false)) {
                    throw new Exception(response.optString("error", "Server tidak merespons"));
                }
                runOnUiThread(() -> {
                    updateHomeConnectionIndicator(true);
                    int pending = 0;
                    try {
                        pending = new OfflineAttendanceQueue(this).pendingCount();
                    } catch (Exception ignored) {
                    }
                    if (pending > 0) {
                        syncOfflineNow(showMessage);
                    } else if (showMessage) {
                        notifyUser("Online. Tidak ada data yang menunggu sinkronisasi.");
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    updateHomeConnectionIndicator(false);
                    if (showMessage) {
                        notifyUser("Offline. Sinkronisasi akan dicoba lagi otomatis.");
                    }
                });
            }
        }).start();
    }

    private void probeServerConnection() {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            updateHomeConnectionIndicator(false);
            return;
        }
        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(
                    httpGet(baseUrl + "/api/app_health?_ts=" + System.currentTimeMillis()),
                    "/api/app_health"
                ));
                boolean online = response.optBoolean("success", false);
                runOnUiThread(() -> updateHomeConnectionIndicator(online));
            } catch (Exception ignored) {
                runOnUiThread(() -> updateHomeConnectionIndicator(false));
            }
        }).start();
    }

    private void loadUnreadMessageCount() {
        if (baseUrl == null || baseUrl.trim().isEmpty() || homeNotificationBadge == null) {
            return;
        }
        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(
                    httpGet(baseUrl + "/api/app_messages?_ts=" + System.currentTimeMillis()),
                    "/api/app_messages"
                ));
                int unread = response.optInt("unread_count", 0);
                JSONArray messages = response.optJSONArray("messages");
                runOnUiThread(() -> {
                    if (homeNotificationBadge != null) {
                        homeNotificationBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
                        homeNotificationBadge.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);
                        updateNotificationBlink(unread > 0);
                    }
                    maybeShowUpdateNotification(messages);
                });
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void maybeShowUpdateNotification(JSONArray messages) {
        if (messages == null || isFinishing() || updateNotificationPopup != null) {
            return;
        }
        for (int index = 0; index < messages.length(); index += 1) {
            JSONObject message = messages.optJSONObject(index);
            if (message == null
                || !message.optBoolean("unread", false)
                || !"update".equalsIgnoreCase(message.optString("type", ""))) {
                continue;
            }
            int messageId = message.optInt("id", 0);
            if (messageId <= 0) {
                return;
            }
            String updateVersion = message.optString("version", "").trim();
            if (!isUpdateNewerThanInstalled(updateVersion)
                || shownUpdateMessageIdThisLogin == messageId) {
                return;
            }
            shownUpdateMessageIdThisLogin = messageId;
            showUpdateNotification(message);
            return;
        }
    }

    private boolean isUpdateNewerThanInstalled(String updateVersion) {
        if (updateVersion == null || updateVersion.trim().isEmpty()) {
            return true;
        }
        String offered = updateVersion.trim().replaceFirst("^[vV]", "");
        String installed = installedVersionName().replaceFirst("^[vV]", "");
        String[] offeredParts = offered.split("[._-]");
        String[] installedParts = installed.split("[._-]");
        int count = Math.max(offeredParts.length, installedParts.length);
        for (int index = 0; index < count; index += 1) {
            int offeredPart = versionNumberPart(offeredParts, index);
            int installedPart = versionNumberPart(installedParts, index);
            if (offeredPart != installedPart) {
                return offeredPart > installedPart;
            }
        }
        return false;
    }

    private String installedVersionName() {
        try {
            String version = getPackageManager()
                .getPackageInfo(getPackageName(), 0)
                .versionName;
            return version == null ? "" : version.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private int versionNumberPart(String[] parts, int index) {
        if (parts == null || index >= parts.length) {
            return 0;
        }
        Matcher matcher = Pattern.compile("^(\\d+)").matcher(parts[index]);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void showUpdateNotification(JSONObject message) {
        FrameLayout shade = new FrameLayout(this);
        shade.setBackgroundColor(0x99000000);
        shade.setPadding(dp(22), dp(22), dp(22), dp(22));

        LinearLayout card = vertical();
        card.setPadding(dp(22), dp(22), dp(22), dp(20));
        card.setBackground(round(0xFFFFFFFF, dp(14), COLOR_STROKE, 1));
        FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
            Math.min(getResources().getDisplayMetrics().widthPixels - dp(44), dp(390)),
            -2,
            Gravity.CENTER
        );
        shade.addView(card, cardParams);

        TextView label = text("UPDATE TERSEDIA", 12, COLOR_ACCENT, true);
        label.setGravity(Gravity.CENTER);
        card.addView(label, matchWrap());

        String version = message.optString("version", "").trim();
        TextView title = text(
            version.isEmpty() ? "Versi terbaru tersedia" : "Versi " + version + " tersedia",
            21,
            COLOR_TEXT,
            true
        );
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(7), 0, 0);
        card.addView(title, matchWrap());

        TextView body = text(
            message.optString("text", "Update aplikasi terbaru sudah tersedia."),
            14,
            COLOR_MUTED,
            false
        );
        body.setGravity(Gravity.CENTER);
        body.setPadding(0, dp(10), 0, dp(4));
        card.addView(body, matchWrap());

        LinearLayout buttons = horizontal();
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams buttonsParams = matchWrap();
        buttonsParams.setMargins(0, dp(18), 0, 0);
        card.addView(buttons, buttonsParams);

        Button ignore = button("Abaikan", 0xFFFFFFFF, COLOR_TEXT, COLOR_STROKE);
        LinearLayout.LayoutParams ignoreParams = new LinearLayout.LayoutParams(0, dp(50), 1);
        ignoreParams.setMargins(0, 0, dp(6), 0);
        buttons.addView(ignore, ignoreParams);

        Button download = button("Download", COLOR_ACCENT, 0xFFFFFFFF, 0);
        LinearLayout.LayoutParams downloadParams = new LinearLayout.LayoutParams(0, dp(50), 1);
        downloadParams.setMargins(dp(6), 0, 0, 0);
        buttons.addView(download, downloadParams);

        PopupWindow popup = new PopupWindow(shade, -1, -1, true);
        updateNotificationPopup = popup;
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(false);
        popup.setOnDismissListener(() -> {
            if (updateNotificationPopup == popup) {
                updateNotificationPopup = null;
            }
        });

        int messageId = message.optInt("id", 0);
        ignore.setOnClickListener(v -> {
            acknowledgeMessage(messageId);
            popup.dismiss();
        });
        download.setOnClickListener(v -> {
            acknowledgeMessage(messageId);
            String actionUrl = message.optString("action_url", "").trim();
            if (actionUrl.isEmpty()) {
                actionUrl = "/attendance_apk";
            }
            popup.dismiss();
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(absoluteUrl(actionUrl))));
            } catch (Exception error) {
                notifyUser("Halaman download belum dapat dibuka.");
            }
        });
        popup.showAtLocation(getWindow().getDecorView(), Gravity.CENTER, 0, 0);
    }

    private void startNotificationPolling() {
        notificationPollingActive = true;
        handler.removeCallbacks(notificationPollRunnable);
        handler.postDelayed(notificationPollRunnable, 30000);
    }

    private void stopNotificationPolling() {
        notificationPollingActive = false;
        handler.removeCallbacks(notificationPollRunnable);
        updateNotificationBlink(false);
    }

    private void updateNotificationBlink(boolean active) {
        if (notificationBlinkAnimator != null) {
            notificationBlinkAnimator.cancel();
            notificationBlinkAnimator = null;
        }
        if (homeBellIcon == null) {
            return;
        }
        homeBellIcon.setAlpha(1f);
        if (!active) {
            return;
        }
        notificationBlinkAnimator = ValueAnimator.ofFloat(1f, 0.3f);
        notificationBlinkAnimator.setDuration(480);
        notificationBlinkAnimator.setRepeatMode(ValueAnimator.REVERSE);
        notificationBlinkAnimator.setRepeatCount(ValueAnimator.INFINITE);
        notificationBlinkAnimator.addUpdateListener(animation -> {
            if (homeBellIcon != null) {
                homeBellIcon.setAlpha((Float) animation.getAnimatedValue());
            }
        });
        notificationBlinkAnimator.start();
    }

    private void showNotificationOverlay(View anchor) {
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            notifyUser("Server notifikasi belum tersambung.");
            return;
        }
        LinearLayout panel = vertical();
        panel.setPadding(dp(14), dp(13), dp(14), dp(13));
        panel.setBackground(round(0xFFFFFFFF, dp(12), COLOR_STROKE, 1));
        setSoftElevation(panel, 8);

        LinearLayout titleRow = horizontal();
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        panel.addView(titleRow, matchWrap());
        titleRow.addView(text("Notifikasi", 16, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView allMessages = text("Lihat semua", 12, COLOR_ACCENT, true);
        allMessages.setPadding(dp(10), dp(6), 0, dp(6));
        titleRow.addView(allMessages);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        LinearLayout list = vertical();
        TextView loading = text("Memuat pesan...", 13, COLOR_MUTED, false);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(28), 0, dp(28));
        list.addView(loading, matchWrap());
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, dp(250));
        scrollParams.setMargins(0, dp(9), 0, 0);
        panel.addView(scroll, scrollParams);

        int popupWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(32), dp(360));
        PopupWindow popup = new PopupWindow(panel, popupWidth, -2, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popup.setOutsideTouchable(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            popup.setElevation(dp(10));
        }
        allMessages.setOnClickListener(v -> {
            popup.dismiss();
            openMessageCenter();
        });
        popup.showAtLocation(anchor, Gravity.TOP | Gravity.RIGHT, dp(16), dp(82));

        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(
                    httpGet(baseUrl + "/api/app_messages?_ts=" + System.currentTimeMillis()),
                    "/api/app_messages"
                ));
                JSONArray messages = response.optJSONArray("messages");
                runOnUiThread(() -> renderNotificationOverlay(
                    list,
                    popup,
                    messages == null ? new JSONArray() : messages
                ));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    list.removeAllViews();
                    TextView failed = text("Notifikasi belum dapat dimuat.", 13, COLOR_MUTED, false);
                    failed.setGravity(Gravity.CENTER);
                    failed.setPadding(0, dp(28), 0, dp(28));
                    list.addView(failed, matchWrap());
                });
            }
        }).start();
    }

    private void renderNotificationOverlay(LinearLayout list, PopupWindow popup, JSONArray messages) {
        list.removeAllViews();
        if (messages.length() == 0) {
            TextView empty = text("Belum ada notifikasi.", 13, COLOR_MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, dp(28));
            list.addView(empty, matchWrap());
            return;
        }
        int shown = Math.min(3, messages.length());
        for (int index = 0; index < shown; index += 1) {
            JSONObject message = messages.optJSONObject(index);
            if (message == null) {
                continue;
            }
            boolean unread = message.optBoolean("unread", false);
            LinearLayout card = vertical();
            card.setPadding(dp(12), dp(11), dp(12), dp(11));
            card.setBackground(round(unread ? 0xFFFFF1F2 : 0xFFF9FAFB, dp(9), COLOR_STROKE, 1));
            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.setMargins(0, 0, 0, dp(8));
            list.addView(card, cardParams);

            LinearLayout top = horizontal();
            top.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(top, matchWrap());
            top.addView(text(message.optString("sender_fullname", "Admin"), 13, COLOR_TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
            top.addView(text(message.optString("created_at", ""), 10, COLOR_MUTED, false));
            String updateVersion = message.optString("version", "").trim();
            if ("update".equals(message.optString("type", "")) && !updateVersion.isEmpty()) {
                TextView version = text("Update versi " + updateVersion, 11, COLOR_ACCENT, true);
                version.setPadding(0, dp(6), 0, 0);
                card.addView(version, matchWrap());
            }
            TextView body = text(message.optString("text", "-"), 12, COLOR_TEXT, false);
            body.setMaxLines(3);
            body.setPadding(0, dp(6), 0, 0);
            card.addView(body, matchWrap());

            String actionUrl = message.optString("action_url", "").trim();
            if (!actionUrl.isEmpty()) {
                TextView action = text(message.optString("action_label", "Buka update"), 12, COLOR_ACCENT, true);
                action.setPadding(0, dp(8), 0, 0);
                card.addView(action, matchWrap());
            }
            card.setOnClickListener(v -> {
                acknowledgeMessage(message.optInt("id", 0));
                popup.dismiss();
                if (!actionUrl.isEmpty()) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(absoluteUrl(actionUrl))));
                }
            });
            attachMessageSwipe(card, message.optInt("id", 0), () -> {
                list.removeView(card);
                if (list.getChildCount() == 0) {
                    popup.dismiss();
                }
            });
        }
    }

    private void openMessageCenter() {
        currentSection = "";
        stopNotificationPolling();
        showingHistory = true;
        setSystemBarStyle(false);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFFFFFFF);
        LinearLayout page = vertical();
        page.setPadding(dp(18), dp(4), dp(18), dp(88));
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));
        FrameLayout back = new FrameLayout(this);
        back.addView(icon(R.drawable.ic_back, COLOR_TEXT), new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        back.setOnClickListener(v -> openHomePage());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        TextView title = text("Notifikasi", 18, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(new View(this), new LinearLayout.LayoutParams(dp(42), dp(48)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout list = vertical();
        TextView loading = text("Memuat pesan...", 13, COLOR_MUTED, false);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(30), 0, 0);
        list.addView(loading, matchWrap());
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        addFixedBottomNav(root, "Beranda");
        setContentView(root);
        loadMessageCenter(list);
    }

    private void loadMessageCenter(LinearLayout list) {
        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(
                    httpGet(baseUrl + "/api/app_messages?_ts=" + System.currentTimeMillis()),
                    "/api/app_messages"
                ));
                if (!response.optBoolean("success", false)) {
                    throw new Exception(response.optString("error", "Pesan gagal dimuat"));
                }
                JSONArray messages = response.optJSONArray("messages");
                runOnUiThread(() -> renderMessageCenter(list, messages == null ? new JSONArray() : messages));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    list.removeAllViews();
                    TextView message = text("Notifikasi belum dapat dimuat.", 13, COLOR_MUTED, false);
                    message.setGravity(Gravity.CENTER);
                    message.setPadding(0, dp(30), 0, 0);
                    list.addView(message, matchWrap());
                });
            }
        }).start();
    }

    private void renderMessageCenter(LinearLayout list, JSONArray messages) {
        list.removeAllViews();
        if (messages.length() == 0) {
            TextView empty = text("Belum ada notifikasi.", 13, COLOR_MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(30), 0, 0);
            list.addView(empty, matchWrap());
            return;
        }
        for (int index = 0; index < messages.length(); index += 1) {
            JSONObject message = messages.optJSONObject(index);
            if (message == null) {
                continue;
            }
            LinearLayout card = vertical();
            card.setPadding(dp(15), dp(14), dp(15), dp(14));
            boolean unread = message.optBoolean("unread", false);
            card.setBackground(round(unread ? 0xFFFFF4F5 : 0xFFFFFFFF, dp(10), COLOR_STROKE, 1));
            LinearLayout.LayoutParams cardParams = matchWrap();
            cardParams.setMargins(0, 0, 0, dp(11));
            list.addView(card, cardParams);

            LinearLayout top = horizontal();
            top.setGravity(Gravity.CENTER_VERTICAL);
            card.addView(top, matchWrap());
            TextView sender = text(message.optString("sender_fullname", "Admin"), 14, COLOR_TEXT, true);
            top.addView(sender, new LinearLayout.LayoutParams(0, -2, 1));
            TextView date = text(message.optString("created_at", ""), 11, COLOR_MUTED, false);
            top.addView(date);
            String updateVersion = message.optString("version", "").trim();
            if ("update".equals(message.optString("type", "")) && !updateVersion.isEmpty()) {
                TextView version = text("Update versi " + updateVersion, 12, COLOR_ACCENT, true);
                version.setPadding(0, dp(7), 0, 0);
                card.addView(version, matchWrap());
            }
            TextView body = text(message.optString("text", "-"), 13, COLOR_TEXT, false);
            body.setPadding(0, dp(8), 0, 0);
            card.addView(body, matchWrap());

            String imagePath = message.optString("image_path", "");
            if (!imagePath.isEmpty()) {
                ImageView image = new ImageView(this);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(-1, dp(170));
                imageParams.setMargins(0, dp(10), 0, 0);
                card.addView(image, imageParams);
                loadRemoteImage(image, absoluteUrl(imagePath));
            }

            String actionUrl = message.optString("action_url", "");
            if (!actionUrl.isEmpty()) {
                Button action = button(message.optString("action_label", "Buka"), COLOR_ACCENT, 0xFFFFFFFF, 0);
                LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(46));
                actionParams.setMargins(0, dp(12), 0, 0);
                card.addView(action, actionParams);
                action.setOnClickListener(v -> {
                    acknowledgeMessage(message.optInt("id", 0));
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(absoluteUrl(actionUrl))));
                });
            }
            card.setOnClickListener(v -> {
                acknowledgeMessage(message.optInt("id", 0));
                card.setBackground(round(0xFFFFFFFF, dp(10), COLOR_STROKE, 1));
            });
            attachMessageSwipe(card, message.optInt("id", 0), () -> {
                list.removeView(card);
                if (list.getChildCount() == 0) {
                    TextView empty = text("Belum ada notifikasi.", 13, COLOR_MUTED, false);
                    empty.setGravity(Gravity.CENTER);
                    empty.setPadding(0, dp(30), 0, 0);
                    list.addView(empty, matchWrap());
                }
            });
        }
    }

    private void attachMessageSwipe(View card, int messageId, Runnable onRemoved) {
        if (messageId <= 0) {
            return;
        }
        final float[] downX = {0f};
        final float[] downY = {0f};
        final boolean[] swiping = {false};
        card.setOnTouchListener((view, event) -> {
            float deltaX = event.getRawX() - downX[0];
            float deltaY = event.getRawY() - downY[0];
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX[0] = event.getRawX();
                    downY[0] = event.getRawY();
                    swiping[0] = false;
                    return false;
                case MotionEvent.ACTION_MOVE:
                    if (!swiping[0] && Math.abs(deltaX) > dp(8) && Math.abs(deltaX) > Math.abs(deltaY)) {
                        swiping[0] = true;
                        ViewParent parent = view.getParent();
                        if (parent != null) {
                            parent.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    if (!swiping[0]) {
                        return false;
                    }
                    view.setTranslationX(deltaX);
                    view.setAlpha(Math.max(0.35f, 1f - Math.abs(deltaX) / Math.max(1f, view.getWidth())));
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (!swiping[0]) {
                        return false;
                    }
                    if (event.getActionMasked() == MotionEvent.ACTION_UP && Math.abs(deltaX) >= dp(72)) {
                        float target = deltaX >= 0 ? view.getWidth() : -view.getWidth();
                        view.animate()
                            .translationX(target)
                            .alpha(0f)
                            .setDuration(180)
                            .withEndAction(() -> {
                                if (onRemoved != null) {
                                    onRemoved.run();
                                }
                                deleteMessage(messageId);
                            })
                            .start();
                    } else {
                        view.animate().translationX(0f).alpha(1f).setDuration(160).start();
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    private void deleteMessage(int messageId) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", messageId);
                JSONObject response = new JSONObject(requireJsonObject(
                    postJson(baseUrl + "/api/app_messages/delete", payload.toString()),
                    "/api/app_messages/delete"
                ));
                if (!response.optBoolean("success", false)) {
                    throw new Exception(response.optString("error", "Pesan gagal dihapus"));
                }
                runOnUiThread(this::loadUnreadMessageCount);
            } catch (Exception error) {
                runOnUiThread(() -> notifyUser("Pesan belum dapat dihapus dari server."));
            }
        }).start();
    }

    private void acknowledgeMessage(int messageId) {
        if (messageId <= 0) {
            return;
        }
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", messageId);
                postJson(baseUrl + "/api/app_messages/ack", payload.toString());
                runOnUiThread(this::loadUnreadMessageCount);
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void openHomePageLegacy() {
        if (mainRoot == null) {
            return;
        }
        showingHistory = false;
        FrameLayout pageRoot = new FrameLayout(this);
        pageRoot.setBackgroundColor(COLOR_PANEL);

        FrameLayout redHeader = new FrameLayout(this);
        redHeader.setBackground(redGradient(0));
        pageRoot.addView(redHeader, new FrameLayout.LayoutParams(-1, dp(152), Gravity.TOP));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(36), dp(18), 0);
        redHeader.addView(header, new FrameLayout.LayoutParams(-1, dp(116), Gravity.TOP));

        LinearLayout intro = vertical();
        header.addView(intro, new LinearLayout.LayoutParams(0, -2, 1));
        intro.addView(text("Selamat pagi,", 15, 0xFFFFFFFF, false));
        TextView homeName = text(valueOrDash(fullname), 28, 0xFFFFFFFF, true);
        homeName.setTypeface(Typeface.DEFAULT_BOLD);
        homeName.setPadding(0, dp(2), 0, dp(1));
        intro.addView(homeName);
        intro.addView(text(displayRole(), 15, 0xFFFFFFFF, false));

        FrameLayout bell = new FrameLayout(this);
        ImageView bellIcon = icon(R.drawable.ic_bell, 0xFFFFFFFF);
        bell.addView(bellIcon, new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        header.addView(bell, new LinearLayout.LayoutParams(dp(42), dp(42)));

        View whiteWave = new HomeWaveView(this);
        FrameLayout.LayoutParams waveParams = new FrameLayout.LayoutParams(-1, dp(82), Gravity.BOTTOM);
        waveParams.setMargins(0, 0, 0, dp(-24));
        redHeader.addView(whiteWave, waveParams);

        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(148), dp(18), 0);
        pageRoot.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout statusCard = vertical();
        statusCard.setPadding(dp(0), dp(10), dp(0), dp(10));
        statusCard.setBackgroundColor(0x00FFFFFF);
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.setMargins(0, 0, 0, dp(14));
        shell.addView(statusCard, statusParams);

        LinearLayout statusTop = horizontal();
        statusTop.setGravity(Gravity.CENTER_VERTICAL);
        statusCard.addView(statusTop, matchWrap());

        LinearLayout statusTextStack = vertical();
        statusTop.addView(statusTextStack, new LinearLayout.LayoutParams(0, -2, 1));
        statusTextStack.addView(text("Status Hari Ini", 14, COLOR_TEXT, false));
        TextView statusValue = text(isClockOut() ? "Sudah Absen Masuk" : "Belum Absen", 30, COLOR_ACCENT, true);
        statusValue.setTypeface(Typeface.DEFAULT_BOLD);
        statusValue.setPadding(0, dp(3), 0, dp(3));
        statusTextStack.addView(statusValue);
        statusTextStack.addView(text(new SimpleDateFormat("EEEE, dd MMMM yyyy", new Locale("id", "ID")).format(new Date()), 15, COLOR_MUTED, false));

        FrameLayout clockWrap = new FrameLayout(this);
        clockWrap.setBackground(round(0xFFFFE4E6, dp(31), 0, 0));
        ImageView clockIcon = icon(R.drawable.ic_clock, 0xFFFB7185);
        clockWrap.addView(clockIcon, new FrameLayout.LayoutParams(dp(30), dp(30), Gravity.CENTER));
        statusTop.addView(clockWrap, new LinearLayout.LayoutParams(dp(62), dp(62)));

        Button action = button(isClockOut() ? "Absen Pulang" : "Absen Masuk", COLOR_ACCENT, 0xFFFFFFFF, 0);
        action.setTextSize(16);
        action.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(50));
        actionParams.setMargins(0, dp(18), 0, 0);
        statusCard.addView(action, actionParams);
        action.setOnClickListener(v -> openAttendanceFlowFromHome());

        shell.addView(text("Informasi Absen", 15, COLOR_TEXT, true), matchWrap());
        LinearLayout infoCard = vertical();
        infoCard.setPadding(0, dp(6), 0, dp(5));
        infoCard.setBackgroundColor(0x00FFFFFF);
        LinearLayout.LayoutParams infoParams = matchWrap();
        infoParams.setMargins(0, dp(8), 0, dp(15));
        shell.addView(infoCard, infoParams);
        infoCard.addView(homeInfoRow("Lokasi Aktif", valueOrDash(address), gpsLocked ? "GPS" : "-", COLOR_SUCCESS, COLOR_SOFT_GREEN, R.drawable.ic_location));
        infoCard.addView(homeInfoRow("Face Score", faceScore > 0 ? faceScore + "%" : "Belum tersedia", faceScore > 0 ? "Baik" : "-", 0xFF0EA5E9, 0xFFE0F2FE, R.drawable.ic_face));
        infoCard.addView(homeInfoRow("Jadwal Kerja", "-", "-", 0xFFF97316, 0xFFFFEDD5, R.drawable.ic_clock));

        shell.addView(text("Ringkasan Bulan Ini", 15, COLOR_TEXT, true), matchWrap());
        LinearLayout summary = horizontal();
        summary.setGravity(Gravity.CENTER);
        summary.setPadding(0, dp(12), 0, dp(10));
        summary.setBackgroundColor(0x00FFFFFF);
        LinearLayout.LayoutParams summaryParams = matchWrap();
        summaryParams.setMargins(0, dp(6), 0, 0);
        shell.addView(summary, summaryParams);
        summary.addView(homeSummaryItem("-", "Hadir", COLOR_SUCCESS, R.drawable.ic_calendar), new LinearLayout.LayoutParams(0, -2, 1));
        summary.addView(homeSummaryItem("-", "Terlambat", 0xFFF97316, R.drawable.ic_clock), new LinearLayout.LayoutParams(0, -2, 1));
        summary.addView(homeSummaryItem("-", "Tidak Hadir", COLOR_ERROR, R.drawable.ic_back), new LinearLayout.LayoutParams(0, -2, 1));
        summary.addView(homeSummaryItem("-", "Kehadiran", 0xFF2563EB, R.drawable.ic_face), new LinearLayout.LayoutParams(0, -2, 1));

        addBottomNav(shell, "Beranda");
        setContentView(pageRoot);
    }

    private void openAttendanceFlowFromHome() {
        if (mainRoot == null) {
            return;
        }
        showingHistory = false;
        setSystemBarStyle(false);
        setContentView(mainRoot);
        if (isClockOut()) {
            startClockOutFlow();
        } else {
            resetPhotoStep();
            requestGps();
        }
    }

    private void onHomeAttendanceAction() {
        if (isClockOut()) {
            openAttendanceFlowFromHome();
            return;
        }
        if (totalShift >= 2) {
            showHomeShiftChoices();
            return;
        }
        selectedShiftId = "1";
        openAttendanceFlowFromHome();
    }

    private void configureHomePrimaryAction(String label, View.OnClickListener listener) {
        if (homeActionButton == null) {
            return;
        }
        homeActionButton.removeAllViews();
        homeActionButton.setBackground(round(COLOR_ACCENT, dp(12), 0, 0));
        homeActionLabel = text(label, 15, 0xFFFFFFFF, true);
        homeActionLabel.setGravity(Gravity.CENTER);
        homeActionButton.addView(homeActionLabel, new FrameLayout.LayoutParams(-1, -1));
        ImageView arrow = icon(R.drawable.ic_chevron_right, 0xFFFFFFFF);
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(
            dp(21), dp(21), Gravity.CENTER_VERTICAL | Gravity.RIGHT
        );
        arrowParams.setMargins(0, 0, dp(16), 0);
        homeActionButton.addView(arrow, arrowParams);
        homeActionButton.setOnClickListener(listener);
    }

    private void showHomeShiftChoices() {
        if (homeActionButton == null || isClockOut()) {
            openAttendanceFlowFromHome();
            return;
        }
        homeActionButton.setOnClickListener(null);
        homeActionButton.removeAllViews();
        homeActionButton.setBackgroundColor(0x00FFFFFF);

        LinearLayout choices = horizontal();
        choices.setGravity(Gravity.CENTER);
        TextView shiftOne = homeShiftChoice("Shift 1");
        TextView shiftTwo = homeShiftChoice("Shift 2");
        LinearLayout.LayoutParams firstParams = new LinearLayout.LayoutParams(0, -1, 1);
        firstParams.setMargins(0, 0, dp(4), 0);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, -1, 1);
        secondParams.setMargins(dp(4), 0, 0, 0);
        choices.addView(shiftOne, firstParams);
        choices.addView(shiftTwo, secondParams);
        homeActionButton.addView(choices, new FrameLayout.LayoutParams(-1, -1));

        shiftOne.setOnClickListener(v -> {
            selectedShiftId = "1";
            openAttendanceFlowFromHome();
        });
        shiftTwo.setOnClickListener(v -> {
            selectedShiftId = "2";
            openAttendanceFlowFromHome();
        });
    }

    private TextView homeShiftChoice(String label) {
        TextView choice = text(label, 15, 0xFFFFFFFF, true);
        choice.setGravity(Gravity.CENTER);
        choice.setBackground(round(COLOR_ACCENT, dp(12), 0, 0));
        choice.setClickable(true);
        return choice;
    }

    private LinearLayout homeInfoRow(String title, String value, String badge, int accent, int softColor, int iconRes) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(10), dp(14), dp(10));

        FrameLayout iconWrap = new FrameLayout(this);
        iconWrap.setBackground(round(softColor, dp(20), 0, 0));
        ImageView icon = icon(iconRes, accent);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(dp(23), dp(23), Gravity.CENTER));
        row.addView(iconWrap, new LinearLayout.LayoutParams(dp(38), dp(38)));

        LinearLayout textStack = vertical();
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, -2, 1);
        textParams.setMargins(dp(11), 0, dp(7), 0);
        row.addView(textStack, textParams);
        textStack.addView(text(title, 12, COLOR_TEXT, true));
        textStack.addView(text(value, 12, COLOR_MUTED, false));

        TextView pill = text(badge, 10, accent, true);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(11), dp(6), dp(11), dp(6));
        pill.setBackground(round(softColor, dp(14), 0, 0));
        row.addView(pill);
        return row;
    }

    private LinearLayout homeSummaryItem(String value, String label, int accent, int iconRes) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setPadding(dp(3), dp(7), dp(3), dp(7));
        item.setBackground(round(Color.argb(18, Color.red(accent), Color.green(accent), Color.blue(accent)), dp(8), 0, 0));
        FrameLayout iconWrap = new FrameLayout(this);
        iconWrap.setBackground(round(Color.argb(30, Color.red(accent), Color.green(accent), Color.blue(accent)), dp(18), 0, 0));
        ImageView icon = icon(iconRes, accent);
        iconWrap.addView(icon, new FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER));
        item.addView(iconWrap, new LinearLayout.LayoutParams(dp(34), dp(34)));
        TextView number = text(value, 20, COLOR_TEXT, true);
        number.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        number.setGravity(Gravity.CENTER);
        number.setPadding(0, dp(6), 0, 0);
        item.addView(number);
        TextView caption = text(label, 11, COLOR_MUTED, false);
        caption.setGravity(Gravity.CENTER);
        item.addView(caption);
        return item;
    }

    private FrameLayout homePrimaryAction(String label) {
        FrameLayout action = new FrameLayout(this);
        action.setBackground(round(COLOR_ACCENT, dp(12), 0, 0));
        TextView labelView = text(label, 15, 0xFFFFFFFF, true);
        labelView.setGravity(Gravity.CENTER);
        action.addView(labelView, new FrameLayout.LayoutParams(-1, -1));
        ImageView arrow = icon(R.drawable.ic_chevron_right, 0xFFFFFFFF);
        FrameLayout.LayoutParams arrowParams = new FrameLayout.LayoutParams(dp(21), dp(21), Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        arrowParams.setMargins(0, 0, dp(16), 0);
        action.addView(arrow, arrowParams);
        return action;
    }

    private View thinDivider(int leftMargin) {
        View divider = new View(this);
        divider.setBackgroundColor(0xFFF0F1F3);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.setMargins(dp(leftMargin), 0, dp(12), 0);
        divider.setLayoutParams(params);
        return divider;
    }

    private LinearLayout.LayoutParams summaryItemParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -1, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private TextView historyTab(String label, boolean active) {
        TextView tab = text(label, 10, active ? 0xFFFFFFFF : COLOR_TEXT, active);
        tab.setGravity(Gravity.CENTER);
        tab.setBackground(round(active ? COLOR_ACCENT : 0xFFFFFFFF, dp(10), active ? 0 : COLOR_STROKE, active ? 0 : 1));
        return tab;
    }

    private void applyHistoryFilter(String filter) {
        activeHistoryFilter = filter;
        styleHistoryTab(historyAllTab, "all".equals(filter));
        styleHistoryTab(historyOnTimeTab, "present".equals(filter));
        styleHistoryTab(historyLateTab, "late".equals(filter));
        styleHistoryTab(historyAbsentTab, "absent".equals(filter));
        if (nativeHistoryList == null) {
            return;
        }
        nativeHistoryList.removeAllViews();
        int visibleCount = 0;
        for (int index = 0; index < nativeHistoryRows.length(); index += 1) {
            JSONObject row = nativeHistoryRows.optJSONObject(index);
            if (row == null) {
                continue;
            }
            String statusClass = normalizedHistoryStatusClass(row);
            if (!"all".equals(filter) && !filter.equals(statusClass)) {
                continue;
            }
            addHistoryRow(nativeHistoryList, row);
            visibleCount += 1;
        }
        if (visibleCount == 0) {
            TextView empty = text("Tidak ada data untuk filter ini.", 13, COLOR_MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(32), 0, 0);
            nativeHistoryList.addView(empty, matchWrap());
        }
    }

    private void styleHistoryTab(TextView tab, boolean active) {
        if (tab == null) {
            return;
        }
        tab.setTextColor(active ? 0xFFFFFFFF : COLOR_TEXT);
        tab.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tab.setBackground(round(active ? COLOR_ACCENT : 0xFFFFFFFF, dp(10), active ? 0 : COLOR_STROKE, active ? 0 : 1));
    }

    private String currentMonthLabel() {
        String value = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID")).format(new Date());
        return value.length() == 0 ? value : value.substring(0, 1).toUpperCase(new Locale("id", "ID")) + value.substring(1);
    }

    private void openAttendanceDetail(JSONObject row) {
        currentSection = "";
        stopNotificationPolling();
        showingHistory = true;
        setSystemBarStyle(false);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFFFFFFF);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(4), dp(18), dp(18));
        scroll.addView(shell, new ScrollView.LayoutParams(-1, -1));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        shell.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));
        FrameLayout back = new FrameLayout(this);
        back.addView(icon(R.drawable.ic_back, COLOR_TEXT), new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        back.setOnClickListener(v -> openHistory());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        TextView title = text("Detail Absen", 18, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(new View(this), new LinearLayout.LayoutParams(dp(42), dp(48)));

        String statusClass = normalizedHistoryStatusClass(row);
        int statusColor = historyStatusColor(statusClass);
        String statusLabel = historyStatusLabel(row, statusClass);
        LinearLayout statusHeader = vertical();
        statusHeader.setPadding(dp(18), dp(20), dp(18), dp(24));
        statusHeader.setBackground(round(statusColor, dp(12), 0, 0));
        shell.addView(statusHeader, new LinearLayout.LayoutParams(-1, dp(118)));
        statusHeader.addView(text(statusLabel, 26, 0xFFFFFFFF, true));
        TextView statusDate = text(row.optString("hari", "") + ", " + row.optString("tanggal", "-"), 14, 0xEEFFFFFF, false);
        statusDate.setPadding(0, dp(6), 0, 0);
        statusHeader.addView(statusDate);

        LinearLayout attendanceCard = vertical();
        attendanceCard.setPadding(dp(18), dp(18), dp(18), dp(18));
        attendanceCard.setBackground(round(0xFFFFFFFF, dp(12), 0xFFF0F1F3, 1));
        setSoftElevation(attendanceCard, 1);
        LinearLayout.LayoutParams attendanceParams = matchWrap();
        attendanceParams.setMargins(0, dp(-12), 0, 0);
        shell.addView(attendanceCard, attendanceParams);

        String place = row.optString("lokasi", "-");
        String outPlace = row.optString("clock_out_lokasi", "-");
        String inScore = row.optInt("face_score", 0) > 0 ? row.optInt("face_score", 0) + "%" : "-";
        String outScore = row.optInt("clock_out_face_score", 0) > 0 ? row.optInt("clock_out_face_score", 0) + "%" : "-";
        FrameLayout timeline = new FrameLayout(this);
        View connector = new View(this);
        connector.setBackgroundColor(0xFFB7E4C2);
        FrameLayout.LayoutParams connectorParams = new FrameLayout.LayoutParams(dp(2), dp(76));
        connectorParams.leftMargin = dp(20);
        connectorParams.topMargin = dp(42);
        timeline.addView(connector, connectorParams);
        LinearLayout timelineRows = vertical();
        timeline.addView(timelineRows, new FrameLayout.LayoutParams(-1, -2));
        timelineRows.addView(
            detailTimelineRow("Masuk", row.optString("jam", "-"), place, row.optString("koordinat", "-"), inScore, COLOR_SUCCESS, R.drawable.ic_arrow_down),
            matchWrap()
        );
        timelineRows.addView(
            detailTimelineRow("Pulang", row.optString("clock_out", "-"), outPlace, row.optString("clock_out_koordinat", "-"), outScore, 0xFF168CD5, R.drawable.ic_arrow_up),
            new LinearLayout.LayoutParams(-1, -2)
        );
        attendanceCard.addView(timeline, matchWrap());

        String inPhotoUrl = row.optString("photo_url", "");
        String outPhotoUrl = row.optString("clock_out_photo_url", "");
        if (!inPhotoUrl.isEmpty() || !outPhotoUrl.isEmpty()) {
            LinearLayout photoActions = horizontal();
            LinearLayout.LayoutParams photoActionParams = new LinearLayout.LayoutParams(-1, dp(46));
            photoActionParams.setMargins(0, dp(8), 0, 0);
            attendanceCard.addView(photoActions, photoActionParams);
            if (!inPhotoUrl.isEmpty()) {
                Button showInPhoto = button("Lihat Foto Masuk", 0xFFFFFFFF, COLOR_ACCENT, COLOR_ACCENT_STROKE);
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, -1, 1);
                buttonParams.setMargins(0, 0, dp(5), 0);
                photoActions.addView(showInPhoto, buttonParams);
                showInPhoto.setOnClickListener(v -> openAttendancePhoto(row, "Foto Absen Masuk", inPhotoUrl));
            }
            if (!outPhotoUrl.isEmpty()) {
                Button showOutPhoto = button("Lihat Foto Pulang", 0xFFFFFFFF, COLOR_ACCENT, COLOR_ACCENT_STROKE);
                LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, -1, 1);
                buttonParams.setMargins(dp(5), 0, 0, 0);
                photoActions.addView(showOutPhoto, buttonParams);
                showOutPhoto.setOnClickListener(v -> openAttendancePhoto(row, "Foto Absen Pulang", outPhotoUrl));
            }
        }

        LinearLayout additional = vertical();
        additional.setBackground(round(0xFFFFFFFF, dp(12), 0xFFF0F1F3, 1));
        setSoftElevation(additional, 1);
        LinearLayout.LayoutParams additionalParams = matchWrap();
        additionalParams.setMargins(0, dp(16), 0, 0);
        shell.addView(additional, additionalParams);
        TextView additionalTitle = text("Informasi Tambahan", 15, COLOR_TEXT, true);
        additionalTitle.setPadding(dp(16), dp(16), dp(16), dp(12));
        additional.addView(additionalTitle);
        additional.addView(additionalInfoRow("Perangkat", preferredDetailValue(row, "device_info", "clock_out_device_info"), R.drawable.ic_device));
        additional.addView(thinDivider(dp(42)));
        additional.addView(additionalInfoRow("IP Address", preferredDetailValue(row, "ip_address", "clock_out_ip_address"), R.drawable.ic_map));
        additional.addView(thinDivider(dp(42)));
        additional.addView(additionalInfoRow("Versi Aplikasi", row.optString("app_version", "-"), R.drawable.ic_info));

        setContentView(root);
    }

    private LinearLayout detailTimelineRow(String label, String time, String place, String coordinates, String score, int color, int iconRes) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.TOP);
        row.setMinimumHeight(dp(110));

        FrameLayout marker = new FrameLayout(this);
        marker.setBackground(round(color, dp(21), 0, 0));
        marker.addView(icon(iconRes, 0xFFFFFFFF), new FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER));
        row.addView(marker, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout copy = vertical();
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.setMargins(dp(12), 0, dp(8), 0);
        row.addView(copy, copyParams);
        copy.addView(text(label, 15, COLOR_TEXT, true));
        TextView timeView = text(valueOrDash(time), 18, COLOR_TEXT, false);
        timeView.setPadding(0, dp(3), 0, dp(7));
        copy.addView(timeView);
        TextView placeView = text(valueOrDash(place), 13, COLOR_MUTED, false);
        placeView.setMaxLines(2);
        copy.addView(placeView);
        TextView coordinateView = text("Koordinat: " + valueOrDash(coordinates), 11, COLOR_MUTED, false);
        coordinateView.setPadding(0, dp(4), 0, 0);
        copy.addView(coordinateView);

        LinearLayout scoreStack = vertical();
        scoreStack.setGravity(Gravity.RIGHT);
        TextView scoreLabel = text("Face Score", 11, COLOR_MUTED, false);
        scoreLabel.setGravity(Gravity.RIGHT);
        scoreStack.addView(scoreLabel);
        TextView scoreValue = text(score, 17, "-".equals(score) ? COLOR_MUTED : COLOR_SUCCESS, true);
        scoreValue.setGravity(Gravity.RIGHT);
        scoreValue.setPadding(0, dp(5), 0, 0);
        scoreStack.addView(scoreValue);
        row.addView(scoreStack, new LinearLayout.LayoutParams(dp(78), -2));
        return row;
    }

    private String preferredDetailValue(JSONObject row, String primary, String secondary) {
        String value = row.optString(primary, "").trim();
        if (value.isEmpty() || "-".equals(value)) {
            value = row.optString(secondary, "").trim();
        }
        return valueOrDash(value);
    }

    private void openAttendancePhoto(JSONObject attendanceRow, String titleText, String photoUrl) {
        currentSection = "";
        stopNotificationPolling();
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF111827);
        LinearLayout page = vertical();
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10), 0, dp(10), 0);
        page.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));
        FrameLayout back = new FrameLayout(this);
        back.addView(icon(R.drawable.ic_back, 0xFFFFFFFF), new FrameLayout.LayoutParams(dp(25), dp(25), Gravity.CENTER));
        back.setOnClickListener(v -> openAttendanceDetail(attendanceRow));
        header.addView(back, new LinearLayout.LayoutParams(dp(44), dp(48)));
        TextView title = text(titleText, 17, 0xFFFFFFFF, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(new View(this), new LinearLayout.LayoutParams(dp(44), dp(48)));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        page.addView(photo, new LinearLayout.LayoutParams(-1, 0, 1));
        loadRemoteImage(photo, absoluteUrl(photoUrl));
        setContentView(root);
    }

    private LinearLayout additionalInfoRow(String label, String value, int iconRes) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(16), dp(14), dp(16));
        row.addView(icon(iconRes, COLOR_MUTED), new LinearLayout.LayoutParams(dp(21), dp(21)));
        TextView labelView = text(label, 13, COLOR_MUTED, false);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.setMargins(dp(10), 0, dp(8), 0);
        row.addView(labelView, labelParams);
        TextView valueView = text(valueOrDash(value), 13, COLOR_TEXT, false);
        valueView.setGravity(Gravity.RIGHT);
        row.addView(valueView);
        return row;
    }

    private void openLeavePage() {
        currentSection = "Izin";
        stopNotificationPolling();
        showingHistory = true;
        setSystemBarStyle(false);
        FrameLayout pageRoot = new FrameLayout(this);
        pageRoot.setBackgroundColor(0xFFFFFFFF);
        ScrollView pageScroll = new ScrollView(this);
        pageScroll.setFillViewport(true);
        pageRoot.addView(pageScroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(4), dp(18), dp(94));
        pageScroll.addView(shell, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("Pengajuan Izin", 18, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        shell.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        LinearLayout form = vertical();
        form.setPadding(dp(14), dp(10), dp(14), dp(14));
        form.setBackground(round(0xFFFFFFFF, dp(12), 0xFFF0F1F3, 1));
        setSoftElevation(form, 1);
        shell.addView(form, matchWrap());

        TextView typeLabel = text("Jenis Izin", 12, COLOR_TEXT, true);
        typeLabel.setPadding(0, dp(8), 0, dp(6));
        form.addView(typeLabel);
        leaveTypeSpinner = new Spinner(this);
        ArrayAdapter<String> leaveAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            new String[]{"IZIN", "SAKIT"}
        );
        leaveAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leaveTypeSpinner.setAdapter(leaveAdapter);
        leaveTypeSpinner.setPadding(dp(8), 0, dp(8), 0);
        leaveTypeSpinner.setBackground(round(0xFFF9FAFB, dp(9), COLOR_STROKE, 1));
        form.addView(leaveTypeSpinner, new LinearLayout.LayoutParams(-1, dp(46)));

        leaveDateInput = addLeaveInput(
            form,
            "Tanggal",
            "Pilih tanggal",
            new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date())
        );
        leaveDateInput.setFocusable(false);
        leaveDateInput.setOnClickListener(v -> showLeaveDatePicker());

        TextView noteLabel = text("Keterangan", 12, COLOR_TEXT, true);
        noteLabel.setPadding(0, dp(8), 0, dp(6));
        form.addView(noteLabel);
        leaveNoteInput = new EditText(this);
        leaveNoteInput.setHint("Tulis alasan izin");
        leaveNoteInput.setSingleLine(false);
        leaveNoteInput.setMinLines(3);
        leaveNoteInput.setGravity(Gravity.TOP);
        leaveNoteInput.setTextSize(13);
        leaveNoteInput.setTextColor(COLOR_TEXT);
        leaveNoteInput.setHintTextColor(0xFF98A2B3);
        leaveNoteInput.setPadding(dp(12), dp(10), dp(12), dp(10));
        leaveNoteInput.setBackground(round(0xFFF9FAFB, dp(9), COLOR_STROKE, 1));
        form.addView(leaveNoteInput, new LinearLayout.LayoutParams(-1, dp(78)));

        TextView attachmentLabel = text("Lampiran", 12, COLOR_TEXT, true);
        attachmentLabel.setPadding(0, dp(8), 0, dp(6));
        form.addView(attachmentLabel);
        leaveAttachmentLabel = text("Pilih foto lampiran", 13, COLOR_MUTED, false);
        leaveAttachmentLabel.setGravity(Gravity.CENTER_VERTICAL);
        leaveAttachmentLabel.setPadding(dp(12), 0, dp(12), 0);
        leaveAttachmentLabel.setBackground(round(0xFFF9FAFB, dp(9), COLOR_STROKE, 1));
        leaveAttachmentLabel.setOnClickListener(v -> openImagePicker(LEAVE_ATTACHMENT_REQUEST));
        form.addView(leaveAttachmentLabel, new LinearLayout.LayoutParams(-1, dp(46)));

        Button send = button("Kirim Izin", COLOR_ACCENT, 0xFFFFFFFF, 0);
        send.setTextSize(14);
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(-1, dp(48));
        sendParams.setMargins(0, dp(12), 0, 0);
        form.addView(send, sendParams);
        send.setOnClickListener(v -> submitLeaveRequest(send));

        TextView historyTitle = text("Riwayat Izin Bulan Ini", 15, COLOR_TEXT, true);
        historyTitle.setPadding(dp(2), dp(18), 0, dp(10));
        shell.addView(historyTitle, matchWrap());

        LinearLayout historyCard = vertical();
        historyCard.setPadding(dp(12), dp(6), dp(12), dp(6));
        historyCard.setBackground(round(0xFFFFFFFF, dp(12), 0xFFF0F1F3, 1));
        setSoftElevation(historyCard, 1);
        shell.addView(historyCard, new LinearLayout.LayoutParams(-1, responsiveLeaveHistoryHeight()));
        ScrollView historyScroll = new ScrollView(this);
        historyScroll.setFillViewport(true);
        historyCard.addView(historyScroll, new LinearLayout.LayoutParams(-1, -1));
        leaveHistoryList = vertical();
        TextView loading = text("Memuat riwayat izin...", 13, COLOR_MUTED, false);
        loading.setGravity(Gravity.CENTER);
        loading.setPadding(0, dp(28), 0, 0);
        leaveHistoryList.addView(loading, matchWrap());
        historyScroll.addView(leaveHistoryList, new ScrollView.LayoutParams(-1, -2));

        addFixedBottomNav(pageRoot, "Izin");
        setContentView(pageRoot);
        loadLeaveHistory();
    }

    private void showLeaveDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(
            this,
            (view, year, month, day) -> leaveDateInput.setText(String.format(
                Locale.US,
                "%04d-%02d-%02d",
                year,
                month + 1,
                day
            )),
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        picker.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        picker.show();
    }

    private void openImagePicker(int requestCode) {
        Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
        picker.setType("image/*");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(picker, "Pilih Foto"), requestCode);
    }

    private EditText addLeaveInput(LinearLayout parent, String label, String hint, String value) {
        TextView labelView = text(label, 12, COLOR_TEXT, true);
        labelView.setPadding(0, dp(8), 0, dp(6));
        parent.addView(labelView);
        EditText input = new EditText(this);
        input.setText(value);
        input.setHint(hint);
        input.setSingleLine(true);
        input.setTextSize(13);
        input.setTextColor(COLOR_TEXT);
        input.setHintTextColor(0xFF98A2B3);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(round(0xFFF9FAFB, dp(9), COLOR_STROKE, 1));
        parent.addView(input, new LinearLayout.LayoutParams(-1, dp(44)));
        return input;
    }

    private void submitLeaveRequest(Button sendButton) {
        String type = leaveTypeSpinner == null
            ? "IZIN"
            : String.valueOf(leaveTypeSpinner.getSelectedItem()).trim().toUpperCase(Locale.US);
        String date = leaveDateInput.getText().toString().trim();
        String note = leaveNoteInput.getText().toString().trim();
        if (!("IZIN".equals(type) || "SAKIT".equals(type))) {
            notifyUser("Jenis izin harus IZIN atau SAKIT.");
            return;
        }
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            notifyUser("Tanggal harus berformat YYYY-MM-DD.");
            return;
        }
        if (note.isEmpty()) {
            notifyUser("Keterangan izin wajib diisi.");
            return;
        }
        sendButton.setEnabled(false);
        sendButton.setText("Mengirim...");
        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(
                    postLeaveMultipart(type, date, note),
                    "/api/attendance_leave"
                ));
                if (!response.optBoolean("success", false)) {
                    throw new Exception(response.optString("error", "Gagal menyimpan izin"));
                }
                runOnUiThread(() -> {
                    sendButton.setEnabled(true);
                    sendButton.setText("Kirim Izin");
                    leaveNoteInput.setText("");
                    leaveAttachmentBitmap = null;
                    leaveAttachmentLabel.setText("Pilih foto lampiran");
                    leaveAttachmentLabel.setTextColor(COLOR_MUTED);
                    notifyUser("Pengajuan izin tersimpan.");
                    loadLeaveHistory();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    sendButton.setEnabled(true);
                    sendButton.setText("Kirim Izin");
                    notifyUser(error.getMessage());
                });
            }
        }).start();
    }

    private void loadLeaveHistory() {
        if (leaveHistoryList == null || baseUrl == null || baseUrl.trim().isEmpty()) {
            return;
        }
        final String url = baseUrl + "/api/attendance_leave?month="
            + new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date())
            + "&_ts=" + System.currentTimeMillis();
        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(httpGet(url), "/api/attendance_leave"));
                if (!response.optBoolean("success", false)) {
                    throw new Exception(response.optString("error", "Gagal memuat riwayat izin"));
                }
                JSONArray rows = response.getJSONObject("history").optJSONArray("rows");
                runOnUiThread(() -> renderLeaveHistory(rows == null ? new JSONArray() : rows));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    leaveHistoryList.removeAllViews();
                    TextView message = text("Riwayat izin belum dapat dimuat.", 13, COLOR_MUTED, false);
                    message.setGravity(Gravity.CENTER);
                    message.setPadding(0, dp(28), 0, 0);
                    leaveHistoryList.addView(message, matchWrap());
                });
            }
        }).start();
    }

    private void renderLeaveHistory(JSONArray rows) {
        leaveHistoryList.removeAllViews();
        if (rows.length() == 0) {
            TextView empty = text("Belum ada riwayat izin bulan ini.", 13, COLOR_MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, 0);
            leaveHistoryList.addView(empty, matchWrap());
            return;
        }
        for (int index = 0; index < rows.length(); index += 1) {
            JSONObject row = rows.optJSONObject(index);
            if (row != null) {
                addLeaveHistoryRow(leaveHistoryList, row);
            }
        }
    }

    private void addLeaveHistoryRow(LinearLayout parent, JSONObject rowData) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(10), 0, dp(10));
        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(78)));

        LinearLayout dateBox = vertical();
        dateBox.setGravity(Gravity.CENTER);
        dateBox.setBackground(round(0xFFFFFFFF, dp(5), COLOR_STROKE, 1));
        row.addView(dateBox, new LinearLayout.LayoutParams(dp(46), dp(56)));
        String day = rowData.optString("hari", "-");
        TextView dayView = text(day.length() >= 3 ? day.substring(0, 3).toUpperCase(Locale.US) : day, 9, 0xFFFFFFFF, true);
        dayView.setGravity(Gravity.CENTER);
        dayView.setBackground(round(COLOR_ACCENT, dp(4), 0, 0));
        dateBox.addView(dayView, new LinearLayout.LayoutParams(-1, dp(20)));
        String dateKey = rowData.optString("date_key", "");
        TextView number = text(dateKey.length() >= 10 ? dateKey.substring(8, 10) : "-", 18, COLOR_TEXT, true);
        number.setGravity(Gravity.CENTER);
        dateBox.addView(number, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout copy = vertical();
        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, -2, 1);
        copyParams.setMargins(dp(12), 0, dp(8), 0);
        row.addView(copy, copyParams);
        copy.addView(text(rowData.optString("tanggal", "-"), 12, COLOR_TEXT, true));
        TextView note = text(rowData.optString("keterangan", "-"), 11, COLOR_MUTED, false);
        note.setMaxLines(2);
        note.setPadding(0, dp(4), 0, 0);
        copy.addView(note);

        TextView type = text(rowData.optString("type", "IZIN"), 10, COLOR_ACCENT, true);
        type.setGravity(Gravity.CENTER);
        type.setPadding(dp(10), dp(7), dp(10), dp(7));
        type.setBackground(round(0xFFFFE5E7, dp(14), 0, 0));
        row.addView(type);
        parent.addView(thinDivider(dp(58)));
    }

    private void openLeavePageCurrentLegacy() {
        showingHistory = true;
        setSystemBarStyle(false);
        FrameLayout pageRoot = new FrameLayout(this);
        pageRoot.setBackgroundColor(0xFFFFFFFF);
        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(4), dp(18), dp(84));
        pageRoot.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        TextView title = text("Pengajuan Izin", 16, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        shell.addView(title, new LinearLayout.LayoutParams(-1, dp(62)));

        TextView section = text("Form Izin", 14, COLOR_TEXT, true);
        section.setPadding(0, dp(8), 0, dp(10));
        shell.addView(section);

        LinearLayout form = vertical();
        form.setPadding(dp(16), dp(8), dp(16), dp(16));
        form.setBackground(round(0xFFFFFFFF, dp(12), 0xFFF0F1F3, 1));
        setSoftElevation(form, 2);
        shell.addView(form, matchWrap());
        form.addView(leaveField("Jenis Izin", "Sakit / Izin / Cuti"));
        form.addView(leaveField("Tanggal", "Pilih tanggal izin"));
        form.addView(leaveField("Keterangan", "Tulis alasan izin"));
        Button send = button("Kirim Izin", COLOR_ACCENT, 0xFFFFFFFF, 0);
        send.setTextSize(15);
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(-1, dp(52));
        sendParams.setMargins(0, dp(16), 0, 0);
        form.addView(send, sendParams);
        send.setOnClickListener(v -> notifyUser("Pengajuan izin siap dikirim."));

        addFixedBottomNav(pageRoot, "Izin");
        setContentView(pageRoot);
    }

    private void openLeavePageLegacy() {
        showingHistory = true;
        FrameLayout pageRoot = new FrameLayout(this);
        addAppBackground(pageRoot);
        LinearLayout shell = vertical();
        shell.setPadding(dp(16), dp(16), dp(16), dp(16));
        pageRoot.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout top = horizontal();
        top.setGravity(Gravity.CENTER_VERTICAL);
        shell.addView(top, matchWrap());
        Button back = button("KEMBALI", 0xFFFFFFFF, COLOR_ACCENT, COLOR_ACCENT_STROKE);
        top.addView(back, new LinearLayout.LayoutParams(dp(104), dp(44)));
        back.setOnClickListener(v -> onBackPressed());
        TextView title = text("Pengajuan Izin", 18, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        View topSpacer = new View(this);
        top.addView(topSpacer, new LinearLayout.LayoutParams(dp(104), dp(1)));

        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(round(0xFFFFFFFF, dp(18), COLOR_STROKE, 1));
        setSoftElevation(card, 6);
        LinearLayout.LayoutParams cardParams = matchWrap();
        cardParams.setMargins(0, dp(18), 0, 0);
        shell.addView(card, cardParams);
        card.addView(text("Form izin sementara", 16, COLOR_TEXT, true));
        TextView desc = text("Halaman ini sudah disiapkan. Nanti data izin akan dikirim ke server dan masuk ke pengaturan izin di attendance dashboard.", 12, COLOR_SUBTLE, false);
        desc.setPadding(0, dp(6), 0, dp(12));
        card.addView(desc);
        card.addView(dummyField("Jenis Izin", "Sakit / Izin / Cuti"));
        card.addView(dummyField("Tanggal", "Pilih tanggal izin"));
        card.addView(dummyField("Keterangan", "Tulis alasan izin"));
        Button send = button("KIRIM IZIN", COLOR_ACCENT, 0xFFFFFFFF, 0);
        LinearLayout.LayoutParams sendParams = new LinearLayout.LayoutParams(-1, dp(52));
        sendParams.setMargins(0, dp(14), 0, 0);
        card.addView(send, sendParams);
        send.setOnClickListener(v -> notifyUser("Dummy dulu. Nanti pengajuan izin dikirim ke server."));

        addBottomNav(shell, "Izin");
        setContentView(pageRoot);
    }

    private void openAccountPage() {
        currentSection = "Akun";
        stopNotificationPolling();
        showingHistory = true;
        setSystemBarStyle(false);
        FrameLayout pageRoot = new FrameLayout(this);
        pageRoot.setBackgroundColor(0xFFFFFFFF);

        ScrollView accountScroll = new ScrollView(this);
        accountScroll.setFillViewport(true);
        pageRoot.addView(accountScroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(4), dp(18), dp(94));
        accountScroll.addView(shell, new ScrollView.LayoutParams(-1, -2));

        TextView title = text("Akun", 18, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        shell.addView(title, new LinearLayout.LayoutParams(-1, dp(66)));

        LinearLayout profile = vertical();
        profile.setGravity(Gravity.CENTER);
        profile.setPadding(dp(16), dp(15), dp(16), dp(14));
        profile.setBackground(redGradient(dp(12)));
        LinearLayout.LayoutParams profileParams = new LinearLayout.LayoutParams(-1, dp(190));
        profileParams.setMargins(0, dp(2), 0, dp(14));
        shell.addView(profile, profileParams);

        FrameLayout avatarFrame = new FrameLayout(this);
        avatarFrame.setBackground(round(0xFFFFFFFF, dp(36), 0, 0));
        avatarFrame.setClipToOutline(true);
        ImageView profilePhoto = new ImageView(this);
        profilePhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (profilePhotoBitmap != null) {
            profilePhoto.setImageBitmap(profilePhotoBitmap);
        }
        avatarFrame.addView(profilePhoto, new FrameLayout.LayoutParams(-1, -1));
        TextView initial = text(initialText(), 25, COLOR_ACCENT, true);
        initial.setGravity(Gravity.CENTER);
        initial.setVisibility(profilePhoto.getDrawable() == null ? View.VISIBLE : View.GONE);
        avatarFrame.addView(initial, new FrameLayout.LayoutParams(-1, -1));
        if (profilePhoto.getDrawable() == null && profilePhotoUrl != null && !profilePhotoUrl.trim().isEmpty()) {
            loadRemoteImage(profilePhoto, absoluteUrl(profilePhotoUrl));
            initial.setVisibility(View.GONE);
        }
        profile.addView(avatarFrame, new LinearLayout.LayoutParams(dp(80), dp(80)));

        TextView name = text(displayName(), 20, 0xFFFFFFFF, true);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(7), 0, 0);
        profile.addView(name, matchWrap());
        TextView role = text(displayRole(), 14, 0xFFFFFFFF, false);
        role.setGravity(Gravity.CENTER);
        profile.addView(role, matchWrap());
        TextView employeeType = text("@" + valueOrDash(username), 12, 0xFFFFFFFF, true);
        employeeType.setGravity(Gravity.CENTER);
        employeeType.setPadding(dp(12), dp(5), dp(12), dp(5));
        employeeType.setBackground(round(0x22FFFFFF, dp(14), 0, 0));
        LinearLayout.LayoutParams employeeTypeParams = new LinearLayout.LayoutParams(-2, -2);
        employeeTypeParams.setMargins(0, dp(6), 0, 0);
        profile.addView(employeeType, employeeTypeParams);

        LinearLayout menu = vertical();
        menu.setBackground(round(0xFFFFFFFF, dp(12), 0xFFF0F1F3, 1));
        setSoftElevation(menu, 1);
        shell.addView(menu, matchWrap());
        menu.addView(accountMenuRow("Profil Saya", R.drawable.ic_user, COLOR_TEXT, v -> openProfilePage()));
        menu.addView(thinDivider(dp(52)));
        menu.addView(accountMenuRow("Pengaturan", R.drawable.ic_settings, COLOR_TEXT, null));
        menu.addView(thinDivider(dp(52)));
        menu.addView(accountMenuRow("Riwayat Perangkat", R.drawable.ic_device, COLOR_TEXT, null));
        menu.addView(thinDivider(dp(52)));
        menu.addView(accountMenuRow("Tentang Aplikasi", R.drawable.ic_info, COLOR_TEXT, v -> openAboutPage()));
        menu.addView(thinDivider(dp(52)));
        menu.addView(accountMenuRow("Keluar", R.drawable.ic_logout, COLOR_ACCENT, v -> showLoginScreen()));

        addFixedBottomNav(pageRoot, "Akun");
        setContentView(pageRoot);
    }

    private void openProfilePage() {
        currentSection = "";
        stopNotificationPolling();
        showingHistory = true;
        setSystemBarStyle(false);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFFFFFFF);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(4), dp(18), dp(96));
        scroll.addView(shell, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        shell.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));
        FrameLayout back = new FrameLayout(this);
        back.addView(icon(R.drawable.ic_back, COLOR_TEXT), new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        back.setOnClickListener(v -> openAccountPage());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        TextView title = text("Profil Saya", 18, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(new View(this), new LinearLayout.LayoutParams(dp(42), dp(48)));

        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(16), dp(16), dp(18));
        card.setBackground(round(0xFFFFFFFF, dp(12), 0xFFF0F1F3, 1));
        shell.addView(card, matchWrap());

        FrameLayout avatar = new FrameLayout(this);
        avatar.setBackground(round(0xFFFFE5E7, dp(48), 0, 0));
        ImageView photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (profilePhotoBitmap != null) {
            photo.setImageBitmap(profilePhotoBitmap);
        }
        avatar.addView(photo, new FrameLayout.LayoutParams(-1, -1));
        TextView initial = text(initialText(), 30, COLOR_ACCENT, true);
        initial.setGravity(Gravity.CENTER);
        initial.setVisibility(photo.getDrawable() == null ? View.VISIBLE : View.GONE);
        avatar.addView(initial, new FrameLayout.LayoutParams(-1, -1));
        if (photo.getDrawable() == null && profilePhotoUrl != null && !profilePhotoUrl.trim().isEmpty()) {
            loadRemoteImage(photo, absoluteUrl(profilePhotoUrl));
            initial.setVisibility(View.GONE);
        }
        avatar.setOnClickListener(v -> openImagePicker(PROFILE_PHOTO_REQUEST));
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dp(96), dp(96));
        avatarParams.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(avatar, avatarParams);
        TextView photoAction = text("Pilih Foto Profil", 13, COLOR_ACCENT, true);
        photoAction.setGravity(Gravity.CENTER);
        photoAction.setPadding(0, dp(9), 0, dp(6));
        photoAction.setOnClickListener(v -> openImagePicker(PROFILE_PHOTO_REQUEST));
        card.addView(photoAction, matchWrap());

        EditText displayNameInput = addProfileInput(card, "Nama Tampilan", displayName());
        EditText usernameInput = addProfileInput(card, "Username", username);
        EditText passwordInput = addProfileInput(card, "Password Baru (opsional)", "");
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText departmentInput = addProfileInput(card, "Department", department);

        TextView note = text("Nama tampilan hanya berlaku di aplikasi. Nama resmi yang dibuat Account Management tidak berubah.", 12, COLOR_MUTED, false);
        note.setPadding(0, dp(10), 0, 0);
        card.addView(note, matchWrap());

        Button save = button("Simpan Perubahan", COLOR_ACCENT, 0xFFFFFFFF, 0);
        save.setTextSize(14);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(50));
        saveParams.setMargins(0, dp(16), 0, 0);
        card.addView(save, saveParams);
        save.setOnClickListener(v -> submitAppProfile(
            save,
            displayNameInput.getText().toString().trim(),
            usernameInput.getText().toString().trim(),
            passwordInput.getText().toString(),
            departmentInput.getText().toString().trim()
        ));

        addFixedBottomNav(root, "Akun");
        setContentView(root);
    }

    private EditText addProfileInput(LinearLayout parent, String label, String value) {
        TextView labelView = text(label, 12, COLOR_TEXT, true);
        labelView.setPadding(0, dp(9), 0, dp(6));
        parent.addView(labelView);
        EditText input = new EditText(this);
        input.setText(value == null ? "" : value);
        input.setSingleLine(true);
        input.setTextSize(13);
        input.setTextColor(COLOR_TEXT);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(round(0xFFF9FAFB, dp(9), COLOR_STROKE, 1));
        parent.addView(input, new LinearLayout.LayoutParams(-1, dp(46)));
        return input;
    }

    private void submitAppProfile(Button button, String displayNameValue, String usernameValue, String passwordValue, String departmentValue) {
        if (!usernameValue.matches("[A-Za-z0-9._-]{3,40}")) {
            notifyUser("Format username belum valid.");
            return;
        }
        button.setEnabled(false);
        button.setText("Menyimpan...");
        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(
                    postProfileMultipart(displayNameValue, usernameValue, passwordValue, departmentValue),
                    "/api/app_profile"
                ));
                if (!response.optBoolean("success", false)) {
                    throw new Exception(response.optString("error", "Profil gagal disimpan"));
                }
                JSONObject profile = response.getJSONObject("profile");
                runOnUiThread(() -> {
                    applyProfileData(profile);
                    SharedPreferences.Editor editor = getSharedPreferences("attendance_app", MODE_PRIVATE)
                        .edit()
                        .putString("cached_username", username);
                    try {
                        if (passwordValue != null && !passwordValue.isEmpty()) {
                            editor.putString("cached_login_hash", loginHash(username, passwordValue));
                        } else {
                            editor.remove("cached_login_hash");
                        }
                    } catch (Exception ignored) {
                    }
                    editor.apply();
                    profilePhotoBitmap = null;
                    notifyUser("Profil berhasil diperbarui.");
                    openAccountPage();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    button.setText("Simpan Perubahan");
                    notifyUser(error.getMessage());
                });
            }
        }).start();
    }

    private void openAboutPage() {
        currentSection = "";
        stopNotificationPolling();
        showingHistory = true;
        setSystemBarStyle(false);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFFFFFFFF);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        root.addView(scroll, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(4), dp(18), dp(96));
        scroll.addView(shell, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = horizontal();
        header.setGravity(Gravity.CENTER_VERTICAL);
        shell.addView(header, new LinearLayout.LayoutParams(-1, dp(64)));
        FrameLayout back = new FrameLayout(this);
        back.addView(icon(R.drawable.ic_back, COLOR_TEXT), new FrameLayout.LayoutParams(dp(24), dp(24), Gravity.CENTER));
        back.setOnClickListener(v -> openAccountPage());
        header.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        TextView title = text("Tentang Aplikasi", 18, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        header.addView(new View(this), new LinearLayout.LayoutParams(dp(42), dp(48)));

        LinearLayout card = vertical();
        card.setPadding(dp(18), dp(22), dp(18), dp(22));
        card.setBackground(round(0xFFFFFFFF, dp(12), 0xFFF0F1F3, 1));
        shell.addView(card, matchWrap());
        TextView appIcon = text("Tracer", 34, COLOR_ACCENT, true);
        appIcon.setTypeface(Typeface.create("sans-serif-black", Typeface.NORMAL));
        appIcon.setGravity(Gravity.CENTER);
        card.addView(appIcon, new LinearLayout.LayoutParams(-1, dp(82)));
        TextView appName = text("Tracer", 22, COLOR_TEXT, true);
        appName.setGravity(Gravity.CENTER);
        appName.setPadding(0, dp(12), 0, dp(4));
        card.addView(appName, matchWrap());
        TextView version = text("Versi " + appVersionName(), 13, COLOR_MUTED, false);
        version.setGravity(Gravity.CENTER);
        card.addView(version, matchWrap());

        TextView description = text("Aplikasi absensi karyawan untuk pencatatan clock in, clock out, lokasi, face score, izin, dan riwayat kehadiran.", 14, COLOR_TEXT, false);
        description.setGravity(Gravity.CENTER);
        description.setPadding(0, dp(18), 0, dp(18));
        card.addView(description, matchWrap());
        card.addView(additionalInfoRow("Author", "Parta - asyscntr.com", R.drawable.ic_user));
        card.addView(thinDivider(dp(42)));
        LinearLayout contact = additionalInfoRow("Contact", "+62 878 1589 0193", R.drawable.ic_device);
        contact.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+6287815890193"))));
        card.addView(contact);
        card.addView(thinDivider(dp(42)));
        LinearLayout email = additionalInfoRow("Email", "agungpartabarbara@gmail.com", R.drawable.ic_send);
        email.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:agungpartabarbara@gmail.com"))));
        card.addView(email);

        addFixedBottomNav(root, "Akun");
        setContentView(root);
    }

    private void openAccountPageLegacy() {
        showingHistory = true;
        FrameLayout pageRoot = new FrameLayout(this);
        pageRoot.setBackgroundColor(COLOR_APP_BG);
        LinearLayout shell = vertical();
        shell.setPadding(dp(18), dp(18), dp(18), dp(12));
        pageRoot.addView(shell, new FrameLayout.LayoutParams(-1, -1));

        TextView title = text("Akun", 20, COLOR_TEXT, true);
        title.setGravity(Gravity.CENTER);
        shell.addView(title, matchWrap());

        LinearLayout profile = vertical();
        profile.setGravity(Gravity.CENTER);
        profile.setPadding(dp(16), dp(16), dp(16), dp(18));
        profile.setBackground(redGradient(dp(16)));
        setSoftElevation(profile, 8);
        LinearLayout.LayoutParams profileParams = matchWrap();
        profileParams.setMargins(0, dp(18), 0, dp(12));
        shell.addView(profile, profileParams);

        FrameLayout avatarFrame = new FrameLayout(this);
        avatarFrame.setBackground(round(0xFFFFFFFF, dp(34), 0, 0));
        TextView avatar = text(initialText(), 28, COLOR_ACCENT, true);
        avatar.setGravity(Gravity.CENTER);
        avatarFrame.addView(avatar, new FrameLayout.LayoutParams(-1, -1));
        profile.addView(avatarFrame, new LinearLayout.LayoutParams(dp(68), dp(68)));
        TextView name = text(valueOrDash(fullname), 18, 0xFFFFFFFF, true);
        name.setGravity(Gravity.CENTER);
        name.setPadding(0, dp(10), 0, 0);
        profile.addView(name, matchWrap());
        TextView role = text(displayRole(), 12, 0xEEFFFFFF, false);
        role.setGravity(Gravity.CENTER);
        profile.addView(role, matchWrap());
        TextView pill = text("@" + valueOrDash(username), 11, 0xFFFFFFFF, true);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(dp(12), dp(6), dp(12), dp(6));
        pill.setBackground(round(0x22FFFFFF, dp(16), 0, 0));
        LinearLayout.LayoutParams pillParams = new LinearLayout.LayoutParams(-2, -2);
        pillParams.setMargins(0, dp(8), 0, 0);
        profile.addView(pill, pillParams);

        LinearLayout menu = vertical();
        menu.setPadding(dp(14), dp(8), dp(14), dp(8));
        menu.setBackground(round(0xFFFFFFFF, dp(16), COLOR_STROKE, 1));
        setSoftElevation(menu, 4);
        shell.addView(menu, matchWrap());
        menu.addView(accountRow("Profil Saya", R.drawable.ic_user, COLOR_TEXT));
        menu.addView(accountRow("Pengaturan", R.drawable.ic_building, COLOR_TEXT));
        menu.addView(accountRow("Riwayat Perangkat", R.drawable.ic_send, COLOR_TEXT));
        menu.addView(accountRow("Tentang Aplikasi", R.drawable.ic_clock, COLOR_TEXT));
        TextView logout = accountRow("Keluar", R.drawable.ic_back, COLOR_ERROR);
        logout.setTextColor(COLOR_ERROR);
        menu.addView(logout);

        addBottomNav(shell, "Akun");
        setContentView(pageRoot);
    }

    private void loadNativeHistory(LinearLayout shell, ProgressBar loader, TextView loading) {
        new Thread(() -> {
            try {
                OfflineAttendanceQueue queue = new OfflineAttendanceQueue(this);
                if (queue.pendingCount() > 0) {
                    OfflineSyncJobService.syncPendingNow(this);
                }
                String historyUrl = baseUrl + "/api/attendance_history?month="
                    + new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date())
                    + "&_ts=" + System.currentTimeMillis();
                if (syncToken != null && syncToken.length() > 0) {
                    historyUrl += "&sync_token=" + Uri.encode(syncToken);
                } else if (appToken != null && appToken.length() > 0) {
                    historyUrl += "&token=" + appToken;
                }
                String response = httpGet(historyUrl);
                JSONObject root = new JSONObject(requireJsonObject(response, "/api/attendance_history"));
                if (!root.optBoolean("success", false)) {
                    throw new Exception(root.optString("error", "Gagal memuat riwayat"));
                }
                JSONObject history = root.getJSONObject("history");
                queue.removeEventsAlreadyOnServer(username, history.optJSONArray("rows"));
                if (queue.latestPendingMode(username).trim().isEmpty()) {
                    replaceAttendanceStateFromServer(history.optJSONObject("today"));
                }
                getSharedPreferences("attendance_app", MODE_PRIVATE)
                    .edit()
                    .putString("cached_history_username", username)
                    .putString("cached_history", history.toString())
                    .apply();
                JSONObject mergedHistory = mergePendingHistory(history);
                runOnUiThread(() -> renderNativeHistory(shell, loader, loading, mergedHistory));
            } catch (Exception error) {
                runOnUiThread(() -> {
                    try {
                        SharedPreferences prefs = getSharedPreferences(
                            "attendance_app",
                            MODE_PRIVATE
                        );
                        String cachedOwner = prefs.getString(
                            "cached_history_username",
                            ""
                        );
                        String cachedText = prefs.getString(
                            "cached_history",
                            ""
                        );
                        JSONObject cachedHistory;
                        if (
                            cachedOwner.equalsIgnoreCase(username) &&
                            cachedText.length() > 0
                        ) {
                            cachedHistory = new JSONObject(cachedText);
                        } else {
                            cachedHistory = new JSONObject();
                            cachedHistory.put("present_count", 0);
                            cachedHistory.put("late_count", 0);
                            cachedHistory.put("absent_count", 0);
                            cachedHistory.put("range", "Riwayat offline");
                            cachedHistory.put("rows", new JSONArray());
                        }
                        renderNativeHistory(
                            shell,
                            loader,
                            loading,
                            mergePendingHistory(cachedHistory)
                        );
                        notifyUser("Menampilkan riwayat yang tersimpan di perangkat.");
                    } catch (Exception cacheError) {
                        loader.setVisibility(View.GONE);
                        loading.setText("Riwayat lokal belum tersedia.");
                    }
                });
            }
        }).start();
    }

    private JSONObject mergePendingHistory(JSONObject history) throws Exception {
        JSONObject merged = new JSONObject(history.toString());
        JSONArray serverRows = merged.optJSONArray("rows");
        if (serverRows == null) {
            serverRows = new JSONArray();
        }
        OfflineAttendanceQueue queue = new OfflineAttendanceQueue(this);
        JSONArray pendingRows = queue.pendingHistoryRows(username);
        JSONArray rows = new JSONArray();
        Set<String> seenDates = new HashSet<>();
        JSONObject cachedRow = cachedAttendanceHistoryRow();
        boolean includeCachedRow = cachedRow != null &&
            (hasPendingAttendanceForCurrentUser() || !historyRowsContainToday(serverRows));
        if (includeCachedRow) {
            appendNewestHistoryRow(rows, seenDates, cachedRow);
        }
        for (int index = pendingRows.length() - 1; index >= 0; index--) {
            appendNewestHistoryRow(rows, seenDates, pendingRows.getJSONObject(index));
        }
        for (int index = 0; index < serverRows.length(); index++) {
            appendNewestHistoryRow(rows, seenDates, serverRows.getJSONObject(index));
        }
        merged.put("rows", rows);
        refreshMergedHistorySummary(merged, rows);
        if (pendingRows.length() > 0 || includeCachedRow) {
            merged.put("range", "Termasuk data lokal yang menunggu sinkronisasi");
        }
        return merged;
    }

    private void appendNewestHistoryRow(JSONArray target, Set<String> seenDates, JSONObject row) {
        if (row == null) {
            return;
        }
        String dateKey = row.optString("date_key", "").trim();
        if (dateKey.isEmpty()) {
            dateKey = normalizeAttendanceDate(row.optString("tanggal", ""));
        }
        if (!dateKey.isEmpty()) {
            if (seenDates.contains(dateKey)) {
                return;
            }
            seenDates.add(dateKey);
            try {
                row.put("date_key", dateKey);
            } catch (Exception ignored) {
            }
        }
        target.put(row);
    }

    private void refreshMergedHistorySummary(JSONObject history, JSONArray rows) {
        int presentCount = 0;
        int lateCount = 0;
        int absentCount = 0;
        int leaveCount = 0;
        JSONObject todayRow = new JSONObject();
        String today = todayDate();
        for (int index = 0; index < rows.length(); index += 1) {
            JSONObject row = rows.optJSONObject(index);
            if (row == null) {
                continue;
            }
            String statusClass = normalizedHistoryStatusClass(row);
            if ("present".equals(statusClass) || "late".equals(statusClass) || "pending".equals(statusClass)) {
                presentCount += 1;
                if ("late".equals(statusClass)) {
                    lateCount += 1;
                }
            } else if ("leave".equals(statusClass)) {
                leaveCount += 1;
            } else {
                absentCount += 1;
            }
            if (today.equals(row.optString("date_key", ""))) {
                todayRow = row;
            }
        }
        int measuredDays = presentCount + absentCount;
        int attendanceRate = measuredDays == 0
            ? 0
            : Math.round(presentCount * 100f / measuredDays);
        try {
            history.put("count", rows.length());
            history.put("present_count", presentCount);
            history.put("on_time_count", Math.max(0, presentCount - lateCount));
            history.put("late_count", lateCount);
            history.put("absent_count", absentCount);
            history.put("leave_count", leaveCount);
            history.put("attendance_rate", attendanceRate);
            history.put("today", todayRow);
        } catch (Exception ignored) {
        }
    }

    private boolean historyRowsContainToday(JSONArray rows) {
        try {
            String today = todayDate();
            for (int index = 0; index < rows.length(); index++) {
                JSONObject row = rows.optJSONObject(index);
                if (row == null) {
                    continue;
                }
                String dateKey = row.optString("date_key", "").trim();
                if (dateKey.isEmpty()) {
                    dateKey = normalizeAttendanceDate(row.optString("tanggal", ""));
                }
                if (today.equals(dateKey)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private JSONObject cachedAttendanceHistoryRow() {
        try {
            JSONObject cached = getTodayCachedAttendance(username);
            if (cached == null) {
                return null;
            }
            JSONObject row = new JSONObject();
            String shiftId = cached.optString("shift_id", selectedShiftId);
            String cachedDate = cached.optString("tanggal", todayDate());
            row.put("date_key", normalizeAttendanceDate(cachedDate));
            row.put("tanggal", cachedDate);
            row.put("hari", new SimpleDateFormat("EEEE", new Locale("id", "ID")).format(new Date()));
            row.put("shift", cached.optString("shift_label", "Shift " + shiftId));
            String clockIn = cached.optString("clock_in", "-");
            row.put("jam", clockIn);
            row.put("zona", "");
            row.put("clock_out", cached.optString("clock_out", "-"));
            row.put("lokasi", cached.optString("address", "-"));
            String lat = cached.optString("latitude", "");
            String lon = cached.optString("longitude", "");
            row.put("koordinat", lat.length() > 0 && lon.length() > 0 ? lat + ", " + lon : "");
            row.put("clock_out_lokasi", cached.optString("clock_out_address", cached.optString("address", "-")));
            String outLat = cached.optString("clock_out_latitude", "");
            String outLon = cached.optString("clock_out_longitude", "");
            row.put("clock_out_koordinat", outLat.length() > 0 && outLon.length() > 0 ? outLat + ", " + outLon : "");
            row.put("face_score", cached.optInt("face_score", 0));
            row.put("clock_out_face_score", cached.optInt("clock_out_face_score", 0));
            row.put("photo_url", cached.optString("photo_url", ""));
            row.put("clock_out_photo_url", cached.optString("clock_out_photo_url", ""));
            row.put("device_info", cached.optString("device_info", "-"));
            row.put("clock_out_device_info", cached.optString("clock_out_device_info", "-"));
            row.put("ip_address", cached.optString("ip_address", "-"));
            row.put("clock_out_ip_address", cached.optString("clock_out_ip_address", "-"));
            row.put("app_version", cached.optString("app_version", "-"));
            String cutoff = cached.optString("shift_start", "").trim();
            if (cutoff.isEmpty()) {
                String shiftRange = cached.optString("shift_range", "");
                int separator = shiftRange.indexOf('-');
                cutoff = separator > 0 ? shiftRange.substring(0, separator).trim() : "";
            }
            String computedStatus = attendanceStatus(clockIn, cutoff);
            String statusClass = "Late".equals(computedStatus) ? "late" : "present";
            if (clockIn.trim().isEmpty() || "-".equals(clockIn.trim())) {
                statusClass = "absent";
            }
            row.put("status", "late".equals(statusClass) ? "Late" : ("present".equals(statusClass) ? "On Time" : "Tidak Hadir"));
            row.put("status_class", statusClass);
            return row;
        } catch (Exception ignored) {
            return null;
        }
    }

    private void renderNativeHistory(LinearLayout shell, ProgressBar loader, TextView loading, JSONObject history) {
        loader.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);

        TextView month = text(currentMonthLabel(), 13, COLOR_TEXT, true);
        month.setPadding(0, dp(2), 0, dp(8));
        shell.addView(month, matchWrap());

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        nativeHistoryList = vertical();
        scroll.addView(nativeHistoryList, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        nativeHistoryRows = history.optJSONArray("rows");
        if (nativeHistoryRows == null || nativeHistoryRows.length() == 0) {
            TextView empty = text("Belum ada data riwayat absensi.", 13, COLOR_MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(32), 0, 0);
            nativeHistoryList.addView(empty, matchWrap());
            return;
        }
        applyHistoryFilter(activeHistoryFilter);
    }

    private void renderNativeHistoryLegacy(LinearLayout shell, ProgressBar loader, TextView loading, JSONObject history) {
        loader.setVisibility(View.GONE);
        loading.setVisibility(View.GONE);

        LinearLayout stats = horizontal();
        stats.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statsParams = matchWrap();
        statsParams.setMargins(0, dp(14), 0, dp(10));
        shell.addView(stats, statsParams);
        addHistoryStat(stats, "Hadir", String.valueOf(history.optInt("present_count", 0)), 0xFFDCFCE7, 0xFF059669);
        addHistoryStat(stats, "Telat", String.valueOf(history.optInt("late_count", 0)), 0xFFFFEDD5, 0xFFD97706);
        addHistoryStat(stats, "Absen", String.valueOf(history.optInt("absent_count", 0)), 0xFFFEE2E2, 0xFFDC2626);

        TextView range = text(history.optString("range", ""), 12, COLOR_SUBTLE, false);
        range.setGravity(Gravity.CENTER);
        range.setPadding(0, 0, 0, dp(10));
        shell.addView(range, matchWrap());

        ScrollView scroll = new ScrollView(this);
        LinearLayout list = vertical();
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        shell.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        JSONArray rows = history.optJSONArray("rows");
        if (rows == null || rows.length() == 0) {
            TextView empty = text("Belum ada data riwayat absensi.", 13, COLOR_SUBTLE, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(28), 0, 0);
            list.addView(empty, matchWrap());
            return;
        }

        for (int i = 0; i < rows.length(); i += 1) {
            JSONObject row = rows.optJSONObject(i);
            if (row != null) {
                addHistoryRow(list, row);
            }
        }
    }

    private void addHistoryStat(LinearLayout parent, String label, String value, int bg, int fg) {
        LinearLayout card = vertical();
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(10), dp(8), dp(10));
        card.setBackground(round(COLOR_FIELD, dp(14), COLOR_STROKE, 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        parent.addView(card, params);

        TextView valueView = text(value, 20, fg, true);
        valueView.setGravity(Gravity.CENTER);
        valueView.setBackground(round(bg, dp(16), 0, 0));
        valueView.setPadding(dp(12), dp(5), dp(12), dp(5));
        card.addView(valueView);

        TextView labelView = text(label, 11, COLOR_SUBTLE, true);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, dp(7), 0, 0);
        card.addView(labelView, matchWrap());
    }

    private String normalizedHistoryStatusClass(JSONObject row) {
        String statusClass = row.optString("status_class", "").trim().toLowerCase(Locale.US);
        if ("pending".equals(statusClass)) {
            String clockIn = row.optString("jam", row.optString("clock_in", "")).trim();
            if (clockIn.isEmpty() || "-".equals(clockIn)) {
                return "absent";
            }
            String cutoff = row.optString("shift_start", "").trim();
            if (cutoff.isEmpty()) {
                String shiftRange = row.optString("shift_range", "");
                int separator = shiftRange.indexOf('-');
                cutoff = separator > 0 ? shiftRange.substring(0, separator).trim() : "";
            }
            return "Late".equals(attendanceStatus(clockIn, cutoff)) ? "late" : "present";
        }
        if ("present".equals(statusClass) || "late".equals(statusClass) ||
            "absent".equals(statusClass) || "leave".equals(statusClass)) {
            return statusClass;
        }
        String status = row.optString("status", "").trim().toLowerCase(Locale.US);
        if (status.contains("late") || status.contains("terlambat")) {
            return "late";
        }
        if (status.contains("tidak hadir")) {
            return "absent";
        }
        if (status.contains("on time") || status.contains("hadir")) {
            return "present";
        }
        if (status.contains("izin") || status.contains("sakit")) {
            return "leave";
        }
        return "absent";
    }

    private String historyStatusLabel(JSONObject row, String statusClass) {
        if ("present".equals(statusClass)) {
            return "On Time";
        }
        if ("late".equals(statusClass)) {
            return "Late";
        }
        if ("leave".equals(statusClass)) {
            return row.optString("status", "Izin");
        }
        return "Tidak Hadir";
    }

    private int historyStatusColor(String statusClass) {
        if ("present".equals(statusClass)) {
            return COLOR_SUCCESS;
        }
        if ("late".equals(statusClass)) {
            return 0xFFEA580C;
        }
        if ("leave".equals(statusClass)) {
            return 0xFF2563EB;
        }
        return COLOR_ERROR;
    }

    private int historyStatusBackground(String statusClass) {
        if ("present".equals(statusClass)) {
            return COLOR_SOFT_GREEN;
        }
        if ("late".equals(statusClass)) {
            return COLOR_SOFT_ORANGE;
        }
        if ("leave".equals(statusClass)) {
            return 0xFFDBEAFE;
        }
        return COLOR_SOFT_RED;
    }

    private void addHistoryRow(LinearLayout parent, JSONObject row) {
        LinearLayout card = horizontal();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(0, dp(10), 0, dp(10));
        card.setBackgroundColor(0xFFFFFFFF);
        card.setOnClickListener(v -> openAttendanceDetail(row));
        parent.addView(card, new LinearLayout.LayoutParams(-1, dp(76)));

        LinearLayout dateBox = vertical();
        dateBox.setGravity(Gravity.CENTER);
        dateBox.setBackground(round(0xFFFFFFFF, dp(5), COLOR_STROKE, 1));
        card.addView(dateBox, new LinearLayout.LayoutParams(dp(44), dp(54)));

        String tanggal = row.optString("tanggal", "-");
        String dateKey = row.optString("date_key", "");
        if (dateKey.length() < 10) {
            dateKey = normalizeAttendanceDate(tanggal);
        }
        String dayNumber = dateKey.length() >= 10 ? dateKey.substring(8, 10) : "";
        if (dayNumber.isEmpty()) {
            Matcher dayMatcher = Pattern.compile("^(\\d{1,2})(?:\\D|$)").matcher(tanggal);
            dayNumber = dayMatcher.find() ? dayMatcher.group(1) : "-";
        }
        String dayName = row.optString("hari", "-");
        String dayShort = dayName.length() >= 3 ? dayName.substring(0, 3).toUpperCase(new Locale("id", "ID")) : dayName.toUpperCase(new Locale("id", "ID"));
        TextView dayBadge = text(dayShort, 9, 0xFFFFFFFF, true);
        dayBadge.setGravity(Gravity.CENTER);
        String statusClass = normalizedHistoryStatusClass(row);
        int badgeBg = historyStatusColor(statusClass);
        dayBadge.setBackground(round(badgeBg, dp(4), 0, 0));
        dateBox.addView(dayBadge, new LinearLayout.LayoutParams(-1, dp(20)));
        TextView number = text(dayNumber, 18, COLOR_TEXT, true);
        number.setGravity(Gravity.CENTER);
        dateBox.addView(number, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout details = vertical();
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(0, -2, 1);
        detailsParams.setMargins(dp(12), 0, dp(8), 0);
        card.addView(details, detailsParams);
        details.addView(text(tanggal, 11, COLOR_TEXT, false));

        LinearLayout times = horizontal();
        times.setPadding(0, dp(5), 0, 0);
        details.addView(times, matchWrap());
        LinearLayout inStack = vertical();
        inStack.addView(text("Masuk", 10, COLOR_SUCCESS, true));
        inStack.addView(text(row.optString("jam", "-"), 10, COLOR_MUTED, false));
        times.addView(inStack, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout outStack = vertical();
        outStack.addView(text("Pulang", 10, COLOR_TEXT, false));
        outStack.addView(text(row.optString("clock_out", "-"), 10, COLOR_MUTED, false));
        times.addView(outStack, new LinearLayout.LayoutParams(0, -2, 1));

        int statusBg = historyStatusBackground(statusClass);
        int statusFg = historyStatusColor(statusClass);
        String statusLabel = historyStatusLabel(row, statusClass);
        TextView status = text(statusLabel, 10, statusFg, true);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(11), dp(7), dp(11), dp(7));
        status.setBackground(round(statusBg, dp(14), 0, 0));
        card.addView(status);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFEAECF0);
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void addHistoryRowLegacy(LinearLayout parent, JSONObject row) {
        LinearLayout card = horizontal();
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(0), dp(10), dp(0), dp(10));
        card.setBackgroundColor(0x00FFFFFF);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, dp(2));
        parent.addView(card, params);

        LinearLayout dateBox = vertical();
        dateBox.setGravity(Gravity.CENTER);
        dateBox.setBackground(round(0xFFFFFFFF, dp(5), COLOR_STROKE, 1));
        card.addView(dateBox, new LinearLayout.LayoutParams(dp(48), dp(56)));
        String tanggal = row.optString("tanggal", "-");
        String dayNumber = tanggal.replaceAll("[^0-9]", "");
        if (dayNumber.length() > 2) {
            dayNumber = dayNumber.substring(0, 2);
        }
        TextView dayBadge = text(row.optString("hari", "-").length() >= 3 ? row.optString("hari", "-").substring(0, 3).toUpperCase(Locale.US) : row.optString("hari", "-"), 10, 0xFFFFFFFF, true);
        dayBadge.setGravity(Gravity.CENTER);
        dayBadge.setBackground(round(COLOR_ACCENT, dp(4), 0, 0));
        dateBox.addView(dayBadge, new LinearLayout.LayoutParams(-1, dp(20)));
        TextView number = text(dayNumber.length() > 0 ? dayNumber : "-", 18, COLOR_TEXT, true);
        number.setGravity(Gravity.CENTER);
        dateBox.addView(number, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout dateStack = vertical();
        LinearLayout.LayoutParams stackParams = new LinearLayout.LayoutParams(0, -2, 1);
        stackParams.setMargins(dp(12), 0, dp(8), 0);
        card.addView(dateStack, stackParams);
        dateStack.addView(text(row.optString("tanggal", "-"), 12, COLOR_TEXT, false));
        TextView inLine = text("Masuk   " + row.optString("jam", "-"), 11, COLOR_SUCCESS, true);
        inLine.setPadding(0, dp(3), 0, 0);
        dateStack.addView(inLine);
        TextView outLine = text("Pulang  " + row.optString("clock_out", "-"), 11, COLOR_TEXT, false);
        outLine.setPadding(0, dp(2), 0, 0);
        dateStack.addView(outLine);

        String statusClass = row.optString("status_class", "absent");
        int badgeBg = "present".equals(statusClass) ? 0xFFDCFCE7 : ("late".equals(statusClass) ? 0xFFFFEDD5 : 0xFFFEE2E2);
        int badgeFg = "present".equals(statusClass) ? 0xFF059669 : ("late".equals(statusClass) ? 0xFFD97706 : 0xFFDC2626);
        TextView status = text(row.optString("status", "-"), 11, badgeFg, true);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(10), dp(6), dp(10), dp(6));
        status.setBackground(round(badgeBg, dp(16), 0, 0));
        card.addView(status);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFE5E7EB);
        parent.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
    }

    private void addHistoryLine(LinearLayout parent, String label, String value) {
        TextView line = text(label + "\n" + value, 12, COLOR_MUTED, false);
        line.setPadding(0, dp(8), 0, 0);
        parent.addView(line, matchWrap());
    }

    private void requestGps() {
        // Radius boleh dilewati, tetapi koordinat asli tetap harus direkam.
        // gpsLocked baru menjadi true setelah setLocation menerima lokasi perangkat.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestRuntimePermissions();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        gpsLocked = false;
        locationText.setText("Mengambil lokasi...");
        gpsBadge.setText("Mencari GPS...");
        if (attendanceLocationAreaBadge != null) {
            attendanceLocationAreaBadge.setText("Mencari...");
            attendanceLocationAreaBadge.setTextColor(COLOR_MUTED);
            attendanceLocationAreaBadge.setBackground(round(0xFFF2F4F7, dp(18), 0, 0));
        }

        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                setLocation(location);
                if (location.hasAccuracy() && location.getAccuracy() <= MAX_LOCATION_ACCURACY_METERS) {
                    manager.removeUpdates(this);
                }
            }

            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onProviderDisabled(String provider) {}
        };

        try {
            Location last = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last == null) {
                last = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (last != null) {
                long locationAge = Math.abs(System.currentTimeMillis() - last.getTime());
                if (locationAge <= 120000L) {
                    setLocation(last);
                    if (gpsLocked) {
                        return;
                    }
                }
            }
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, listener);
            manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1, listener);
        } catch (Exception error) {
            progressBar.setVisibility(View.GONE);
            locationText.setText("GPS Error : " + error.getMessage());
            gpsBadge.setText("GPS Nonaktif");
        }
    }

    private void setLocation(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        locationAccuracyMeters = location.hasAccuracy()
            ? Math.max(0f, location.getAccuracy())
            : Float.MAX_VALUE;
        distanceFromAttendanceCenter = Float.MAX_VALUE;
        activeAttendanceRadiusMeters = ATTENDANCE_RADIUS_METERS;
        nearestAttendanceLocationName = "Kantor Utama";
        if (attendanceLocations != null && attendanceLocations.length() > 0) {
            for (int index = 0; index < attendanceLocations.length(); index += 1) {
                JSONObject point = attendanceLocations.optJSONObject(index);
                if (point == null) {
                    continue;
                }
                double pointLatitude = point.optDouble("latitude", Double.NaN);
                double pointLongitude = point.optDouble("longitude", Double.NaN);
                if (Double.isNaN(pointLatitude) || Double.isNaN(pointLongitude)) {
                    continue;
                }
                float[] candidateDistance = new float[1];
                Location.distanceBetween(
                    latitude,
                    longitude,
                    pointLatitude,
                    pointLongitude,
                    candidateDistance
                );
                if (candidateDistance[0] < distanceFromAttendanceCenter) {
                    distanceFromAttendanceCenter = candidateDistance[0];
                    activeAttendanceRadiusMeters = Math.max(
                        20f,
                        (float) point.optDouble("radius", ATTENDANCE_RADIUS_METERS)
                    );
                    nearestAttendanceLocationName = point.optString("name", "Lokasi Absensi");
                }
            }
        }
        if (distanceFromAttendanceCenter == Float.MAX_VALUE) {
            float[] fallbackDistance = new float[1];
            Location.distanceBetween(
                latitude,
                longitude,
                ATTENDANCE_CENTER_LATITUDE,
                ATTENDANCE_CENTER_LONGITUDE,
                fallbackDistance
            );
            distanceFromAttendanceCenter = fallbackDistance[0];
        }
        boolean accurateEnough = locationAccuracyMeters <= MAX_LOCATION_ACCURACY_METERS;
        gpsLocked = disableLocationLock || (accurateEnough && distanceFromAttendanceCenter <= activeAttendanceRadiusMeters);
        address = latitude + ", " + longitude;
        locationSummary = "Koordinat\n" + address;

        progressBar.setVisibility(View.GONE);
        locationText.setText(String.format(Locale.US, "%.6f, %.6f", latitude, longitude));
        String areaLabel = disableLocationLock
            ? "Lock Lokasi Nonaktif"
            : (gpsLocked ? "Dalam Area - " + nearestAttendanceLocationName
            : (accurateEnough ? "Di Luar Area" : "Akurasi Rendah"));
        int areaColor = gpsLocked ? 0xFF16833B : COLOR_ERROR;
        int areaBackground = gpsLocked ? COLOR_SOFT_GREEN : COLOR_SOFT_RED;
        gpsBadge.setText(areaLabel);
        if (readyBadge != null) {
            readyBadge.setText(areaLabel);
            readyBadge.setTextColor(areaColor);
            readyBadge.setBackground(round(areaBackground, dp(16), 0, 0));
        }
        if (attendanceLocationAreaBadge != null) {
            attendanceLocationAreaBadge.setText(areaLabel);
            attendanceLocationAreaBadge.setTextColor(areaColor);
            attendanceLocationAreaBadge.setBackground(round(areaBackground, dp(18), 0, 0));
        }
        gpsAddress.setText(address);
        gpsCoordinate.setText(String.format(Locale.US, "%.6f, %.6f", latitude, longitude));
        gpsSummary.setText(locationSummary);
        if (attendanceLocationAddress != null) {
            attendanceLocationAddress.setText(address == null || address.trim().isEmpty()
                ? String.format(Locale.US, "%.6f, %.6f", latitude, longitude)
                : address);
        }
        if (attendanceLocationAccuracy != null) {
            String accuracyLabel = locationAccuracyMeters == Float.MAX_VALUE
                ? "tidak tersedia"
                : String.format(Locale.US, "%.0f meter", locationAccuracyMeters);
            attendanceLocationAccuracy.setText(
                "Akurasi GPS: " + accuracyLabel +
                "  |  " + nearestAttendanceLocationName + ": " +
                formatLocationDistance(distanceFromAttendanceCenter)
            );
        }
        if (mapView != null) {
            loadMapInto(mapView, latitude, longitude);
        }
        resolveLocationAddressAsync(latitude, longitude);
    }

    private void resolveLocationAddressAsync(double targetLatitude, double targetLongitude) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("id", "ID"));
                List<Address> addresses = geocoder.getFromLocation(
                    targetLatitude,
                    targetLongitude,
                    1
                );
                if (addresses == null || addresses.isEmpty()) {
                    return;
                }
                Address data = addresses.get(0);
                final String resolvedAddress = valueOrDash(data.getAddressLine(0));
                final String resolvedSummary =
                    "Provinsi\n" + valueOrDash(data.getAdminArea()) +
                    "\n\nKota\n" + valueOrDash(data.getSubAdminArea()) +
                    "\n\nKecamatan\n" + valueOrDash(data.getLocality());
                runOnUiThread(() -> {
                    if (Math.abs(latitude - targetLatitude) > 0.000001 ||
                        Math.abs(longitude - targetLongitude) > 0.000001) {
                        return;
                    }
                    address = resolvedAddress;
                    locationSummary = resolvedSummary;
                    if (attendanceLocationAddress != null) {
                        attendanceLocationAddress.setText(resolvedAddress);
                    }
                    if (gpsAddress != null) {
                        gpsAddress.setText(resolvedAddress);
                    }
                    if (gpsSummary != null) {
                        gpsSummary.setText(resolvedSummary);
                    }
                });
            } catch (Exception ignored) {
            }
        }).start();
    }

    private String locationLockMessage() {
        if (disableLocationLock) {
            return "Menunggu koordinat GPS. Radius lokasi tidak dikunci untuk akun ini.";
        }
        if (locationAccuracyMeters == Float.MAX_VALUE) {
            return "Menunggu lokasi GPS yang akurat.";
        }
        if (locationAccuracyMeters > MAX_LOCATION_ACCURACY_METERS) {
            return String.format(
                Locale.US,
                "Akurasi GPS masih %.0f meter. Tunggu sampai maksimal %.0f meter.",
                locationAccuracyMeters,
                MAX_LOCATION_ACCURACY_METERS
            );
        }
        if (distanceFromAttendanceCenter > activeAttendanceRadiusMeters) {
            return String.format(
                Locale.US,
                "Anda di luar area %s. Jarak %.0f meter, maksimal %.0f meter.",
                nearestAttendanceLocationName,
                distanceFromAttendanceCenter,
                activeAttendanceRadiusMeters
            );
        }
        return "Menunggu lokasi GPS ditemukan.";
    }

    private String formatLocationDistance(float distanceMeters) {
        if (distanceMeters == Float.MAX_VALUE || Float.isNaN(distanceMeters)) {
            return "-";
        }
        if (distanceMeters < 1000f) {
            return String.format(Locale.US, "%.0f meter", distanceMeters);
        }
        return String.format(new Locale("id", "ID"), "%.2f km", distanceMeters / 1000f);
    }

    private void loadMapInto(WebView target, double mapLatitude, double mapLongitude) {
        if (target == null) {
            return;
        }
        String mapUrl = String.format(
            Locale.US,
            "https://maps.google.com/maps?q=%f,%f&z=16&output=embed",
            mapLatitude,
            mapLongitude
        );
        String mapHtml =
            "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">" +
            "<style>html,body{margin:0;height:100%;overflow:hidden;background:#f1f5f9}iframe{border:0;width:100%;height:100%;display:block}</style>" +
            "</head><body><iframe src=\"" + mapUrl + "\" loading=\"eager\" referrerpolicy=\"no-referrer-when-downgrade\"></iframe></body></html>";
        target.loadDataWithBaseURL("https://maps.google.com/", mapHtml, "text/html", "UTF-8", null);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private String deviceInfo() {
        String maker = (Build.MANUFACTURER + " " + Build.MODEL).trim();
        return maker + " | Android " + Build.VERSION.RELEASE;
    }

    private String appVersionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            return "-";
        }
    }

    private String displayName() {
        String preferred = appDisplayName == null ? "" : appDisplayName.trim();
        String value = valueOrDash(preferred.isEmpty() ? fullname : preferred).trim();
        if (value.length() <= 1 || "-".equals(value)) {
            return value.toUpperCase(new Locale("id", "ID"));
        }
        return value.substring(0, 1).toUpperCase(new Locale("id", "ID")) + value.substring(1);
    }

    private String displayRole() {
        String clean = department == null ? "" : department.trim();
        if (clean.isEmpty()) {
            clean = userRole == null ? "" : userRole.trim().replace('_', ' ');
        }
        if (clean.isEmpty()) {
            return "Karyawan";
        }
        String[] words = clean.toLowerCase(new Locale("id", "ID")).split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(word.substring(0, 1).toUpperCase(new Locale("id", "ID")));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.length() == 0 ? "Karyawan" : result.toString();
    }

    private void loadAppProfile(boolean reopenAccount) {
        if (baseUrl == null || baseUrl.trim().isEmpty() || offlineMode) {
            return;
        }
        new Thread(() -> {
            try {
                JSONObject response = new JSONObject(requireJsonObject(
                    httpGet(baseUrl + "/api/app_profile?_ts=" + System.currentTimeMillis()),
                    "/api/app_profile"
                ));
                if (!response.optBoolean("success", false)) {
                    return;
                }
                JSONObject profile = response.optJSONObject("profile");
                if (profile == null) {
                    return;
                }
                runOnUiThread(() -> {
                    applyProfileData(profile);
                    if (reopenAccount) {
                        openAccountPage();
                    }
                });
            } catch (Exception ignored) {
            }
        }).start();
    }

    private void applyProfileData(JSONObject profile) {
        fullname = profile.optString("fullname", fullname);
        username = profile.optString("username", username);
        userRole = profile.optString("level", userRole);
        appDisplayName = profile.optString("display_name", appDisplayName);
        department = profile.optString("department", department);
        profilePhotoUrl = profile.optString("profile_photo_url", profilePhotoUrl);
        if (homeNameView != null) {
            homeNameView.setText(displayName());
        }
        if (homeRoleView != null) {
            homeRoleView.setText(displayRole());
        }
        applyAccountView();
    }

    private String summaryValue(String label) {
        if (locationSummary == null || locationSummary.trim().isEmpty()) {
            return "-";
        }
        String[] parts = locationSummary.split("\\n+");
        for (int index = 0; index < parts.length - 1; index++) {
            if (label.equalsIgnoreCase(parts[index].trim())) {
                return valueOrDash(parts[index + 1]);
            }
        }
        return "-";
    }

    private void applyAccountView() {
        String cleanName = valueOrDash(fullname);
        String cleanUsername = valueOrDash(username);
        nameText.setText(cleanName);
        usernameText.setText("@" + cleanUsername);
        String initialSource = !"-".equals(cleanName) ? cleanName : cleanUsername;
        String initial = initialSource.length() > 0 && !"-".equals(initialSource)
            ? initialSource.substring(0, 1).toUpperCase(new Locale("id", "ID"))
            : "A";
        avatarText.setText(initial);
        if (avatarImage.getDrawable() == null) {
            avatarText.setVisibility(View.VISIBLE);
        }
    }

    private String absoluteUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "";
        }
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        if (url.startsWith("/") && baseUrl != null && baseUrl.length() > 0) {
            return baseUrl + url;
        }
        return url;
    }

    private String normalizeServerBaseUrl(String url) {
        return SERVER_URL;
    }

    private boolean isSupportedServerBaseUrl(String url) {
        String value = url == null ? "" : url.trim().toLowerCase(Locale.US);
        return value.contains("attendance-api.asyscntr.com");
    }

    private void updateServerText() {
        if (serverText == null) {
            return;
        }
        serverText.setText("Server: attendance-api.asyscntr.com");
    }
    private GradientDrawable appGradientBackground() {
        return new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {0xFFE50914, 0xEEC90012, 0x44FEE2E2, 0x00F8FAFC}
        );
    }

    private void addAppBackground(FrameLayout root) {
        View background = new View(this);
        background.setBackgroundColor(COLOR_APP_BG);
        root.addView(background, new FrameLayout.LayoutParams(-1, -1));

        View overlay = new View(this);
        overlay.setBackground(appGradientBackground());
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
    }

    private void setSoftElevation(View view, int elevationDp) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setElevation(dp(Math.min(elevationDp, 1)));
        }
    }

    private void applySystemBars() {
        setSystemBarStyle(true);
    }

    private void setSystemBarStyle(boolean redStatusBar) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(redStatusBar ? COLOR_ACCENT : 0xFFFFFFFF);
            getWindow().setNavigationBarColor(0xFFFFFFFF);
        }
        int flags = 0;
        if (!redStatusBar && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private boolean isOldServerBaseUrl(String url) {
        String value = url == null ? "" : url.trim().toLowerCase(Locale.US);
        return value.contains("besti.asyscntr.com") || value.contains("look.asyscntr.com") || value.contains("icommerce.asyscntr.com");
    }
    private boolean isLocalIpBaseUrl(String url) {
        String value = url == null ? "" : url.trim();
        return value.startsWith("http://192.168.") ||
            value.startsWith("https://192.168.") ||
            value.startsWith("http://10.") ||
            value.startsWith("https://10.") ||
            value.startsWith("http://172.16.") ||
            value.startsWith("https://172.16.");
    }

    private void loadAvatarPhoto() {
        if (lastPhotoUrl == null || lastPhotoUrl.length() == 0 || lastPhotoUrl.contains("nophoto.png")) {
            avatarImage.setImageDrawable(null);
            avatarText.setVisibility(View.VISIBLE);
            return;
        }

        String photoUrl = lastPhotoUrl;
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(photoUrl).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        avatarImage.setImageBitmap(bitmap);
                        avatarText.setVisibility(View.GONE);
                    } else {
                        avatarText.setVisibility(View.VISIBLE);
                    }
                });
            } catch (Exception ignored) {
                runOnUiThread(() -> avatarText.setVisibility(View.VISIBLE));
            }
        }).start();
    }

    private void loadRemoteImage(ImageView target, String photoUrl) {
        if (target == null || photoUrl == null || photoUrl.trim().isEmpty()) {
            if (target != null) {
                target.setImageDrawable(null);
            }
            return;
        }
        new Thread(() -> {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(photoUrl).openConnection();
                connection.setConnectTimeout(12000);
                connection.setReadTimeout(12000);
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        target.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void updateFaceBadgeColor() {
        int background = 0xFFFEE2E2;
        int foreground = 0xFFB91C1C;
        int stroke = 0xFFFCA5A5;
        if (faceScore >= 85) {
            background = 0xFFDCFCE7;
            foreground = 0xFF047857;
            stroke = 0xFF86EFAC;
        } else if (faceScore >= 80) {
            background = 0xFFFEF3C7;
            foreground = 0xFF92400E;
            stroke = 0xFFFCD34D;
        }
        String quality = faceScore >= 90 ? "Sangat Baik" : (faceScore >= 80 ? "Baik" : "Foto Ulang");
        faceBadge.setText(quality);
        faceBadge.setTextColor(foreground);
        faceBadge.setBackground(round(background, dp(16), stroke, 1));
        if (faceScoreValue != null) {
            faceScoreValue.setTextColor(faceScore >= 80 ? COLOR_SUCCESS : COLOR_ERROR);
        }
    }

    private void updateFaceRingColor() {
        if (faceScoreRing == null) {
            return;
        }
        int ringColor = 0xFFD0D5DD;
        if (selfieBitmap != null) {
            ringColor = faceScore >= 90
                ? COLOR_SUCCESS
                : (faceScore >= 80 ? COLOR_WARNING : COLOR_ERROR);
        }
        faceScoreRing.setScore(selfieBitmap == null ? -1 : faceScore, ringColor, false);
    }

    private void animateFaceScore(int targetScore) {
        int safeScore = Math.max(0, Math.min(100, targetScore));
        int progressColor = safeScore >= 90
            ? COLOR_SUCCESS
            : (safeScore >= 80 ? COLOR_WARNING : COLOR_ERROR);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            faceScoreProgress.setProgressTintList(ColorStateList.valueOf(progressColor));
        }
        faceScoreProgress.setProgress(0);
        faceScoreValue.setText("0%");
        ValueAnimator scoreAnimator = ValueAnimator.ofInt(0, safeScore);
        scoreAnimator.setDuration(900);
        scoreAnimator.setInterpolator(new DecelerateInterpolator());
        scoreAnimator.addUpdateListener(animation -> {
            int animatedValue = (Integer) animation.getAnimatedValue();
            faceScoreValue.setText(animatedValue + "%");
            faceScoreProgress.setProgress(animatedValue);
        });
        scoreAnimator.start();
        if (faceScoreRing != null) {
            faceScoreRing.setScore(safeScore, progressColor, true);
        }
    }

    private int responsiveFaceSize() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        return Math.max(dp(232), Math.min(dp(300), screenWidth - dp(72)));
    }

    private int responsiveSuccessPhotoHeight() {
        return dp(100);
    }

    private void spaceSuccessFields() {
        TextView[] values = {
            successDate, successClockIn, successClockOut, successStatus,
            successFaceScore, successCutoff, successShift, successAddress,
            successCoordinate
        };
        for (TextView value : values) {
            if (value == null) {
                continue;
            }
            ViewParent parent = value.getParent();
            if (parent instanceof LinearLayout) {
                ((LinearLayout) parent).setPadding(0, dp(8), 0, dp(8));
            }
        }
        if (successAddress != null) {
            successAddress.setMaxLines(2);
        }
        if (successCoordinate != null) {
            successCoordinate.setMaxLines(2);
        }
    }

    private void toggleSuccessPhotoSize() {
        successPhotoExpanded = !successPhotoExpanded;
        applySuccessPhotoSize(successPhotoExpanded);
    }

    private void applySuccessPhotoSize(boolean expanded) {
        if (successPhoto == null || !(successPhoto.getLayoutParams() instanceof LinearLayout.LayoutParams)) {
            return;
        }
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) successPhoto.getLayoutParams();
        params.height = expanded
            ? Math.max(dp(320), getResources().getDisplayMetrics().heightPixels - dp(180))
            : responsiveSuccessPhotoHeight();
        successPhoto.setLayoutParams(params);
        successPhoto.setScaleType(expanded ? ImageView.ScaleType.FIT_CENTER : ImageView.ScaleType.CENTER_CROP);
        successPhoto.requestLayout();
        if (!expanded && attendanceContent != null) {
            attendanceContent.post(this::fitSuccessContentToViewport);
        }
    }

    private void fitSuccessContentToViewport() {
        if (successPhotoExpanded || attendanceScroll == null || attendanceContent == null ||
            successPanel == null || successPhoto == null || photoFacePrompt == null) {
            return;
        }
        int viewportHeight = attendanceScroll.getHeight();
        int panelHeight = successPanel.getMeasuredHeight();
        int photoHeight = successPhoto.getMeasuredHeight();
        if (viewportHeight <= 0 || panelHeight <= 0 || photoHeight <= 0) {
            return;
        }
        int panelMargins = 0;
        if (successPanel.getLayoutParams() instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams panelParams = (LinearLayout.LayoutParams) successPanel.getLayoutParams();
            panelMargins = panelParams.topMargin + panelParams.bottomMargin;
        }
        int fixedHeight = attendanceContent.getPaddingTop() + attendanceContent.getPaddingBottom() +
            photoFacePrompt.getMeasuredHeight() + panelMargins + panelHeight - photoHeight;
        int targetPhotoHeight = Math.max(dp(72), viewportHeight - fixedHeight);
        LinearLayout.LayoutParams photoParams = (LinearLayout.LayoutParams) successPhoto.getLayoutParams();
        if (photoParams.height != targetPhotoHeight) {
            photoParams.height = targetPhotoHeight;
            successPhoto.setLayoutParams(photoParams);
            successPhoto.requestLayout();
        }
    }

    private int responsiveLeaveHistoryHeight() {
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        return Math.max(dp(220), Math.min(dp(330), screenHeight - dp(520)));
    }

    private void notifyUser(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        if (statusText != null) {
            statusText.setText(message);
        }
    }

    private void submitAttendance() {
        if (selfieBitmap == null || !gpsLocked) {
            notifyUser(selfieBitmap == null ? "Foto wajib diisi." : locationLockMessage());
            return;
        }
        if (offlineMode) {
            saveAttendanceOffline("Mode offline aktif");
            return;
        }
        progressBar.setVisibility(View.GONE);
        captureButton.setEnabled(false);
        captureButton.setTextSize(11);
        captureButton.setText("MENGIRIM...");
        statusText.setText("Mengirim absensi...");

        new Thread(() -> {
            try {
                String endpoint = baseUrl + (isClockOut() ? "/api/attendance_clock_out" : "/api/attendance");
                String response = postMultipart(endpoint);
                boolean success = response.contains("\"success\":true") || response.contains("\"success\": true");
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    captureButton.setEnabled(true);
                    captureButton.setTextSize(12);
                    captureButton.setText(isClockOut() ? "KIRIM CLOCK OUT" : "KIRIM ABSENSI");
                    if (success) {
                        showSuccess(response);
                    } else {
                        statusText.setText("Gagal: " + response);
                    }
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    captureButton.setEnabled(true);
                    captureButton.setTextSize(12);
                    captureButton.setText(isClockOut() ? "KIRIM CLOCK OUT" : "KIRIM ABSENSI");
                    saveAttendanceOffline(error.getMessage());
                });
            }
        }).start();
    }

    private void saveAttendanceOffline(String networkError) {
        try {
            boolean savingClockOut = isClockOut();
            String capturedAt = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.US
            ).format(new Date());
            JSONObject previousAttendance = getTodayCachedAttendance(username);
            OfflineAttendanceQueue queue = new OfflineAttendanceQueue(this);
            queue.enqueue(
                baseUrl,
                syncToken == null ? "" : syncToken,
                username,
                fullname,
                savingClockOut ? "clock_out" : "clock_in",
                selectedShiftId,
                capturedAt,
                latitude,
                longitude,
                address,
                faceScore,
                deviceInfo(),
                appVersionName(),
                selfieBitmap
            );

            JSONObject localAttendance = new JSONObject();
            localAttendance.put("username", username);
            localAttendance.put("fullname", fullname);
            localAttendance.put("tanggal", capturedAt.substring(0, 10));
            localAttendance.put(
                "clock_in",
                savingClockOut && previousAttendance != null
                    ? previousAttendance.optString("clock_in", "-")
                    : capturedAt.substring(11)
            );
            localAttendance.put("clock_out", savingClockOut ? capturedAt.substring(11) : "");
            localAttendance.put("address", address);
            localAttendance.put("latitude", String.valueOf(latitude));
            localAttendance.put("longitude", String.valueOf(longitude));
            localAttendance.put(
                "face_score",
                savingClockOut && previousAttendance != null
                    ? previousAttendance.optInt("face_score", faceScore)
                    : faceScore
            );
            localAttendance.put("device_info", deviceInfo());
            localAttendance.put("app_version", appVersionName());
            if (savingClockOut) {
                localAttendance.put("clock_out_face_score", faceScore);
                localAttendance.put("clock_out_device_info", deviceInfo());
            }
            localAttendance.put("shift_id", selectedShiftId);
            localAttendance.put(
                "shift_label",
                "2".equals(selectedShiftId) ? "Shift 2" : "Shift 1"
            );
            String localShiftStart = "2".equals(selectedShiftId)
                ? shift2ClockIn
                : shift1ClockIn;
            String localShiftEnd = "2".equals(selectedShiftId)
                ? shift2ClockOut
                : shift1ClockOut;
            if (localShiftStart.length() > 0) {
                localAttendance.put("shift_start", localShiftStart);
            }
            if (localShiftStart.length() > 0 && localShiftEnd.length() > 0) {
                localAttendance.put(
                    "shift_range",
                    localShiftStart + " - " + localShiftEnd
                );
            }
            cacheAttendanceState(localAttendance);
            renderAttendanceSuccess(localAttendance, true);
            statusText.setText(
                "Tersimpan lokal. " + queue.pendingCount() +
                " data menunggu server online."
            );
            refreshOfflineSyncStatus(false);
            startOfflineSyncPolling();
            notifyUser("Absensi berhasil disimpan. Sinkronisasi menunggu server online.");
        } catch (Exception queueError) {
            statusText.setText("Gagal menyimpan lokal: " + queueError.getMessage());
            notifyUser("Data belum tersimpan. Jangan tutup aplikasi.");
        }
    }

    private String httpGet(String urlText) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlText).openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setUseCaches(false);
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(12000);
        connection.setRequestProperty("Cache-Control", "no-cache, no-store");
        connection.setRequestProperty("Pragma", "no-cache");
        connection.setRequestProperty("User-Agent", "AttendanceApp/1.0 Android");
        return readStream(connection.getInputStream());
    }

    private String postJson(String endpoint, String json) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "AttendanceApp/1.0 Android");
        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
        output.write(json.getBytes("UTF-8"));
        output.flush();
        output.close();
        InputStream stream = connection.getResponseCode() >= 400
            ? connection.getErrorStream()
            : connection.getInputStream();
        return readStream(stream);
    }

    private String requireJsonObject(String response, String endpointName) throws Exception {
        String clean = response == null ? "" : response.trim();
        if (clean.length() == 0) {
            throw new Exception("Server tidak mengirim data. Coba lagi.");
        }
        String lower = clean.toLowerCase(Locale.US);
        if (lower.startsWith("<!doctype") || lower.startsWith("<html")) {
            throw new Exception(
                "Server domain belum memakai endpoint APK " + endpointName
                    + ". Restart atau deploy look.py terbaru di server apk.asyscntr.com."
            );
        }
        if (!clean.startsWith("{")) {
            String preview = clean.length() > 80 ? clean.substring(0, 80) : clean;
            throw new Exception("Respons server tidak valid: " + preview);
        }
        return clean;
    }

    private String postMultipart(String endpoint) throws Exception {
        String boundary = "AttendanceBoundary" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "AttendanceApp/1.0 Android");

        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
        writeField(output, boundary, "latitude", String.valueOf(latitude));
        writeField(output, boundary, "longitude", String.valueOf(longitude));
        writeField(output, boundary, "address", address);
        if (!isClockOut()) {
            writeField(output, boundary, "shift_id", selectedShiftId);
        }
        writeField(output, boundary, "face_score", String.valueOf(faceScore));
        writeField(output, boundary, "device_info", deviceInfo());
        writeField(output, boundary, "app_version", appVersionName());

        ByteArrayOutputStream photoBytes = new ByteArrayOutputStream();
        selfieBitmap.compress(Bitmap.CompressFormat.JPEG, 88, photoBytes);
        writeFile(output, boundary, "photo", isClockOut() ? "clock-out.jpg" : "attendance.jpg", photoBytes.toByteArray());

        output.writeBytes("--" + boundary + "--\r\n");
        output.flush();
        output.close();

        InputStream stream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
        return readStream(stream);
    }

    private String postLeaveMultipart(String type, String date, String note) throws Exception {
        String boundary = "LeaveBoundary" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + "/api/attendance_leave").openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "AttendanceApp/1.0 Android");
        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
        writeField(output, boundary, "type", type);
        writeField(output, boundary, "tanggal", date);
        writeField(output, boundary, "keterangan", note);
        if (leaveAttachmentBitmap != null) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            leaveAttachmentBitmap.compress(Bitmap.CompressFormat.JPEG, 88, bytes);
            writeFile(output, boundary, "photo", "lampiran-izin.jpg", bytes.toByteArray());
        }
        output.writeBytes("--" + boundary + "--\r\n");
        output.flush();
        output.close();
        InputStream stream = connection.getResponseCode() >= 400
            ? connection.getErrorStream()
            : connection.getInputStream();
        return readStream(stream);
    }

    private String postProfileMultipart(String displayNameValue, String usernameValue, String passwordValue, String departmentValue) throws Exception {
        String boundary = "ProfileBoundary" + System.currentTimeMillis();
        HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + "/api/app_profile").openConnection();
        connection.setConnectTimeout(12000);
        connection.setReadTimeout(15000);
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setRequestProperty("User-Agent", "AttendanceApp/1.0 Android");
        DataOutputStream output = new DataOutputStream(connection.getOutputStream());
        writeField(output, boundary, "display_name", displayNameValue);
        writeField(output, boundary, "username", usernameValue);
        writeField(output, boundary, "password", passwordValue == null ? "" : passwordValue);
        writeField(output, boundary, "department", departmentValue);
        if (profilePhotoBitmap != null) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            profilePhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
            writeFile(output, boundary, "photo", "profile.jpg", bytes.toByteArray());
        }
        output.writeBytes("--" + boundary + "--\r\n");
        output.flush();
        output.close();
        InputStream stream = connection.getResponseCode() >= 400
            ? connection.getErrorStream()
            : connection.getInputStream();
        return readStream(stream);
    }

    private String readStream(InputStream stream) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line).append("\n");
        }
        return builder.toString();
    }

    private String parseHtml(String html, String pattern) {
        Matcher matcher = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(html == null ? "" : html);
        return matcher.find() ? matcher.group(1).trim() : "";
    }

    private void writeField(DataOutputStream output, String boundary, String name, String value) throws Exception {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        output.write(value.getBytes("UTF-8"));
        output.writeBytes("\r\n");
    }

    private void writeFile(DataOutputStream output, String boundary, String name, String filename, byte[] bytes) throws Exception {
        output.writeBytes("--" + boundary + "\r\n");
        output.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n");
        output.writeBytes("Content-Type: image/jpeg\r\n\r\n");
        output.write(bytes);
        output.writeBytes("\r\n");
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private LinearLayout horizontal() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        return layout;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        if (bold) {
            textView.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        }
        return textView;
    }

    private Button button(String value, int bg, int fg, int stroke) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(fg);
        button.setTextSize(12);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setGravity(Gravity.CENTER);
        button.setPadding(0, 0, 0, 0);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumHeight(0);
        button.setMinimumWidth(0);
        button.setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            button.setStateListAnimator(null);
            button.setElevation(0);
        }
        button.setBackground(round(bg, dp(14), stroke, stroke == 0 ? 0 : 1));
        return button;
    }

    private Button button(String value, int bg, int fg, int stroke, int iconRes) {
        Button button = button(value, bg, fg, stroke);
        button.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        button.setCompoundDrawablePadding(dp(8));
        return button;
    }

    private ImageView icon(int iconRes) {
        return icon(iconRes, COLOR_MUTED);
    }

    private ImageView icon(int iconRes, int color) {
        ImageView iconView = new ImageView(this);
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(color);
        return iconView;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams actionButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(52), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams shiftButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
        params.setMargins(dp(3), 0, dp(3), 0);
        return params;
    }

    private LinearLayout.LayoutParams fullButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.setMargins(0, dp(8), 0, 0);
        return params;
    }

    private View navItem(String label, int iconRes, boolean active, View.OnClickListener listener) {
        LinearLayout item = vertical();
        item.setGravity(Gravity.CENTER);
        item.setBackgroundColor(0x00FFFFFF);
        item.setOnClickListener(listener);

        ImageView iconView = icon(iconRes, active ? COLOR_ACCENT : COLOR_SUBTLE);
        item.addView(iconView, new LinearLayout.LayoutParams(dp(24), dp(24)));

        TextView labelView = text(label, 11, active ? COLOR_ACCENT : COLOR_SUBTLE, active);
        labelView.setGravity(Gravity.CENTER);
        labelView.setPadding(0, dp(5), 0, 0);
        item.addView(labelView, matchWrap());
        return item;
    }

    private void addFixedBottomNav(FrameLayout root, String active) {
        FrameLayout navShell = new FrameLayout(this);
        navShell.setBackgroundColor(0xFFFFFFFF);
        setSoftElevation(navShell, 10);
        FrameLayout.LayoutParams shellParams = new FrameLayout.LayoutParams(-1, dp(82), Gravity.BOTTOM);
        root.addView(navShell, shellParams);

        View divider = new View(this);
        divider.setBackgroundColor(0xFFEAECF0);
        FrameLayout.LayoutParams dividerParams = new FrameLayout.LayoutParams(-1, dp(1), Gravity.TOP);
        dividerParams.setMargins(dp(18), 0, dp(18), 0);
        navShell.addView(divider, dividerParams);

        LinearLayout nav = horizontal();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(7), dp(5), dp(7), dp(5));
        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(-1, -1);
        navShell.addView(nav, navParams);
        nav.addView(navItem("Beranda", R.drawable.ic_home, "Beranda".equals(active), v -> openHomePage()), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem("Riwayat", R.drawable.ic_clock, "Riwayat".equals(active), v -> openHistory()), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem("Izin", R.drawable.ic_clipboard, "Izin".equals(active), v -> openLeavePage()), new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navItem("Akun", R.drawable.ic_user, "Akun".equals(active), v -> openAccountPage()), new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void addBottomNav(LinearLayout shell, String active) {
        View spacer = new View(this);
        shell.addView(spacer, new LinearLayout.LayoutParams(-1, 0, 1));

        View divider = new View(this);
        divider.setBackgroundColor(0xFFF1F3F6);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(-1, dp(1));
        dividerParams.setMargins(dp(10), 0, dp(10), 0);
        shell.addView(divider, dividerParams);

        LinearLayout nav = horizontal();
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(6), dp(9), dp(6), dp(8));
        nav.setBackgroundColor(0x00FFFFFF);
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, 0, 0, 0);
        shell.addView(nav, params);
        nav.addView(navItem("Beranda", R.drawable.ic_home, "Beranda".equals(active), v -> openHomePage()), new LinearLayout.LayoutParams(0, dp(74), 1));
        nav.addView(navItem("Riwayat", R.drawable.ic_clock, "Riwayat".equals(active), v -> openHistory()), new LinearLayout.LayoutParams(0, dp(74), 1));
        nav.addView(navItem("Izin", R.drawable.ic_clipboard, "Izin".equals(active), v -> openLeavePage()), new LinearLayout.LayoutParams(0, dp(74), 1));
        nav.addView(navItem("Akun", R.drawable.ic_user, "Akun".equals(active), v -> openAccountPage()), new LinearLayout.LayoutParams(0, dp(74), 1));
    }

    private TextView dummyField(String label, String value) {
        TextView field = text(label + "\n" + value, 13, COLOR_TEXT, false);
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        field.setBackground(round(0xFFFFFFFF, dp(12), COLOR_STROKE, 1));
        LinearLayout.LayoutParams params = matchWrap();
        params.setMargins(0, dp(8), 0, 0);
        field.setLayoutParams(params);
        return field;
    }

    private LinearLayout leaveField(String label, String value) {
        LinearLayout field = vertical();
        TextView labelView = text(label, 11, COLOR_TEXT, true);
        labelView.setPadding(0, dp(10), 0, dp(7));
        field.addView(labelView);
        TextView valueView = text(value, 12, COLOR_MUTED, false);
        valueView.setGravity(Gravity.CENTER_VERTICAL);
        valueView.setPadding(dp(13), 0, dp(13), 0);
        valueView.setBackground(round(0xFFF9FAFB, dp(9), COLOR_STROKE, 1));
        field.addView(valueView, new LinearLayout.LayoutParams(-1, dp(48)));
        return field;
    }

    private TextView accountRow(String label, int iconRes, int color) {
        TextView row = text(label + "                                      >", 13, color, false);
        row.setPadding(dp(4), dp(14), dp(4), dp(14));
        row.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0);
        row.setCompoundDrawablePadding(dp(12));
        return row;
    }

    private LinearLayout accountMenuRow(String label, int iconRes, int color, View.OnClickListener listener) {
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), 0, dp(14), 0);
        if (listener != null) {
            row.setOnClickListener(listener);
        }
        ImageView leading = icon(iconRes, color);
        row.addView(leading, new LinearLayout.LayoutParams(dp(24), dp(24)));
        TextView labelView = text(label, 14, color, "Keluar".equals(label));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
        labelParams.setMargins(dp(13), 0, dp(8), 0);
        row.addView(labelView, labelParams);
        row.addView(icon(R.drawable.ic_chevron_right, COLOR_TEXT), new LinearLayout.LayoutParams(dp(20), dp(20)));
        row.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(64)));
        return row;
    }

    private String initialText() {
        String cleanName = valueOrDash(displayName());
        String cleanUsername = valueOrDash(username);
        String source = !"-".equals(cleanName) ? cleanName : cleanUsername;
        return source.length() > 0 && !"-".equals(source)
            ? source.substring(0, 1).toUpperCase(new Locale("id", "ID"))
            : "A";
    }

    private GradientDrawable blueGradient(int radius) {
        return redGradient(radius);
    }

    private GradientDrawable redGradient(int radius) {
        GradientDrawable drawable = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {COLOR_ACCENT, COLOR_ACCENT_DARK}
        );
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private GradientDrawable softRedPanel(int radius) {
        GradientDrawable drawable = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[] {0xFFFFF1F2, 0xFFFFE4E6}
        );
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private class FaceScoreRingView extends View {
        private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private float sweepAngle = 0f;
        private ValueAnimator animator;

        FaceScoreRingView(Context context) {
            super(context);
            setClickable(false);
            trackPaint.setStyle(Paint.Style.STROKE);
            trackPaint.setStrokeWidth(dp(5));
            trackPaint.setColor(0xFFD0D5DD);
            progressPaint.setStyle(Paint.Style.STROKE);
            progressPaint.setStrokeWidth(dp(5));
            progressPaint.setStrokeCap(Paint.Cap.ROUND);
            progressPaint.setColor(COLOR_SUCCESS);
        }

        void setScore(int score, int color, boolean animated) {
            float targetSweep = score < 0 ? 0f : 360f;
            progressPaint.setColor(color);
            if (animator != null) {
                animator.cancel();
            }
            if (!animated) {
                sweepAngle = targetSweep;
                invalidate();
                return;
            }
            animator = ValueAnimator.ofFloat(0f, targetSweep);
            animator.setDuration(1000);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                sweepAngle = (Float) animation.getAnimatedValue();
                invalidate();
            });
            animator.start();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float inset = dp(4);
            bounds.set(inset, inset, getWidth() - inset, getHeight() - inset);
            canvas.drawArc(bounds, 0f, 360f, false, trackPaint);
            if (sweepAngle > 0f) {
                canvas.drawArc(bounds, 90f, sweepAngle, false, progressPaint);
            }
        }
    }

    private class LoginHeaderView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint logisticsPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Path path = new Path();
        private Bitmap logisticsBitmap;

        LoginHeaderView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            int logisticsId = getResources().getIdentifier("login_isometric", "drawable", getPackageName());
            if (logisticsId != 0) {
                logisticsBitmap = BitmapFactory.decodeResource(getResources(), logisticsId);
            }
            android.graphics.ColorMatrix softRedMatrix = new android.graphics.ColorMatrix(new float[] {
                .50f, .10f, .10f, 0f, 70f,
                .20f, .20f, .10f, 0f, 35f,
                .20f, .10f, .20f, 0f, 40f,
                0f, 0f, 0f, 1f, 0f
            });
            logisticsPaint.setColorFilter(new android.graphics.ColorMatrixColorFilter(softRedMatrix));
            logisticsPaint.setAlpha(230);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();
            canvas.drawColor(COLOR_ACCENT);

            paint.setColor(0x26FFFFFF);
            for (int row = 0; row < 9; row += 1) {
                for (int column = 0; column < 14; column += 1) {
                    float dx = column - 6.5f;
                    float dy = row - 4f;
                    if ((dx * dx) / 48f + (dy * dy) / 17f <= 1f && (row + column) % 3 != 0) {
                        canvas.drawCircle(width * 0.57f + column * dp(8), dp(42) + row * dp(7), dp(1), paint);
                    }
                }
            }

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(1));
            paint.setColor(0x55FFFFFF);
            path.reset();
            path.moveTo(width * .59f, height * .42f);
            path.cubicTo(width * .67f, height * .36f, width * .70f, height * .57f, width * .78f, height * .54f);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(width * .59f, height * .42f, dp(3), paint);
            canvas.drawCircle(width * .78f, height * .54f, dp(3), paint);

            drawLogisticsArt(canvas, width, height);
        }

        private void drawLogisticsArt(Canvas canvas, float width, float height) {
            if (logisticsBitmap == null || logisticsBitmap.isRecycled()) {
                return;
            }
            android.graphics.Rect source = new android.graphics.Rect(
                (int) (logisticsBitmap.getWidth() * .27f),
                (int) (logisticsBitmap.getHeight() * .04f),
                (int) (logisticsBitmap.getWidth() * .76f),
                (int) (logisticsBitmap.getHeight() * .94f)
            );
            RectF target = new RectF(
                width * .65f,
                height * .08f,
                width * .98f,
                height * .70f
            );
            canvas.drawBitmap(logisticsBitmap, source, target, logisticsPaint);
        }

        private Bitmap extractLogisticsForeground(Bitmap source, android.graphics.Rect sourceRect) {
            Bitmap crop = Bitmap.createBitmap(
                source,
                sourceRect.left,
                sourceRect.top,
                sourceRect.width(),
                sourceRect.height()
            ).copy(Bitmap.Config.ARGB_8888, true);
            int width = crop.getWidth();
            int height = crop.getHeight();
            int total = width * height;
            int[] pixels = new int[total];
            boolean[] background = new boolean[total];
            int[] queue = new int[total];
            int head = 0;
            int tail = 0;
            crop.getPixels(pixels, 0, width, 0, 0, width, height);

            for (int x = 0; x < width; x += 1) {
                tail = enqueueLoginBackground(pixels, background, queue, tail, x);
                tail = enqueueLoginBackground(pixels, background, queue, tail, (height - 1) * width + x);
            }

            while (head < tail) {
                int index = queue[head++];
                int x = index % width;
                int y = index / width;
                if (x > 0) {
                    tail = enqueueLoginBackground(pixels, background, queue, tail, index - 1);
                }
                if (x + 1 < width) {
                    tail = enqueueLoginBackground(pixels, background, queue, tail, index + 1);
                }
                if (y > 0) {
                    tail = enqueueLoginBackground(pixels, background, queue, tail, index - width);
                }
                if (y + 1 < height) {
                    tail = enqueueLoginBackground(pixels, background, queue, tail, index + width);
                }
            }

            for (int index = 0; index < total; index += 1) {
                if (background[index]) {
                    pixels[index] = Color.TRANSPARENT;
                }
            }
            crop.setPixels(pixels, 0, width, 0, 0, width, height);
            return crop;
        }

        private int enqueueLoginBackground(
            int[] pixels,
            boolean[] background,
            int[] queue,
            int tail,
            int index
        ) {
            if (index < 0 || index >= pixels.length || background[index]) {
                return tail;
            }
            int color = pixels[index];
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            int maximum = Math.max(red, Math.max(green, blue));
            int minimum = Math.min(red, Math.min(green, blue));
            if (red < 240 || green < 240 || blue < 240 || maximum - minimum > 14) {
                return tail;
            }
            background[index] = true;
            queue[tail] = index;
            return tail + 1;
        }

        private void drawLoginPin(Canvas canvas, float x, float y, float radius) {
            paint.setColor(0x38A4000A);
            canvas.drawOval(
                new RectF(x - radius * 1.4f, y + radius * 2.1f, x + radius * 1.4f, y + radius * 2.8f),
                paint
            );
            paint.setColor(0xFFFFFFFF);
            path.reset();
            path.moveTo(x, y + radius * 2.35f);
            path.cubicTo(
                x - radius * .45f, y + radius * 1.55f,
                x - radius, y + radius * 1.05f,
                x - radius, y
            );
            path.cubicTo(x - radius, y - radius * 1.35f, x + radius, y - radius * 1.35f, x + radius, y);
            path.cubicTo(
                x + radius, y + radius * 1.05f,
                x + radius * .45f, y + radius * 1.55f,
                x, y + radius * 2.35f
            );
            path.close();
            canvas.drawPath(path, paint);
            detailPaint.setColor(COLOR_ACCENT);
            canvas.drawCircle(x, y, radius * .36f, detailPaint);
        }

        private void drawLoginPackage(Canvas canvas, float x, float y, float size) {
            float middleX = x + size * .50f;
            float topY = y;
            float shoulderY = y + size * .22f;
            float middleY = y + size * .43f;
            float bottomY = y + size * .86f;

            paint.setColor(0xFFFFFFFF);
            path.reset();
            path.moveTo(middleX, topY);
            path.lineTo(x + size, shoulderY);
            path.lineTo(middleX, middleY);
            path.lineTo(x, shoulderY);
            path.close();
            canvas.drawPath(path, paint);

            paint.setColor(0xFFF1F3F5);
            path.reset();
            path.moveTo(x, shoulderY);
            path.lineTo(middleX, middleY);
            path.lineTo(middleX, bottomY);
            path.lineTo(x, bottomY - size * .22f);
            path.close();
            canvas.drawPath(path, paint);

            paint.setColor(0xFFDCE1E6);
            path.reset();
            path.moveTo(middleX, middleY);
            path.lineTo(x + size, shoulderY);
            path.lineTo(x + size, bottomY - size * .22f);
            path.lineTo(middleX, bottomY);
            path.close();
            canvas.drawPath(path, paint);

            detailPaint.setStyle(Paint.Style.STROKE);
            detailPaint.setStrokeWidth(Math.max(1f, dp(1)));
            detailPaint.setColor(0xFFCBD1D8);
            path.reset();
            path.moveTo(middleX, topY);
            path.lineTo(x + size, shoulderY);
            path.lineTo(x + size, bottomY - size * .22f);
            path.lineTo(middleX, bottomY);
            path.lineTo(x, bottomY - size * .22f);
            path.lineTo(x, shoulderY);
            path.close();
            canvas.drawPath(path, detailPaint);
            detailPaint.setStyle(Paint.Style.FILL);

            detailPaint.setColor(0xFFFF6974);
            path.reset();
            path.moveTo(middleX - size * .07f, topY + size * .03f);
            path.lineTo(middleX + size * .08f, topY + size * .07f);
            path.lineTo(middleX + size * .08f, bottomY - size * .03f);
            path.lineTo(middleX - size * .07f, bottomY);
            path.close();
            canvas.drawPath(path, detailPaint);
        }
    }

    private static class HomeWaveView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final Path accentPath = new Path();

        HomeWaveView(Context context) {
            super(context);
            paint.setColor(0xFFFFFFFF);
            paint.setStyle(Paint.Style.FILL);
            accentPaint.setColor(0x88FF9FA5);
            accentPaint.setStyle(Paint.Style.FILL);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float width = getWidth();
            float height = getHeight();

            accentPath.reset();
            accentPath.moveTo(0, height * .10f);
            accentPath.cubicTo(width * .18f, height * .30f, width * .38f, height * .46f, width * .58f, height * .31f);
            accentPath.cubicTo(width * .76f, height * .18f, width * .90f, height * .06f, width, height * .38f);
            accentPath.lineTo(width, height);
            accentPath.lineTo(0, height);
            accentPath.close();
            canvas.drawPath(accentPath, accentPaint);

            path.reset();
            path.moveTo(0, height * .72f);
            path.cubicTo(width * .05f, height * .30f, width * .20f, height * .29f, width * .34f, height * .38f);
            path.cubicTo(width * .52f, height * .50f, width * .69f, height * .24f, width * .82f, height * .27f);
            path.cubicTo(width * .91f, height * .28f, width * .97f, height * .48f, width, height * .62f);
            path.lineTo(width, height);
            path.lineTo(0, height);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    private void animateStepIn(View view) {
        if (view == null || view.getVisibility() != View.VISIBLE) {
            return;
        }
        view.animate().cancel();
        view.setAlpha(0f);
        view.setTranslationY(dp(10));
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(160)
            .start();
    }

    private GradientDrawable processPanelBackground(int radius) {
        GradientDrawable drawable = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[] {0xFFFFF7F7, 0xFFFFFBFB, 0xFFFFFFFF}
        );
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), 0xFFFECACA);
        return drawable;
    }

    private GradientDrawable round(int color, int radius, int strokeColor, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeWidth > 0) {
            drawable.setStroke(dp(strokeWidth), strokeColor);
        }
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}




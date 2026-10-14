package kz.atatv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Служебное меню для внука: режим запуска, разрешения, выход в систему. */
public class ServiceMenuActivity extends Activity {

    private interface TextSetter {
        void set(String value);
    }

    private Prefs prefs;
    private TextView status;
    private TextView journal;
    private Button modeButton;
    private Button channelButton;
    private Button labelButton;
    private Button packageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(32);
        column.setPadding(padding, padding, padding, padding);

        TextView header = new TextView(this);
        header.setText(R.string.service_menu_title);
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
        column.addView(header);

        status = new TextView(this);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        status.setPadding(0, dp(16), 0, dp(16));
        column.addView(status);

        Button start = addButton(column, getString(R.string.menu_start_tv));
        start.setOnClickListener(v -> {
            startActivity(new Intent(this, LauncherActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        });

        modeButton = addButton(column, "");
        modeButton.setOnClickListener(v -> {
            prefs.setMode(prefs.mode().next());
            refresh();
        });

        channelButton = addButton(column, "");
        channelButton.setOnClickListener(v -> editText(R.string.dialog_channel_id,
                prefs.channelId(), InputType.TYPE_CLASS_TEXT, prefs::setChannelId));

        labelButton = addButton(column, "");
        labelButton.setOnClickListener(v -> editText(R.string.dialog_tab_label,
                prefs.tabLabel(), InputType.TYPE_CLASS_TEXT, prefs::setTabLabel));

        packageButton = addButton(column, "");
        packageButton.setOnClickListener(v -> editText(R.string.dialog_package,
                prefs.targetPackage(), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI,
                prefs::setTargetPackage));

        addButton(column, getString(R.string.menu_make_default_home))
                .setOnClickListener(v -> openHomeSettings());
        addButton(column, getString(R.string.menu_accessibility))
                .setOnClickListener(v -> openSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addButton(column, getString(R.string.menu_overlay)).setOnClickListener(v -> openSettings(
                    new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()))));
        }
        addButton(column, getString(R.string.menu_all_apps))
                .setOnClickListener(v -> startActivity(new Intent(this, AppsActivity.class)));
        addButton(column, getString(R.string.menu_android_settings))
                .setOnClickListener(v -> openSettings(new Intent(Settings.ACTION_SETTINGS)));

        TextView journalHeader = new TextView(this);
        journalHeader.setText(R.string.journal_title);
        journalHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        journalHeader.setPadding(0, dp(24), 0, dp(8));
        column.addView(journalHeader);

        journal = new TextView(this);
        journal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        journal.setTypeface(Typeface.MONOSPACE);
        column.addView(journal);

        addButton(column, getString(R.string.menu_clear_journal)).setOnClickListener(v -> {
            DiagLog.clear(this);
            journal.setText(R.string.journal_empty);
        });

        ScrollView scroll = new ScrollView(this);
        scroll.addView(column);
        setContentView(scroll);
        start.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        String pkg = prefs.targetPackage();
        String version = TargetApp.installedVersion(this, pkg);
        String home = TargetApp.defaultHomePackage(this);
        StringBuilder text = new StringBuilder();
        text.append(version != null
                ? getString(R.string.status_target_installed, pkg, version)
                : getString(R.string.status_target_missing, pkg)).append('\n');
        text.append(getPackageName().equals(home)
                ? getString(R.string.status_home_ok)
                : getString(R.string.status_home_other, home == null ? getString(R.string.status_none) : home))
                .append('\n');
        text.append(TargetApp.isAccessibilityEnabled(this)
                ? getString(R.string.status_a11y_on)
                : getString(R.string.status_a11y_off)).append('\n');
        text.append(TvAccessibilityService.isRunning()
                ? getString(R.string.status_a11y_running)
                : getString(R.string.status_a11y_not_running)).append('\n');
        text.append(TargetApp.canDrawOverlays(this)
                ? getString(R.string.status_overlay_on)
                : getString(R.string.status_overlay_off)).append('\n');
        text.append(getString(R.string.status_version, ownVersion())).append('\n');
        text.append(getString(R.string.status_device, Build.MANUFACTURER, Build.MODEL,
                Build.VERSION.RELEASE, Build.VERSION.SDK_INT));
        status.setText(text);

        modeButton.setText(getString(R.string.menu_mode, getString(modeLabel(prefs.mode()))));
        String channel = prefs.channelId();
        channelButton.setText(getString(R.string.menu_channel_id,
                channel.isEmpty() ? getString(R.string.status_none) : channel));
        labelButton.setText(getString(R.string.menu_tab_label, prefs.tabLabel()));
        packageButton.setText(getString(R.string.menu_package, pkg));
        String log = DiagLog.read(this);
        journal.setText(log.isEmpty() ? getString(R.string.journal_empty) : log);
    }

    private static int modeLabel(LaunchMode mode) {
        switch (mode) {
            case OPEN_APP:
                return R.string.mode_open_app;
            case CHANNEL:
                return R.string.mode_channel;
            case TV_CHANNELS:
            default:
                return R.string.mode_tv_channels;
        }
    }

    /**
     * Экран выбора главного экрана есть не на всех прошивках. Запасной путь —
     * открыть свойства текущего лаунчера, чтобы сбросить «по умолчанию».
     */
    private void openHomeSettings() {
        if (TargetApp.tryStart(this, new Intent(Settings.ACTION_HOME_SETTINGS))) {
            return;
        }
        String home = TargetApp.defaultHomePackage(this);
        if (home != null && !home.equals(getPackageName())) {
            Toast.makeText(this, R.string.toast_clear_defaults, Toast.LENGTH_LONG).show();
            openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + home)));
            return;
        }
        // Умолчания нет — система сама предложит выбрать главный экран.
        Toast.makeText(this, R.string.toast_choose_home, Toast.LENGTH_LONG).show();
        TargetApp.tryStart(this, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void openSettings(Intent intent) {
        if (!TargetApp.tryStart(this, intent)) {
            Toast.makeText(this, R.string.toast_no_settings_screen, Toast.LENGTH_LONG).show();
        }
    }

    private void editText(int titleRes, String current, int inputType, TextSetter setter) {
        EditText input = new EditText(this);
        input.setInputType(inputType);
        input.setSingleLine(true);
        input.setText(current);
        input.setSelection(current.length());
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setView(input)
                .setPositiveButton(R.string.dialog_save, (d, w) -> {
                    setter.set(input.getText().toString());
                    TvAccessibilityService.onSettingsChanged();
                    refresh();
                })
                .setNegativeButton(R.string.dialog_cancel, null)
                .show();
    }

    private Button addButton(LinearLayout column, String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(8);
        column.addView(button, params);
        return button;
    }

    private String ownVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "?";
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

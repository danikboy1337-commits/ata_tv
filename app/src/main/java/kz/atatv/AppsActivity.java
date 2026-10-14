package kz.atatv;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Все приложения приставки, включая штатные главные экраны —
 * чтобы из ATA TV можно было попасть куда угодно.
 */
public class AppsActivity extends Activity {

    private static final class Entry {
        final String label;
        final Drawable icon;
        final Intent intent;

        Entry(String label, Drawable icon, Intent intent) {
            this.label = label;
            this.icon = icon;
            this.intent = intent;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadEntries();
        ListView list = new ListView(this);
        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return entries.size();
            }

            @Override
            public Object getItem(int position) {
                return entries.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView row = convertView instanceof TextView
                        ? (TextView) convertView : createRow();
                Entry entry = entries.get(position);
                row.setText(entry.label);
                entry.icon.setBounds(0, 0, dp(48), dp(48));
                row.setCompoundDrawablesRelative(entry.icon, null, null, null);
                return row;
            }
        });
        list.setOnItemClickListener((parent, view, position, id) -> {
            if (!TargetApp.tryStart(this, entries.get(position).intent)) {
                Toast.makeText(this, R.string.toast_cannot_open_app, Toast.LENGTH_LONG).show();
            }
        });
        setContentView(list);
        list.requestFocus();
    }

    private void loadEntries() {
        PackageManager pm = getPackageManager();
        Set<String> seen = new HashSet<>();
        String homeSuffix = getString(R.string.apps_home_suffix);
        addAll(pm, queryCategory(pm, Intent.CATEGORY_LEANBACK_LAUNCHER), "", seen);
        addAll(pm, queryCategory(pm, Intent.CATEGORY_LAUNCHER), "", seen);
        addAll(pm, queryCategory(pm, Intent.CATEGORY_HOME), homeSuffix, seen);
        Collator collator = Collator.getInstance();
        Collections.sort(entries, (a, b) -> collator.compare(a.label, b.label));
    }

    private static List<ResolveInfo> queryCategory(PackageManager pm, String category) {
        return pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(category), 0);
    }

    private void addAll(PackageManager pm, List<ResolveInfo> infos, String suffix, Set<String> seen) {
        for (ResolveInfo info : infos) {
            ActivityInfo activity = info.activityInfo;
            String key = activity.packageName + "/" + activity.name;
            if (activity.packageName.equals(getPackageName()) || !seen.add(key)) {
                continue;
            }
            Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setClassName(activity.packageName, activity.name)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            entries.add(new Entry(info.loadLabel(pm) + suffix, info.loadIcon(pm), intent));
        }
    }

    private TextView createRow() {
        TextView row = new TextView(this);
        row.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setCompoundDrawablePadding(dp(16));
        row.setPadding(dp(24), dp(12), dp(24), dp(12));
        return row;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

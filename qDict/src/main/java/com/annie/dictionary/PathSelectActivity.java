package com.annie.dictionary;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.annie.dictionary.frags.SettingFragment;
import com.annie.dictionary.utils.Utils;
import com.mmt.app.ActionBarListActivity;
import com.mmt.widget.QButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PathSelectActivity extends ActionBarListActivity implements OnItemClickListener {
    public static final int SELECT_TYPE_FOLDER = 0x000;
    public static final int SELECT_TYPE_FILE = 0x001;
    public static final String DEFAULT_PATH = "DEFAULT_PATH";
    public static final String SELECT_TYPE = "SELECT_TYPE";
    private final String TAG = "PathSelectActivity";
    public String dictDefaultPath;

    private String rootPath = Utils.getSDCardPath();
    private String curPath = null;

    private TextView mDictPath = null;
    private String mFilePath = null;
    private List<String> paths = null;

    private int mSelectType = 0;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        SharedPreferences share = getSharedPreferences(Utils.Def.APP_NAME, Context.MODE_PRIVATE);
        int mThemeIndex = share.getInt("prefs_key_theme", 0);
        Utils.onActivityCreateSetTheme(this, mThemeIndex, Utils.ThemeActivity.DIALOG);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();
        setContentView(R.layout.file_select);

        dictDefaultPath = Utils.getRootDictFolder(share);
        Intent intent = this.getIntent();
        Bundle bundle = intent.getExtras();

        if (null != bundle) {
            curPath = bundle.getString(DEFAULT_PATH);
            mSelectType = bundle.getInt(SELECT_TYPE);
        }

        if (null == curPath) {
            curPath = rootPath;
        }
        if (0 == mSelectType) {
            mSelectType = SELECT_TYPE_FOLDER;
        }

        mDictPath = findViewById(R.id.dictPath);
        QButton buttonConfirm = findViewById(R.id.buttonConfirm);
        getListView().setOnItemClickListener(this);
        buttonConfirm.setOnClickListener(v -> {
            String filePath;
            if (SELECT_TYPE_FILE == mSelectType) {
                if (null == mFilePath) {
                    finish();
                    return;
                }
                filePath = mFilePath;
            } else {
                filePath = curPath;
            }

            Intent intent1 = new Intent(SettingFragment.DATA_SOURCE_INTENT);
            Bundle bundle1 = new Bundle();
            bundle1.putString("filePath", filePath);
            intent1.putExtras(bundle1);
            sendBroadcast(intent1);
            finish();
        });

        QButton buttonCancle = findViewById(R.id.buttonCancle);
        buttonCancle.setOnClickListener(v -> finish());

        getFileDir(curPath);
    }

    private void getFileDir(String filePath) {
        File f = new File(filePath);

        if (!f.exists()) {
            f = new File(dictDefaultPath);
        }

        if (!f.canRead())
            return;

        mDictPath.setText(filePath);
        List<String> items = new ArrayList<>();
        paths = new ArrayList<>();

        File[] files = f.listFiles();

        if (!rootPath.startsWith(filePath)) {
            items.add("b1");
            paths.add(rootPath);

            if (null != f.getParent()) {
                items.add("b2");
                paths.add(f.getParent());
            }
        }

        if (f.exists() && files != null) {
            for (File file : files) {
                if (file.canRead()) {
                    items.add(file.getName());
                    paths.add(file.getPath());
                }
            }
        }

        setListAdapter(new FileManagerAdapter(this, items, paths));
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String filePath = paths.get(position);
        File file = new File(filePath);
        if (file.isDirectory()) {
            curPath = paths.get(position);
            getFileDir(curPath);
            mFilePath = null;
        } else if (SELECT_TYPE_FILE == mSelectType) {
            mDictPath.setText(filePath);
            mFilePath = filePath;
        }
    }

    // Adapter
    private class FileManagerAdapter extends BaseAdapter {
        private LayoutInflater mInflater;
        private Drawable mIcon1;
        private Drawable mIcon2;
        private Drawable mIcon3;
        private List<String> items;
        private List<String> filePaths;

        public FileManagerAdapter(Context context, List<String> it,
                                  List<String> pa) {
            mInflater = LayoutInflater.from(context);
            items = it;
            filePaths = pa;
            mIcon1 = Utils.getDrawable(PathSelectActivity.this, R.attr.icon_folder_back);
            mIcon2 = Utils.getDrawable(PathSelectActivity.this, R.attr.icon_folder);
            mIcon3 = Utils.getDrawable(PathSelectActivity.this, R.attr.icon_file);
        }

        public int getCount() {
            return items.size();
        }

        public Object getItem(int position) {
            return items.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.file_row, null);
                holder = new ViewHolder();
                holder.text = convertView.findViewById(R.id.text);
                holder.icon = convertView.findViewById(R.id.icon);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            File f = new File(filePaths.get(position));

            if (items.get(position).equals("b1")) {
                holder.text.setText("  /");
                holder.icon.setImageDrawable(mIcon1);
            } else if (items.get(position).equals("b2")) {
                holder.text.setText("  ..");
                holder.icon.setImageDrawable(mIcon1);
            } else {
                holder.text.setText(String.format("  %s", f.getName()));
                if (f.isDirectory()) {
                    holder.icon.setImageDrawable(mIcon2);
                } else {
                    holder.icon.setImageDrawable(mIcon3);
                }
            }
            return convertView;
        }

        private class ViewHolder {
            TextView text;
            ImageView icon;
        }
    }
}
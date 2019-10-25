package com.annie.dictionary.frags;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.ListFragment;

import com.annie.dictionary.MainActivity;
import com.annie.dictionary.QDictions;
import com.annie.dictionary.R;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;
import com.annie.dictionary.utils.Utils.RECV_UI;
import com.mmt.widget.DragSortListView;
import com.mmt.widget.draglistview.DragSortController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ListDictFragment extends ListFragment implements Def, OnItemClickListener {

    String mDictsPath;
    private ArrayList<String> mAllValueArrays;
    private List<String> mCheckValues;
    private QDictions mDictions;
    private SharedPreferences mSharedPreferences;
    private ArrayAdapter<String> mAdapter;
    private DragSortListView mList;
    private DragSortListView.DropListener onDrop = new DragSortListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (from != to) {
                String item = mAdapter.getItem(from);
                String itemValue = mAllValueArrays.get(from);
                mAdapter.remove(item);
                mAdapter.insert(item, to);
                mAllValueArrays.remove(itemValue);
                mAllValueArrays.add(to, itemValue);
                mList.moveCheckState(from, to);
                String valueSets = "";
                for (String string : mAllValueArrays) {
                    valueSets += string + ";";
                }
                mSharedPreferences.edit().putString(Def.PREF_INDEX_ALL, valueSets).apply();
                String checkValues = "";
                if (mCheckValues.contains(itemValue)) {
                    int count = mAdapter.getCount();
                    for (int i = 0; i < count; i++) {
                        if (mList.isItemChecked(i)) {
                            checkValues += mAllValueArrays.get(i) + ";";
                        }
                    }
                    mSharedPreferences.edit().putString(Def.PREF_INDEX_CHECKED, checkValues).apply();
                    String[] checks = checkValues.split(";");
                    mCheckValues = Arrays.asList(checks);
                }
            }
        }
    };
    private Switch mCheckBox;
    private TextView mEmptyDictTv, mDictCountTv;
    private RelativeLayout mEmptyDictLayout;

    public ListDictFragment() {

    }

    public static ListDictFragment newInstance(QDictions dictions) {
        ListDictFragment f = new ListDictFragment();
        f.mDictions = dictions;
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_list_dict, container, false);
        SharedPreferences shares = getActivity().getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
        mDictsPath = Utils.getRootDictFolder(shares) + Def.DICT_FOLDER;
        mCheckBox = root.findViewById(R.id.check_all);
        mEmptyDictLayout = root.findViewById(R.id.layout_empty);
        mEmptyDictTv = root.findViewById(R.id.tv_empty);
        mDictCountTv = root.findViewById(R.id.tv_dict_count);
        Button gotoFTPServer = root.findViewById(R.id.goto_ftp_server);
        gotoFTPServer.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClassName("com.m2t.ftpserver", "com.m2t.ftpserver.FTPServerActivity");
            intent.putExtra("stay_in_folder", mDictsPath);
            if (getActivity().getPackageManager().resolveActivity(intent, 0) != null) {
                getActivity().startActivity(intent);
            } else {
                try {
                    Intent ftpIntent = Utils.goToFTPServer();
                    getActivity().startActivity(ftpIntent);
                } catch (ActivityNotFoundException ex) {
                    Intent ftpIntent = Utils.goToFTPServerLink();
                    getActivity().startActivity(ftpIntent);
                }
            }

        });
        mCheckBox.setOnClickListener(v -> {
            mCheckBox.setChecked(mCheckBox.isChecked());
            checked(mCheckBox.isChecked());
            isCurrentCheckAll();
        });
        ImageButton backBtn = root.findViewById(R.id.action_back);
        backBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.ACTION_UPDATE_UI);
            intent.putExtra(MainActivity.ACTION_UPDATE_KEY, RECV_UI.SELECT_DICT);
            getActivity().sendBroadcast(intent);
        });
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSharedPreferences = getActivity().getSharedPreferences(APP_NAME, Context.MODE_PRIVATE);
        if (mDictions == null) {
            mDictions = new QDictions(getActivity().getApplicationContext());
        }
        mDictions.initDicts();
        getDictInfo();
        DragSortListView list = getListView();
        DragSortController mController = new MyDSController(list);
        list.setFloatViewManager(mController);
        list.setDragEnabled(true);
        list.setDropListener(onDrop);
        list.setOnItemClickListener(this);
        mCheckBox.setChecked(isCurrentCheckAll());
    }

    @Override
    public DragSortListView getListView() {
        return (DragSortListView) super.getListView();
    }

    private void getDictInfo() {
        String dictAlls;
        String selections;
        String emptySet = "";
        ArrayList<String> mAllInfoArrays = new ArrayList<>();
        selections = mSharedPreferences.getString(Def.PREF_INDEX_CHECKED, emptySet);
        dictAlls = mSharedPreferences.getString(Def.PREF_INDEX_ALL, emptySet);
        if (dictAlls.equals("")) {
            setEmptyDict(true, mDictsPath);
            return;
        }
        String[] dictAllArrays = dictAlls.split(";");
        String[] selectionArrays = selections.split(";");
        mCheckValues = Arrays.asList(selectionArrays);
        for (String dictIndex : dictAllArrays) {
            String dictPath = mDictsPath + "/" + dictIndex;
            String dictName = Utils.getFileInfoName(dictPath);
            if (null == dictName)
                continue;
            String bookIfoPath = dictPath + "/" + dictName + ".ifo";
            String bookName = mDictions.getBookName(bookIfoPath);
            mAllInfoArrays.add(bookName);
        }
        List<String> abcs = Arrays.asList(dictAllArrays);
        mAllValueArrays = new ArrayList<>(abcs);
        setEmptyDict(mAllInfoArrays.isEmpty(), mDictsPath);
        mAdapter = new ArrayAdapter<>(getActivity(), R.layout.listitem_check, R.id.text, mAllInfoArrays);
        setListAdapter(mAdapter);
        mList = getListView();
        for (String dictIndex : dictAllArrays) {
            String dictPath = mDictsPath + "/" + dictIndex;
            String dictName = Utils.getFileInfoName(dictPath);
            if (null != dictName) {
                if (mCheckValues.contains(dictIndex)) {
                    DragSortListView list = getListView();
                    int pos = mAllValueArrays.indexOf(dictIndex);
                    list.setItemChecked(pos, true);
                }
            }
        }
    }

    private void setEmptyDict(boolean isEmpty, String dictPath) {
        if (isEmpty) {
            mEmptyDictLayout.setVisibility(View.VISIBLE);
            mDictCountTv.setVisibility(View.GONE);
            getListView().setVisibility(View.GONE);
            mEmptyDictTv.setText(getResources().getString(R.string.dict_not_found, dictPath));
        } else {
            mEmptyDictLayout.setVisibility(View.GONE);
            getListView().setVisibility(View.VISIBLE);
            mDictCountTv.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        String checkValues = "";
        int count = mAdapter.getCount();
        for (int i = 0; i < count; i++) {
            if (mList.isItemChecked(i)) {
                checkValues += mAllValueArrays.get(i) + ";";
            }
        }
        mSharedPreferences.edit().putString(Def.PREF_INDEX_CHECKED, checkValues).apply();
        String[] checks = checkValues.split(";");
        mCheckValues = Arrays.asList(checks);
        reloadDict();
        mCheckBox.setChecked(isCurrentCheckAll());
    }

    private void reloadDict() {
        Intent intent = new Intent(MainActivity.ACTION_UPDATE_UI);
        intent.putExtra(MainActivity.ACTION_UPDATE_KEY, RECV_UI.RELOAD_DICT);
        getActivity().sendBroadcast(intent);
    }

    private void checked(boolean isall) {
        if (mAdapter == null)
            return;
        int count = mAdapter.getCount();
        String checkValues = "";
        if (isall) {
            for (int i = 0; i < count; i++) {
                mList.setItemChecked(i, true);
                checkValues += mAllValueArrays.get(i) + ";";
            }
            mSharedPreferences.edit().putString(Def.PREF_INDEX_CHECKED, checkValues).apply();
            String[] checks = checkValues.split(";");
            mCheckValues = Arrays.asList(checks);
        } else {
            for (int i = 0; i < count; i++) {
                mList.setItemChecked(i, false);
            }
            mSharedPreferences.edit().putString(Def.PREF_INDEX_CHECKED, checkValues).apply();
            mCheckValues = new ArrayList<>();
        }
        mAdapter.notifyDataSetChanged();
        reloadDict();
    }

    private boolean isCurrentCheckAll() {
        if (mAdapter == null)
            return false;
        int count = mAdapter.getCount();
        int checkCount = 0;
        for (int i = 0; i < count; i++) {
            if (mList.isItemChecked(i)) {
                checkCount++;
            }
        }
        mDictCountTv.setText(getResources().getQuantityString(R.plurals.items_count, checkCount, checkCount));
        return (checkCount == count);
    }

    private class MyDSController extends DragSortController {

        DragSortListView mDslv;

        public MyDSController(DragSortListView dslv) {
            super(dslv);
            setDragHandleId(R.id.text);
            mDslv = dslv;
        }

        @Override
        public View onCreateFloatView(int position) {
            View v = mAdapter.getView(position, null, mDslv);
            v.getBackground().setLevel(10000);
            return v;
        }

        @Override
        public void onDestroyFloatView(View floatView) {
        }

        @Override
        public int startDragPosition(MotionEvent ev) {
            int res = super.dragHandleHitPosition(ev);
            int width = mDslv.getWidth();

            if ((int) ev.getX() < width / 3) {
                return res;
            } else {
                return DragSortController.MISS;
            }
        }
    }
}

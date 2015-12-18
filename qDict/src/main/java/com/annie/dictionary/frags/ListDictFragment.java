
package com.annie.dictionary.frags;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.annie.dictionary.MainActivity;
import com.annie.dictionary.QDictions;
import com.annie.dictionary.R;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;
import com.annie.dictionary.utils.Utils.RECV_UI;
import com.mmt.widget.DragSortListView;
import com.mmt.widget.draglistview.DragSortController;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
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
import android.widget.TextView;

public class ListDictFragment extends ListFragment implements Def, OnItemClickListener {

    public ListDictFragment() {

    }

    public ListDictFragment(QDictions dictions) {
        mDictions = dictions;
    }

    public void setDictions(QDictions dictions) {
        mDictions = dictions;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_list_dict, container, false);
        mDictsPath = Utils.getRootFolder() + Def.DICT_FOLDER;
        mCheckBox = (CheckBox)root.findViewById(R.id.check_all);
        mEmptyDictLayout = (RelativeLayout)root.findViewById(R.id.layout_empty);
        mEmptyDictTv = (TextView)root.findViewById(R.id.tv_empty);
        mDictCountTv = (TextView)root.findViewById(R.id.tv_dict_count);
        mGotoFTPServer = (Button)root.findViewById(R.id.goto_ftp_server);
        mGotoFTPServer.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
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

            }
        });
        mCheckBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                mCheckBox.setChecked(mCheckBox.isChecked());
                checked(mCheckBox.isChecked());
                isCurrentCheckAll();
            }
        });
        mBackBtn = (ImageButton)root.findViewById(R.id.action_back);
        mBackBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.ACTION_UPDATE_UI);
                intent.putExtra(MainActivity.ACTION_UPDATE_KEY, RECV_UI.SELECT_DICT);
                getActivity().sendBroadcast(intent);
            }
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
        mController = new MyDSController(list);
        list.setFloatViewManager(mController);
        list.setDragEnabled(true);
        list.setDropListener(onDrop);
        list.setOnItemClickListener(this);
        mCheckBox.setChecked(isCurrentCheckAll());
    }

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

    @Override
    public DragSortListView getListView() {
        return (DragSortListView)super.getListView();
    }

    private void getDictInfo() {
        String dictAlls = "";
        String selections = "";
        String emptySet = "";
        mAllInfoArrays = new ArrayList<String>();
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
            if (null != dictName) {
                String bookIfoPath = "";
                bookIfoPath = dictPath + "/" + dictName + ".ifo";
                String bookName = mDictions.getBookName(bookIfoPath);
                mAllInfoArrays.add(bookName);
            }
        }
        List<String> abcs = Arrays.asList(dictAllArrays);
        mAllValueArrays = new ArrayList<String>(abcs);
        setEmptyDict(mAllInfoArrays.isEmpty(), mDictsPath);
        mAdapter = new ArrayAdapter<String>(getActivity(), R.layout.listitem_check, R.id.text, mAllInfoArrays);
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
            mAdapter.notifyDataSetChanged();
            reloadDict();
            return;
        } else {
            for (int i = 0; i < count; i++) {
                mList.setItemChecked(i, false);
            }
            mSharedPreferences.edit().putString(Def.PREF_INDEX_CHECKED, checkValues).apply();
            mCheckValues = new ArrayList<String>();
            mAdapter.notifyDataSetChanged();
            reloadDict();
            return;
        }
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

            if ((int)ev.getX() < width / 3) {
                return res;
            } else {
                return DragSortController.MISS;
            }
        }
    }

    String mDictsPath;

    private DragSortController mController;

    private ArrayList<String> mAllInfoArrays, mAllValueArrays;

    private List<String> mCheckValues;

    private QDictions mDictions;

    private SharedPreferences mSharedPreferences;

    private ArrayAdapter<String> mAdapter;

    private DragSortListView mList;

    private ImageButton mBackBtn;

    private CheckBox mCheckBox;

    private TextView mEmptyDictTv, mDictCountTv;

    private RelativeLayout mEmptyDictLayout;

    private Button mGotoFTPServer;
}

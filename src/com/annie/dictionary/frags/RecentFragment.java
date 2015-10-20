
package com.annie.dictionary.frags;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.annie.dictionary.MainActivity;
import com.annie.dictionary.R;
import com.annie.dictionary.utils.Utils;
import com.annie.dictionary.utils.Utils.Def;
import com.annie.dictionary.utils.Utils.NAVIG;
import com.annie.dictionary.utils.Utils.RECV_UI;
import com.annie.dictionary.utils.WordsFileUtils;
import com.mmt.widget.M2tToast;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class RecentFragment extends ListFragment {

    public RecentFragment() {
        // default constructor
    }

    @SuppressWarnings("deprecation")
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_recent, container, false);
        Bundle b = getArguments();
        if (b != null)
            mIsFavorite = b.getBoolean("qdict_is_favorite", false);
        activity = (ActionBarActivity)getActivity();
        mTvRecentTitle = (TextView)root.findViewById(R.id.tv_title);
        mTvCount = (TextView)root.findViewById(R.id.tv_count);
        mEmpty = (TextView)root.findViewById(R.id.tv_empty);
        mShares = activity.getSharedPreferences(Def.APP_NAME, Context.MODE_PRIVATE);
        mCheckedColor = getResources().getColor(R.color.mmt_grey_500);
        return root;
    }

    private void setState() {
        mTvRecentTitle.setText(mIsFavorite ? R.string.favorite_lable : R.string.recent_lable);
        mEmpty.setText(mIsFavorite ? R.string.favorites_no_word : R.string.recent_no_word);
        mHistoryFileUtils = new WordsFileUtils(mShares, mIsFavorite ? Def.TYPE_FAVORITEWORDS : Def.TYPE_RECENTWORDS);
        mWordsArrayList = mHistoryFileUtils.getArrayList();
        setTyeView();

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        setState();
        super.onResume();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.recent_menu, menu);
    };

    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete) {
            questionDeleteAllDlg(mIsFavorite);
        } else if (id == R.id.action_rotate) {
            mIsFavorite = !mIsFavorite;
            setState();
            Intent intent = new Intent(MainActivity.ACTION_UPDATE_UI);
            intent.putExtra(MainActivity.ACTION_UPDATE_KEY, RECV_UI.CHANGE_FRAG);
            intent.putExtra("receiver_frag_position", mIsFavorite ? NAVIG.FAVORITE : NAVIG.RECENT);
            activity.sendBroadcast(intent);
        }
        return true;

    };

    private void questionDeleteAllDlg(final boolean favorite) {
        AlertDialog.Builder alertDialogBuilder = null;
        if (Utils.hasHcAbove()) {
            alertDialogBuilder = new AlertDialog.Builder(activity, R.style.QDialog);
        } else {
            alertDialogBuilder = new AlertDialog.Builder(activity);
        }

        alertDialogBuilder
                .setMessage(favorite ? R.string.delete_all_favorite_summary : R.string.delete_all_recent_summary);
        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mHistoryFileUtils.removeAll();
                mHistoryFileUtils.save();
                mWordsArrayList.clear();
                mAdapter.notifyDataSetChanged();
                mEmpty.setVisibility(View.VISIBLE);
                dialog.dismiss();
            }
        });
        alertDialogBuilder.show();
    }

    ActionMode mActionMode;

    private void questionDeleteDlg(final ActionMode mode, final boolean favorite) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);
        alertDialogBuilder.setMessage(favorite ? R.string.delete_favorite_summary : R.string.delete_recent_summary);
        alertDialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                StringBuilder sb = new StringBuilder();
                Set<Integer> set = mAdapter.getCurrentCheckedPosition();
                List<String> keywords = new ArrayList<String>();
                for (Integer pos : set) {
                    String keyword = mAdapter.getItem(pos.intValue());
                    if (!TextUtils.isEmpty(keyword)) {
                        sb.append(keyword + "\n ");
                        keywords.add(keyword);
                    }
                }
                for (String key : keywords) {
                    mHistoryFileUtils.remove(key);
                }
                if (set.size() > 0) {
                    mWordsArrayList = mHistoryFileUtils.getArrayList();
                    checkUIInfor();
                    sb.append("\n has deleted.");
                }
                M2tToast.makeText(activity, sb.toString(), M2tToast.LENGTH_LONG).show();
                // clear selection android finish actionmode
                mAdapter.clearSelection();
                dialog.dismiss();
                mode.finish();
            }
        });
        alertDialogBuilder.show();
    }

    public void setTyeView() {
        mWordsArrayList = mHistoryFileUtils.getArrayList();
        mAdapter = new WordsListAdapter(mWordsArrayList);
        setListAdapter(mAdapter);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> listview, View item, int position, long id) {
                getListView().setItemChecked(position, true);
                if (mActionMode != null) {
                    return false;
                }
                mAdapter.setNewSelection(position, true);
                mActionMode = ((MainActivity)activity).getToolbar().startActionMode(mActionModeCallback);
                int count = mAdapter.getCheckCount();
                mActionMode.setTitle(getResources().getQuantityString(R.plurals.items_count, count, count));
                return true;
            }

        });
        checkUIInfor();
    }

    private void checkUIInfor() {
        if (!mWordsArrayList.isEmpty()) {
            mEmpty.setVisibility(View.GONE);
        } else {
            mEmpty.setVisibility(View.VISIBLE);
        }
        int count = mWordsArrayList.size();
        mTvCount.setText(getResources().getQuantityString(R.plurals.words_count, count, count));
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            // Inflate a menu resource providing context menu items
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.cabselection_menu, menu);
            return true;
        }

        // Called each time the action mode is shown. Always called after
        // onCreateActionMode, but
        // may be called multiple times if the mode is invalidated.
        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return
                          // false
                          // if
                          // nothing
                          // is
                          // done
        }

        // Called when the user selects a contextual menu item
        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    questionDeleteDlg(mode, mIsFavorite);
                    return true;
                default:
                    return false;
            }
        }

        // Called when the user exits the action mode
        @Override
        public void onDestroyActionMode(ActionMode mode) {
            mAdapter.clearSelection();
            mActionMode = null;
        }

    };

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mActionMode != null) {
            boolean check = !mAdapter.isPositionChecked(position);
            if (check)
                mAdapter.setNewSelection(position, check);
            else
                mAdapter.removeSelection(position);
            int count = mAdapter.getCheckCount();
            mActionMode.setTitle(getResources().getQuantityString(R.plurals.items_count, count, count));
            return;
        }
        String word = mAdapter.getItem(position).toString();
        Intent intent = new Intent(MainActivity.ACTION_UPDATE_UI);
        intent.putExtra(MainActivity.ACTION_UPDATE_KEY, RECV_UI.SEARCH_WORD);
        intent.putExtra("receiver_keyword", word);
        activity.sendBroadcast(intent);
    }

    public void onPause() {
        mHistoryFileUtils.save();
        mHistoryFileUtils = null;
        mWordsArrayList = null;
        super.onPause();
    }

    class WordsListAdapter extends ArrayAdapter<String> {
        List<String> mListWords;

        LayoutInflater inflater;

        WordsListAdapter(List<String> list) {
            super(activity, R.layout.simple_list_item_1, list);
            mListWords = list;
            inflater = activity.getLayoutInflater();
        }

        public List<String> getList() {
            return mListWords;
        }

        public void setNewSelection(int position, boolean value) {
            mSelection.put(position, value);
            notifyDataSetChanged();
        }

        public String getItem(int postion) {
            return mListWords.get(postion);
        }

        public boolean isPositionChecked(int position) {
            Boolean result = mSelection.get(position);
            return (result == null) ? false : result.booleanValue();
        }

        public Set<Integer> getCurrentCheckedPosition() {
            return mSelection.keySet();
        }

        public int getCheckCount() {
            return mSelection.size();
        }

        public void removeSelection(int position) {
            mSelection.remove(position);
            notifyDataSetChanged();
        }

        public void clearSelection() {
            mSelection = new HashMap<Integer, Boolean>();
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView text = (TextView)convertView;

            if (text == null) {
                text = (TextView)inflater.inflate(R.layout.simple_list_item_1, null);
            }
            if (mWordsArrayList == null || mWordsArrayList.size() == 0)
                return null;

            text.setText(mListWords.get(position));
            if (mSelection.get(position) != null) {
                text.setBackgroundColor(mCheckedColor);
            } else {
                text.setBackgroundColor(Color.TRANSPARENT);
            }
            return (text);
        }
    }

    private HashMap<Integer, Boolean> mSelection = new HashMap<Integer, Boolean>();

    int mCheckedColor;

    private SharedPreferences mShares;

    private WordsFileUtils mHistoryFileUtils;

    private ActionBarActivity activity;

    public static final int WORDS_RESULT_CODE = 1;

    public static final String WORDS_TYPE = "wordsType";

    private WordsListAdapter mAdapter;

    private TextView mEmpty, mTvRecentTitle, mTvCount;

    private boolean mIsFavorite = false;

    private List<String> mWordsArrayList = null;

}

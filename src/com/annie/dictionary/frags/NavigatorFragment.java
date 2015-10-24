
package com.annie.dictionary.frags;

import com.annie.dictionary.R;
import com.mmt.widget.M2tListView;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class NavigatorFragment extends Fragment implements OnItemClickListener {

    private M2tListView mListView1, mListView2;

    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationCallbacks mCallbacks;

    NavigatorAdapter mAdapter1, mAdapter2;

    String[] mFunctionLables;

    String[] mSystemLables;

    public NavigatorFragment() {
        // default constructor
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_navigatior, container, false);
        mListView1 = (M2tListView)root.findViewById(R.id.list1);
        mListView2 = (M2tListView)root.findViewById(R.id.list2);
        mListView1.setExpanded(true);
        mListView2.setExpanded(true);
        return root;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter1 = new NavigatorAdapter(getActivity());
        mFunctionLables = getResources().getStringArray(R.array.function_lables);
        mAdapter1.add(new NavigatorItem(mFunctionLables[0], R.drawable.ic_guide));
        mAdapter1.add(new NavigatorItem(mFunctionLables[1], R.drawable.ic_recent));
        mAdapter1.add(new NavigatorItem(mFunctionLables[2], R.drawable.ic_favorite));
        mListView1.setAdapter(mAdapter1);
        mSystemLables = getResources().getStringArray(R.array.system_lables);
        mAdapter2 = new NavigatorAdapter(getActivity());
        mAdapter2.add(new NavigatorItem(mSystemLables[0], R.drawable.ic_select_dict));
        mAdapter2.add(new NavigatorItem(mSystemLables[1], R.drawable.ic_setting));
        mAdapter2.add(new NavigatorItem(mSystemLables[2], R.drawable.ic_facebook));
        mListView2.setAdapter(mAdapter2);
        mListView1.setOnItemClickListener(this);
        mListView2.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.list1) {
            mCallbacks.onNavigationItemSelected(mFunctionLables[position], position);
        } else if (parent.getId() == R.id.list2) {
            mCallbacks.onNavigationItemSelected(mSystemLables[position], position + mAdapter1.getCount());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallbacks = (NavigationCallbacks)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NavigationCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    private class NavigatorItem {
        public String tag;

        public int iconRes;

        public NavigatorItem(String tag, int iconRes) {
            this.tag = tag;
            this.iconRes = iconRes;
        }
    }

    public static class NavigatorAdapter extends ArrayAdapter<NavigatorItem> {

        public NavigatorAdapter(Context context) {
            super(context, 0);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.navi_row, parent, false);
            }
            ImageView icon = (ImageView)convertView.findViewById(R.id.row_icon);
            icon.setImageResource(getItem(position).iconRes);
            TextView title = (TextView)convertView.findViewById(R.id.row_title);
            title.setText(getItem(position).tag);
            return convertView;
        }

    }

    /**
     * Callbacks interface that all activities using this fragment must
     * implement.
     */
    public static interface NavigationCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationItemSelected(String title, int position);
    }
}

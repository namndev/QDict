package com.annie.dictionary.frags;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.annie.dictionary.R;
import com.mmt.widget.M2tListView;

public class NavigatorFragment extends Fragment implements OnItemClickListener {

    private NavigatorAdapter mAdapter1, mAdapter2;
    private String[] mFunctionLabels, mSystemLabels;
    private M2tListView mListView1, mListView2;
    /**
     * A pointer to the current callbacks instance (the Activity).
     */
    private NavigationCallbacks mCallbacks;

    public NavigatorFragment() {
        // default constructor
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_navigatior, container, false);
        mListView1 = root.findViewById(R.id.list1);
        mListView2 = root.findViewById(R.id.list2);
        mListView1.setExpanded(true);
        mListView2.setExpanded(true);
        return root;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter1 = new NavigatorAdapter(getActivity());
        mFunctionLabels = getResources().getStringArray(R.array.function_lables);
        mAdapter1.add(new NavigatorItem(mFunctionLabels[0], R.drawable.ic_guide));
        mAdapter1.add(new NavigatorItem(mFunctionLabels[1], R.drawable.ic_recent));
        mAdapter1.add(new NavigatorItem(mFunctionLabels[2], R.drawable.ic_favorite));
        mListView1.setAdapter(mAdapter1);
        mSystemLabels = getResources().getStringArray(R.array.system_lables);
        mAdapter2 = new NavigatorAdapter(getActivity());
        mAdapter2.add(new NavigatorItem(mSystemLabels[0], R.drawable.ic_select_dict));
        mAdapter2.add(new NavigatorItem(mSystemLabels[1], R.drawable.ic_setting));
        mAdapter2.add(new NavigatorItem(mSystemLabels[2], R.drawable.ic_facebook));
        mListView2.setAdapter(mAdapter2);
        mListView1.setOnItemClickListener(this);
        mListView2.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() == R.id.list1) {
            mCallbacks.onNavigationItemSelected(mFunctionLabels[position], position);
        } else if (parent.getId() == R.id.list2) {
            mCallbacks.onNavigationItemSelected(mSystemLabels[position], position + mAdapter1.getCount());
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mCallbacks = (NavigationCallbacks) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Context must implement NavigationCallbacks.");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    /**
     * Callbacks interface that all activities using this fragment must
     * implement.
     */
    public interface NavigationCallbacks {
        /**
         * Called when an item in the navigation drawer is selected.
         */
        void onNavigationItemSelected(String title, int position);
    }

    public static class NavigatorAdapter extends ArrayAdapter<NavigatorItem> {

        public NavigatorAdapter(Context context) {
            super(context, 0);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.navi_row, parent, false);
            }
            ImageView icon = convertView.findViewById(R.id.row_icon);
            icon.setImageResource(getItem(position).iconRes);
            TextView title = convertView.findViewById(R.id.row_title);
            title.setText(getItem(position).tag);
            return convertView;
        }
    }

    private class NavigatorItem {
        public String tag;

        public int iconRes;

        public NavigatorItem(String tag, int iconRes) {
            this.tag = tag;
            this.iconRes = iconRes;
        }
    }
}

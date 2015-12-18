
package com.annie.dictionary.frags;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class BaseListFragment extends ListFragment {
    public boolean hasInitializedRootView = false;

    public View rootView;

    public View getPersistentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, int layout) {
        if (rootView == null) {
            // Inflate the layout for this fragment
            rootView = inflater.inflate(layout, null);
            Log.e("NAMND", "Create New View");
        } else {
            // Do not inflate the layout again.
            // The returned View of onCreateView will be added into the
            // fragment.
            // However it is not allowed to be added twice even if the parent is
            // same.
            // So we must remove rootView from the existing parent view group
            // (it will be added back).
            ((ViewGroup)rootView.getParent()).removeView(rootView);
            Log.e("NAMND", "No Create new View");
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!hasInitializedRootView) {
            hasInitializedRootView = true;
            // Do initial setup of UI
            Log.e("NAMND", "doInitialSetUpOfUI");
            doInitialSetUpOfUI();
        } else {
            Log.e("NAMND", "No doInitialSetUpOfUI");
        }
    }

    public abstract void doInitialSetUpOfUI();
}

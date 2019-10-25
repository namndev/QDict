package com.annie.dictionary.frags;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.ListFragment;

public abstract class BaseListFragment extends ListFragment {
    public boolean hasInitializedRootView = false;

    public View rootView;

    public View getPersistentView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState, int layout) {
        if (rootView == null) {
            // Inflate the layout for this fragment
            rootView = inflater.inflate(layout, null);
        } else {
            ((ViewGroup) rootView.getParent()).removeView(rootView);
        }
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!hasInitializedRootView) {
            hasInitializedRootView = true;
            // Do initial setup of UI
            doInitialSetUpOfUI();
        }
    }

    public abstract void doInitialSetUpOfUI();
}

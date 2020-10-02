/*
* Copyright (C) 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.example.android.basicgesturedetect;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;

import com.example.android.common.logger.LogFragment;

public class StackingCardFragment extends Fragment{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        LinearLayout stack = getActivity().findViewById(R.id.grpStack);
        LinearLayout detail = getActivity().findViewById(R.id.grpCardDetail);
        // BEGIN_INCLUDE(init_detector)stack
        WalletLayoutSolver solver = new WalletLayoutSolver();
        solver.setup(stack, detail, getActivity());
        // END_INCLUDE(init_detector)
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clear_action) {
            clearLog();
        }
        else if (item.getItemId() == R.id.hide_log_action) {
            View logView = getActivity().findViewById(R.id.log_fragment);
            logView.setVisibility(View.GONE);
        }
        return true;
    }

    public void clearLog() {
        LogFragment logFragment =  ((LogFragment) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment));
        logFragment.getLogView().setText("");
    }
}

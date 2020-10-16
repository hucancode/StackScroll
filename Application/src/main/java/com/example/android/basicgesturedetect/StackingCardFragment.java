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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.android.common.logger.LogFragment;

import java.util.Random;

public class StackingCardFragment extends Fragment
{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ViewGroup stack = getActivity().findViewById(R.id.grpStack);
        // BEGIN_INCLUDE(init_detector)stack
        WalletLayoutSolver solver = new WalletLayoutSolver(){
            @Override
            public void onBind(int index, View view)
            {
                super.onBind(index, view);
                ImageView graphic = (ImageView)view.findViewById(R.id.imgGraphic);
                final int cards[] = {R.drawable.credit_card_1,
                        R.drawable.credit_card_2,
                        R.drawable.credit_card_3,
                        R.drawable.credit_card_4 };
                Random r = new Random();
                graphic.setImageResource(cards[r.nextInt(cards.length)]);
            }

            @Override
            public void onEnterCard(int index, View view)
            {
            }

            @Override
            public void onLeaveCard(int index, View view)
            {
            }

            @Override
            public void onEnterDetailConfirmed(int index)
            {
            }
        };
        solver.setItemTemplate(R.layout.card_template)
                .setStackLayout(stack)
                .setParentActivity(getActivity())
                .populate(15);
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
